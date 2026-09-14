package com.delivery.media.merchant;

import com.delivery.media.common.ApiError;
import com.delivery.media.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Cross-service merchant authorization; media never reads the identity database. */
@Component
public class IdentityMerchantDirectory {
  private final RestClient client;
  private final String serviceToken;

  public IdentityMerchantDirectory(
      RestClient.Builder builder,
      @Value("${identity.internal-url:http://identity-service:8080}") String identityUrl,
      @Value("${security.internal-service-token}") String serviceToken) {
    this.client = builder.baseUrl(identityUrl).build();
    this.serviceToken = serviceToken;
  }

  public void requireActive(long merchantId) {
    try {
      MerchantSnapshot snapshot =
          client
              .get()
              .uri("/internal/v1/merchants/{id}", merchantId)
              .header("X-Service-Token", serviceToken)
              .retrieve()
              .body(MerchantSnapshot.class);
      if (snapshot == null) throw new BusinessException(ApiError.MERCHANT_SUSPENDED);
    } catch (RestClientResponseException exception) {
      HttpStatusCode status = exception.getStatusCode();
      if (status.value() == 404) throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
      if (status.value() == 403) throw new BusinessException(ApiError.MERCHANT_SUSPENDED);
      if (status.value() == 401) throw new BusinessException(ApiError.UNAUTHENTICATED);
      throw new BusinessException(ApiError.INTERNAL_ERROR);
    }
  }

  private record MerchantSnapshot(long id, String status) {}
}
