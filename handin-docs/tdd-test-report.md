# Extracted-service unit test report

The extracted services contain controller, security, and exception-handler scenarios. Tests use mocked collaborators, so the service suites run without MySQL, Kafka, or another running service.

| Service | Test classes | Behaviors covered |
|---|---|---|
| Identity | Controllers, security, common handler, `IdentityTokenTests`, `InternalMerchantControllerTest` | 82 tests covering user, address, merchant, authentication, validation, and internal merchant lookup |
| Catalog | Controllers, security, common handler, `ItemServiceImplTest`, `InternalStockControllerTest`, `StockReservationReaperTest` | 85 tests covering products, shops, stock reserve/restore/confirm authorization, and reaper configuration |
| Cart | `ShoppingControllerTests`, security, common handler, `ShoppingServiceImplTest`, `InternalCartControllerTest`, `CheckoutClaimReaperTest` | 52 tests covering cart behavior, checkout commands, service-token enforcement, and recovery configuration |
| Order | `OrderControllerTests`, security, common handler, `CheckoutCoordinatorTest`, `CheckoutDecisionControllerTest` | 57 tests covering order HTTP behavior, checkout idempotency, and internal decision lookup |
| Media | `ImageControllerTests`, `ImageContentControllerTest`, security, common handler, `ImageServiceImplTest` | 38 tests covering upload/read validation and byte-content cache headers |

Run the backend unit suites with:

```bash
for service in identity catalog cart order media; do
  (cd services/${service}-service && ./mvnw -q test)
done
```

The gateway suite is run separately with `cd gateway && ./mvnw -q test`. These tests are regression coverage added after the service implementations; the repository’s existing implementation history is intentionally preserved.

Database DAO tests and multi-instance concurrency scenarios remain integration-level work: they require disposable service databases and a Compose deployment with scaled instances, rather than ordinary unit-test execution. Service-only APIs are covered with successful, authentication, and forwarding assertions; additional downstream-failure cases belong in the integration harness.
