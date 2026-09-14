CREATE TABLE IF NOT EXISTS cart_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL, sku_id BIGINT UNSIGNED NULL, quantity INT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_cart_user_sku(user_id,sku_id), KEY idx_cart_user(user_id), CHECK(quantity>0)
);
-- Local read models replace cross-schema views. The owning service publishes
-- updates; cart never treats these projections as authoritative write tables.
CREATE TABLE IF NOT EXISTS users (id BIGINT UNSIGNED PRIMARY KEY, account VARCHAR(100), password_hash VARCHAR(255), nickname VARCHAR(100), phone VARCHAR(20), status VARCHAR(30), created_at DATETIME(3), updated_at DATETIME(3));
CREATE TABLE IF NOT EXISTS user_addresses (id BIGINT UNSIGNED PRIMARY KEY, user_id BIGINT UNSIGNED, recipient VARCHAR(100), phone VARCHAR(20), region VARCHAR(100), detail VARCHAR(200), is_default BOOLEAN, deleted_at DATETIME(3), created_at DATETIME(3), updated_at DATETIME(3));
CREATE TABLE IF NOT EXISTS merchants (id BIGINT UNSIGNED PRIMARY KEY, account VARCHAR(100), password_hash VARCHAR(255), name VARCHAR(100), phone VARCHAR(20), status VARCHAR(30), created_at DATETIME(3), updated_at DATETIME(3));
CREATE TABLE IF NOT EXISTS shops (id BIGINT UNSIGNED PRIMARY KEY, merchant_id BIGINT UNSIGNED, name VARCHAR(100), description VARCHAR(500), status VARCHAR(30), address_region VARCHAR(100), address_detail VARCHAR(200), address_phone VARCHAR(20), created_at DATETIME(3), updated_at DATETIME(3), deleted_at DATETIME(3));
CREATE TABLE IF NOT EXISTS product_categories (id BIGINT UNSIGNED PRIMARY KEY, shop_id BIGINT UNSIGNED, name VARCHAR(100), sort_order INT, created_at DATETIME(3), updated_at DATETIME(3), deleted_at DATETIME(3));
CREATE TABLE IF NOT EXISTS products (id BIGINT UNSIGNED PRIMARY KEY, shop_id BIGINT UNSIGNED, category_id BIGINT UNSIGNED, name VARCHAR(100), description VARCHAR(1000), price DECIMAL(10,2), stock INT UNSIGNED, status VARCHAR(20), version BIGINT UNSIGNED, image_id BIGINT UNSIGNED, created_at DATETIME(3), updated_at DATETIME(3));
CREATE TABLE IF NOT EXISTS product_skus (id BIGINT UNSIGNED PRIMARY KEY, product_id BIGINT UNSIGNED, name VARCHAR(100), price DECIMAL(10,2), stock INT UNSIGNED, status VARCHAR(20), version BIGINT UNSIGNED, created_at DATETIME(3), updated_at DATETIME(3));
CREATE TABLE IF NOT EXISTS images (id BIGINT UNSIGNED PRIMARY KEY, url VARCHAR(500), content_type VARCHAR(100), size BIGINT UNSIGNED, content LONGBLOB, created_at DATETIME(3));
