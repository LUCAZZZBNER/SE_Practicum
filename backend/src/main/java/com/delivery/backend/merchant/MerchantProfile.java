package com.delivery.backend.merchant;

import java.util.Objects;

/** Merchant information exposed to the rest of the monolith. */
public record MerchantProfile(long id, long userId, String name, MerchantStatus status) {
	public MerchantProfile {
		if (id < 1 || userId < 1) {
			throw new IllegalArgumentException("id and userId must be positive");
		}
		Objects.requireNonNull(name, "name must not be null");
		if (name.isBlank()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		Objects.requireNonNull(status, "status must not be null");
	}
}
