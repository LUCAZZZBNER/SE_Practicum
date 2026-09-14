package com.delivery.cart.shopping.dao;

import com.delivery.cart.shopping.entity.CartItemEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis data-access contract for cart items. */
@Mapper
public interface ShoppingDao {
  class Claimed { private String checkoutId; public String getCheckoutId(){return checkoutId;} public void setCheckoutId(String v){checkoutId=v;} }

  CartItemEntity findByUserAndProduct(
      @Param("userId") long userId, @Param("productId") long productId);

  CartItemEntity findByUserAndSku(@Param("userId") long userId, @Param("skuId") long skuId);

  CartItemEntity findOwnedById(@Param("userId") long userId, @Param("id") long id);

  List<CartItemEntity> listByUser(@Param("userId") long userId);

  List<CartItemEntity> listSelected(@Param("userId") long userId, @Param("ids") List<Long> ids);
  int claimSelected(@Param("userId") long userId, @Param("ids") List<Long> ids, @Param("checkoutId") String checkoutId);

  int insert(CartItemEntity item);

  int updateQuantity(
      @Param("userId") long userId, @Param("id") long id, @Param("quantity") int quantity);

  int incrementQuantity(
      @Param("userId") long userId,
      @Param("id") long id,
      @Param("addition") int addition,
      @Param("maximum") int maximum);

  int deleteOwned(@Param("userId") long userId, @Param("id") long id);

  int deleteSelected(@Param("userId") long userId, @Param("ids") List<Long> ids, @Param("checkoutId") String checkoutId);
  int releaseSelected(@Param("userId") long userId, @Param("ids") List<Long> ids, @Param("checkoutId") String checkoutId);
  int claimReaperRows(@Param("worker") String worker, @Param("limit") int limit);
  List<Claimed> findClaimed(@Param("worker") String worker);
  int clearReaperClaim(@Param("checkoutId") String checkoutId, @Param("worker") String worker);
  int deleteClaimed(@Param("checkoutId") String checkoutId);
  int releaseClaimed(@Param("checkoutId") String checkoutId);
}
