package com.delivery.cart.shopping.service;

import com.delivery.cart.shopping.dao.ShoppingDao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CheckoutClaimReaper {
  private final ShoppingDao dao; private final String orderUrl; private final String token;
  private final ObjectMapper json = new ObjectMapper();
  private final String workerId = java.util.UUID.randomUUID().toString();
  public CheckoutClaimReaper(ShoppingDao dao, @Value("${services.order-url:}") String orderUrl,
      @Value("${security.internal-service-token:}") String token) {
    this.dao=dao; this.orderUrl=orderUrl==null?"":orderUrl.replaceAll("/$",""); this.token=token;
  }
  @Scheduled(fixedDelayString="${checkout.claim-recovery-poll-ms:30000}")
  public void reconcile() {
    if (orderUrl.isBlank() || token.isBlank()) return;
    dao.claimReaperRows(workerId, 100);
    for (var claim: dao.findClaimed(workerId)) {
      try {
        String state=state(claim.getCheckoutId());
        if ("COMMITTED".equals(state)) dao.deleteClaimed(claim.getCheckoutId());
        else if ("ABORTED".equals(state)) dao.releaseClaimed(claim.getCheckoutId());
      } finally {
        dao.clearReaperClaim(claim.getCheckoutId(), workerId);
      }
    }
  }
  private String state(String id) {
    try {
      HttpRequest request=HttpRequest.newBuilder(URI.create(orderUrl+"/internal/v1/checkouts/"+id)).timeout(Duration.ofSeconds(3)).header("X-Service-Token",token).GET().build();
      HttpResponse<String> response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
      if(response.statusCode()/100!=2)return "UNKNOWN"; JsonNode n=json.readTree(response.body()).path("data").path("state"); return n.asText("UNKNOWN");
    } catch(Exception e){return "UNKNOWN";}
  }
}
