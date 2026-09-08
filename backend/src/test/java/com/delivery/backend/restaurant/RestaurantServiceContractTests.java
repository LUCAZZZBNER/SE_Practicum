package com.delivery.backend.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.ServiceContractTestSupport;
import com.delivery.backend.common.ApiError;
import com.delivery.backend.common.PageResult;
import com.delivery.backend.merchant.service.MerchantService;
import com.delivery.backend.restaurant.service.RestaurantService;

@SpringBootTest
@Transactional
class RestaurantServiceContractTests extends ServiceContractTestSupport {

	@Autowired
	private RestaurantService service;
	@Autowired
	private MerchantService merchantService;

	@Test
	void createDefaultsToClosedAndRejectsDuplicateNameForTheSameMerchant() {
		long merchantId = merchant("shop-owner-a").id();

		RestaurantService.ShopView shop = service.create(merchantId,
				new RestaurantService.CreateRequest("Rice Shop", null));
		assertThat(shop.id()).isPositive();
		assertThat(shop.merchantId()).isEqualTo(merchantId);
		assertThat(shop.status()).isEqualTo("CLOSED");
		assertBusinessError(ApiError.RESOURCE_CONFLICT, () -> service.create(merchantId,
				new RestaurantService.CreateRequest("Rice Shop", "duplicate")));
	}

	@Test
	void publicAndMineListsApplyPagingFilteringAndOwnership() {
		long merchantId = merchant("shop-owner-b").id();
		service.create(merchantId, new RestaurantService.CreateRequest("Noodle Shop", null));

		PageResult<RestaurantService.ShopView> mine = service.list(
				new RestaurantService.ListQuery(1, 10, "Noodle", "CLOSED", true, "name", "asc", merchantId));
		assertThat(mine.page()).isEqualTo(1);
		assertThat(mine.pageSize()).isEqualTo(10);
		assertThat(mine.items()).allMatch(shop -> shop.merchantId() == merchantId);

		PageResult<RestaurantService.ShopView> publiclyVisible = service.list(
				new RestaurantService.ListQuery(1, 10, null, null, false, null, null, null));
		assertThat(publiclyVisible.items()).allMatch(shop -> shop.status().equals("OPEN"));
	}

	@Test
	void onlyTheOwnerCanUpdateAndRequireOwnedReturnsTheSnapshot() {
		long ownerId = merchant("shop-owner-c").id();
		long otherId = merchant("shop-other-c").id();
		RestaurantService.ShopView shop = service.create(ownerId,
				new RestaurantService.CreateRequest("Owner Shop", null));
		RestaurantService.UpdateRequest update = new RestaurantService.UpdateRequest();
		update.setDescription("updated");

		assertThat(service.update(ownerId, shop.id(), update).description()).isEqualTo("updated");
		assertThat(service.get(shop.id()).description()).isEqualTo("updated");
		assertThat(service.requireOwned(ownerId, shop.id()).merchantId()).isEqualTo(ownerId);
		assertBusinessError(ApiError.FORBIDDEN, () -> service.update(otherId, shop.id(), update));
	}

	@Test
	void onlyOpenShopsAreOrderable() {
		long merchantId = merchant("shop-owner-d").id();
		RestaurantService.ShopView shop = service.create(merchantId,
				new RestaurantService.CreateRequest("Orderable Shop", null));
		assertBusinessError(ApiError.SHOP_NOT_OPEN, () -> service.requireOrderable(shop.id()));

		RestaurantService.UpdateRequest open = new RestaurantService.UpdateRequest();
		open.setStatus("OPEN");
		service.update(merchantId, shop.id(), open);
		assertThat(service.requireOrderable(shop.id()).status()).isEqualTo("OPEN");
	}

	private MerchantService.MerchantView merchant(String account) {
		return merchantService.register(new MerchantService.RegisterRequest(account, "ExamplePass123!",
				"ExamplePass123!", "Store", "13900000000"));
	}
}
