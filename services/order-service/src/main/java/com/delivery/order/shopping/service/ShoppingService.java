package com.delivery.order.shopping.service;

import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;

/** Shopping-cart business contract. Implementation and persistence are intentionally pending. */
public interface ShoppingService {

  /** Legacy response shapes retained for source compatibility; cart data is owned by cart-service. */
  record CartProductView(long id, long shopId, String name, BigDecimal price, int stock,
      String status, long version, String imageUrl) {}
  record CartSkuView(long id, String name, BigDecimal price, int stock, String status, long version) {}
  record CartItemView(long id, CartProductView product, int quantity, BigDecimal subtotal,
      boolean available, String unavailableReason, Instant createdAt, Instant updatedAt, CartSkuView sku) {}

  List<CheckoutItem> loadForCheckout(long userId, List<Long> cartItemIds);
  void prepareForCheckout(long userId, List<Long> cartItemIds, String checkoutId);

  void removeAfterCheckout(long userId, List<Long> cartItemIds, String checkoutId);
  void releaseAfterCheckout(long userId, List<Long> cartItemIds, String checkoutId);

  record CheckoutItem(
      long cartItemId,
      long productId,
      long shopId,
      int quantity,
      long confirmedVersion,
      long skuId,
      long skuVersion) {
    public CheckoutItem(
        long cartItemId, long productId, long shopId, int quantity, long confirmedVersion) {
      this(cartItemId, productId, shopId, quantity, confirmedVersion, 0, confirmedVersion);
    }
  }
}
