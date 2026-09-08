package com.delivery.backend.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.delivery.backend.common.ApiResponse;
import com.delivery.backend.security.CurrentPrincipal;
import com.delivery.backend.security.PublicEndpoint;
import com.delivery.backend.security.RequireRole;
import com.delivery.backend.security.Role;
import com.delivery.backend.user.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	@PublicEndpoint
	public ResponseEntity<ApiResponse<UserService.UserView>> register(
			@Valid @RequestBody UserService.RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(userService.register(request)));
	}

	@PostMapping("/login")
	@PublicEndpoint
	public ApiResponse<UserService.AuthSession> login(@Valid @RequestBody UserService.LoginRequest request) {
		return ApiResponse.success(userService.login(request));
	}

	@GetMapping("/me")
	@RequireRole(Role.USER)
	public ApiResponse<UserService.UserView> getCurrent(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal) {
		return ApiResponse.success(userService.getCurrent(principal.id()));
	}

	@PatchMapping("/me")
	@RequireRole(Role.USER)
	public ApiResponse<UserService.UserView> updateCurrent(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@Valid @RequestBody UserService.UpdateRequest request) {
		return ApiResponse.success(userService.updateCurrent(principal.id(), request));
	}
}
