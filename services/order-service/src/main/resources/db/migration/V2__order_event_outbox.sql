CREATE TABLE IF NOT EXISTS order_event_outbox (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  event_id CHAR(36) NOT NULL,
  order_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  reservation_id VARCHAR(100) NOT NULL,
  payload JSON NOT NULL,
  attempts INT UNSIGNED NOT NULL DEFAULT 0,
  available_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  published_at DATETIME(3),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_order_event_event_id(event_id),
  KEY idx_order_event_ready(published_at, available_at, id)
);
