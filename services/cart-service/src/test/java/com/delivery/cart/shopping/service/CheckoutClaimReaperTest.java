package com.delivery.cart.shopping.service;

import static org.mockito.Mockito.*;

import com.delivery.cart.shopping.dao.ShoppingDao;
import org.junit.jupiter.api.Test;

class CheckoutClaimReaperTest {
  @Test void skipsRecoveryWhenOrderEndpointIsNotConfigured() {
    var dao = mock(ShoppingDao.class);
    new CheckoutClaimReaper(dao, "", "secret").reconcile();
    verifyNoInteractions(dao);
  }
}
