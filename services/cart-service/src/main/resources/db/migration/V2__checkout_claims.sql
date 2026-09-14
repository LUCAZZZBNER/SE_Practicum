ALTER TABLE cart_items ADD COLUMN checkout_id VARCHAR(100) NULL, ADD COLUMN checkout_claimed_at DATETIME(3) NULL;
CREATE INDEX idx_cart_checkout_claim ON cart_items(checkout_id);
