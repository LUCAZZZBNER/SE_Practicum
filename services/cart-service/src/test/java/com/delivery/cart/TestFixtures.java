package com.delivery.cart;

import com.delivery.cart.common.PageResult;
import com.delivery.cart.shopping.service.ShoppingService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class TestFixtures {
  private static final Instant TIME = Instant.parse("2026-09-04T00:00:00Z");
  public static ShoppingService.CartItemView cartItem(long id) {
    ShoppingService.CartProductView product = new ShoppingService.CartProductView(30, 10, "Rice", new BigDecimal("12.50"), 5, "ON_SALE", 3);
    return new ShoppingService.CartItemView(id, product, 1, new BigDecimal("12.50"), true, null, TIME, TIME);
  }
  public static ShoppingService.CartView cart() { return new ShoppingService.CartView(List.of(cartItem(31)), new BigDecimal("12.50")); }
}
