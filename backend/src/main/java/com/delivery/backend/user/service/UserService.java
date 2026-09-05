package com.delivery.backend.user.service;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.delivery.backend.common.PatchRequest;

/** User business contract. Implementation and persistence are intentionally pending. */
public interface UserService {

	UserView register(RegisterRequest request);

	AuthSession login(LoginRequest request);

	UserView getCurrent(long userId);

	UserView updateCurrent(long userId, UpdateRequest request);

	UserSnapshot requireActive(long userId);

	record RegisterRequest(@NotBlank String account, @NotBlank String password, @NotBlank String passwordConfirm,
			@NotBlank String nickname, @Pattern(regexp = "(?s).*\\S.*") String phone) {
	}

	record LoginRequest(@NotBlank String account, @NotBlank String password) {
	}

	final class UpdateRequest extends PatchRequest {
		@Pattern(regexp = "(?s).*\\S.*")
		private String nickname;
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

		public String nickname() {
			return nickname;
		}

		public void setNickname(String nickname) {
			this.nickname = nickname;
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

	record UserView(long id, String account, String nickname, String phone, String status,
			Instant createdAt, Instant updatedAt) {
	}

	record AuthSession(String accessToken, String tokenType, long expiresIn, UserView user, List<String> roles) {
		public AuthSession {
			roles = List.copyOf(roles);
		}
	}

	record UserSnapshot(long id, String status) {
	}
}
