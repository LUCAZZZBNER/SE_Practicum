package com.delivery.gateway;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import static java.util.Map.entry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Compatibility gateway. Internal services can replace the target without changing the client API. */
@RestController
public class ApiGatewayController {
  private static final Set<String> FORWARDED_HEADERS =
      Set.of(
          "Authorization",
          "Content-Type",
          "Accept",
          "Idempotency-Key",
          "X-Idempotency-Key",
          "If-Match");

  private final RestClient restClient;
  private final String backendBaseUrl;
  private final Map<String, String> serviceUrls;

  public ApiGatewayController(
      RestClient.Builder restClientBuilder,
      @Value("${gateway.backend-base-url:http://localhost:8080}") String backendBaseUrl,
      @Value("${gateway.identity-url:}") String identityUrl,
      @Value("${gateway.catalog-url:}") String catalogUrl,
      @Value("${gateway.cart-url:}") String cartUrl,
      @Value("${gateway.order-url:}") String orderUrl,
      @Value("${gateway.media-url:}") String mediaUrl) {
    this.restClient = restClientBuilder.build();
    this.backendBaseUrl = backendBaseUrl;
    this.serviceUrls =
        Map.ofEntries(
            entry("users", identityUrl), entry("merchants", identityUrl), entry("user-addresses", identityUrl),
            entry("shops", catalogUrl), entry("products", catalogUrl), entry("skus", catalogUrl),
            entry("categories", catalogUrl), entry("cart-items", cartUrl), entry("orders", orderUrl),
            entry("merchant", orderUrl), entry("files", mediaUrl));
  }

  @RequestMapping("/api/v1/**")
  public ResponseEntity<byte[]> forward(HttpServletRequest request) throws IOException {
    String path = request.getRequestURI();
    String query = request.getQueryString();
    String baseUrl = targetBaseUrl(path);
    if (baseUrl == null || baseUrl.isBlank()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    URI target = URI.create(baseUrl + path + (query == null ? "" : "?" + query));
    byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
    String correlationId =
        request.getHeader("X-Correlation-Id") == null
            ? UUID.randomUUID().toString()
            : request.getHeader("X-Correlation-Id");

    RestClient.RequestBodySpec spec =
        restClient.method(HttpMethod.valueOf(request.getMethod())).uri(target);
    if (request.getContentType() != null) {
      spec.contentType(MediaType.parseMediaType(request.getContentType()));
    }
    spec.header("X-Correlation-Id", correlationId);
    Collections.list(request.getHeaderNames()).stream()
        .filter(FORWARDED_HEADERS::contains)
        .forEach(name -> Collections.list(request.getHeaders(name)).forEach(value -> spec.header(name, value)));

    RestClient.RequestHeadersSpec<?> requestSpec = body.length == 0 ? spec : spec.body(body);
    try {
      return requestSpec.exchange(
        (ignored, response) -> {
          HttpHeaders headers = new HttpHeaders();
          List<String> contentTypes = response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
          if (contentTypes != null) headers.put(HttpHeaders.CONTENT_TYPE, contentTypes);
          List<String> locations = response.getHeaders().get(HttpHeaders.LOCATION);
          if (locations != null) headers.put(HttpHeaders.LOCATION, locations);
          headers.set("X-Correlation-Id", correlationId);
          byte[] responseBody = response.getBody() == null ? new byte[0] : response.getBody().readAllBytes();
          HttpStatusCode status = response.getStatusCode();
          return ResponseEntity.status(status).headers(headers).body(responseBody);
        });
    } catch (ResourceAccessException unavailable) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "upstream service unavailable", unavailable);
    }
  }

  private String targetBaseUrl(String path) {
    String[] segments = path.split("/");
    String candidate = segments.length > 3 ? segments[3] : "";
    String configured = serviceUrls.get(candidate);
    return configured == null || configured.isBlank() ? backendBaseUrl : configured;
  }
}
