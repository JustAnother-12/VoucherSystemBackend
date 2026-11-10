-- VoucherSystem performance-test dataset.
-- WARNING: This script TRUNCATES application tables and resets identity sequences.
-- It refuses to run unless the current database is exactly vouchersystem_perf
-- and Flyway V1..V6 have already been applied successfully.
-- Run this only against the dedicated performance database.
DO $guard$
DECLARE
    applied_migrations INTEGER;
BEGIN
    IF current_database() <> 'vouchersystem_perf' THEN
        RAISE EXCEPTION
            'Refusing to seed database "%". Expected vouchersystem_perf.',
            current_database();
    END IF;

    IF to_regclass('public.flyway_schema_history') IS NULL THEN
        RAISE EXCEPTION
            'Flyway history table does not exist. Start the Spring Boot perf profile first.';
    END IF;

    SELECT COUNT(*)
      INTO applied_migrations
      FROM public.flyway_schema_history
    WHERE success = TRUE
      AND version IN (
          '1', '2', '3', '4', '5',
          '6', '7', '8', '9'
      );

    IF applied_migrations <> 9 THEN
        RAISE EXCEPTION
            'Expected successful Flyway migrations V1..V6; found %.',
            applied_migrations;
    END IF;
END
$guard$;

-- This list intentionally excludes flyway_schema_history.
TRUNCATE TABLE
    sessions,
    redemption_logs,
    fraud_logs,
    distribution_logs,
    customer_vouchers,
    vouchers,
    voucher_batches,
    batch_rules,
    rewards,
    conditions,
    campaigns,
    rate_limit_configs,
    black_lists,
    admin_accounts
RESTART IDENTITY CASCADE;

DO $seed$
DECLARE
    v_admin_id BIGINT;
    v_campaign_id BIGINT;
    v_condition_id BIGINT;
    v_reward_id BIGINT;
    v_rule_id BIGINT;
    v_batch_id BIGINT;
    v_voucher_id BIGINT;
    i INTEGER;
BEGIN
    -- One admin is enough to satisfy the campaign foreign key.
    -- The placeholder password is deliberately not a usable login credential.
    INSERT INTO admin_accounts (
        username,
        hashed_password,
        is_active,
        account_role
    ) VALUES (
        'perf-seed-admin',
        'NOT_A_LOGIN_PASSWORD_PERF_DATA_ONLY',
        TRUE,
        'SYSTEM'
    )
    RETURNING id INTO v_admin_id;

    INSERT INTO campaigns (
        campaign_name,
        start_time,
        end_time,
        status,
        admin_id
    ) VALUES (
        'PERF Campaign - Active',
        LOCALTIMESTAMP - INTERVAL '1 day',
        LOCALTIMESTAMP + INTERVAL '60 days',
        'ACTIVE',
        v_admin_id
    )
    RETURNING id INTO v_campaign_id;

    INSERT INTO conditions (
        condition_name,
        condition_type,
        status,
        condition_config_json
    ) VALUES (
        'PERF - Minimum cart amount 50',
        'MIN_CART_AMOUNT',
        'PUBLISHED',
        '{"minimumAmount": 50}'::jsonb
    )
    RETURNING id INTO v_condition_id;

    INSERT INTO rewards (
        reward_name,
        reward_type,
        status,
        reward_config_json
    ) VALUES (
        'PERF - 10 percent discount capped at 50',
        'PERCENTAGE',
        'PUBLISHED',
        '{"discountPercent": 10, "maxDiscountAmount": 50}'::jsonb
    )
    RETURNING id INTO v_reward_id;

    -- A BatchRule is unique to each VoucherBatch in this project model.
    -- Primary GENERIC voucher used by the standard 500 req/s test.
    INSERT INTO batch_rules (reward_id, condition_id)
    VALUES (v_reward_id, v_condition_id)
    RETURNING id INTO v_rule_id;

    INSERT INTO voucher_batches (
        campaign_id,
        rule_id,
        batch_type,
        code_config_json,
        duration_unit,
        duration_value,
        generated_count,
        max_usage_count,
        distribution_channels_json,
        status
    ) VALUES (
        v_campaign_id,
        v_rule_id,
        'GENERIC',
        '{}'::jsonb,
        'DAYS',
        60,
        1,
        500000,
        '[]'::jsonb,
        'GENERATED'
    )
    RETURNING id INTO v_batch_id;

    INSERT INTO vouchers (batch_id, campaign_id, code)
    VALUES (v_batch_id, v_campaign_id, 'PERF-GENERIC-001');

    -- Twenty separate GENERIC batches for distributed-batch experiments.
    FOR i IN 1..32 LOOP
        INSERT INTO batch_rules (reward_id, condition_id)
        VALUES (v_reward_id, v_condition_id)
        RETURNING id INTO v_rule_id;

        INSERT INTO voucher_batches (
            campaign_id,
            rule_id,
            batch_type,
            code_config_json,
            duration_unit,
            duration_value,
            generated_count,
            max_usage_count,
            distribution_channels_json,
            status
        ) VALUES (
            v_campaign_id,
            v_rule_id,
            'GENERIC',
            '{}'::jsonb,
            'DAYS',
            60,
            1,
            500000,
            '[]'::jsonb,
            'GENERATED'
        )
        RETURNING id INTO v_batch_id;

        INSERT INTO vouchers (batch_id, campaign_id, code)
        VALUES (
            v_batch_id,
            v_campaign_id,
            'PERF-MULTI-' || LPAD(i::TEXT, 3, '0')
        );
    END LOOP;

    -- =========================================================
	-- PRIMARY BULK VOUCHER
	-- =========================================================
	-- Each customer receives exactly one unique voucher.
	--
	-- Customer IDs:
	--   PERF-BULK-0
	--   PERF-BULK-1
	--   ...
	--   PERF-BULK-199999
	--
	-- Voucher codes follow the same naming convention.
	--
	-- Expected:
	--   200,000 vouchers
	--   200,000 customer_vouchers assignments
	--   1 voucher per customer

	INSERT INTO batch_rules (reward_id, condition_id)
	VALUES (v_reward_id, v_condition_id)
	RETURNING id INTO v_rule_id;

	INSERT INTO voucher_batches (
	    campaign_id,
	    rule_id,
	    batch_type,
	    code_config_json,
	    duration_unit,
	    duration_value,
	    generated_count,
	    max_usage_count,
	    distribution_channels_json,
	    status
	) VALUES (
	    v_campaign_id,
	    v_rule_id,
	    'BULK',
	    '{}'::jsonb,
	    'DAYS',
	    60,
	    200000,
	    200000,
	    '[]'::jsonb,
	    'GENERATED'
	)
	RETURNING id INTO v_batch_id;


	-- Generate 200,000 individual vouchers.
	-- Each generated voucher is returned with its ID and code.

	WITH inserted_vouchers AS (
	    INSERT INTO vouchers (
	        batch_id,
	        campaign_id,
	        code
	    )
	    SELECT
	        v_batch_id,
	        v_campaign_id,
	        'PERF-BULK-' || n::TEXT
	    FROM generate_series(0, 199999) AS s(n)
	    RETURNING id, code
	)

	-- Assign each voucher to exactly one customer.
	INSERT INTO customer_vouchers (
	    customer_id,
	    voucher_id,
	    status,
	    expired_at
	)
	SELECT
	    code,
	    id,
	    'AVAILABLE',
	    LOCALTIMESTAMP + INTERVAL '60 days'
	FROM inserted_vouchers;

    -- High limits for a benchmark run. The k6 container usually shares one
    -- source IP, so the IP limit must be high enough for the whole load test.
    INSERT INTO rate_limit_configs (
        rate_limit_config_name,
        endpoint_pattern,
        limit_scope,
        max_request,
        time_window_seconds,
        action_on_exceed,
        priority,
        is_active
    ) VALUES
    (
        'PERF - Customer limit (effectively disabled)',
        '/redemptions',
        'CUSTOMER_ID',
        1000000,
        60,
        'REJECT_429',
        30,
        TRUE
    ),
    (
        'PERF - Device limit (effectively disabled)',
        '/redemptions',
        'DEVICE_FINGERPRINT',
        1000000,
        60,
        'BLACKLIST',
        20,
        TRUE
    ),
    (
        'PERF - Shared load-generator IP limit',
        '/**',
        'IP',
        1000000,
        60,
        'REJECT_429',
        10,
        TRUE
    );
END
$seed$;

ANALYZE;

-- Quick verification after seeding.
SELECT 'campaigns' AS table_name, COUNT(*) AS row_count FROM campaigns
UNION ALL SELECT 'conditions', COUNT(*) FROM conditions
UNION ALL SELECT 'rewards', COUNT(*) FROM rewards
UNION ALL SELECT 'batch_rules', COUNT(*) FROM batch_rules
UNION ALL SELECT 'voucher_batches', COUNT(*) FROM voucher_batches
UNION ALL SELECT 'vouchers', COUNT(*) FROM vouchers
UNION ALL SELECT 'customer_vouchers', COUNT(*) FROM customer_vouchers
UNION ALL SELECT 'rate_limit_configs', COUNT(*) FROM rate_limit_configs;

SELECT
    vb.batch_type,
    COUNT(DISTINCT v.id) AS voucher_count,
    COUNT(DISTINCT cv.id) AS assigned_voucher_count,
    COUNT(DISTINCT cv.customer_id) AS assigned_customer_count
FROM voucher_batches vb
LEFT JOIN vouchers v
    ON v.batch_id = vb.id
LEFT JOIN customer_vouchers cv
    ON cv.voucher_id = v.id
WHERE vb.batch_type = 'BULK'
GROUP BY vb.id, vb.batch_type;


-- Check invalid assignments.
-- Expected result: 0

SELECT COUNT(*) AS invalid_assignments
FROM vouchers v
JOIN voucher_batches vb
    ON vb.id = v.batch_id
LEFT JOIN customer_vouchers cv
    ON cv.voucher_id = v.id
WHERE vb.batch_type = 'BULK'
  AND (
      cv.id IS NULL
      OR cv.customer_id <> v.code
  );


-- Preview some generated BULK vouchers.

SELECT
    v.code,
    v.id AS voucher_id,
    cv.customer_id,
    cv.status
FROM vouchers v
JOIN voucher_batches vb
    ON vb.id = v.batch_id
JOIN customer_vouchers cv
    ON cv.voucher_id = v.id
WHERE vb.batch_type = 'BULK'
ORDER BY v.id
LIMIT 10;
