package com.delivery.order.item.service.impl;

import com.delivery.order.common.ApiError;
import com.delivery.order.common.BusinessException;
import com.delivery.order.item.dao.ItemDao;
import com.delivery.order.item.dao.SkuDao;
import com.delivery.order.item.entity.ProductEntity;
import com.delivery.order.item.entity.SkuEntity;
import com.delivery.order.item.service.SkuService;
import com.delivery.order.merchant.shop.service.RestaurantService;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkuServiceImpl implements SkuService {
  private static final String ON_SALE = "ON_SALE";
  private static final String OFF_SALE = "OFF_SALE";

  private final SkuDao skuDao;
  private final ItemDao itemDao;
  private final RestaurantService restaurantService;

  public SkuServiceImpl(SkuDao dao, ItemDao items, RestaurantService shops) {
    this.skuDao = dao;
    this.itemDao = items;
    this.restaurantService = shops;
  }

  @Override
  @Transactional
  public SkuView create(long merchantId, long productId, CreateRequest request) {
    ProductEntity product = requireProduct(productId);
    restaurantService.requireOwned(merchantId, product.getShopId());
    SkuEntity sku = new SkuEntity();
    sku.setProductId(productId);
    sku.setName(request.name().trim());
    sku.setPrice(request.price());
    sku.setStock(request.stock());
    sku.setStatus(OFF_SALE);
    sku.setVersion(1L);
    try {
      skuDao.insert(sku);
    } catch (DuplicateKeyException e) {
      throw new BusinessException(ApiError.RESOURCE_CONFLICT);
    }
    return view(sku);
  }

  @Override
  @Transactional
  public SkuView update(long merchantId, long skuId, UpdateRequest request) {
    SkuEntity current = skuDao.findById(skuId);
    if (current == null) throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
    ProductEntity product = requireProduct(current.getProductId());
    restaurantService.requireOwned(merchantId, product.getShopId());
    if (request.version() == null || request.version() <= 0)
      throw new BusinessException(ApiError.VALIDATION_ERROR);
    String name = request.name() == null ? current.getName() : request.name().trim();
    var price = request.price() == null ? current.getPrice() : request.price();
    var stock = request.stock() == null ? current.getStock() : request.stock();
    var status = request.status() == null ? current.getStatus() : request.status();
    if (name.isBlank()
        || price == null
        || price.signum() <= 0
        || stock < 0
        || (!"ON_SALE".equals(status) && !"OFF_SALE".equals(status)))
      throw new BusinessException(ApiError.VALIDATION_ERROR);
    if (skuDao.update(skuId, request.version(), name, price, stock, status) != 1)
      throw new BusinessException(ApiError.SKU_VERSION_CONFLICT);
    return view(skuDao.findById(skuId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SkuView> list(long productId, boolean includeOffSale, Long merchantId) {
    ProductEntity product = requireProduct(productId);
    if (includeOffSale) {
      if (merchantId == null) throw new BusinessException(ApiError.FORBIDDEN);
      restaurantService.requireOwned(merchantId, product.getShopId());
    }
    return skuDao.listByProduct(productId).stream()
        .filter(s -> includeOffSale || ON_SALE.equals(s.getStatus()))
        .map(SkuServiceImpl::view)
        .toList();
  }

  private ProductEntity requireProduct(long id) {
    ProductEntity product = itemDao.findProductById(id);
    if (product == null) throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
    return product;
  }

  private static SkuView view(SkuEntity s) {
    return new SkuView(
        s.getId(),
        s.getProductId(),
        s.getName(),
        s.getPrice(),
        s.getStock(),
        s.getStatus(),
        s.getVersion(),
        s.getCreatedAt(),
        s.getUpdatedAt());
  }
}
