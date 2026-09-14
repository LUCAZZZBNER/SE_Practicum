package com.delivery.identity.user.address.dao;

import com.delivery.identity.user.address.entity.UserAddressEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAddressDao {
  int insert(UserAddressEntity address);

  List<UserAddressEntity> list(@Param("userId") long userId);

  UserAddressEntity findOwned(@Param("userId") long userId, @Param("id") long id);

  int clearDefaults(@Param("userId") long userId);

  int update(
      @Param("id") long id,
      @Param("userId") long userId,
      @Param("recipientSpecified") boolean recipientSpecified,
      @Param("recipient") String recipient,
      @Param("phoneSpecified") boolean phoneSpecified,
      @Param("phone") String phone,
      @Param("regionSpecified") boolean regionSpecified,
      @Param("region") String region,
      @Param("detailSpecified") boolean detailSpecified,
      @Param("detail") String detail,
      @Param("defaultSpecified") boolean defaultSpecified,
      @Param("defaultAddress") Boolean defaultAddress);

  int softDelete(@Param("userId") long userId, @Param("id") long id);
}
