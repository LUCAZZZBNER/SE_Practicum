package com.delivery.backend.restaurant;

import org.springframework.stereotype.Service;

/** Entry point for restaurant browsing and operating-status rules. */
@Service
public class RestaurantModule {
	public boolean acceptsOrders(RestaurantSummary restaurant) {
		return restaurant != null && restaurant.status() == RestaurantStatus.OPEN;
	}
}
