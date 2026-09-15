package com.delivery.identity.merchant.client;

import com.delivery.identity.merchant.service.MerchantService.MerchantView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CatalogMerchantProjectionClient {
  private final String url, token; private final ObjectMapper json=new ObjectMapper().registerModule(new JavaTimeModule());
  public CatalogMerchantProjectionClient(@Value("${catalog.internal-url:}") String url,@Value("${security.internal-service-token:}") String token){this.url=url==null?"":url.replaceAll("/$","");this.token=token;}
  public void upsert(MerchantView m){if(url.isBlank()||token==null||token.isBlank())return;try{HttpRequest r=HttpRequest.newBuilder(URI.create(url+"/internal/v1/catalog/merchants")).header("Content-Type","application/json").header("X-Service-Token",token).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(m))).build();HttpResponse<Void> response=HttpClient.newHttpClient().send(r,HttpResponse.BodyHandlers.discarding());if(response.statusCode()<200||response.statusCode()>=300)throw new IllegalStateException("projection failed");}catch(Exception e){throw new IllegalStateException("catalog projection failed",e);}}
}
