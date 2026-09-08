package com.delivery.backend.item.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import com.delivery.backend.common.ApiResponse;
import com.delivery.backend.common.DeleteResult;
import com.delivery.backend.common.PageResult;
import com.delivery.backend.item.service.ItemService;
import com.delivery.backend.security.CurrentPrincipal;
import com.delivery.backend.security.PublicEndpoint;
import com.delivery.backend.security.RequireRole;
import com.delivery.backend.security.Role;

@RestController
@RequestMapping("/api/v1")
public class ItemController {

	private final ItemService itemService;

	public ItemController(ItemService itemService) {
		this.itemService = itemService;
	}

	@PostMapping("/shops/{shopId}/categories")
	@RequireRole(Role.MERCHANT)
	public ResponseEntity<ApiResponse<ItemService.CategoryView>> createCategory(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@PathVariable @Positive long shopId, @Valid @RequestBody ItemService.CreateCategoryRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(itemService.createCategory(principal.id(), shopId, request)));
	}

	@GetMapping("/shops/{shopId}/categories")
	@PublicEndpoint
	public ApiResponse<List<ItemService.CategoryView>> listCategories(@PathVariable @Positive long shopId) {
		return ApiResponse.success(itemService.listCategories(shopId));
	}

	@PatchMapping("/categories/{categoryId}")
	@RequireRole(Role.MERCHANT)
	public ApiResponse<ItemService.CategoryView> updateCategory(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@PathVariable @Positive long categoryId, @Valid @RequestBody ItemService.UpdateCategoryRequest request) {
		return ApiResponse.success(itemService.updateCategory(principal.id(), categoryId, request));
	}

	@DeleteMapping("/categories/{categoryId}")
	@RequireRole(Role.MERCHANT)
	public ApiResponse<DeleteResult> deleteCategory(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@PathVariable @Positive long categoryId) {
		return ApiResponse.success(itemService.deleteCategory(principal.id(), categoryId));
	}

	@PostMapping("/products")
	@RequireRole(Role.MERCHANT)
	public ResponseEntity<ApiResponse<ItemService.ProductView>> createProduct(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@Valid @RequestBody ItemService.CreateProductRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(itemService.createProduct(principal.id(), request)));
	}

	@GetMapping("/shops/{shopId}/products")
	@PublicEndpoint(optionalAuthentication = true)
	public ApiResponse<PageResult<ItemService.ProductView>> listProducts(@PathVariable @Positive long shopId,
			@RequestParam(required = false) @Positive Long categoryId,
			@RequestParam(required = false) String keyword, @RequestParam(required = false) @Min(1) Integer page,
			@RequestParam(required = false) @Min(1) @Max(100) Integer pageSize,
			@RequestParam(required = false) @Pattern(regexp = "name|price|createdAt") String sortBy,
			@RequestParam(required = false) @Pattern(regexp = "asc|desc") String sortOrder,
			@RequestParam(required = false) Boolean includeOffSale,
			@RequestAttribute(name = "currentPrincipal", required = false) CurrentPrincipal principal) {
		Long merchantId = principal != null && principal.role() == Role.MERCHANT ? principal.id() : null;
		return ApiResponse.success(itemService.listProducts(shopId,
				new ItemService.ProductQuery(categoryId, keyword, page, pageSize, sortBy, sortOrder, includeOffSale,
						merchantId)));
	}

	@GetMapping("/products/{productId}")
	@PublicEndpoint(optionalAuthentication = true)
	public ApiResponse<ItemService.ProductView> getProduct(@PathVariable @Positive long productId,
			@RequestParam(defaultValue = "false") boolean includeOffSale,
			@RequestAttribute(name = "currentPrincipal", required = false) CurrentPrincipal principal) {
		Long merchantId = principal != null && principal.role() == Role.MERCHANT ? principal.id() : null;
		return ApiResponse.success(itemService.getProduct(productId, includeOffSale, merchantId));
	}

	@PatchMapping("/products/{productId}")
	@RequireRole(Role.MERCHANT)
	public ApiResponse<ItemService.ProductView> updateProduct(
			@RequestAttribute("currentPrincipal") CurrentPrincipal principal,
			@PathVariable @Positive long productId, @Valid @RequestBody ItemService.UpdateProductRequest request) {
		return ApiResponse.success(itemService.updateProduct(principal.id(), productId, request));
	}
}
