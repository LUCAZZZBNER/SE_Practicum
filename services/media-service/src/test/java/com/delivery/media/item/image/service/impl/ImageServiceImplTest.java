package com.delivery.media.item.image.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.delivery.media.common.ApiError;
import com.delivery.media.common.BusinessException;
import com.delivery.media.item.image.dao.ImageDao;
import com.delivery.media.merchant.IdentityMerchantDirectory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageServiceImplTest {
  private final ImageDao images = mock(ImageDao.class);
  private final IdentityMerchantDirectory merchants = mock(IdentityMerchantDirectory.class);
  private final ImageServiceImpl service = new ImageServiceImpl(images, merchants);

  @Test
  void rejectsUnsupportedContentTypesBeforePersistingBytes() {
    var file = new MockMultipartFile("file", "payload.gif", "image/gif", new byte[] {1, 2, 3, 4});
    assertThatThrownBy(() -> service.upload(3L, file))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).error()).isEqualTo(ApiError.IMAGE_INVALID);
    verify(merchants).requireActive(3L);
    verifyNoInteractions(images);
  }

  @Test
  void reportsMissingImageContentAsNotFound() {
    when(images.findById(99L)).thenReturn(null);
    assertThatThrownBy(() -> service.content(99L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).error()).isEqualTo(ApiError.RESOURCE_NOT_FOUND);
  }
}
