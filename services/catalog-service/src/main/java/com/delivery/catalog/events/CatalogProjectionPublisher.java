package com.delivery.catalog.events;

import com.delivery.catalog.item.entity.ProductEntity;
import com.delivery.catalog.item.entity.SkuEntity;
import com.delivery.catalog.merchant.shop.entity.ShopEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CatalogProjectionPublisher {
  private final CatalogProjectionEventDao outbox; private final KafkaTemplate<String,String> kafka; private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final String worker = UUID.randomUUID().toString();
  public CatalogProjectionPublisher(CatalogProjectionEventDao outbox, KafkaTemplate<String,String> kafka){this.outbox=outbox;this.kafka=kafka;}
  public void shop(ShopEntity s){ Map<String,Object> v=new HashMap<>();v.put("id",s.getId());v.put("merchantId",s.getMerchantId());v.put("name",s.getName());v.put("description",s.getDescription());v.put("status",s.getStatus());v.put("addressRegion",s.getAddressRegion());v.put("addressDetail",s.getAddressDetail());v.put("addressPhone",s.getAddressPhone()); enqueue(s.getId(), Map.of("type","ShopChanged","version",version(s.getUpdatedAt()),"shop",v)); }
  public void product(ProductEntity p){ Map<String,Object> v=new HashMap<>();v.put("id",p.getId());v.put("shopId",p.getShopId());v.put("categoryId",p.getCategoryId());v.put("name",p.getName());v.put("description",p.getDescription());v.put("price",p.getPrice());v.put("stock",p.getStock());v.put("status",p.getStatus());v.put("version",p.getVersion());v.put("imageId",p.getImageId()); enqueue(p.getId(), Map.of("type","ProductChanged","version",p.getVersion(),"product",v)); }
  public void sku(SkuEntity s){ Map<String,Object> v=new HashMap<>();v.put("id",s.getId());v.put("productId",s.getProductId());v.put("name",s.getName());v.put("price",s.getPrice());v.put("stock",s.getStock());v.put("status",s.getStatus());v.put("version",s.getVersion()); enqueue(s.getId(), Map.of("type","SkuChanged","version",s.getVersion(),"sku",v)); }
  private void enqueue(long id,Object body){try{CatalogProjectionEvent e=new CatalogProjectionEvent();e.setEventId(UUID.randomUUID().toString());e.setAggregateId(id);e.setPayload(json.writeValueAsString(body));outbox.insert(e);}catch(Exception ex){throw new IllegalStateException("catalog projection event failed",ex);}}
  private static long version(Instant v){return v==null?System.currentTimeMillis():v.toEpochMilli();}
  @Scheduled(fixedDelayString="${events.outbox-poll-ms:1000}") public void publishPending(){outbox.claimReady(worker,50);for(CatalogProjectionEvent e:outbox.findClaimed(worker)){try{kafka.send("delivery.catalog.projections.v1",Long.toString(e.getAggregateId()),e.getPayload()).whenComplete((r,x)->{if(x==null)outbox.markPublished(e.getId(),worker);});}catch(RuntimeException x){outbox.markAttempt(e.getId(),worker);}}}
}
