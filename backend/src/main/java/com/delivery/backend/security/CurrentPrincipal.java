package com.delivery.backend.security;

import java.util.Objects;

/** Authenticated subject exposed to controllers by the security interceptor. */
public record CurrentPrincipal(long id, Role role) {

	public CurrentPrincipal {
		if (id < 1) {
			throw new IllegalArgumentException("principal id must be positive");
		}
		Objects.requireNonNull(role, "role must not be null");
	}
}
