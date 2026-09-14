package com.delivery.identity.merchant.dao;

import com.delivery.identity.merchant.entity.MerchantEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis data-access contract for merchants. */
@Mapper
public interface MerchantDao {

  MerchantEntity findByAccount(@Param("account") String account);

  MerchantEntity findById(@Param("id") long id);

  int insert(MerchantEntity merchant);

  int updateProfile(
      @Param("id") long id,
      @Param("nameSpecified") boolean nameSpecified,
      @Param("name") String name,
      @Param("phoneSpecified") boolean phoneSpecified,
      @Param("phone") String phone);
}
