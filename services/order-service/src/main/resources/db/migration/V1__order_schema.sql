CREATE TABLE IF NOT EXISTS orders (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, order_no VARCHAR(40) NOT NULL, user_id BIGINT UNSIGNED NOT NULL,
  idempotency_key VARCHAR(100) NOT NULL, request_fingerprint CHAR(64) NOT NULL,
  shop_id BIGINT UNSIGNED NOT NULL, shop_name VARCHAR(100) NOT NULL, total_amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT', created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), cancelled_at DATETIME(3) NULL,
  payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID', refund_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REFUNDED',
  remark VARCHAR(500), cancel_reason VARCHAR(200), completed_at DATETIME(3),
  user_address_snapshot JSON, shop_address_snapshot JSON,
  cancel_idempotency_key VARCHAR(100), pay_idempotency_key VARCHAR(100), prepare_idempotency_key VARCHAR(100),
  deliver_idempotency_key VARCHAR(100), receipt_idempotency_key VARCHAR(100),
  PRIMARY KEY(id), UNIQUE KEY uk_order_no(order_no), UNIQUE KEY uk_order_idempotency(user_id,idempotency_key),
  UNIQUE KEY uk_order_cancel_idempotency(user_id,cancel_idempotency_key), KEY idx_order_user_created(user_id,created_at),
  KEY idx_order_shop_status(shop_id,status,created_at), CHECK(total_amount>0),
  CHECK(status IN ('PENDING_PAYMENT','PAID','PREPARING','DELIVERING','COMPLETED','CANCELLED'))
);
CREATE TABLE IF NOT EXISTS order_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, order_id BIGINT UNSIGNED NOT NULL, product_id BIGINT UNSIGNED NOT NULL,
  product_name VARCHAR(100) NOT NULL, unit_price DECIMAL(10,2) NOT NULL, quantity INT UNSIGNED NOT NULL,
  sku_id BIGINT UNSIGNED NULL, sku_name VARCHAR(100), image_url VARCHAR(500), PRIMARY KEY(id), KEY idx_order_item_order(order_id), CHECK(unit_price>0), CHECK(quantity>0)
);
CREATE TABLE IF NOT EXISTS payments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, order_id BIGINT UNSIGNED NOT NULL, payment_number VARCHAR(64) NOT NULL,
  amount DECIMAL(10,2) NOT NULL, status VARCHAR(20) NOT NULL, idempotency_key VARCHAR(100) NOT NULL,
  paid_at DATETIME(3), created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY(id),
  UNIQUE KEY uk_payment_order(order_id), UNIQUE KEY uk_payment_key(idempotency_key)
);
CREATE TABLE IF NOT EXISTS refunds (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, order_id BIGINT UNSIGNED NOT NULL, payment_id BIGINT UNSIGNED NULL,
  refund_number VARCHAR(64) NOT NULL, amount DECIMAL(10,2) NOT NULL, status VARCHAR(20) NOT NULL,
  idempotency_key VARCHAR(100), completed_at DATETIME(3), created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_refund_order(order_id), UNIQUE KEY uk_refund_key(idempotency_key)
);

-- Local read models replace cross-schema views. They are populated from the
-- owning services (or their event streams) and are read-only from the order
-- domain's point of view; order writes remain confined to order-owned tables.
CREATE TABLE IF NOT EXISTS users (id BIGINT UNSIGNED PRIMARY KEY, account VARCHAR(100), password_hash VARCHAR(255), nickname VARCHAR(100), phone VARCHAR(20), status VARCHAR(30), created_at DATETIME(3), updated_at DATETIME(3));
CREATE TABLE IF NOT EXISTS user_addresses (id BIGINT UNSIGNED PRIMARY KEY, user_id BIGINT UNSIGNED, recipient VARCHAR(100), phone VARCHAR(20), region VARCHAR(100), detail VARCHAR(200), is_default BOOLEAN, deleted_at DATETIME(3), created_at DATETIME(3), updated_at DATETIME(3));
CREATE TABLE IF NOT EXISTS merchants (id BIGINT UNSIGNED PRIMARY KEY, account VARCHAR(100), password_hash VARCHAR(255), name VARCHAR(100), phone VARCHAR(20), status VARCHAR(30), created_at DATETIME(3), updated_at DATETIME(3));
CREATE TABLE IF NOT EXISTS shops (id BIGINT UNSIGNED PRIMARY KEY, merchant_id BIGINT UNSIGNED, name VARCHAR(100), description VARCHAR(500), status VARCHAR(30), address_region VARCHAR(100), address_detail VARCHAR(200), address_phone VARCHAR(20), created_at DATETIME(3), updated_at DATETIME(3), deleted_at DATETIME(3));
CREATE TABLE IF NOT EXISTS product_categories (id BIGINT UNSIGNED PRIMARY KEY, shop_id BIGINT UNSIGNED, name VARCHAR(100), sort_order INT, created_at DATETIME(3), updated_at DATETIME(3), deleted_at DATETIME(3));
CREATE TABLE IF NOT EXISTS products (id BIGINT UNSIGNED PRIMARY KEY, shop_id BIGINT UNSIGNED, category_id BIGINT UNSIGNED, name VARCHAR(100), description VARCHAR(1000), price DECIMAL(10,2), stock INT UNSIGNED, status VARCHAR(20), version BIGINT UNSIGNED, image_id BIGINT UNSIGNED, created_at DATETIME(3), updated_at DATETIME(3));
CREATE TABLE IF NOT EXISTS product_skus (id BIGINT UNSIGNED PRIMARY KEY, product_id BIGINT UNSIGNED, name VARCHAR(100), price DECIMAL(10,2), stock INT UNSIGNED, status VARCHAR(20), version BIGINT UNSIGNED, created_at DATETIME(3), updated_at DATETIME(3));
CREATE TABLE IF NOT EXISTS images (id BIGINT UNSIGNED PRIMARY KEY, url VARCHAR(500), content_type VARCHAR(100), size BIGINT UNSIGNED, content LONGBLOB, created_at DATETIME(3));
CREATE TABLE IF NOT EXISTS cart_items (id BIGINT UNSIGNED PRIMARY KEY, user_id BIGINT UNSIGNED, product_id BIGINT UNSIGNED, sku_id BIGINT UNSIGNED, quantity INT UNSIGNED, created_at DATETIME(3), updated_at DATETIME(3));
