package com.delivery.backend.address.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import com.delivery.backend.address.service.UserAddressService;
import com.delivery.backend.common.ApiResponse;
import com.delivery.backend.common.DeleteResult;
import com.delivery.backend.security.CurrentPrincipal;
import com.delivery.backend.security.RequireRole;
import com.delivery.backend.security.Role;

@RestController
@RequestMapping("/api/v1/user-addresses")
@RequireRole(Role.USER)
public class UserAddressController {
	private final UserAddressService service;
	public UserAddressController(UserAddressService service) { this.service = service; }
	@PostMapping public ResponseEntity<ApiResponse<UserAddressService.AddressView>> create(@RequestAttribute("currentPrincipal") CurrentPrincipal p, @Valid @RequestBody UserAddressService.CreateRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.create(p.id(), request))); }
	@GetMapping public ApiResponse<List<UserAddressService.AddressView>> list(@RequestAttribute("currentPrincipal") CurrentPrincipal p) { return ApiResponse.success(service.list(p.id())); }
	@PatchMapping("/{id}") public ApiResponse<UserAddressService.AddressView> update(@RequestAttribute("currentPrincipal") CurrentPrincipal p, @PathVariable @Positive long id, @Valid @RequestBody UserAddressService.UpdateRequest request) { return ApiResponse.success(service.update(p.id(), id, request)); }
	@DeleteMapping("/{id}") public ApiResponse<DeleteResult> remove(@RequestAttribute("currentPrincipal") CurrentPrincipal p, @PathVariable @Positive long id) { return ApiResponse.success(service.remove(p.id(), id)); }
}
