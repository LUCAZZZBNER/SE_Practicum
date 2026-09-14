package com.delivery.backend.item.image.dao;

import com.delivery.backend.item.image.entity.ImageEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImageDao {
  int insert(ImageEntity image);

  int updateUrl(
      @org.apache.ibatis.annotations.Param("id") long id,
      @org.apache.ibatis.annotations.Param("url") String url);

  ImageEntity findById(long id);
}
