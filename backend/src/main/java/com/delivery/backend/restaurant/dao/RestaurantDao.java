package com.delivery.backend.restaurant.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.delivery.backend.restaurant.entity.ShopEntity;

/** MyBatis data-access contract for shops. */
@Mapper
public interface RestaurantDao {

	ShopEntity findById(@Param("id") long id);

	ShopEntity findByMerchantAndName(@Param("merchantId") long merchantId,
			@Param("name") String name);

	int insert(ShopEntity shop);

	int update(@Param("id") long id,
			@Param("nameSpecified") boolean nameSpecified,
			@Param("name") String name,
			@Param("descriptionSpecified") boolean descriptionSpecified,
			@Param("description") String description,
			@Param("statusSpecified") boolean statusSpecified,
			@Param("status") String status);

	List<ShopEntity> list(@Param("mine") boolean mine,
			@Param("merchantId") Long merchantId,
			@Param("keyword") String keyword,
			@Param("status") String status,
			@Param("sortBy") String sortBy,
			@Param("sortOrder") String sortOrder,
			@Param("limit") int limit,
			@Param("offset") int offset);

	long count(@Param("mine") boolean mine,
			@Param("merchantId") Long merchantId,
			@Param("keyword") String keyword,
			@Param("status") String status);
}
