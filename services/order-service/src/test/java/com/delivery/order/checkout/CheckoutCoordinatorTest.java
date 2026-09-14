package com.delivery.order.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.delivery.order.common.ApiError;
import com.delivery.order.common.BusinessException;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

class CheckoutCoordinatorTest {
  @Test
  void commitsADecisionOnlyAfterTheWorkSucceeds() {
    var decisions = mock(CheckoutDecisionDao.class);
    var manager = mock(PlatformTransactionManager.class);
    var status = mock(TransactionStatus.class);
    when(manager.getTransaction(any())).thenReturn(status);
    var decision = new CheckoutDecision();
    decision.setFingerprint("fingerprint");
    decision.setState("OPEN");
    when(decisions.lock("checkout-1")).thenReturn(decision);

    var coordinator = new CheckoutCoordinator(decisions, manager);
    String result = coordinator.execute("checkout-1", 7L, "key", "fingerprint", () -> "created");

    assertThat(result).isEqualTo("created");
    verify(decisions).begin("checkout-1", 7L, "key", "fingerprint");
    verify(decisions).commit("checkout-1");
    verify(manager, times(2)).commit(status);
    verify(manager, never()).rollback(any());
  }

  @Test
  void rejectsAReusedKeyWithADifferentRequestFingerprint() {
    var decisions = mock(CheckoutDecisionDao.class);
    var manager = mock(PlatformTransactionManager.class);
    var status = mock(TransactionStatus.class);
    when(manager.getTransaction(any())).thenReturn(status);
    var decision = new CheckoutDecision();
    decision.setFingerprint("original");
    decision.setState("OPEN");
    when(decisions.lock("checkout-2")).thenReturn(decision);
    var coordinator = new CheckoutCoordinator(decisions, manager);

    assertThatThrownBy(() -> coordinator.execute("checkout-2", 7L, "key", "changed", () -> "nope"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).error()).isEqualTo(ApiError.IDEMPOTENCY_CONFLICT);
    verify(decisions, never()).commit("checkout-2");
  }
}
