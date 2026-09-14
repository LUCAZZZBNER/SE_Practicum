package com.delivery.backend.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfiguration {
  @Bean NewTopic catalogEvents() { return new NewTopic("delivery.catalog.events.v1", 3, (short) 1); }
  @Bean NewTopic cartEvents() { return new NewTopic("delivery.cart.events.v1", 3, (short) 1); }
  @Bean NewTopic orderEvents() { return new NewTopic("delivery.order.events.v1", 3, (short) 1); }
  @Bean NewTopic paymentEvents() { return new NewTopic("delivery.payment.events.v1", 3, (short) 1); }
  @Bean NewTopic deadLetterEvents() { return new NewTopic("delivery.events.dead-letter.v1", 3, (short) 1); }
}
