-- Run after all services have completed Flyway migrations. The statements are
-- repeatable and preserve identifiers so JWTs, cart references, and order URLs
-- remain valid during the gateway cutover.
SET FOREIGN_KEY_CHECKS = 0;

INSERT IGNORE INTO delivery_identity.users
  (id,account,password_hash,nickname,phone,status,created_at,updated_at)
SELECT id,account,password_hash,nickname,phone,status,created_at,updated_at
FROM delivery_dev.users;
INSERT IGNORE INTO delivery_identity.merchants
  (id,account,password_hash,name,phone,status,created_at,updated_at)
SELECT id,account,password_hash,name,phone,status,created_at,updated_at
FROM delivery_dev.merchants;
INSERT IGNORE INTO delivery_identity.user_addresses
  (id,user_id,recipient,phone,region,detail,is_default,deleted_at,created_at,updated_at)
SELECT id,user_id,recipient,phone,region,detail,is_default,deleted_at,created_at,updated_at
FROM delivery_dev.user_addresses;

INSERT IGNORE INTO delivery_catalog.shops
  (id,merchant_id,name,description,status,address_region,address_detail,address_phone,created_at,updated_at,deleted_at)
SELECT id,merchant_id,name,description,status,address_region,address_detail,address_phone,created_at,updated_at,deleted_at
FROM delivery_dev.shops;
INSERT IGNORE INTO delivery_catalog.product_categories
  (id,shop_id,name,sort_order,created_at,updated_at,deleted_at)
SELECT id,shop_id,name,sort_order,created_at,updated_at,deleted_at
FROM delivery_dev.product_categories;
INSERT IGNORE INTO delivery_catalog.products
  (id,shop_id,category_id,name,description,price,stock,status,version,image_id,created_at,updated_at)
SELECT id,shop_id,category_id,name,description,price,stock,status,version,image_id,created_at,updated_at
FROM delivery_dev.products;
INSERT IGNORE INTO delivery_catalog.product_skus
  (id,product_id,name,price,stock,status,version,created_at,updated_at)
SELECT id,product_id,name,price,stock,status,version,created_at,updated_at
FROM delivery_dev.product_skus;

INSERT IGNORE INTO delivery_media.images
  (id,url,content_type,size,content,created_at)
SELECT id,url,content_type,size,content,created_at
FROM delivery_dev.images;

-- Populate non-authoritative read models used by cart, catalog, and order.
-- These tables are local to each service; they are never written across schemas.
INSERT IGNORE INTO delivery_catalog.merchants
  (id,account,password_hash,name,phone,status,created_at,updated_at)
SELECT id,account,password_hash,name,phone,status,created_at,updated_at
FROM delivery_identity.merchants;
INSERT IGNORE INTO delivery_catalog.images
  (id,url,content_type,size,content,created_at)
SELECT id,url,content_type,size,content,created_at
FROM delivery_media.images;

INSERT IGNORE INTO delivery_cart.users
  (id,account,password_hash,nickname,phone,status,created_at,updated_at)
SELECT id,account,password_hash,nickname,phone,status,created_at,updated_at
FROM delivery_identity.users;
INSERT IGNORE INTO delivery_cart.user_addresses
  (id,user_id,recipient,phone,region,detail,is_default,deleted_at,created_at,updated_at)
SELECT id,user_id,recipient,phone,region,detail,is_default,deleted_at,created_at,updated_at
FROM delivery_identity.user_addresses;
INSERT IGNORE INTO delivery_cart.merchants
  (id,account,password_hash,name,phone,status,created_at,updated_at)
SELECT id,account,password_hash,name,phone,status,created_at,updated_at
FROM delivery_identity.merchants;
INSERT IGNORE INTO delivery_cart.shops
  (id,merchant_id,name,description,status,address_region,address_detail,address_phone,created_at,updated_at,deleted_at)
SELECT id,merchant_id,name,description,status,address_region,address_detail,address_phone,created_at,updated_at,deleted_at
FROM delivery_catalog.shops;
INSERT IGNORE INTO delivery_cart.product_categories
  (id,shop_id,name,sort_order,created_at,updated_at,deleted_at)
SELECT id,shop_id,name,sort_order,created_at,updated_at,deleted_at
FROM delivery_catalog.product_categories;
INSERT IGNORE INTO delivery_cart.products
  (id,shop_id,category_id,name,description,price,stock,status,version,image_id,created_at,updated_at)
SELECT id,shop_id,category_id,name,description,price,stock,status,version,image_id,created_at,updated_at
FROM delivery_catalog.products;
INSERT IGNORE INTO delivery_cart.product_skus
  (id,product_id,name,price,stock,status,version,created_at,updated_at)
SELECT id,product_id,name,price,stock,status,version,created_at,updated_at
FROM delivery_catalog.product_skus;
INSERT IGNORE INTO delivery_cart.images
  (id,url,content_type,size,content,created_at)
SELECT id,url,content_type,size,content,created_at
FROM delivery_media.images;

INSERT IGNORE INTO delivery_order.users
  (id,account,password_hash,nickname,phone,status,created_at,updated_at)
SELECT id,account,password_hash,nickname,phone,status,created_at,updated_at
FROM delivery_identity.users;
INSERT IGNORE INTO delivery_order.user_addresses
  (id,user_id,recipient,phone,region,detail,is_default,deleted_at,created_at,updated_at)
SELECT id,user_id,recipient,phone,region,detail,is_default,deleted_at,created_at,updated_at
FROM delivery_identity.user_addresses;
INSERT IGNORE INTO delivery_order.merchants
  (id,account,password_hash,name,phone,status,created_at,updated_at)
SELECT id,account,password_hash,name,phone,status,created_at,updated_at
FROM delivery_identity.merchants;
INSERT IGNORE INTO delivery_order.shops
  (id,merchant_id,name,description,status,address_region,address_detail,address_phone,created_at,updated_at,deleted_at)
SELECT id,merchant_id,name,description,status,address_region,address_detail,address_phone,created_at,updated_at,deleted_at
FROM delivery_catalog.shops;
INSERT IGNORE INTO delivery_order.product_categories
  (id,shop_id,name,sort_order,created_at,updated_at,deleted_at)
SELECT id,shop_id,name,sort_order,created_at,updated_at,deleted_at
FROM delivery_catalog.product_categories;
INSERT IGNORE INTO delivery_order.products
  (id,shop_id,category_id,name,description,price,stock,status,version,image_id,created_at,updated_at)
SELECT id,shop_id,category_id,name,description,price,stock,status,version,image_id,created_at,updated_at
FROM delivery_catalog.products;
INSERT IGNORE INTO delivery_order.product_skus
  (id,product_id,name,price,stock,status,version,created_at,updated_at)
SELECT id,product_id,name,price,stock,status,version,created_at,updated_at
FROM delivery_catalog.product_skus;
INSERT IGNORE INTO delivery_order.images
  (id,url,content_type,size,content,created_at)
SELECT id,url,content_type,size,content,created_at
FROM delivery_media.images;

INSERT IGNORE INTO delivery_cart.cart_items
  (id,user_id,product_id,sku_id,quantity,created_at,updated_at)
SELECT id,user_id,product_id,sku_id,quantity,created_at,updated_at
FROM delivery_dev.cart_items;

INSERT IGNORE INTO delivery_order.orders
  (id,order_no,user_id,idempotency_key,request_fingerprint,shop_id,shop_name,total_amount,status,created_at,
   updated_at,cancelled_at,payment_status,refund_status,remark,cancel_reason,completed_at,user_address_snapshot,
   shop_address_snapshot,cancel_idempotency_key,pay_idempotency_key,prepare_idempotency_key,deliver_idempotency_key,receipt_idempotency_key)
SELECT id,order_no,user_id,idempotency_key,request_fingerprint,shop_id,shop_name,total_amount,status,created_at,
       updated_at,cancelled_at,payment_status,refund_status,remark,cancel_reason,completed_at,user_address_snapshot,
       shop_address_snapshot,cancel_idempotency_key,pay_idempotency_key,prepare_idempotency_key,deliver_idempotency_key,receipt_idempotency_key
FROM delivery_dev.orders;
INSERT IGNORE INTO delivery_order.order_items
  (id,order_id,product_id,product_name,unit_price,quantity,sku_id,sku_name,image_url)
SELECT id,order_id,product_id,product_name,unit_price,quantity,sku_id,sku_name,image_url
FROM delivery_dev.order_items;
INSERT IGNORE INTO delivery_order.payments
  (id,order_id,payment_number,amount,status,idempotency_key,paid_at,created_at)
SELECT id,order_id,payment_number,amount,status,idempotency_key,paid_at,created_at
FROM delivery_dev.payments;
INSERT IGNORE INTO delivery_order.refunds
  (id,order_id,payment_id,refund_number,amount,status,idempotency_key,completed_at,created_at)
SELECT id,order_id,payment_id,refund_number,amount,status,idempotency_key,completed_at,created_at
FROM delivery_dev.refunds;

SET FOREIGN_KEY_CHECKS = 1;
