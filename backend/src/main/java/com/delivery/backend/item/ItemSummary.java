package com.delivery.backend.item;

import java.math.BigDecimal;
import java.util.Objects;

/** Menu item snapshot shared with shopping and order use cases. */
public record ItemSummary(long id, long restaurantId, String name, BigDecimal price,
		int stock, ItemStatus status) {
	public ItemSummary {
		if (id < 1 || restaurantId < 1) {
			throw new IllegalArgumentException("id and restaurantId must be positive");
		}
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(price, "price must not be null");
		if (price.signum() <= 0 || stock < 0) {
			throw new IllegalArgumentException("price must be positive and stock cannot be negative");
		}
		Objects.requireNonNull(status, "status must not be null");
	}

	public boolean purchasable(int quantity) {
		return quantity > 0 && status == ItemStatus.ON_SALE && quantity <= stock;
	}
}
