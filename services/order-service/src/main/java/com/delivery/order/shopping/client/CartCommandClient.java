package com.delivery.order.shopping.client;

import com.delivery.order.common.ApiError;
import com.delivery.order.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Authenticated command client for cart ownership. */
@Component
public class CartCommandClient {
  private final HttpClient client = HttpClient.newHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();
  private final String baseUrl;
  private final String token;
  private final boolean enabled;

  public CartCommandClient(@Value("${services.cart-url:}") String baseUrl,
      @Value("${security.internal-service-token:}") String token) {
    this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/$", "");
    this.token = token;
    this.enabled = baseUrl != null && !baseUrl.isBlank() && token != null && !token.isBlank();
  }

  public boolean enabled() { return enabled; }

  public void prepareForCheckout(long userId, List<Long> cartItemIds, String checkoutId) {
    postCommand("/internal/v1/cart/prepare-checkout", new CheckoutRequest(userId, cartItemIds, checkoutId));
  }

  public List<com.delivery.order.shopping.service.ShoppingService.CheckoutItem> loadForCheckout(
      long userId, List<Long> cartItemIds) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/internal/v1/cart/load-for-checkout"))
          .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
          .header("X-Service-Token", token)
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(
              new CheckoutItemsRequest(userId, cartItemIds))))
          .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) throw new BusinessException(ApiError.INTERNAL_ERROR);
      var rows = mapper.readTree(response.body()).path("data");
      java.util.ArrayList<com.delivery.order.shopping.service.ShoppingService.CheckoutItem> result = new java.util.ArrayList<>();
      rows.forEach(row -> result.add(new com.delivery.order.shopping.service.ShoppingService.CheckoutItem(
          row.path("cartItemId").asLong(), row.path("productId").asLong(), row.path("shopId").asLong(),
          row.path("quantity").asInt(), row.path("confirmedVersion").asLong(), row.path("skuId").asLong(),
          row.path("skuVersion").asLong())));
      return List.copyOf(result);
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException(ApiError.INTERNAL_ERROR);
    }
  }

  public void removeAfterCheckout(long userId, List<Long> cartItemIds, String checkoutId) {
    postCommand("/internal/v1/cart/remove-after-checkout", new CheckoutRequest(userId, cartItemIds, checkoutId));
  }

  public void releaseAfterCheckout(long userId, List<Long> cartItemIds, String checkoutId) {
    postCommand("/internal/v1/cart/release-after-checkout", new CheckoutRequest(userId, cartItemIds, checkoutId));
  }

  private void postCommand(String path, CheckoutRequest body) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
          .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
          .header("X-Service-Token", token)
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
          .build();
      HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() < 200 || response.statusCode() >= 300) throw new BusinessException(ApiError.INTERNAL_ERROR);
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException(ApiError.INTERNAL_ERROR);
    }
  }

  private record CheckoutRequest(long userId, List<Long> cartItemIds, String checkoutId) {}
  private record CheckoutItemsRequest(long userId, List<Long> cartItemIds) {}
}
