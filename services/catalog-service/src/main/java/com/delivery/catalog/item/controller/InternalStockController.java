package com.delivery.catalog.item.controller;

import com.delivery.catalog.common.ApiResponse;
import com.delivery.catalog.common.ApiError;
import com.delivery.catalog.common.BusinessException;
import com.delivery.catalog.item.service.ItemService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Private stock commands used by the order service and never routed by the gateway. */
@RestController
@RequestMapping("/internal/v1/catalog/stock")
public class InternalStockController {
  private final ItemService itemService;
  private final String serviceToken;

  public InternalStockController(ItemService itemService,
      @Value("${security.internal-service-token:}") String serviceToken) {
    this.itemService = itemService;
    this.serviceToken = serviceToken;
  }

  @PostMapping("/reserve")
  public ApiResponse<List<ItemService.ProductSnapshot>> reserve(
      @RequestHeader(value = "X-Service-Token", required = false) String token,
      @RequestBody List<ItemService.ReservationRequest> requests) {
    requireToken(token);
    return ApiResponse.success(itemService.reserveForOrder(requests));
  }

  @PostMapping("/restore")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void restore(@RequestHeader(value = "X-Service-Token", required = false) String token,
      @RequestBody List<ItemService.StockRestore> restorations) {
    requireToken(token);
    itemService.restoreStock(restorations);
  }

  @PostMapping("/confirm")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void confirm(@RequestHeader(value = "X-Service-Token", required = false) String token,
      @RequestBody List<String> reservationIds) {
    requireToken(token);
    itemService.confirmReservations(reservationIds);
  }

  private void requireToken(String token) {
    if (serviceToken.isBlank() || token == null || !serviceToken.equals(token)) {
      throw new BusinessException(ApiError.UNAUTHENTICATED);
    }
  }
}
