ALTER TABLE refunds ADD COLUMN payment_id BIGINT UNSIGNED NULL;
ALTER TABLE refunds ADD CONSTRAINT fk_refunds_payment FOREIGN KEY(payment_id) REFERENCES payments(id);
