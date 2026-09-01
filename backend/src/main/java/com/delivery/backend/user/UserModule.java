package com.delivery.backend.user;

import org.springframework.stereotype.Service;

/** Entry point for user-related use cases. */
@Service
public class UserModule {
	public boolean canAct(UserProfile user) {
		return user != null && user.status() == UserStatus.ACTIVE;
	}
}
