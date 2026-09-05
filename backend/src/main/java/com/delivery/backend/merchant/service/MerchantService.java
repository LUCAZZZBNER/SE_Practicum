package com.delivery.backend.merchant.service;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.delivery.backend.common.PatchRequest;

/** Merchant business contract. Implementation and persistence are intentionally pending. */
public interface MerchantService {

	MerchantView register(RegisterRequest request);

	AuthSession login(LoginRequest request);

	MerchantView getCurrent(long merchantId);

	MerchantView updateCurrent(long merchantId, UpdateRequest request);

	MerchantSnapshot requireActive(long merchantId);

	record RegisterRequest(@NotBlank String account, @NotBlank String password, String passwordConfirm,
			@NotBlank String name, @NotBlank String phone) {
	}

	record LoginRequest(@NotBlank String account, @NotBlank String password) {
	}

	final class UpdateRequest extends PatchRequest {
		@Pattern(regexp = "(?s).*\\S.*")
		private String name;
		@Pattern(regexp = "(?s).*\\S.*")
		private String phone;

		public UpdateRequest() {
		}

		@Override
		@JsonIgnore
		@AssertTrue
		public boolean isUpdateSpecified() {
			return super.isUpdateSpecified();
		}

		public String name() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
			markUpdateSpecified();
		}

		public String phone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
			markUpdateSpecified();
		}
	}

	record MerchantView(long id, String account, String name, String phone, String status,
			Instant createdAt, Instant updatedAt) {
	}

	record AuthSession(String accessToken, String tokenType, long expiresIn, MerchantView merchant,
			List<String> roles) {
		public AuthSession {
			roles = List.copyOf(roles);
		}
	}

	record MerchantSnapshot(long id, String status) {
	}
}
