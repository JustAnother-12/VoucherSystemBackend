CREATE INDEX ix_black_lists_lookup
    ON black_lists (
        blacklist_type,
        target_value
    )
    WHERE is_active = TRUE;

CREATE UNIQUE INDEX ux_customer_vouchers_voucher_customer
    ON customer_vouchers (voucher_id, customer_id);