package com.delivery.cart.shopping.controller;

import com.delivery.cart.shopping.service.ShoppingService;
import com.delivery.cart.common.ApiError;
import com.delivery.cart.common.BusinessException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Private cart command used by order checkout and never routed by the gateway. */
@RestController
@RequestMapping("/internal/v1/cart")
public class InternalCartController {
  private final ShoppingService shoppingService;
  private final String serviceToken;

  public InternalCartController(ShoppingService shoppingService,
      @Value("${security.internal-service-token:}") String serviceToken) {
    this.shoppingService = shoppingService;
    this.serviceToken = serviceToken;
  }

  @PostMapping("/remove-after-checkout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeAfterCheckout(
      @RequestHeader(value = "X-Service-Token", required = false) String token,
      @RequestBody RemoveCartRequest request) {
    requireToken(token);
    shoppingService.removeAfterCheckout(request.userId(), request.cartItemIds(), request.checkoutId());
  }

  @PostMapping("/load-for-checkout")
  public com.delivery.cart.common.ApiResponse<List<ShoppingService.CheckoutItem>> loadForCheckout(
      @RequestHeader(value = "X-Service-Token", required = false) String token,
      @RequestBody CheckoutItemsRequest request) {
    requireToken(token);
    return com.delivery.cart.common.ApiResponse.success(
        shoppingService.loadForCheckout(request.userId(), request.cartItemIds()));
  }

  @PostMapping("/prepare-checkout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void prepare(@RequestHeader(value = "X-Service-Token", required = false) String token,
      @RequestBody RemoveCartRequest request) {
    requireToken(token);
    shoppingService.prepareForCheckout(request.userId(), request.cartItemIds(), request.checkoutId());
  }

  @PostMapping("/release-after-checkout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void release(@RequestHeader(value = "X-Service-Token", required = false) String token,
      @RequestBody RemoveCartRequest request) {
    requireToken(token);
    shoppingService.releaseAfterCheckout(request.userId(), request.cartItemIds(), request.checkoutId());
  }

  private void requireToken(String token) {
    if (serviceToken.isBlank() || token == null || !serviceToken.equals(token)) {
      throw new BusinessException(ApiError.UNAUTHENTICATED);
    }
  }

  public record RemoveCartRequest(long userId, List<Long> cartItemIds, String checkoutId) {
    public RemoveCartRequest(long userId, List<Long> cartItemIds) { this(userId, cartItemIds, null); }
  }

  public record CheckoutItemsRequest(long userId, List<Long> cartItemIds) {}
}
