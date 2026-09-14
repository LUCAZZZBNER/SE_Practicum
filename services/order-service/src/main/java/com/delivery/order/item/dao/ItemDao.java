package com.delivery.order.item.dao;

import com.delivery.order.item.entity.CategoryEntity;
import com.delivery.order.item.entity.ProductEntity;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis data-access contract for categories, products, and atomic stock changes. */
@Mapper
public interface ItemDao {

  CategoryEntity findCategoryById(@Param("id") long id);

  CategoryEntity findCategoryByShopAndName(
      @Param("shopId") long shopId, @Param("name") String name);

  List<CategoryEntity> listCategories(@Param("shopId") long shopId);

  int insertCategory(CategoryEntity category);

  int updateCategory(
      @Param("id") long id,
      @Param("nameSpecified") boolean nameSpecified,
      @Param("name") String name,
      @Param("sortOrderSpecified") boolean sortOrderSpecified,
      @Param("sortOrder") Integer sortOrder);

  int logicalDeleteCategory(@Param("id") long id);

  long countProductsByCategory(@Param("categoryId") long categoryId);

  ProductEntity findProductById(@Param("id") long id);

  int insertProduct(ProductEntity product);

  int updateProduct(
      @Param("id") long id,
      @Param("expectedVersion") long expectedVersion,
      @Param("categoryIdSpecified") boolean categoryIdSpecified,
      @Param("categoryId") Long categoryId,
      @Param("nameSpecified") boolean nameSpecified,
      @Param("name") String name,
      @Param("descriptionSpecified") boolean descriptionSpecified,
      @Param("description") String description,
      @Param("priceSpecified") boolean priceSpecified,
      @Param("price") BigDecimal price,
      @Param("stockSpecified") boolean stockSpecified,
      @Param("stock") Integer stock,
      @Param("statusSpecified") boolean statusSpecified,
      @Param("status") String status,
      @Param("imageIdSpecified") boolean imageIdSpecified,
      @Param("imageId") Long imageId);

  default int updateProduct(
      Long id,
      long version,
      boolean categoryIdSpecified,
      Long categoryId,
      boolean nameSpecified,
      String name,
      boolean descriptionSpecified,
      String description,
      boolean priceSpecified,
      java.math.BigDecimal price,
      boolean stockSpecified,
      Integer stock,
      boolean statusSpecified,
      String status) {
    return updateProduct(
        id,
        version,
        categoryIdSpecified,
        categoryId,
        nameSpecified,
        name,
        descriptionSpecified,
        description,
        priceSpecified,
        price,
        stockSpecified,
        stock,
        statusSpecified,
        status,
        false,
        null);
  }

  List<ProductEntity> listProducts(
      @Param("shopId") long shopId,
      @Param("categoryId") Long categoryId,
      @Param("keyword") String keyword,
      @Param("includeOffSale") boolean includeOffSale,
      @Param("sortBy") String sortBy,
      @Param("sortOrder") String sortOrder,
      @Param("limit") int limit,
      @Param("offset") long offset);

  long countProducts(
      @Param("shopId") long shopId,
      @Param("categoryId") Long categoryId,
      @Param("keyword") String keyword,
      @Param("includeOffSale") boolean includeOffSale);

}
