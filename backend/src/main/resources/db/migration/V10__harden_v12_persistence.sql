-- Harden the V1.2 persistence contract without changing the published V1-V9 migrations.

-- 1. Every legacy product needs a SKU that can become the single source of truth
-- for price, stock, status and optimistic-lock version.
INSERT INTO `product_skus` (
    `product_id`, `name`, `price`, `stock`, `status`, `version`
)
SELECT
    p.`id`, '默认规格', p.`price`, p.`stock`, p.`status`, p.`version`
FROM `products` p
WHERE NOT EXISTS (
    SELECT 1
    FROM `product_skus` s
    WHERE s.`product_id` = p.`id`
);

-- 2. Backfill legacy cart rows. Prefer the named default SKU, then use the
-- lowest SKU id of the same product as a deterministic fallback.
UPDATE `cart_items` c
JOIN `product_skus` s
    ON s.`product_id` = c.`product_id`
   AND s.`name` = '默认规格'
SET
    c.`sku_id` = s.`id`,
    c.`sku_version` = s.`version`
WHERE c.`sku_id` IS NULL;

UPDATE `cart_items` c
JOIN (
    SELECT `product_id`, MIN(`id`) AS `sku_id`
    FROM `product_skus`
    GROUP BY `product_id`
) chosen
    ON chosen.`product_id` = c.`product_id`
JOIN `product_skus` s
    ON s.`id` = chosen.`sku_id`
SET
    c.`sku_id` = s.`id`,
    c.`sku_version` = s.`version`
WHERE c.`sku_id` IS NULL;

UPDATE `cart_items` c
JOIN `product_skus` s
    ON s.`id` = c.`sku_id`
SET c.`sku_version` = s.`version`
WHERE c.`sku_version` IS NULL;

ALTER TABLE `cart_items`
    MODIFY COLUMN `sku_id` BIGINT UNSIGNED NOT NULL,
    MODIFY COLUMN `sku_version` BIGINT UNSIGNED NOT NULL;

-- 3. Keep the oldest active default address when legacy data contains more
-- than one, then let a generated-column unique key enforce the rule in MySQL.
UPDATE `user_addresses` a
JOIN `user_addresses` earlier
    ON earlier.`user_id` = a.`user_id`
   AND earlier.`deleted_at` IS NULL
   AND earlier.`is_default` = TRUE
   AND earlier.`id` < a.`id`
SET a.`is_default` = FALSE
WHERE a.`deleted_at` IS NULL
  AND a.`is_default` = TRUE;

ALTER TABLE `user_addresses`
    ADD COLUMN `active_default_user_id` BIGINT UNSIGNED
        GENERATED ALWAYS AS (
            CASE
                WHEN `deleted_at` IS NULL AND `is_default` = TRUE THEN `user_id`
                ELSE NULL
            END
        ) STORED,
    ADD UNIQUE KEY `uk_user_addresses_active_default` (`active_default_user_id`);

-- 4. Resolve image ownership from products -> shops. The temporary table has
-- image_id as its primary key, so a legacy image referenced by two merchants
-- deliberately aborts the migration instead of assigning an arbitrary owner.
CREATE TEMPORARY TABLE `v10_image_owners` (
    `image_id` BIGINT UNSIGNED NOT NULL,
    `merchant_id` BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (`image_id`)
);

INSERT INTO `v10_image_owners` (`image_id`, `merchant_id`)
SELECT DISTINCT p.`image_id`, s.`merchant_id`
FROM `products` p
JOIN `shops` s ON s.`id` = p.`shop_id`
WHERE p.`image_id` IS NOT NULL;

ALTER TABLE `images`
    ADD COLUMN `merchant_id` BIGINT UNSIGNED NULL AFTER `id`;

UPDATE `images` i
JOIN `v10_image_owners` owner ON owner.`image_id` = i.`id`
SET i.`merchant_id` = owner.`merchant_id`;

DROP TEMPORARY TABLE `v10_image_owners`;

ALTER TABLE `images`
    ADD INDEX `idx_images_merchant_created` (`merchant_id`, `created_at`),
    ADD CONSTRAINT `fk_images_merchant`
        FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT;

-- 5. Idempotency is scoped by actor and action. actor_id is intentionally not
-- a foreign key because its target table depends on actor_type.
CREATE TABLE `order_action_idempotency` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `actor_type` VARCHAR(20) NOT NULL,
    `actor_id` BIGINT UNSIGNED NOT NULL,
    `action_name` VARCHAR(40) NOT NULL,
    `idempotency_key` VARCHAR(100) NOT NULL,
    `request_fingerprint` CHAR(64) NOT NULL,
    `order_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_order_action_actor_key`
        UNIQUE (`actor_type`, `actor_id`, `action_name`, `idempotency_key`),
    INDEX `idx_order_action_order` (`order_id`),
    CONSTRAINT `fk_order_action_order`
        FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- 6. Payment/refund keys are no longer globally unique. Actor/action scoping is
-- handled by order_action_idempotency. Existing order-level uniqueness remains.
ALTER TABLE `payments`
    DROP INDEX `uk_payments_key`;

ALTER TABLE `refunds`
    DROP INDEX `uk_refunds_idempotency_key`;

-- Link legacy refunds to their real payment. If an invalid refund has no
-- matching payment, the NOT NULL change fails visibly and requires data review.
UPDATE `refunds` r
JOIN `payments` p ON p.`order_id` = r.`order_id`
SET r.`payment_id` = p.`id`
WHERE r.`payment_id` IS NULL;

ALTER TABLE `refunds`
    MODIFY COLUMN `payment_id` BIGINT UNSIGNED NOT NULL,
    ADD UNIQUE KEY `uk_refunds_payment` (`payment_id`);

-- 7. Required query paths are covered by existing V1-V9 indexes plus the
-- merchant-image and action-order indexes introduced above:
-- orders(user_id, created_at), orders(shop_id, status, created_at),
-- cart_items(user_id, sku_id), product_skus(product_id),
-- payments(order_id), refunds(order_id), refunds(payment_id),
-- images(merchant_id, created_at), and the scoped action unique key.
