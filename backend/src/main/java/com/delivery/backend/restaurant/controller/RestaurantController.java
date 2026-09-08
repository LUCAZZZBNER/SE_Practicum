package com.delivery.backend.restaurant.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import com.delivery.backend.common.ApiResponse;
import com.delivery.backend.common.PageResult;
import com.delivery.backend.restaurant.service.RestaurantService;
import com.delivery.backend.security.CurrentPrincipal;
import com.delivery.backend.security.PublicEndpoint;
import com.delivery.backend.security.RequireRole;
import com.delivery.backend.security.Role;

@RestController
@RequestMapping("/api/v1/shops")
public class RestaurantController {

	private final RestaurantService restaurantService;

	public RestaurantController(RestaurantService restaurantService) {
		this.restaurantService = restaurantService;
	}

	@PostMapping
	@RequireRole(Role.MERCHANT)
	public ResponseEntity<ApiResponse<RestaurantService.ShopView>> create(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@Valid @RequestBody RestaurantService.CreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(restaurantService.create(principal.id(), request)));
	}

	@GetMapping
	@PublicEndpoint(optionalAuthentication = true)
	public ApiResponse<PageResult<RestaurantService.ShopView>> list(
			@RequestParam(required = false) @Min(1) Integer page,
			@RequestParam(required = false) @Min(1) @Max(100) Integer pageSize,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) @Pattern(regexp = "OPEN|CLOSED|TEMPORARILY_CLOSED") String status,
			@RequestParam(required = false) Boolean mine,
			@RequestParam(required = false) @Pattern(regexp = "name|createdAt") String sortBy,
			@RequestParam(required = false) @Pattern(regexp = "asc|desc") String sortOrder,
			@RequestAttribute(name = "currentPrincipal", required = false) CurrentPrincipal principal) {
		Long merchantId = principal != null && principal.role() == Role.MERCHANT ? principal.id() : null;
		return ApiResponse.success(restaurantService.list(
				new RestaurantService.ListQuery(page, pageSize, keyword, status, mine, sortBy, sortOrder, merchantId)));
	}

	@GetMapping("/{shopId}")
	@PublicEndpoint
	public ApiResponse<RestaurantService.ShopView> get(@PathVariable @Positive long shopId) {
		return ApiResponse.success(restaurantService.get(shopId));
	}

	@PatchMapping("/{shopId}")
	@RequireRole(Role.MERCHANT)
	public ApiResponse<RestaurantService.ShopView> update(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@PathVariable @Positive long shopId,
			@Valid @RequestBody RestaurantService.UpdateRequest request) {
		return ApiResponse.success(restaurantService.update(principal.id(), shopId, request));
	}
}
