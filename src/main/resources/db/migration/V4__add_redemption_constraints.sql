-- Only one SUCCESS redemption for each customer
CREATE UNIQUE INDEX ux_redemption_logs_success_voucher_customer
    ON redemption_logs (voucher_id, customer_id)
    WHERE status = 'SUCCESS';

-- Composite index to help query redemption log by customer id.
CREATE INDEX ix_redemption_logs_customer_created
    ON redemption_logs (customer_id, created_at DESC);