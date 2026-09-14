package com.delivery.order.item.dao;

import com.delivery.order.item.entity.SkuEntity;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SkuDao {
  SkuEntity findById(long id);

  List<SkuEntity> listByProduct(long productId);

  int insert(SkuEntity sku);

  int update(
      @Param("id") long id,
      @Param("version") long version,
      @Param("name") String name,
      @Param("price") BigDecimal price,
      @Param("stock") Integer stock,
      @Param("status") String status);

}
