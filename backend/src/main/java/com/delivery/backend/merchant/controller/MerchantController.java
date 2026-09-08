package com.delivery.backend.merchant.controller;

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
import com.delivery.backend.merchant.service.MerchantService;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

	private final MerchantService merchantService;

	public MerchantController(MerchantService merchantService) {
		this.merchantService = merchantService;
	}

	@PostMapping
	@PublicEndpoint
	public ResponseEntity<ApiResponse<MerchantService.MerchantView>> register(
			@Valid @RequestBody MerchantService.RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(merchantService.register(request)));
	}

	@PostMapping("/login")
	@PublicEndpoint
	public ApiResponse<MerchantService.AuthSession> login(@Valid @RequestBody MerchantService.LoginRequest request) {
		return ApiResponse.success(merchantService.login(request));
	}

	@GetMapping("/me")
	@RequireRole(Role.MERCHANT)
	public ApiResponse<MerchantService.MerchantView> getCurrent(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal) {
		return ApiResponse.success(merchantService.getCurrent(principal.id()));
	}

	@PatchMapping("/me")
	@RequireRole(Role.MERCHANT)
	public ApiResponse<MerchantService.MerchantView> updateCurrent(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@Valid @RequestBody MerchantService.UpdateRequest request) {
		return ApiResponse.success(merchantService.updateCurrent(principal.id(), request));
	}
}
