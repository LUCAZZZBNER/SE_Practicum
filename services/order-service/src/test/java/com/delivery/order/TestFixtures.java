package com.delivery.order;

import com.delivery.order.common.PageResult;
import com.delivery.order.order.service.OrderService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class TestFixtures {
  private static final Instant TIME = Instant.parse("2026-09-04T00:00:00Z");
  public static OrderService.OrderView order(long id) {
    OrderService.OrderLineView line = new OrderService.OrderLineView(30, "Rice", new BigDecimal("12.50"), 1, new BigDecimal("12.50"));
    return new OrderService.OrderView(id, "ORDER-40", 7, 10, "Shop", List.of(line), new BigDecimal("12.50"), "PENDING_PAYMENT", TIME, TIME, null);
  }
  public static PageResult<OrderService.OrderSummaryView> orderPage() {
    OrderService.OrderSummaryView summary = new OrderService.OrderSummaryView(40, "ORDER-40", 10, "Shop", new BigDecimal("12.50"), "PENDING_PAYMENT", TIME);
    return new PageResult<>(List.of(summary), 1, 10, 1, 1);
  }
}
