package com.delivery.media;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class MediaConfiguration {
  @Bean RestClient.Builder restClientBuilder() { return RestClient.builder(); }
}
