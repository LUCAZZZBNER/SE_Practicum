package com.delivery.identity.merchant.controller;

import com.delivery.identity.merchant.service.MerchantService;
import com.delivery.identity.security.PublicEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PublicEndpoint
@RequestMapping("/internal/v1/merchants")
public class InternalMerchantController {
  private final MerchantService merchantService;

  public InternalMerchantController(MerchantService merchantService) {
    this.merchantService = merchantService;
  }

  @GetMapping("/{merchantId}")
  public MerchantSnapshot get(@PathVariable long merchantId) {
    MerchantService.MerchantSnapshot merchant = merchantService.requireActive(merchantId);
    return new MerchantSnapshot(merchant.id(), merchant.status());
  }

  public record MerchantSnapshot(long id, String status) {}
}
