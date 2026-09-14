CREATE TABLE IF NOT EXISTS shops (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, merchant_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(100) NOT NULL, description VARCHAR(500), status VARCHAR(30) NOT NULL DEFAULT 'CLOSED',
  address_region VARCHAR(100), address_detail VARCHAR(200), address_phone VARCHAR(20),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted_at DATETIME(3), PRIMARY KEY(id), UNIQUE KEY uk_catalog_shop_name(merchant_id,name),
  KEY idx_catalog_shop_public(deleted_at,status,created_at)
);
CREATE TABLE IF NOT EXISTS product_categories (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, shop_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(100) NOT NULL, sort_order INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted_at DATETIME(3), PRIMARY KEY(id), UNIQUE KEY uk_catalog_category(shop_id,name),
  KEY idx_catalog_category_order(shop_id,deleted_at,sort_order,id)
);
CREATE TABLE IF NOT EXISTS products (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, shop_id BIGINT UNSIGNED NOT NULL,
  category_id BIGINT UNSIGNED NOT NULL, name VARCHAR(100) NOT NULL, description VARCHAR(1000),
  price DECIMAL(10,2) NOT NULL, stock INT UNSIGNED NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'OFF_SALE', version BIGINT UNSIGNED NOT NULL DEFAULT 1,
  image_id BIGINT UNSIGNED NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), KEY idx_catalog_product_shop(shop_id,status), KEY idx_catalog_product_category(category_id),
  CHECK(price>0), CHECK(stock>=0), CHECK(status IN ('ON_SALE','OFF_SALE'))
);
CREATE TABLE IF NOT EXISTS product_skus (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, product_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(100) NOT NULL, price DECIMAL(10,2) NOT NULL, stock INT UNSIGNED NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'OFF_SALE', version BIGINT UNSIGNED NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_catalog_sku(product_id,name), KEY idx_catalog_sku_product(product_id),
  CHECK(price>0), CHECK(stock>=0), CHECK(status IN ('ON_SALE','OFF_SALE'))
);

-- Local read models replace cross-schema views. They are refreshed from the owning
-- identity/media services and are never written as authoritative domain records.
CREATE TABLE IF NOT EXISTS merchants (
  id BIGINT UNSIGNED PRIMARY KEY, account VARCHAR(100), password_hash VARCHAR(255),
  name VARCHAR(100), phone VARCHAR(20), status VARCHAR(30),
  created_at DATETIME(3), updated_at DATETIME(3)
);
CREATE TABLE IF NOT EXISTS images (
  id BIGINT UNSIGNED PRIMARY KEY, url VARCHAR(500), content_type VARCHAR(100),
  size BIGINT UNSIGNED, content LONGBLOB, created_at DATETIME(3)
);
