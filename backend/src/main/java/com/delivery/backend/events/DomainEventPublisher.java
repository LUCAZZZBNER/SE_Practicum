package com.delivery.backend.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes keyed events; callers should invoke this after their local transaction commits. */
@Component
public class DomainEventPublisher {
  private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

  public DomainEventPublisher(KafkaTemplate<String, DomainEvent> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void publish(String topic, DomainEvent event) {
    kafkaTemplate.send(topic, event.aggregateId(), event);
  }
}
