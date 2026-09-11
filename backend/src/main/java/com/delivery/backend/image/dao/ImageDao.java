package com.delivery.backend.image.dao;
import org.apache.ibatis.annotations.Mapper;
import com.delivery.backend.image.entity.ImageEntity;
@Mapper public interface ImageDao { int insert(ImageEntity image); ImageEntity findById(long id); }
