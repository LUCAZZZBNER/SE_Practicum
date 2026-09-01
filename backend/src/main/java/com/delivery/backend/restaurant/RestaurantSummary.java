package com.delivery.backend.restaurant;

import java.util.Objects;

/** Restaurant data suitable for list and detail views. */
public record RestaurantSummary(long id, long merchantId, String name, String description,
		RestaurantStatus status) {
	public RestaurantSummary {
		if (id < 1 || merchantId < 1) {
			throw new IllegalArgumentException("id and merchantId must be positive");
		}
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(status, "status must not be null");
	}
}
