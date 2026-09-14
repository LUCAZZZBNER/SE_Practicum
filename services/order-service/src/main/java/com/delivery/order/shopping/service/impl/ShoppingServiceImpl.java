package com.delivery.order.shopping.service.impl;

import com.delivery.order.shopping.client.CartCommandClient;
import com.delivery.order.shopping.service.ShoppingService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Delegates all cart-owned checkout operations to the cart service. */
@Service
public class ShoppingServiceImpl implements ShoppingService {
  private final CartCommandClient cartClient;

  public ShoppingServiceImpl(CartCommandClient cartClient) {
    this.cartClient = cartClient;
  }

  @Override
  public List<CheckoutItem> loadForCheckout(long userId, List<Long> cartItemIds) {
    return cartClient.loadForCheckout(userId, cartItemIds);
  }

  @Override
  public void prepareForCheckout(long userId, List<Long> cartItemIds, String checkoutId) {
    cartClient.prepareForCheckout(userId, cartItemIds, checkoutId);
  }

  @Override
  public void removeAfterCheckout(long userId, List<Long> cartItemIds, String checkoutId) {
    cartClient.removeAfterCheckout(userId, cartItemIds, checkoutId);
  }

  @Override
  public void releaseAfterCheckout(long userId, List<Long> cartItemIds, String checkoutId) {
    cartClient.releaseAfterCheckout(userId, cartItemIds, checkoutId);
  }
}
