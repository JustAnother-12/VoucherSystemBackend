
-- =========================================================
-- 1. Create sharded redemption counter table
-- =========================================================

CREATE TABLE voucher_batch_redemption_counters (
    batch_id BIGINT NOT NULL,
    shard_id SMALLINT NOT NULL,

    redeemed_count INT NOT NULL DEFAULT 0,
    shard_limit INT NOT NULL,

    CONSTRAINT pk_voucher_batch_redemption_counters
        PRIMARY KEY (batch_id, shard_id),

    CONSTRAINT fk_voucher_batch_redemption_counters_batch
        FOREIGN KEY (batch_id)
        REFERENCES voucher_batches(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_voucher_batch_counter_shard_id
        CHECK (shard_id >= 0 AND shard_id < 32),

    CONSTRAINT ck_voucher_batch_counter_quota
        CHECK (
            redeemed_count >= 0
            AND shard_limit >= 0
            AND redeemed_count <= shard_limit
        )
);


-- =========================================================
-- 2. Backfill existing redeemed_count into shards
-- =========================================================

WITH batch_shape AS (
    SELECT
        id,
        max_usage_count,
        redeemed_count AS old_redeemed_count,

        CASE
            WHEN max_usage_count > 0
                THEN LEAST(32, max_usage_count)
            ELSE 1
        END AS shard_count

    FROM voucher_batches
),

shard_limits AS (
    SELECT
        b.id AS batch_id,
        b.max_usage_count,
        b.old_redeemed_count,
        s.shard_id,

        (
            b.max_usage_count / b.shard_count
            +
            CASE
                WHEN s.shard_id <
                     (b.max_usage_count % b.shard_count)
                    THEN 1
                ELSE 0
            END
        ) AS shard_limit

    FROM batch_shape b

    CROSS JOIN LATERAL
        generate_series(0, b.shard_count - 1) AS s(shard_id)
),

allocated_shards AS (
    SELECT
        s.*,

        COALESCE(
            SUM(s.shard_limit) OVER (
                PARTITION BY s.batch_id
                ORDER BY s.shard_id
                ROWS BETWEEN UNBOUNDED PRECEDING
                     AND 1 PRECEDING
            ),
            0
        ) AS capacity_before

    FROM shard_limits s
)

INSERT INTO voucher_batch_redemption_counters (
    batch_id,
    shard_id,
    redeemed_count,
    shard_limit
)
SELECT
    batch_id,
    shard_id,
    LEAST(
        shard_limit,
        GREATEST(0, old_redeemed_count - capacity_before)
    ),
    shard_limit
FROM allocated_shards;


-- =========================================================
-- 3. Remove old centralized counter
-- =========================================================

ALTER TABLE voucher_batches
    DROP CONSTRAINT IF EXISTS
        ck_voucher_batches_redeemed_within_max_usage;

ALTER TABLE voucher_batches
    DROP COLUMN redeemed_count;


-- =========================================================
-- 4. Initialize shard rows automatically
-- =========================================================

CREATE OR REPLACE FUNCTION initialize_voucher_batch_counter_shards()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_max_usage INT;
    v_shard_count INT;
    v_base_limit INT;
    v_remainder INT;
BEGIN

    -- Do not reset counters of an already generated batch.
    IF TG_OP = 'UPDATE' AND OLD.status = 'GENERATED' THEN

        IF NEW.max_usage_count IS DISTINCT FROM OLD.max_usage_count THEN
            RAISE EXCEPTION
                'Cannot change max_usage_count after batch generation';
        END IF;

        RETURN NEW;
    END IF;

    v_max_usage := NEW.max_usage_count;

    v_shard_count :=
        CASE
            WHEN v_max_usage > 0
                THEN LEAST(32, v_max_usage)
            ELSE 1
        END;

    v_base_limit := v_max_usage / v_shard_count;
    v_remainder := v_max_usage % v_shard_count;

    DELETE FROM voucher_batch_redemption_counters
    WHERE batch_id = NEW.id;

    INSERT INTO voucher_batch_redemption_counters (
        batch_id,
        shard_id,
        redeemed_count,
        shard_limit
    )
    SELECT
        NEW.id,
        s.shard_id::SMALLINT,
        0,
        v_base_limit +
            CASE
                WHEN s.shard_id < v_remainder THEN 1
                ELSE 0
            END
    FROM generate_series(0, v_shard_count - 1) AS s(shard_id);

    RETURN NEW;
END;
$$;


CREATE TRIGGER trg_initialize_voucher_batch_counter_shards
AFTER INSERT OR UPDATE OF max_usage_count
ON voucher_batches
FOR EACH ROW
EXECUTE FUNCTION initialize_voucher_batch_counter_shards();
