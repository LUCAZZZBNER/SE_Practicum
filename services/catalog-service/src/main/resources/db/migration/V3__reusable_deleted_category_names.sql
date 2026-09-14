ALTER TABLE product_categories
  ADD COLUMN active_name VARCHAR(100)
    GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN name ELSE NULL END) STORED;

ALTER TABLE product_categories
  DROP INDEX uk_catalog_category,
  ADD UNIQUE KEY uk_catalog_category_active (shop_id, active_name);
