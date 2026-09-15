package com.delivery.backend.image.dao;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import com.delivery.backend.image.entity.ImageEntity;
@Mapper
@Repository("legacyImageDao")
public interface ImageDao { int insert(ImageEntity image); int updateUrl(@org.apache.ibatis.annotations.Param("id") long id, @org.apache.ibatis.annotations.Param("url") String url); ImageEntity findById(long id); ImageEntity findOwned(@org.apache.ibatis.annotations.Param("merchantId") long merchantId, @org.apache.ibatis.annotations.Param("id") long id); }
