package com.delivery.backend.shopping.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import com.delivery.backend.common.ApiResponse;
import com.delivery.backend.common.DeleteResult;
import com.delivery.backend.security.CurrentPrincipal;
import com.delivery.backend.security.RequireRole;
import com.delivery.backend.security.Role;
import com.delivery.backend.shopping.service.ShoppingService;

@RestController
@RequestMapping("/api/v1/cart-items")
public class ShoppingController {

	private final ShoppingService shoppingService;

	public ShoppingController(ShoppingService shoppingService) {
		this.shoppingService = shoppingService;
	}

	@PostMapping
	@RequireRole(Role.USER)
	public ResponseEntity<ApiResponse<ShoppingService.CartItemView>> add(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@Valid @RequestBody ShoppingService.AddRequest request) {
		ShoppingService.AddResult result = shoppingService.add(principal.id(), request);
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(ApiResponse.success(result.item()));
	}

	@GetMapping
	@RequireRole(Role.USER)
	public ApiResponse<ShoppingService.CartView> getCart(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal) {
		return ApiResponse.success(shoppingService.getCart(principal.id()));
	}

	@PatchMapping("/{cartItemId}")
	@RequireRole(Role.USER)
	public ApiResponse<ShoppingService.CartItemView> changeQuantity(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@PathVariable @Positive long cartItemId, @Valid @RequestBody QuantityRequest request) {
		return ApiResponse.success(shoppingService.changeQuantity(principal.id(), cartItemId, request.quantity()));
	}

	@DeleteMapping("/{cartItemId}")
	@RequireRole(Role.USER)
	public ApiResponse<DeleteResult> remove(@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@PathVariable @Positive long cartItemId) {
		return ApiResponse.success(shoppingService.remove(principal.id(), cartItemId));
	}

	public record QuantityRequest(@jakarta.validation.constraints.Positive int quantity) {
	}
}
