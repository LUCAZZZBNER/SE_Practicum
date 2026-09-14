package com.delivery.catalog;

import com.delivery.catalog.common.PageResult;
import com.delivery.catalog.item.service.ItemService;
import com.delivery.catalog.merchant.shop.service.RestaurantService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class TestFixtures {
  private static final Instant TIME = Instant.parse("2026-09-04T00:00:00Z");
  public static RestaurantService.ShopView shop(long id) { return new RestaurantService.ShopView(id, 2, "Shop", null, "OPEN", TIME, TIME); }
  public static PageResult<RestaurantService.ShopView> shopPage() { return new PageResult<>(List.of(shop(10)), 1, 10, 1, 1); }
  public static ItemService.CategoryView category(long id) { return new ItemService.CategoryView(id, 10, "Meals", 0, TIME, TIME); }
  public static ItemService.ProductView product(long id) { return new ItemService.ProductView(id, 10, 21, "Rice", null, new BigDecimal("12.50"), 5, "ON_SALE", 3, TIME, TIME); }
  public static PageResult<ItemService.ProductView> productPage() { return new PageResult<>(List.of(product(30)), 1, 10, 1, 1); }
}
