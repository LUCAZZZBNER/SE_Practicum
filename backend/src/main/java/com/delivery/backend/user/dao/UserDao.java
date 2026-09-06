package com.delivery.backend.user.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.delivery.backend.user.entity.UserEntity;

/** MyBatis data-access contract for users. */
@Mapper
public interface UserDao {

	UserEntity findByAccount(@Param("account") String account);

	UserEntity findById(@Param("id") long id);

	int insert(UserEntity user);

	int updateProfile(@Param("id") long id,
			@Param("nicknameSpecified") boolean nicknameSpecified,
			@Param("nickname") String nickname,
			@Param("phoneSpecified") boolean phoneSpecified,
			@Param("phone") String phone);
}
