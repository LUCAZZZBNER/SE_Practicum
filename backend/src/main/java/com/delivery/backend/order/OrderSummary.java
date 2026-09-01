package com.delivery.backend.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Immutable order summary exposed across module boundaries. */
public record OrderSummary(long id, String orderNumber, long userId, long restaurantId,
		List<OrderLine> lines, BigDecimal total, OrderStatus status) {
	public OrderSummary {
		if (id < 1 || userId < 1 || restaurantId < 1) {
			throw new IllegalArgumentException("id, userId and restaurantId must be positive");
		}
		Objects.requireNonNull(orderNumber, "orderNumber must not be null");
		Objects.requireNonNull(lines, "lines must not be null");
		Objects.requireNonNull(total, "total must not be null");
		Objects.requireNonNull(status, "status must not be null");
		lines = List.copyOf(lines);
	}
}
