package com.delivery.backend.item.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public interface SkuService {
	SkuView create(long merchantId, long productId, CreateRequest request);
	SkuView update(long merchantId, long skuId, UpdateRequest request);
	List<SkuView> list(long productId, boolean includeOffSale, Long merchantId);

	record CreateRequest(@NotBlank String name, @NotNull @DecimalMin("0.01") BigDecimal price,
		@jakarta.validation.constraints.PositiveOrZero int stock) {}

	final class UpdateRequest {
		private String name;
		private BigDecimal price;
		private Integer stock;
		@Pattern(regexp = "ON_SALE|OFF_SALE") private String status;
		@Positive private Long version;
		public String name(){return name;} public void setName(String v){name=v;}
		public BigDecimal price(){return price;} public void setPrice(BigDecimal v){price=v;}
		public Integer stock(){return stock;} public void setStock(Integer v){stock=v;}
		public String status(){return status;} public void setStatus(String v){status=v;}
		public Long version(){return version;} public void setVersion(Long v){version=v;}
	}

	record SkuView(long id, long productId, String name, BigDecimal price, int stock, String status,
		long version, Instant createdAt, Instant updatedAt) {}
}
