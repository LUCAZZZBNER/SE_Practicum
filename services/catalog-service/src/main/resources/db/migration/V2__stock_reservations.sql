CREATE TABLE IF NOT EXISTS stock_reservations (
 reservation_id VARCHAR(100) NOT NULL, product_id BIGINT UNSIGNED NOT NULL, sku_id BIGINT UNSIGNED NOT NULL,
 quantity INT UNSIGNED NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), expires_at DATETIME(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3) + INTERVAL 15 MINUTE), released_at DATETIME(3),
 PRIMARY KEY(reservation_id,product_id,sku_id), KEY idx_stock_reservation_status(status)
);
