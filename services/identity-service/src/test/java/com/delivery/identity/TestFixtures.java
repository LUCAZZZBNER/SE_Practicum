package com.delivery.identity;

import com.delivery.identity.merchant.service.MerchantService;
import com.delivery.identity.user.service.UserService;
import java.time.Instant;
import java.util.List;

public final class TestFixtures {
  private static final Instant TIME = Instant.parse("2026-09-04T00:00:00Z");
  public static UserService.UserView user(long id) { return new UserService.UserView(id, "alice", "Alice", null, "ACTIVE", TIME, TIME); }
  public static UserService.AuthSession userSession(long id) { return new UserService.AuthSession("token", "Bearer", 7200, user(id), List.of("USER")); }
  public static MerchantService.MerchantView merchant(long id) { return new MerchantService.MerchantView(id, "merchant", "Store", "13900000000", "ACTIVE", TIME, TIME); }
  public static MerchantService.AuthSession merchantSession(long id) { return new MerchantService.AuthSession("token", "Bearer", 7200, merchant(id), List.of("MERCHANT")); }
}
