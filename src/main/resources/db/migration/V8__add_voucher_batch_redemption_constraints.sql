ALTER TABLE voucher_batches
ADD CONSTRAINT ck_voucher_batches_redeemed_within_max_usage
CHECK (
    redeemed_count >= 0
    AND redeemed_count <= max_usage_count
);