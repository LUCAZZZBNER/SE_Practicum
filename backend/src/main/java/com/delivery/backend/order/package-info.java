/** Order creation, querying and state transitions. */
@org.springframework.modulith.ApplicationModule(
		displayName = "Order",
		id = "order",
		allowedDependencies = { "user", "shopping", "item", "restaurant" })
package com.delivery.backend.order;
