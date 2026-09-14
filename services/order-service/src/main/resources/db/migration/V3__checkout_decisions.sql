CREATE TABLE checkout_decisions (
  id CHAR(36) NOT NULL PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  idempotency_key VARCHAR(100) NOT NULL,
  fingerprint CHAR(64) NOT NULL,
  state VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  error VARCHAR(50),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_checkout_user_key(user_id,idempotency_key),
  KEY idx_checkout_recovery(state,created_at)
);
