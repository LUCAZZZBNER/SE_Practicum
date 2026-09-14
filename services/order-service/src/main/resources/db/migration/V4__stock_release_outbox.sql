CREATE TABLE order_stock_release_outbox (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reservation_id VARCHAR(100) NOT NULL,
  payload JSON NOT NULL,
  attempts INT UNSIGNED NOT NULL DEFAULT 0,
  available_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  completed_at DATETIME(3),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_release_reservation(reservation_id), KEY idx_release_ready(completed_at,available_at,id)
);
