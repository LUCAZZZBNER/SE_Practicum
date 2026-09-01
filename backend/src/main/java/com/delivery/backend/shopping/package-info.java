/** Shopping cart operations for authenticated users. */
@org.springframework.modulith.ApplicationModule(
		displayName = "Shopping",
		id = "shopping",
		allowedDependencies = { "user", "item", "restaurant" })
package com.delivery.backend.shopping;
