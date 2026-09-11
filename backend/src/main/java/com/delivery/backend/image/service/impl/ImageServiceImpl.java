package com.delivery.backend.image.service.impl;
import java.io.IOException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.BusinessException;
import com.delivery.backend.image.dao.ImageDao;
import com.delivery.backend.image.entity.ImageEntity;
import com.delivery.backend.image.service.ImageService;
import com.delivery.backend.merchant.service.MerchantService;
@Service public class ImageServiceImpl implements ImageService {
 private static final long MAX_SIZE=5*1024*1024; private static final Set<String> TYPES=Set.of("image/jpeg","image/png","image/webp");
 private final ImageDao dao; private final MerchantService merchants; public ImageServiceImpl(ImageDao dao,MerchantService merchants){this.dao=dao;this.merchants=merchants;}
 @Override public ImageView upload(long merchantId,MultipartFile file){merchants.requireActive(merchantId); if(file==null||file.isEmpty()||file.getSize()>MAX_SIZE) throw new BusinessException(ApiError.IMAGE_INVALID); String type=file.getContentType(); if(!TYPES.contains(type)) throw new BusinessException(ApiError.IMAGE_INVALID); try {byte[] bytes=file.getBytes(); if(bytes.length<4) throw new BusinessException(ApiError.IMAGE_INVALID); if(type.equals("image/jpeg") && !(bytes[0]==(byte)0xff&&bytes[1]==(byte)0xd8)) throw new BusinessException(ApiError.IMAGE_INVALID); if(type.equals("image/png") && !(bytes[0]==(byte)0x89&&bytes[1]=='P'&&bytes[2]=='N'&&bytes[3]=='G')) throw new BusinessException(ApiError.IMAGE_INVALID); ImageEntity e=new ImageEntity(); e.setContentType(type);e.setSize(file.getSize());e.setUrl("/uploads/products/pending-"+System.nanoTime());dao.insert(e);e.setUrl("/uploads/products/"+e.getId());return toView(e);} catch(IOException ex){throw new BusinessException(ApiError.IMAGE_INVALID);} }
 @Override public ImageView require(long id){ImageEntity e=dao.findById(id);if(e==null)throw new BusinessException(ApiError.RESOURCE_NOT_FOUND);return toView(e);} private static ImageView toView(ImageEntity e){return new ImageView(e.getId(),e.getUrl(),e.getContentType(),e.getSize(),e.getCreatedAt());}
}
