package com.delivery.backend.shopping.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.delivery.backend.shopping.entity.CartItemEntity;

/** MyBatis data-access contract for cart items. */
@Mapper
public interface ShoppingDao {

	CartItemEntity findByUserAndProduct(@Param("userId") long userId,
			@Param("productId") long productId);

	CartItemEntity findOwnedById(@Param("userId") long userId,
			@Param("id") long id);

	List<CartItemEntity> listByUser(@Param("userId") long userId);

	List<CartItemEntity> listSelected(@Param("userId") long userId,
			@Param("ids") List<Long> ids);

	int insert(CartItemEntity item);

	int updateQuantity(@Param("userId") long userId,
			@Param("id") long id,
			@Param("quantity") int quantity);

	int deleteOwned(@Param("userId") long userId, @Param("id") long id);

	int deleteSelected(@Param("userId") long userId, @Param("ids") List<Long> ids);
}
