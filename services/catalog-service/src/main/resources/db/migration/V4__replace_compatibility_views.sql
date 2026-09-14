-- Replace legacy cross-schema projections with local, non-authoritative read models.
DROP VIEW IF EXISTS merchants;
DROP VIEW IF EXISTS images;
CREATE TABLE IF NOT EXISTS merchants (
  id BIGINT UNSIGNED PRIMARY KEY, account VARCHAR(100), password_hash VARCHAR(255),
  name VARCHAR(100), phone VARCHAR(20), status VARCHAR(30),
  created_at DATETIME(3), updated_at DATETIME(3)
);
CREATE TABLE IF NOT EXISTS images (
  id BIGINT UNSIGNED PRIMARY KEY, url VARCHAR(500), content_type VARCHAR(100),
  size BIGINT UNSIGNED, content LONGBLOB, created_at DATETIME(3)
);
