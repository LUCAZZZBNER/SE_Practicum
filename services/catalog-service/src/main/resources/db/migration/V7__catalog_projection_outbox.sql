CREATE TABLE IF NOT EXISTS catalog_projection_outbox (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  event_id CHAR(36) NOT NULL,
  aggregate_id BIGINT UNSIGNED NOT NULL,
  payload JSON NOT NULL,
  available_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  published_at DATETIME(3) NULL,
  attempts INT NOT NULL DEFAULT 0,
  claimed_by VARCHAR(64) NULL,
  claim_until DATETIME(3) NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_catalog_projection_event(event_id),
  KEY idx_catalog_projection_ready(published_at, available_at, id)
);
