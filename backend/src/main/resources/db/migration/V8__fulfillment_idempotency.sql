ALTER TABLE orders ADD COLUMN pay_idempotency_key VARCHAR(100) NULL;
ALTER TABLE orders ADD COLUMN prepare_idempotency_key VARCHAR(100) NULL;
ALTER TABLE orders ADD COLUMN deliver_idempotency_key VARCHAR(100) NULL;
ALTER TABLE orders ADD COLUMN receipt_idempotency_key VARCHAR(100) NULL;
