package com.delivery.media.item.image.service.impl;

import com.delivery.media.common.ApiError;
import com.delivery.media.common.BusinessException;
import com.delivery.media.item.image.dao.ImageDao;
import com.delivery.media.item.image.entity.ImageEntity;
import com.delivery.media.item.image.service.ImageService;
import com.delivery.media.merchant.IdentityMerchantDirectory;
import java.io.IOException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageServiceImpl implements ImageService {
  private static final long MAX_SIZE = 5 * 1024 * 1024;
  private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");
  private final ImageDao imageDao;
  private final IdentityMerchantDirectory merchantService;

  public ImageServiceImpl(ImageDao dao, IdentityMerchantDirectory merchants) {
    this.imageDao = dao;
    this.merchantService = merchants;
  }

  @Override
  public ImageView upload(long merchantId, MultipartFile file) {
    merchantService.requireActive(merchantId);
    if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE)
      throw new BusinessException(ApiError.IMAGE_INVALID);
    String type = file.getContentType();
    if (!TYPES.contains(type)) throw new BusinessException(ApiError.IMAGE_INVALID);
    try {
      byte[] bytes = file.getBytes();
      if (bytes.length < 4) throw new BusinessException(ApiError.IMAGE_INVALID);
      if (type.equals("image/jpeg") && !(bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8))
        throw new BusinessException(ApiError.IMAGE_INVALID);
      if (type.equals("image/png")
          && !(bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'))
        throw new BusinessException(ApiError.IMAGE_INVALID);
      if (type.equals("image/webp")
          && !(bytes[0] == 'R'
              && bytes[1] == 'I'
              && bytes[2] == 'F'
              && bytes[3] == 'F'
              && bytes.length > 11
              && bytes[8] == 'W'
              && bytes[9] == 'E'
              && bytes[10] == 'B'
              && bytes[11] == 'P')) throw new BusinessException(ApiError.IMAGE_INVALID);
      ImageEntity image = new ImageEntity();
      image.setContentType(type);
      image.setSize(file.getSize());
      image.setContent(bytes);
      image.setUrl("/uploads/products/pending-" + System.nanoTime());
      imageDao.insert(image);
      image.setUrl("/uploads/products/" + image.getId());
      imageDao.updateUrl(image.getId(), image.getUrl());
      return toView(image);
    } catch (IOException ex) {
      throw new BusinessException(ApiError.IMAGE_INVALID);
    }
  }

  @Override
  public ImageView require(long id) {
    ImageEntity image = imageDao.findById(id);
    if (image == null) throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
    return toView(image);
  }

  private static ImageView toView(ImageEntity image) {
    return new ImageView(
        image.getId(),
        image.getUrl(),
        image.getContentType(),
        image.getSize(),
        image.getCreatedAt());
  }

  @Override
  public Content content(long id) {
    ImageEntity image = imageDao.findById(id);
    if (image == null || image.getContent() == null)
      throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);
    return new Content(image.getContent(), image.getContentType());
  }
}
