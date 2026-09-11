ALTER TABLE refunds ADD COLUMN idempotency_key VARCHAR(100) NULL;
ALTER TABLE refunds ADD UNIQUE KEY uk_refunds_idempotency_key(idempotency_key);
