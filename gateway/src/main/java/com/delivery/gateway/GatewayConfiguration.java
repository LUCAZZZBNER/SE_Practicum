package com.delivery.gateway;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class GatewayConfiguration {
  @Bean
  RestClient.Builder restClientBuilder(
      @Value("${gateway.connect-timeout-ms:2000}") long connectTimeoutMs,
      @Value("${gateway.read-timeout-ms:10000}") long readTimeoutMs) {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs)).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
    factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
    return RestClient.builder().requestFactory(factory);
  }
}
