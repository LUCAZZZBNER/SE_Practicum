package com.delivery.backend.shopping;

/** A product and quantity selected by one user before checkout. */
public record CartItem(long id, long userId, long itemId, int quantity) {
	public CartItem {
		if (id < 1 || userId < 1 || itemId < 1) {
			throw new IllegalArgumentException("id, userId and itemId must be positive");
		}
		if (quantity < 1) {
			throw new IllegalArgumentException("quantity must be positive");
		}
	}
}
