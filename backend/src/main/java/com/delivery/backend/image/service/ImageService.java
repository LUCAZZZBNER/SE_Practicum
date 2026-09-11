package com.delivery.backend.image.service;
import java.time.Instant;
import org.springframework.web.multipart.MultipartFile;
public interface ImageService { ImageView upload(long merchantId, MultipartFile file); ImageView require(long imageId); record ImageView(long id,String url,String contentType,long size,Instant createdAt) {} }
