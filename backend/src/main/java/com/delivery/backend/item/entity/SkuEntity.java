package com.delivery.backend.item.entity;

import java.math.BigDecimal;
import java.time.Instant;

public class SkuEntity {
	private Long id;
	private Long productId;
	private String name;
	private BigDecimal price;
	private Integer stock;
	private String status;
	private Long version;
	private Instant createdAt;
	private Instant updatedAt;
	public Long getId(){return id;} public void setId(Long v){id=v;}
	public Long getProductId(){return productId;} public void setProductId(Long v){productId=v;}
	public String getName(){return name;} public void setName(String v){name=v;}
	public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){price=v;}
	public Integer getStock(){return stock;} public void setStock(Integer v){stock=v;}
	public String getStatus(){return status;} public void setStatus(String v){status=v;}
	public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
	public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
	public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
