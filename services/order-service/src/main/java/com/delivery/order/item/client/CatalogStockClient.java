package com.delivery.order.item.client;

import com.delivery.order.common.ApiError;
import com.delivery.order.common.BusinessException;
import com.delivery.order.item.service.ItemService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Authenticated command client for catalog stock ownership. */
@Component
public class CatalogStockClient {
  private final HttpClient client = HttpClient.newHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();
  private final String baseUrl;
  private final String token;
  private final boolean enabled;

  public CatalogStockClient(@Value("${services.catalog-url:}") String baseUrl,
      @Value("${security.internal-service-token:}") String token) {
    this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/$", "");
    this.token = token;
    this.enabled = baseUrl != null && !baseUrl.isBlank() && token != null && !token.isBlank();
  }

  public boolean enabled() { return enabled; }

  public List<ItemService.ProductSnapshot> reserve(List<ItemService.ReservationRequest> requests) {
    HttpResponse<String> response = post("/internal/v1/catalog/stock/reserve", requests);
    if (response.statusCode() < 200 || response.statusCode() >= 300) throw error(response.body());
    try {
      JsonNode rows = mapper.readTree(response.body()).path("data");
      return java.util.stream.StreamSupport.stream(rows.spliterator(), false).map(row ->
              new ItemService.ProductSnapshot(row.path("productId").asLong(), row.path("shopId").asLong(),
              row.path("name").asText(), new java.math.BigDecimal(row.path("unitPrice").asText()),
              row.path("quantity").asInt(), row.path("version").asLong(), row.path("skuId").asLong(),
              row.path("skuName").isNull() ? null : row.path("skuName").asText(),
              row.path("imageUrl").isNull() ? null : row.path("imageUrl").asText())).toList();
    } catch (Exception exception) {
      throw new BusinessException(ApiError.INTERNAL_ERROR);
    }
  }

  public void restore(List<ItemService.StockRestore> restorations) {
    HttpResponse<String> response = post("/internal/v1/catalog/stock/restore", restorations);
    if (response.statusCode() < 200 || response.statusCode() >= 300) throw error(response.body());
  }

  public void confirm(String reservationId) {
    HttpResponse<String> response = post("/internal/v1/catalog/stock/confirm", List.of(reservationId));
    if (response.statusCode() < 200 || response.statusCode() >= 300) throw error(response.body());
  }

  private HttpResponse<String> post(String path, Object body) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
          .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
          .header("X-Service-Token", token)
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception exception) {
      throw new BusinessException(ApiError.INTERNAL_ERROR);
    }
  }

  private static BusinessException error(String body) {
    try {
      JsonNode code = new ObjectMapper().readTree(body).get("code");
      if (code != null) for (ApiError error : ApiError.values())
        if (error.code() == code.asInt()) return new BusinessException(error);
    } catch (Exception ignored) { }
    return new BusinessException(ApiError.INTERNAL_ERROR);
  }
}
