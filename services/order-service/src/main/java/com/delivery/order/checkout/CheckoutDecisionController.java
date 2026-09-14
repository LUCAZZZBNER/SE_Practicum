package com.delivery.order.checkout;

import com.delivery.order.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/checkouts")
public class CheckoutDecisionController {
  private final CheckoutCoordinator coordinator;
  private final String token;
  public CheckoutDecisionController(CheckoutCoordinator coordinator, @Value("${security.internal-service-token:}") String token) {
    this.coordinator = coordinator;
    this.token = token;
  }
  @GetMapping("/{id}")
  public ApiResponse<DecisionView> decision(@PathVariable String id,
      @RequestHeader(value="X-Service-Token",required=false) String supplied) {
    if (token.isBlank() || !token.equals(supplied)) throw new BusinessException(ApiError.UNAUTHENTICATED);
    return ApiResponse.success(new DecisionView(coordinator.state(id)));
  }
  public record DecisionView(String state) {}
}
