ALTER TABLE orders ADD COLUMN cancel_idempotency_key VARCHAR(100) NULL;
ALTER TABLE orders ADD UNIQUE KEY uk_orders_cancel_idempotency_key(user_id, cancel_idempotency_key);
