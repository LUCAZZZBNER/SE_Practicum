package com.delivery.backend.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import com.delivery.backend.common.ApiResponse;
import com.delivery.backend.common.PageResult;
import com.delivery.backend.order.service.OrderService;
import com.delivery.backend.security.CurrentPrincipal;
import com.delivery.backend.security.RequireRole;
import com.delivery.backend.security.Role;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping("/orders")
	@RequireRole(Role.USER)
	public ResponseEntity<ApiResponse<OrderService.OrderView>> create(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@RequestHeader("X-Idempotency-Key") @NotBlank String idempotencyKey,
			@Valid @RequestBody OrderService.CreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(orderService.create(principal.id(), idempotencyKey, request)));
	}

	@GetMapping("/orders")
	@RequireRole(Role.USER)
	public ApiResponse<PageResult<OrderService.OrderSummaryView>> listMine(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@RequestParam(required = false) @Pattern(regexp = "PENDING_PAYMENT|PAID|PREPARING|DELIVERING|COMPLETED|CANCELLED") String status,
			@RequestParam(required = false) @Min(1) Integer page,
			@RequestParam(required = false) @Min(1) @Max(100) Integer pageSize,
			@RequestParam(required = false) @Pattern(regexp = "createdAt|total") String sortBy,
			@RequestParam(required = false) @Pattern(regexp = "asc|desc") String sortOrder) {
		return ApiResponse.success(orderService.listMine(principal.id(),
				new OrderService.ListQuery(status, page, pageSize, sortBy, sortOrder)));
	}

	@GetMapping("/orders/{orderId}")
	@RequireRole(Role.USER)
	public ApiResponse<OrderService.OrderView> getMine(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@PathVariable @Positive long orderId) {
		return ApiResponse.success(orderService.getMine(principal.id(), orderId));
	}

	@PostMapping("/orders/{orderId}/cancel")
	@RequireRole(Role.USER)
	public ApiResponse<OrderService.OrderView> cancel(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@PathVariable @Positive long orderId) {
		return ApiResponse.success(orderService.cancel(principal.id(), orderId));
	}

	@GetMapping("/merchant/orders")
	@RequireRole(Role.MERCHANT)
	public ApiResponse<PageResult<OrderService.OrderSummaryView>> listMerchantOrders(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@RequestParam(required = false) @Positive Long shopId,
			@RequestParam(required = false) @Pattern(regexp = "PENDING_PAYMENT|PAID|PREPARING|DELIVERING|COMPLETED|CANCELLED") String status,
			@RequestParam(required = false) @Min(1) Integer page,
			@RequestParam(required = false) @Min(1) @Max(100) Integer pageSize,
			@RequestParam(required = false) @Pattern(regexp = "createdAt|total") String sortBy,
			@RequestParam(required = false) @Pattern(regexp = "asc|desc") String sortOrder) {
		return ApiResponse.success(orderService.listMerchantOrders(principal.id(),
				new OrderService.MerchantListQuery(shopId, status, page, pageSize, sortBy, sortOrder)));
	}

	@GetMapping("/merchant/orders/{orderId}")
	@RequireRole(Role.MERCHANT)
	public ApiResponse<OrderService.OrderView> getMerchantOrder(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@PathVariable @Positive long orderId) {
		return ApiResponse.success(orderService.getMerchantOrder(principal.id(), orderId));
	}
}
