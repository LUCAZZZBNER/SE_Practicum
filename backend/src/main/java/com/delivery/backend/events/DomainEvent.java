package com.delivery.backend.events;

import java.time.Instant;
import java.util.UUID;

/** Versioned event envelope shared by producers and consumers. */
public record DomainEvent(
    UUID eventId,
    String eventType,
    String aggregateType,
    String aggregateId,
    Instant occurredAt,
    String correlationId,
    int schemaVersion,
    Object payload) {
  public static DomainEvent of(
      String eventType,
      String aggregateType,
      String aggregateId,
      String correlationId,
      Object payload) {
    return new DomainEvent(
        UUID.randomUUID(),
        eventType,
        aggregateType,
        aggregateId,
        Instant.now(),
        correlationId,
        1,
        payload);
  }
}
