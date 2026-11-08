ALTER TABLE voucher_batches
    ADD COLUMN max_usage_count INT NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_voucher_batches_max_usage_count CHECK (max_usage_count >= 0);

ALTER TABLE voucher_batches
    ALTER COLUMN max_usage_count DROP DEFAULT;

UPDATE voucher_batches
    SET max_usage_count = generated_count;

UPDATE voucher_batches b
    SET generated_count = (SELECT COUNT(*) FROM vouchers v WHERE v.batch_id = b.id)
    WHERE b.status = 'GENERATED';