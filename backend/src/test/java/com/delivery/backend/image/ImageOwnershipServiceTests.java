package com.delivery.backend.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.ServiceContractTestSupport;
import com.delivery.backend.common.ApiError;
import com.delivery.backend.image.service.ImageService;
import com.delivery.backend.item.service.ItemService;
import com.delivery.backend.item.service.SkuService;
import com.delivery.backend.merchant.service.MerchantService;
import com.delivery.backend.restaurant.service.RestaurantService;

@SpringBootTest
@Transactional
class ImageOwnershipServiceTests extends ServiceContractTestSupport {

	@Autowired
	private ImageService imageService;
	@Autowired
	private ItemService itemService;
	@Autowired
	private MerchantService merchantService;
	@Autowired
	private RestaurantService restaurantService;

	@Test
	void merchantCanUseAnImageUploadedByItself() {
		MerchantFixture owner = merchant("image-owner");
		long imageId = upload(owner.merchantId());

		ItemService.ProductView product = createProduct(owner, imageId, "Owner Product");

		assertThat(product.image()).isNotNull();
		assertThat(product.image().id()).isEqualTo(imageId);
	}

	@Test
	void merchantCannotUseAnotherMerchantsImage() {
		MerchantFixture owner = merchant("image-source");
		MerchantFixture attacker = merchant("image-attacker");
		long imageId = upload(owner.merchantId());

		assertBusinessError(ApiError.RESOURCE_NOT_FOUND,
				() -> createProduct(attacker, imageId, "Foreign Image Product"));
	}

	private MerchantFixture merchant(String label) {
		String suffix = label + "-" + System.nanoTime();
		long merchantId = merchantService.register(new MerchantService.RegisterRequest("m-" + suffix,
				"ExamplePass123!", "ExamplePass123!", "Store", "13900000000")).id();
		long shopId = restaurantService.create(merchantId,
				new RestaurantService.CreateRequest("Shop " + suffix, null)).id();
		long categoryId = itemService.createCategory(merchantId, shopId,
				new ItemService.CreateCategoryRequest("Meals", 0)).id();
		return new MerchantFixture(merchantId, shopId, categoryId);
	}

	private long upload(long merchantId) {
		byte[] webpHeader = new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
		MockMultipartFile file = new MockMultipartFile("file", "product.webp", "image/webp", webpHeader);
		return imageService.upload(merchantId, file).id();
	}

	private ItemService.ProductView createProduct(MerchantFixture fixture, long imageId, String name) {
		return itemService.createProduct(fixture.merchantId(), new ItemService.CreateProductRequest(
				fixture.shopId(), fixture.categoryId(), name, null, new BigDecimal("12.50"), 5, imageId,
				List.of(new SkuService.CreateRequest("默认规格", new BigDecimal("12.50"), 5))));
	}

	private record MerchantFixture(long merchantId, long shopId, long categoryId) {
	}
}
