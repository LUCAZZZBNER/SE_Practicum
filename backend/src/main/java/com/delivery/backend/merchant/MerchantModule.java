package com.delivery.backend.merchant;

import org.springframework.stereotype.Service;

/** Entry point for merchant registration and access checks. */
@Service
public class MerchantModule {
	public boolean canManageResources(MerchantProfile merchant) {
		return merchant != null && merchant.status() == MerchantStatus.ACTIVE;
	}
}
