package com.delivery.backend.order;

import java.math.BigDecimal;
import java.util.Objects;

/** Immutable order-line snapshot; price and name are captured at checkout. */
public record OrderLine(long itemId, String itemName, BigDecimal unitPrice, int quantity) {
	public OrderLine {
		if (itemId < 1 || quantity < 1) {
			throw new IllegalArgumentException("itemId and quantity must be positive");
		}
		Objects.requireNonNull(itemName, "itemName must not be null");
		Objects.requireNonNull(unitPrice, "unitPrice must not be null");
		if (unitPrice.signum() <= 0) {
			throw new IllegalArgumentException("unitPrice must be positive");
		}
	}

	public BigDecimal subtotal() {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}
}
