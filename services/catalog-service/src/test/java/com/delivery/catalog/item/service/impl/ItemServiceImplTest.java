package com.delivery.catalog.item.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.delivery.catalog.common.ApiError;
import com.delivery.catalog.common.BusinessException;
import com.delivery.catalog.item.entity.ProductEntity;

import com.delivery.catalog.item.dao.ItemDao;
import com.delivery.catalog.merchant.shop.service.RestaurantService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItemServiceImplTest {
  private final ItemDao items = mock(ItemDao.class);
  private final RestaurantService restaurants = mock(RestaurantService.class);
  private final ItemServiceImpl service = new ItemServiceImpl(items, restaurants);

  @Test
  void listsCategoriesFromTheCatalogOwnerStore() {
    when(items.listCategories(12L)).thenReturn(List.of());
    assertThat(service.listCategories(12L)).isEmpty();
    verify(restaurants).get(12L);
    verify(items).listCategories(12L);
  }

  @Test
  void hidesAnOffSaleProductFromPublicReads() {
    var product = new ProductEntity();
    product.setId(8L);
    product.setShopId(12L);
    product.setStatus("OFF_SALE");
    when(items.findProductById(8L)).thenReturn(product);
    assertThatThrownBy(() -> service.getProduct(8L, false, null))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).error()).isEqualTo(ApiError.RESOURCE_NOT_FOUND);
  }
}
