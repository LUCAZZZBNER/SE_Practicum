package com.delivery.backend.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.item.dao.ItemDao;
import com.delivery.backend.item.entity.CategoryEntity;
import com.delivery.backend.item.entity.ProductEntity;
import com.delivery.backend.merchant.service.MerchantService;
import com.delivery.backend.restaurant.service.RestaurantService;

@SpringBootTest
@Transactional
class ItemDaoIntegrationTests {

	@Autowired
	private ItemDao itemDao;
	@Autowired
	private MerchantService merchantService;
	@Autowired
	private RestaurantService restaurantService;

	@Test
	void productUpdateRequiresTheExpectedVersionAndIncrementsExactlyOnce() {
		ProductEntity product = product("item-dao-version", "OFF_SALE", 5);

		int first = itemDao.updateProduct(product.getId(), 1,
				false, null, true, "Updated Rice", false, null,
				false, null, false, null, false, null);
		int stale = itemDao.updateProduct(product.getId(), 1,
				false, null, true, "Stale Update", false, null,
				false, null, false, null, false, null);

		ProductEntity updated = itemDao.findProductById(product.getId());
		assertThat(first).isEqualTo(1);
		assertThat(stale).isZero();
		assertThat(updated.getName()).isEqualTo("Updated Rice");
		assertThat(updated.getVersion()).isEqualTo(2);
	}

	@Test
	void stockReservationIsAtomicAndNeverMakesStockNegative() {
		ProductEntity product = product("item-dao-stock", "ON_SALE", 2);

		int completeReservation = itemDao.reserveStock(product.getId(), 1, 2);
		int insufficientReservation = itemDao.reserveStock(product.getId(), 1, 1);

		assertThat(completeReservation).isEqualTo(1);
		assertThat(insufficientReservation).isZero();
		assertThat(itemDao.findProductById(product.getId()).getStock()).isZero();
	}

	private ProductEntity product(String account, String status, int stock) {
		long merchantId = merchantService.register(new MerchantService.RegisterRequest(account,
				"ExamplePass123!", "ExamplePass123!", "Store", "13900000000")).id();
		long shopId = restaurantService.create(merchantId,
				new RestaurantService.CreateRequest("Shop " + account, null)).id();

		CategoryEntity category = new CategoryEntity();
		category.setShopId(shopId);
		category.setName("Meals");
		category.setSortOrder(0);
		itemDao.insertCategory(category);

		ProductEntity product = new ProductEntity();
		product.setShopId(shopId);
		product.setCategoryId(category.getId());
		product.setName("Rice");
		product.setPrice(new BigDecimal("12.50"));
		product.setStock(stock);
		product.setStatus(status);
		product.setVersion(1L);
		itemDao.insertProduct(product);
		return product;
	}
}
