package com.delivery.backend.image.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.backend.image.service.ImageService;

@RestController
public class ImageContentController {
	private final ImageService service;
	public ImageContentController(ImageService service) { this.service = service; }

	@GetMapping("/uploads/products/{imageId}")
	public ResponseEntity<byte[]> content(@PathVariable long imageId) {
		ImageService.Content content = service.content(imageId);
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(content.contentType()))
				.header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable").body(content.bytes());
	}
}
