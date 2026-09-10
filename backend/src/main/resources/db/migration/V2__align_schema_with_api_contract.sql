-- Align the published V1 schema with the stage-1 API and service contracts.
-- V1 is intentionally left unchanged so existing Flyway checksums remain valid.

-- Merchants use an account system independent from ordinary users.
ALTER TABLE `merchants`
    DROP FOREIGN KEY `fk_merchants_user`;

ALTER TABLE `merchants`
    DROP INDEX `uk_merchants_user_id`,
    DROP COLUMN `user_id`,
    ADD COLUMN `account` VARCHAR(50) NOT NULL AFTER `id`,
    ADD COLUMN `password_hash` VARCHAR(100) NOT NULL AFTER `account`,
    ADD COLUMN `phone` VARCHAR(20) NOT NULL AFTER `name`,
    ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER `phone`,
    ADD COLUMN `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) AFTER `created_at`,
    ADD CONSTRAINT `uk_merchants_account` UNIQUE (`account`),
    ADD CONSTRAINT `chk_merchants_status`
        CHECK (`status` IN ('ACTIVE', 'SUSPENDED'));

-- A merchant may own multiple shops, but shop names must be unique per merchant.
ALTER TABLE `shops`
    DROP INDEX `uk_shops_merchant_id`,
    ADD COLUMN `deleted_at` DATETIME(3) NULL AFTER `updated_at`,
    ADD CONSTRAINT `uk_shops_merchant_name` UNIQUE (`merchant_id`, `name`),
    ADD INDEX `idx_shops_public_list` (`deleted_at`, `status`, `created_at`);

-- Categories are ordered and deleted logically so historical references remain valid.
ALTER TABLE `product_categories`
    ADD COLUMN `sort_order` INT UNSIGNED NOT NULL DEFAULT 0 AFTER `name`,
    ADD COLUMN `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) AFTER `sort_order`,
    ADD COLUMN `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) AFTER `created_at`,
    ADD COLUMN `deleted_at` DATETIME(3) NULL AFTER `updated_at`,
    ADD INDEX `idx_categories_shop_order` (`shop_id`, `deleted_at`, `sort_order`, `id`);

-- Product updates and stock reservations use optimistic locking.
ALTER TABLE `products`
    ADD COLUMN `version` BIGINT UNSIGNED NOT NULL DEFAULT 1 AFTER `status`;

-- Orders retain idempotency data and display snapshots and can represent cancellation.
ALTER TABLE `orders`
    DROP CHECK `chk_orders_status`;

ALTER TABLE `orders`
    ADD COLUMN `idempotency_key` VARCHAR(100) NOT NULL AFTER `user_id`,
    ADD COLUMN `request_fingerprint` CHAR(64) NOT NULL AFTER `idempotency_key`,
    ADD COLUMN `shop_name` VARCHAR(100) NOT NULL AFTER `shop_id`,
    ADD COLUMN `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) AFTER `created_at`,
    ADD COLUMN `cancelled_at` DATETIME(3) NULL AFTER `updated_at`,
    ADD CONSTRAINT `uk_orders_user_idempotency` UNIQUE (`user_id`, `idempotency_key`),
    ADD CONSTRAINT `chk_orders_status`
        CHECK (`status` IN (
            'PENDING_PAYMENT',
            'PAID',
            'PREPARING',
            'DELIVERING',
            'COMPLETED',
            'CANCELLED'
        )),
    ADD INDEX `idx_orders_shop_status_created` (`shop_id`, `status`, `created_at`);
