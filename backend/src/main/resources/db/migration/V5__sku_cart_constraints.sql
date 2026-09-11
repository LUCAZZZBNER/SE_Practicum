ALTER TABLE cart_items DROP INDEX uk_cart_items_user_product;
ALTER TABLE cart_items ADD UNIQUE KEY uk_cart_items_user_sku(user_id, sku_id);
ALTER TABLE cart_items ADD CONSTRAINT fk_cart_items_sku FOREIGN KEY(sku_id) REFERENCES product_skus(id);
ALTER TABLE products ADD CONSTRAINT fk_products_image FOREIGN KEY(image_id) REFERENCES images(id);
