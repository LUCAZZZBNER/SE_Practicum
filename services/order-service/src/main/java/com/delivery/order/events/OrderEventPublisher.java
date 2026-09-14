package com.delivery.order.events;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.delivery.order.item.client.CatalogStockClient;
import com.delivery.order.item.service.ItemService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrderEventPublisher {
  private final KafkaTemplate<String, String> kafka;
  private final OrderEventOutboxDao outbox;
  private final CatalogStockClient catalog;
  private final StockReleaseOutboxDao releases;
  private final ObjectMapper json = new ObjectMapper();
  private final String workerId = java.util.UUID.randomUUID().toString();

  public OrderEventPublisher(KafkaTemplate<String, String> kafka, OrderEventOutboxDao outbox, CatalogStockClient catalog, StockReleaseOutboxDao releases) {
    this.kafka = kafka;
    this.outbox = outbox;
    this.catalog = catalog;
    this.releases = releases;
  }

  public void releaseStock(String reservationId, java.util.List<ItemService.StockRestore> rows) {
    try {
      StockReleaseOutbox row = new StockReleaseOutbox(); row.setReservationId(reservationId);
      row.setPayload(json.writeValueAsString(rows)); releases.insert(row);
    } catch (Exception failure) { throw new com.delivery.order.common.BusinessException(com.delivery.order.common.ApiError.INTERNAL_ERROR); }
  }

  public void orderCreated(long orderId, long userId, String reservationId) {
    String eventId = UUID.randomUUID().toString();
    String payload = "{\"eventId\":\"" + eventId + "\",\"type\":\"OrderCreated\",\"orderId\":" + orderId + ",\"userId\":" + userId + ",\"occurredAt\":\"" + Instant.now() + "\",\"schemaVersion\":1}";
    OrderEventOutbox event = new OrderEventOutbox();
    event.setEventId(eventId);
    event.setOrderId(orderId);
    event.setUserId(userId);
    event.setReservationId(reservationId);
    event.setPayload(payload);
    outbox.insert(event);
  }

  @Scheduled(fixedDelayString = "${events.outbox-poll-ms:1000}")
  public void publishPending() {
    outbox.claimReady(workerId, 50);
    List<OrderEventOutbox> pending = outbox.findClaimed(workerId);
    for (OrderEventOutbox event : pending) {
      try {
        // Confirm the lease independently of Kafka. A broker outage must not
        // cause a committed order's stock reservation to expire.
        catalog.confirm(event.getReservationId());
      } catch (RuntimeException ignored) {
        outbox.markAttempt(event.getId(), workerId);
        continue;
      }
      try {
        kafka.send("delivery.order.events.v1", Long.toString(event.getOrderId()), event.getPayload())
            .whenComplete((result, error) -> { if (error == null) outbox.markPublished(event.getId(), workerId); });
      } catch (RuntimeException ignored) { outbox.markAttempt(event.getId(), workerId); /* retry from the durable outbox */ }
    }
    releases.claimReady(workerId, 50);
    for (StockReleaseOutbox row : releases.findClaimed(workerId)) {
      try {
        java.util.List<ItemService.StockRestore> restorations = json.readerForListOf(ItemService.StockRestore.class).readValue(row.getPayload());
        catalog.restore(restorations);
        releases.markCompleted(row.getId(), workerId);
      } catch (Exception ignored) { releases.markAttempt(row.getId(), workerId); /* retry with exponential backoff */ }
    }
  }
}
