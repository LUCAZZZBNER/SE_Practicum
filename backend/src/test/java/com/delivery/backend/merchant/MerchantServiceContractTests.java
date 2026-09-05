package com.delivery.backend.merchant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.ServiceContractTestSupport;
import com.delivery.backend.common.ApiError;
import com.delivery.backend.merchant.service.MerchantService;

@SpringBootTest
@Transactional
class MerchantServiceContractTests extends ServiceContractTestSupport {

	@Autowired
	private MerchantService service;

	@Test
	void registrationCreatesAnIndependentActiveMerchant() {
		MerchantService.MerchantView merchant = service.register(registration("merchant-register"));

		assertThat(merchant.id()).isPositive();
		assertThat(merchant.account()).isEqualTo("merchant-register");
		assertThat(merchant.status()).isEqualTo("ACTIVE");
		assertThat(merchant.toString()).doesNotContain("ExamplePass123!");
	}

	@Test
	void duplicateMerchantAccountIsRejected() {
		service.register(registration("duplicate-merchant"));

		assertBusinessError(ApiError.MERCHANT_ACCOUNT_EXISTS,
				() -> service.register(registration("duplicate-merchant")));
	}

	@Test
	void loginReturnsMerchantRoleAndRejectsBadCredentials() {
		service.register(registration("login-merchant"));

		MerchantService.AuthSession session = service.login(
				new MerchantService.LoginRequest("login-merchant", "ExamplePass123!"));
		assertThat(session.accessToken()).isNotBlank();
		assertThat(session.roles()).containsExactly("MERCHANT");
		assertBusinessError(ApiError.BAD_CREDENTIALS,
				() -> service.login(new MerchantService.LoginRequest("login-merchant", "wrong-password")));
	}

	@Test
	void profileUpdateAndActiveSnapshotUseTheAuthenticatedMerchantId() {
		MerchantService.MerchantView registered = service.register(registration("profile-merchant"));
		MerchantService.UpdateRequest update = new MerchantService.UpdateRequest();
		update.setName("Updated Store");

		MerchantService.MerchantView updated = service.updateCurrent(registered.id(), update);
		assertThat(updated.name()).isEqualTo("Updated Store");
		assertThat(service.getCurrent(registered.id())).isEqualTo(updated);
		assertThat(service.requireActive(registered.id()).id()).isEqualTo(registered.id());
		assertBusinessError(ApiError.RESOURCE_NOT_FOUND, () -> service.getCurrent(Long.MAX_VALUE));
	}

	private static MerchantService.RegisterRequest registration(String account) {
		return new MerchantService.RegisterRequest(account, "ExamplePass123!", "ExamplePass123!", "Store",
				"13900000000");
	}
}
