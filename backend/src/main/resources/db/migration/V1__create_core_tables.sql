CREATE TABLE `users` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `account` VARCHAR(50) NOT NULL,
    `password_hash` VARCHAR(100) NOT NULL,
    `nickname` VARCHAR(50) NOT NULL,
    `phone` VARCHAR(20) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_users_account` UNIQUE (`account`),
    CONSTRAINT `chk_users_status`
        CHECK (`status` IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE `merchants` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_merchants_user_id` UNIQUE (`user_id`),
    CONSTRAINT `fk_merchants_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE `shops` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `merchant_id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500) NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'CLOSED',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_shops_merchant_id` UNIQUE (`merchant_id`),
    CONSTRAINT `chk_shops_status`
        CHECK (`status` IN ('OPEN', 'CLOSED', 'TEMPORARILY_CLOSED')),
    CONSTRAINT `fk_shops_merchant`
        FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE `product_categories` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `shop_id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_categories_shop_name` UNIQUE (`shop_id`, `name`),
    CONSTRAINT `fk_categories_shop`
        FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE `products` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `shop_id` BIGINT UNSIGNED NOT NULL,
    `category_id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(1000) NULL,
    `price` DECIMAL(10, 2) NOT NULL,
    `stock` INT UNSIGNED NOT NULL DEFAULT 0,
    `status` VARCHAR(20) NOT NULL DEFAULT 'OFF_SALE',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    INDEX `idx_products_shop_status` (`shop_id`, `status`),
    INDEX `idx_products_category` (`category_id`),
    CONSTRAINT `chk_products_price` CHECK (`price` > 0),
    CONSTRAINT `chk_products_stock` CHECK (`stock` >= 0),
    CONSTRAINT `chk_products_status`
        CHECK (`status` IN ('ON_SALE', 'OFF_SALE')),
    CONSTRAINT `fk_products_shop`
        FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_products_category`
        FOREIGN KEY (`category_id`) REFERENCES `product_categories` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE `cart_items` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `product_id` BIGINT UNSIGNED NOT NULL,
    `quantity` INT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_cart_items_user_product` UNIQUE (`user_id`, `product_id`),
    INDEX `idx_cart_items_user` (`user_id`),
    CONSTRAINT `chk_cart_items_quantity` CHECK (`quantity` > 0),
    CONSTRAINT `fk_cart_items_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_cart_items_product`
        FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE `orders` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(40) NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `shop_id` BIGINT UNSIGNED NOT NULL,
    `total_amount` DECIMAL(10, 2) NOT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_orders_order_no` UNIQUE (`order_no`),
    INDEX `idx_orders_user_created` (`user_id`, `created_at`),
    CONSTRAINT `chk_orders_total` CHECK (`total_amount` > 0),
    CONSTRAINT `chk_orders_status` CHECK (`status` = 'PENDING_PAYMENT'),
    CONSTRAINT `fk_orders_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_orders_shop`
        FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE `order_items` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT UNSIGNED NOT NULL,
    `product_id` BIGINT UNSIGNED NOT NULL,
    `product_name` VARCHAR(100) NOT NULL,
    `unit_price` DECIMAL(10, 2) NOT NULL,
    `quantity` INT UNSIGNED NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_order_items_order` (`order_id`),
    CONSTRAINT `chk_order_items_price` CHECK (`unit_price` > 0),
    CONSTRAINT `chk_order_items_quantity` CHECK (`quantity` > 0),
    CONSTRAINT `fk_order_items_order`
        FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_order_items_product`
        FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);
