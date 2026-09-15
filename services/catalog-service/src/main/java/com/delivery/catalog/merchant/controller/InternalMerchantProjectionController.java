package com.delivery.catalog.merchant.controller;

import com.delivery.catalog.common.ApiError;
import com.delivery.catalog.common.ApiResponse;
import com.delivery.catalog.common.BusinessException;
import com.delivery.catalog.merchant.dao.MerchantDao;
import com.delivery.catalog.merchant.entity.MerchantEntity;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/catalog/merchants")
public class InternalMerchantProjectionController {
  private final MerchantDao dao; private final String token;
  public InternalMerchantProjectionController(MerchantDao dao,@Value("${security.internal-service-token:}") String token){this.dao=dao;this.token=token;}
  @PostMapping
  public ApiResponse<Void> upsert(@RequestHeader(value="X-Service-Token",required=false) String supplied,@RequestBody Projection p){
    if(token.isBlank()||supplied==null||!MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8))) throw new BusinessException(ApiError.UNAUTHENTICATED);
    MerchantEntity m=new MerchantEntity();m.setId(p.id());m.setAccount(p.account());m.setName(p.name());m.setPhone(p.phone());m.setStatus(p.status());m.setCreatedAt(p.createdAt());m.setUpdatedAt(p.updatedAt());dao.upsert(m);return ApiResponse.success(null);
  }
  public record Projection(long id,String account,String name,String phone,String status,Instant createdAt,Instant updatedAt){}
}
