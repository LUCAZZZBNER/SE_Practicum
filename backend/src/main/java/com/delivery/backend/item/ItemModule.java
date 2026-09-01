package com.delivery.backend.item;

import org.springframework.stereotype.Service;

/** Entry point for item availability, price and inventory rules. */
@Service
public class ItemModule {
	public boolean canPurchase(ItemSummary item, int quantity) {
		return item != null && item.purchasable(quantity);
	}
}
