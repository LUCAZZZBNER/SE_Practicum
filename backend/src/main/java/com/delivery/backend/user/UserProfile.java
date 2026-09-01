package com.delivery.backend.user;

import java.util.Objects;

/** Public, non-sensitive user information shared with other modules. */
public record UserProfile(long id, String account, String nickname, UserStatus status) {
	public UserProfile {
		if (id < 1) {
			throw new IllegalArgumentException("id must be positive");
		}
		Objects.requireNonNull(account, "account must not be null");
		Objects.requireNonNull(status, "status must not be null");
	}
}
