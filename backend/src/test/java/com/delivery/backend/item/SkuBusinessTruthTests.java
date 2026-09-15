package com.delivery.backend.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.backend.item.service.ItemService;
import com.delivery.backend.item.service.SkuService;
import com.delivery.backend.merchant.service.MerchantService;
import com.delivery.backend.restaurant.service.RestaurantService;
import com.delivery.backend.shopping.service.ShoppingService;
import com.delivery.backend.user.service.UserService;

@SpringBootTest
@Transactional
class SkuBusinessTruthTests {

	@Autowired
	private ItemService itemService;
	@Autowired
	private SkuService skuService;
	@Autowired
	private ShoppingService shoppingService;
	@Autowired
	private UserService userService;
	@Autowired
	private MerchantService merchantService;
	@Autowired
	private RestaurantService restaurantService;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private SqlSession sqlSession;

	@Test
	void productAndCartDerivePriceAndStockFromSkuWhenLegacyColumnsDisagree() {
		Fixture fixture = fixture("sku-truth-view");
		jdbcTemplate.update("UPDATE products SET price=999.00, stock=0, version=999 WHERE id=?",
				fixture.productId());
		sqlSession.clearCache();

		ItemService.ProductView product = itemService.getProduct(fixture.productId(), true, fixture.merchantId());
		assertThat(product.minPrice()).isEqualByComparingTo("12.50");
		assertThat(product.stock()).isEqualTo(5);
		assertThat(product.inStock()).isTrue();

		ShoppingService.CartItemView item = shoppingService.add(fixture.userId(),
				new ShoppingService.AddRequest(fixture.skuId(), 2)).item();
		assertThat(item.sku()).isNotNull();
		assertThat(item.sku().price()).isEqualByComparingTo("12.50");
		assertThat(item.sku().stock()).isEqualTo(5);
		assertThat(item.subtotal()).isEqualByComparingTo("25.00");
		assertThat(item.available()).isTrue();
	}

	@Test
	void reservationAndRestorationChangeOnlySkuStock() {
		Fixture fixture = fixture("sku-truth-stock");
		jdbcTemplate.update("UPDATE products SET stock=77 WHERE id=?", fixture.productId());
		sqlSession.clearCache();

		List<ItemService.ProductSnapshot> snapshots = itemService.reserveForOrder(List.of(
				new ItemService.ReservationRequest(fixture.productId(), fixture.skuVersion(), 2, fixture.skuId())));
		assertThat(snapshots).singleElement().satisfies(snapshot -> {
			assertThat(snapshot.skuId()).isEqualTo(fixture.skuId());
			assertThat(snapshot.unitPrice()).isEqualByComparingTo("12.50");
		});
		assertThat(skuStock(fixture.skuId())).isEqualTo(3);
		assertThat(productStock(fixture.productId())).isEqualTo(77);

		itemService.restoreStock(List.of(new ItemService.StockRestore(fixture.productId(), 2, fixture.skuId())));
		assertThat(skuStock(fixture.skuId())).isEqualTo(5);
		assertThat(productStock(fixture.productId())).isEqualTo(77);
	}

	private Fixture fixture(String label) {
		String suffix = label + "-" + System.nanoTime();
		long userId = userService.register(new UserService.RegisterRequest("u-" + suffix,
				"ExamplePass123!", "ExamplePass123!", "User", null)).id();
		long merchantId = merchantService.register(new MerchantService.RegisterRequest("m-" + suffix,
				"ExamplePass123!", "ExamplePass123!", "Merchant", "13900000000")).id();
		RestaurantService.ShopView shop = restaurantService.create(merchantId,
				new RestaurantService.CreateRequest("Shop " + suffix, null));
		restaurantService.updateAddress(merchantId, shop.id(),
				new RestaurantService.AddressRequest("杭州", "学院路", "05711234567"));
		RestaurantService.UpdateRequest open = new RestaurantService.UpdateRequest();
		open.setStatus("OPEN");
		restaurantService.update(merchantId, shop.id(), open);
		long categoryId = itemService.createCategory(merchantId, shop.id(),
				new ItemService.CreateCategoryRequest("Meals", 0)).id();
		long imageId = insertImage(merchantId);
		ItemService.ProductView product = itemService.createProduct(merchantId,
				new ItemService.CreateProductRequest(shop.id(), categoryId, "Rice", null,
						new BigDecimal("12.50"), 5, imageId,
						List.of(new SkuService.CreateRequest("默认规格", new BigDecimal("12.50"), 5))));
		ItemService.UpdateProductRequest productUpdate = new ItemService.UpdateProductRequest();
		productUpdate.setStatus("ON_SALE");
		productUpdate.setVersion(product.version());
		product = itemService.updateProduct(merchantId, product.id(), productUpdate);
		SkuService.UpdateRequest skuUpdate = new SkuService.UpdateRequest();
		skuUpdate.setStatus("ON_SALE");
		skuUpdate.setVersion(product.skus().get(0).version());
		SkuService.SkuView sku = skuService.update(merchantId, product.skus().get(0).id(), skuUpdate);
		return new Fixture(userId, merchantId, product.id(), sku.id(), sku.version());
	}

	private long insertImage(long merchantId) {
		jdbcTemplate.update("INSERT INTO images(merchant_id,url,content_type,size) VALUES(?, '/uploads/test.webp','image/webp',4)", merchantId);
		return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}

	private int skuStock(long skuId) {
		return jdbcTemplate.queryForObject("SELECT stock FROM product_skus WHERE id=?", Integer.class, skuId);
	}

	private int productStock(long productId) {
		return jdbcTemplate.queryForObject("SELECT stock FROM products WHERE id=?", Integer.class, productId);
	}

	private record Fixture(long userId, long merchantId, long productId, long skuId, long skuVersion) {
	}
}
