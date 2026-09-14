package com.delivery.catalog.item.dao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface StockReservationDao {
  class Expired {
    private String reservationId; private long productId; private long skuId; private int quantity;
    public String getReservationId() { return reservationId; }
    public void setReservationId(String value) { reservationId = value; }
    public long getProductId() { return productId; }
    public void setProductId(long value) { productId = value; }
    public long getSkuId() { return skuId; }
    public void setSkuId(long value) { skuId = value; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int value) { quantity = value; }
  }
  Integer findQuantity(@Param("reservationId") String reservationId, @Param("productId") long productId, @Param("skuId") long skuId);
  int insert(@Param("reservationId") String reservationId, @Param("productId") long productId, @Param("skuId") long skuId, @Param("quantity") int quantity);
  int claimRestore(@Param("reservationId") String reservationId, @Param("productId") long productId, @Param("skuId") long skuId);
  int claimExpired(@Param("reservationId") String reservationId, @Param("productId") long productId, @Param("skuId") long skuId);
  int confirm(@Param("reservationId") String reservationId);
  int claimReaperRows(@Param("worker") String worker, @Param("limit") int limit);
  java.util.List<Expired> findClaimed(@Param("worker") String worker);
  int clearReaperClaim(@Param("reservationId") String reservationId, @Param("worker") String worker);
}
