package com.delivery.cart.shopping.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.delivery.cart.common.ApiError;
import com.delivery.cart.common.BusinessException;
import com.delivery.cart.item.dao.SkuDao;
import com.delivery.cart.item.service.ItemService;
import com.delivery.cart.merchant.shop.service.RestaurantService;
import com.delivery.cart.shopping.dao.ShoppingDao;
import com.delivery.cart.user.service.UserService;
import org.junit.jupiter.api.Test;

class ShoppingServiceImplTest {
  private final ShoppingDao shopping = mock(ShoppingDao.class);
  private final UserService users = mock(UserService.class);
  private final ItemService items = mock(ItemService.class);
  private final RestaurantService restaurants = mock(RestaurantService.class);
  private final SkuDao skus = mock(SkuDao.class);
  private final ShoppingServiceImpl service = new ShoppingServiceImpl(shopping, users, items, restaurants, skus);

  @Test
  void rejectsNonPositiveQuantityBeforeReadingProductData() {
    var request = new com.delivery.cart.shopping.service.ShoppingService.AddRequest(1L, 0);
    assertThatThrownBy(() -> service.add(9L, request))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).error()).isEqualTo(ApiError.VALIDATION_ERROR);
    verify(users).requireActive(9L);
    verifyNoInteractions(skus, items, restaurants, shopping);
  }

  @Test
  void removesOnlyAnOwnedCartItem() {
    when(shopping.deleteOwned(9L, 4L)).thenReturn(1);
    var result = service.remove(9L, 4L);
    assertThat(result.id()).isEqualTo(4L);
    assertThat(result.deleted()).isTrue();
    verify(users).requireActive(9L);
    verify(shopping).deleteOwned(9L, 4L);
  }
}
