package com.delivery.cart.projection;

import com.delivery.cart.item.dao.ItemDao;
import com.delivery.cart.item.dao.SkuDao;
import com.delivery.cart.item.entity.ProductEntity;
import com.delivery.cart.item.entity.SkuEntity;
import com.delivery.cart.merchant.shop.dao.RestaurantDao;
import com.delivery.cart.merchant.shop.entity.ShopEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Builds cart's local catalog read model from versioned catalog events. */
@Component
public class CatalogProjectionConsumer {
  private final ItemDao items; private final SkuDao skus; private final RestaurantDao shops; private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  public CatalogProjectionConsumer(ItemDao items, SkuDao skus, RestaurantDao shops){this.items=items;this.skus=skus;this.shops=shops;}
  @KafkaListener(topics="${catalog.events-topic:delivery.catalog.projections.v1}", groupId="${catalog.consumer-group:cart-catalog-projection}")
  @Transactional
  public void consume(String payload){try{JsonNode root=json.readTree(payload);switch(root.path("type").asText()){case "ShopChanged" -> shops.upsert(shop(root.path("shop")));case "ProductChanged" -> items.upsertProduct(product(root.path("product")));case "SkuChanged" -> skus.upsert(sku(root.path("sku")));default -> {}}}catch(Exception e){throw new IllegalStateException("catalog projection event rejected",e);}}
  private static ShopEntity shop(JsonNode n){ShopEntity s=new ShopEntity();s.setId(n.path("id").asLong());s.setMerchantId(n.path("merchantId").asLong());s.setName(text(n,"name"));s.setDescription(text(n,"description"));s.setStatus(text(n,"status"));s.setAddressRegion(text(n,"addressRegion"));s.setAddressDetail(text(n,"addressDetail"));s.setAddressPhone(text(n,"addressPhone"));return s;}
  private static ProductEntity product(JsonNode n){ProductEntity p=new ProductEntity();p.setId(n.path("id").asLong());p.setShopId(n.path("shopId").asLong());p.setCategoryId(n.path("categoryId").asLong());p.setName(text(n,"name"));p.setDescription(text(n,"description"));p.setPrice(n.path("price").decimalValue());p.setStock(n.path("stock").asInt());p.setStatus(text(n,"status"));p.setVersion(n.path("version").asLong());if(!n.path("imageId").isNull())p.setImageId(n.path("imageId").asLong());return p;}
  private static SkuEntity sku(JsonNode n){SkuEntity s=new SkuEntity();s.setId(n.path("id").asLong());s.setProductId(n.path("productId").asLong());s.setName(text(n,"name"));s.setPrice(n.path("price").decimalValue());s.setStock(n.path("stock").asInt());s.setStatus(text(n,"status"));s.setVersion(n.path("version").asLong());return s;}
  private static String text(JsonNode n,String key){return n.path(key).isNull()?null:n.path(key).asText();}
}
