package com.delivery.backend.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.ServiceContractTestSupport;
import com.delivery.backend.common.ApiError;
import com.delivery.backend.user.service.UserService;

@SpringBootTest
@Transactional
class UserServiceContractTests extends ServiceContractTestSupport {

	@Autowired
	private UserService service;

	@Test
	void registrationReturnsAnActiveNonSensitiveUser() {
		UserService.UserView user = service.register(registration("user-register"));

		assertThat(user.id()).isPositive();
		assertThat(user.account()).isEqualTo("user-register");
		assertThat(user.nickname()).isEqualTo("Alice");
		assertThat(user.status()).isEqualTo("ACTIVE");
		assertThat(user.toString()).doesNotContain("ExamplePass123!");
	}

	@Test
	void duplicateAccountAndPasswordMismatchAreRejectedWithoutPartialRegistration() {
		service.register(registration("duplicate-user"));

		assertBusinessError(ApiError.ACCOUNT_EXISTS,
				() -> service.register(registration("duplicate-user")));
		assertBusinessError(ApiError.VALIDATION_ERROR, () -> service.register(
				new UserService.RegisterRequest("mismatch-user", "ExamplePass123!", "different", "Alice", null)));
	}

	@Test
	void loginReturnsAUserBearerSessionAndRejectsBadCredentials() {
		service.register(registration("login-user"));

		UserService.AuthSession session = service.login(
				new UserService.LoginRequest("login-user", "ExamplePass123!"));
		assertThat(session.accessToken()).isNotBlank();
		assertThat(session.tokenType()).isEqualTo("Bearer");
		assertThat(session.expiresIn()).isPositive();
		assertThat(session.roles()).containsExactly("USER");
		assertBusinessError(ApiError.BAD_CREDENTIALS,
				() -> service.login(new UserService.LoginRequest("login-user", "wrong-password")));
	}

	@Test
	void currentProfileUpdateAndActiveSnapshotAreScopedToTheUserId() {
		UserService.UserView registered = service.register(registration("profile-user"));
		UserService.UpdateRequest update = new UserService.UpdateRequest();
		update.setNickname("Updated Alice");
		update.setPhone(null);

		UserService.UserView updated = service.updateCurrent(registered.id(), update);
		assertThat(updated.nickname()).isEqualTo("Updated Alice");
		assertThat(updated.phone()).isNull();
		assertThat(service.getCurrent(registered.id())).isEqualTo(updated);
		assertThat(service.requireActive(registered.id()).id()).isEqualTo(registered.id());
		assertBusinessError(ApiError.RESOURCE_NOT_FOUND, () -> service.getCurrent(Long.MAX_VALUE));
	}

	private static UserService.RegisterRequest registration(String account) {
		return new UserService.RegisterRequest(account, "ExamplePass123!", "ExamplePass123!", "Alice", null);
	}
}
