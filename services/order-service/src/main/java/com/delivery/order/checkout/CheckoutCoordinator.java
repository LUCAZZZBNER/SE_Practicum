package com.delivery.order.checkout;

import com.delivery.order.common.ApiError;
import com.delivery.order.common.BusinessException;
import java.util.function.Supplier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Holds a local decision lock until the order and its outbox commit together. */
@Service
public class CheckoutCoordinator {
  private final CheckoutDecisionDao decisions;
  private final TransactionTemplate transaction;

  public CheckoutCoordinator(CheckoutDecisionDao decisions, PlatformTransactionManager manager) {
    this.decisions = decisions;
    this.transaction = new TransactionTemplate(manager);
  }

  public <T> T execute(String id, long userId, String key, String fingerprint, Supplier<T> work) {
    transaction.executeWithoutResult(status -> decisions.begin(id, userId, key, fingerprint));
    try {
      return transaction.execute(status -> {
        CheckoutDecision decision = decisions.lock(id);
        if (!fingerprint.equals(decision.getFingerprint())) throw new BusinessException(ApiError.IDEMPOTENCY_CONFLICT);
        if ("ABORTED".equals(decision.getState())) throw new BusinessException(ApiError.valueOf(decision.getError()));
        T result = work.get();
        decisions.commit(id);
        return result;
      });
    } catch (RuntimeException failure) {
      ApiError error = failure instanceof BusinessException business ? business.error() : ApiError.INTERNAL_ERROR;
      // This transaction starts after rollback; it cannot erase a concurrent commit.
      if (error != ApiError.IDEMPOTENCY_CONFLICT) {
        transaction.executeWithoutResult(status -> decisions.abort(id, error.name()));
      }
      throw failure;
    }
  }

  @Scheduled(fixedDelayString = "${checkout.recovery-poll-ms:5000}")
  public void abortAbandoned() {
    transaction.executeWithoutResult(status -> {
      for (CheckoutDecision decision : decisions.abandoned()) decisions.abort(decision.getId(), ApiError.INTERNAL_ERROR.name());
    });
  }

  public String state(String id) {
    CheckoutDecision decision = decisions.find(id);
    return decision == null ? "UNKNOWN" : decision.getState();
  }
}
