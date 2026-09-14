package com.delivery.catalog.item.service;

import com.delivery.catalog.item.dao.StockReservationDao;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Resolves abandoned reservations from the order service's durable decision. */
@Component
public class StockReservationReaper {
  private final StockReservationDao reservations;
  private final ItemService items;
  private final String orderUrl;
  private final String token;
  private final ObjectMapper json = new ObjectMapper();
  private final String workerId = java.util.UUID.randomUUID().toString();

  public StockReservationReaper(StockReservationDao reservations, ItemService items,
      @Value("${services.order-url:}") String orderUrl,
      @Value("${security.internal-service-token:}") String token) {
    this.reservations = reservations; this.items = items;
    this.orderUrl = orderUrl == null ? "" : orderUrl.replaceAll("/$", ""); this.token = token;
  }

  @Scheduled(fixedDelayString = "${stock.reservation-expiry-poll-ms:30000}")
  public void reconcile() {
    if (orderUrl.isBlank() || token.isBlank()) return;
    reservations.claimReaperRows(workerId, 100);
    for (var row : reservations.findClaimed(workerId)) {
      try {
        String state = decision(row.getReservationId());
        if ("COMMITTED".equals(state)) {
          items.confirmReservations(List.of(row.getReservationId()));
        } else if ("ABORTED".equals(state)) {
          items.restoreStock(List.of(new ItemService.StockRestore(row.getProductId(), row.getQuantity(), row.getSkuId(), row.getReservationId())));
        }
      } finally {
        reservations.clearReaperClaim(row.getReservationId(), workerId);
      }
    }
  }

  private String decision(String id) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(orderUrl + "/internal/v1/checkouts/" + id))
          .timeout(Duration.ofSeconds(3)).header("X-Service-Token", token).GET().build();
      HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() / 100 != 2) return "UNKNOWN";
      JsonNode state = json.readTree(response.body()).path("data").path("state");
      return state.asText("UNKNOWN");
    } catch (Exception ignored) { return "UNKNOWN"; }
  }
}
