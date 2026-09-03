# 轻量级外卖服务平台后端架构设计

## 1. 架构目标

后端采用 Java 17、Spring Boot 和 Spring Modulith，以单进程、单数据库部署。`user`、`merchant`、`restaurant`、`item`、`shopping`、`order` 是手册规定的六个核心业务领域，而不是模块数量上限；实现可按职责继续拆分或增加模块。设计目标是：模块边界可自动验证；HTTP、业务和持久化职责清晰；核心下单流程具备事务一致性；后续规则变化可以局部扩展，而不需要大面积改动。

业务边界见[功能分析](../feature-analysis.md)，外部 HTTP 契约见[后端 API 设计](../api/backend-api-design.md)。

`BackendApplication` 是唯一启动入口。六个核心领域及后续新增的独立业务模块使用 `@ApplicationModule` 声明边界。框架配置、统一响应、异常处理、认证上下文等技术代码放入 `com.delivery.backend.config`、`security`、`web` 等包，并配置 `spring.modulith.detection-strategy=explicitly-annotated`，避免纯技术包被误识别为业务模块。

## 2. 模块关系

依赖只能沿箭头方向发生，禁止反向引用或循环依赖：

```text
merchant   ──→ user
restaurant ──→ merchant
item       ──→ merchant, restaurant
shopping   ──→ user, restaurant, item
order      ──→ user, restaurant, item, shopping
```

其中 `A ──→ B` 表示 A 可以调用 B 的公开接口。

六个核心领域的基线依赖如下：

| 调用方 | 允许依赖 |
| --- | --- |
| `user` | 无业务模块 |
| `merchant` | `user::api` |
| `restaurant` | `merchant::api` |
| `item` | `merchant::api`, `restaurant::api` |
| `shopping` | `user::api`, `restaurant::api`, `item::api` |
| `order` | `user::api`, `restaurant::api`, `item::api`, `shopping::api` |

每个模块通过 `package-info.java` 的 `allowedDependencies` 固化该表，并由 `ApplicationModules.of(BackendApplication.class).verify()` 在测试中验证。

新增模块必须拥有单一、可说明的业务职责及明确的数据或流程所有权，并提供命名公开接口。调用方将该接口加入 `allowedDependencies`；新依赖不得形成环。支付、优惠、通知等后续能力可以成为独立模块，无需塞入六个核心领域之一。

## 3. 模块内部结构

所有业务模块使用相同结构，而不是建立全局 `controller/service/mapper` 目录：

```text
com.delivery.backend.<module>/
├── package-info.java             # @ApplicationModule、允许依赖
├── api/
│   ├── package-info.java         # @NamedInterface("api")
│   ├── <Module>Facade.java       # 模块唯一用例入口
│   ├── command/                  # 不可变 Command
│   ├── query/                    # 查询条件
│   ├── view/                     # 跨边界只读 DTO
│   └── event/                    # 可选领域事件
├── web/                          # Controller、Request、Response
├── application/                  # Facade 实现、事务和用例编排
├── domain/                       # Entity、值对象、规则、仓储端口
└── infrastructure/persistence/   # MyBatis Mapper、PO、仓储实现
```

调用方向固定为 `Controller → Facade/Application Service → Domain/Repository Port → Persistence Adapter`。Controller 只处理协议转换、Bean Validation 和当前身份；业务判断位于领域对象或应用服务；Mapper 只执行数据访问。模块外不得引用 `web`、`application`、`domain` 或 `infrastructure` 包。

`api` 包使用 Spring Modulith 命名接口。例如：

```java
@org.springframework.modulith.NamedInterface("api")
package com.delivery.backend.user.api;
```

现有模块根包中的 `*Module`、`*Summary` 和状态枚举应在实现相应用例时迁移为 `api` 下的 facade 或只读契约；不应继续扩大根包暴露面。

## 4. 六个核心业务领域设计

### 4.1 User

**数据所有权**：`users`；密码摘要和账号状态仅由本模块修改。

**公开 facade**：

```java
public interface UserFacade {
    UserView register(RegisterUserCommand command);
    AuthSession login(LoginCommand command);
    UserView getCurrent(long userId);
    UserView updateCurrent(long userId, UpdateUserCommand command);
    UserSnapshot requireActive(long userId);
}
```

前四个方法服务用户 HTTP 用例；其他模块只调用 `requireActive`，且只能得到 `id`、`account`、`status` 等非敏感快照。

**内部接口**：`UserRepository` 提供按 ID/账号查询、账号存在性检查和保存；`PasswordHasher` 负责摘要与校验；`TokenIssuer` 负责令牌签发。`UserRegistrationService`、`AuthenticationService` 和 `ProfileService` 分别编排注册、登录和资料维护。

### 4.2 Merchant

**数据所有权**：`merchants`；一个 `userId` 最多关联一个商家。

```java
public interface MerchantFacade {
    MerchantView register(long userId, RegisterMerchantCommand command);
    MerchantView getCurrent(long userId);
    MerchantSnapshot requireActiveMerchant(long userId);
}
```

注册先调用 `UserFacade.requireActive`。`requireActiveMerchant` 是店铺、商品管理的跨模块入口，返回 `merchantId`、`userId` 和状态，不暴露商家实体。

**内部接口**：`MerchantRepository` 提供按 ID/用户 ID 查询、重复检查和保存；`MerchantRegistrationService` 负责创建档案；`MerchantAccessPolicy` 统一判断 `ACTIVE`/`SUSPENDED` 权限。

### 4.3 Restaurant

**数据所有权**：`shops`。Java 使用 Restaurant 命名，HTTP 和数据库使用 Shop 语义，避免与商家 Merchant 混淆。

```java
public interface RestaurantFacade {
    ShopView create(long userId, CreateShopCommand command);
    ShopView update(long userId, long shopId, UpdateShopCommand command);
    PageView<ShopView> list(ShopQuery query);
    ShopView get(long shopId);
    ShopSnapshot requireOwned(long userId, long shopId);
    ShopSnapshot requireOrderable(long shopId);
}
```

`create`、`update` 和 `requireOwned` 调用 `MerchantFacade.requireActiveMerchant`；`requireOrderable` 供购物车和订单模块校验 `OPEN` 状态。

**内部接口**：`ShopRepository` 提供分页、名称冲突检查、查询和保存；`ShopOwnershipPolicy` 校验归属；`ShopStatusPolicy` 管理合法状态与可下单规则；`ShopCommandService`、`ShopQueryService` 分离写用例和只读查询。

### 4.4 Item

**数据所有权**：`categories`、`products` 和产品库存；历史成交价格不属于本模块。

```java
public interface ItemFacade {
    CategoryView createCategory(long userId, long shopId, CreateCategoryCommand command);
    CategoryView updateCategory(long userId, long categoryId, UpdateCategoryCommand command);
    void deleteCategory(long userId, long categoryId);
    List<CategoryView> listCategories(long shopId);
    ProductView createProduct(long userId, CreateProductCommand command);
    ProductView updateProduct(long userId, long productId, UpdateProductCommand command);
    PageView<ProductView> listProducts(ProductQuery query, Viewer viewer);
    ProductView getProduct(long productId, Viewer viewer);
    ProductSnapshot requirePurchasable(long productId, int quantity);
    List<ProductSnapshot> reserveForOrder(List<PurchaseRequest> requests);
    void restoreStock(List<StockAdjustment> adjustments);
}
```

管理操作通过 Merchant 和 Restaurant facade 校验经营身份及店铺归属。`reserveForOrder` 按 product ID 固定顺序锁定行、校验版本/上架状态/库存、原子扣减并返回名称和成交价格快照；任一商品失败则抛出异常并由外层事务回滚。

**内部接口**：`CategoryRepository`、`ProductRepository`、`StockRepository`；`ProductAccessPolicy`、`PurchasabilityPolicy`、`PricingPolicy`；分类、商品命令服务和商品查询服务。持久化更新库存必须使用行锁或带版本条件的原子 SQL，绝不执行“先读后无条件写”。

### 4.5 Shopping

**数据所有权**：`cart_items`；同一用户和商品建立唯一约束。

```java
public interface ShoppingFacade {
    CartItemView add(long userId, AddCartItemCommand command);
    CartView getCart(long userId);
    CartItemView changeQuantity(long userId, long cartItemId, int quantity);
    void remove(long userId, long cartItemId);
    CheckoutCart loadForCheckout(long userId, List<Long> cartItemIds);
    void removeAfterCheckout(long userId, List<Long> cartItemIds);
}
```

所有操作先调用 `UserFacade.requireActive`。新增和修改数量通过 Restaurant、Item facade 校验店铺和商品；读取购物车时组合最新商品摘要并标记不可用项。跨模块 `CheckoutCart` 只包含 `cartItemId`、`productId`、`shopId`、`quantity`，不把购物车展示价格当作成交价。

**内部接口**：`CartItemRepository` 提供本人范围查询、合并数量、批量查询和删除；`CartOwnershipPolicy` 校验归属；`CartCommandService`、`CartQueryService`、`CheckoutCartService` 承担各用例。

### 4.6 Order

**数据所有权**：`orders`、`order_lines`、`order_idempotency`。订单明细保存商品名称、单价、数量快照。

```java
public interface OrderFacade {
    OrderView create(long userId, String idempotencyKey, CreateOrderCommand command);
    PageView<OrderSummaryView> listMine(long userId, OrderQuery query);
    OrderView getMine(long userId, long orderId);
    OrderView cancel(long userId, long orderId);
}
```

Order 是结算流程的编排者，不允许其他模块直接创建订单记录。`OrderRepository` 提供订单和明细保存、本人查询以及带锁状态读取；`IdempotencyRepository` 保存用户、幂等键、请求摘要和订单 ID；`OrderNumberGenerator` 生成全局唯一业务编号；`OrderStatePolicy` 管理合法迁移；`OrderPricingService` 只根据 Item 返回的快照计算总额。

## 5. 关键跨模块流程

### 5.1 创建订单

`OrderApplicationService.create` 是事务边界，按以下顺序同步调用：

```text
校验并占用幂等键
  → UserFacade.requireActive
  → ShoppingFacade.loadForCheckout
  → 校验选中项来自同一店铺
  → RestaurantFacade.requireOrderable
  → ItemFacade.reserveForOrder（锁定、校验版本并扣库存）
  → 保存 Order 与 OrderLine 快照
  → ShoppingFacade.removeAfterCheckout
  → 提交事务并记录幂等结果
```

六个模块共用同一数据源和 Spring 事务管理器，因此任何异常都会回滚订单、明细、库存、购物车和幂等记录。模块事件不能替代该同步一致性链路。

### 5.2 取消订单

Order 模块锁定订单并验证所有者和 `PENDING_PAYMENT → CANCELLED` 迁移，调用 `ItemFacade.restoreStock`，再更新订单状态和取消时间。状态条件更新保证并发请求只有一个成功，库存仅恢复一次；失败时整个事务回滚。

## 6. 数据与边界规则

- 每张表由一个模块拥有；其他模块不得引用其 Mapper、PO 或 Repository。
- 跨模块关联只保存标量 ID，不建立跨模块实体对象图。删除采用逻辑删除，订单历史不级联删除。
- Controller 使用 Request/Response；应用层使用 Command/View；持久化层使用 PO。三类对象不得混用。
- 统一 `ApiResponse<T>`、`PageView<T>`、业务异常映射和 `CurrentPrincipal` 属于技术基础设施，不承载业务规则。
- 密码、令牌、数据库异常和堆栈不得写入接口响应。账号、库存和状态冲突映射为稳定业务码。

## 7. 领域事件与扩展

可发布 `MerchantRegistered`、`ShopStatusChanged`、`ProductChanged`、`OrderCreated`、`OrderCancelled` 事件，用于日志、缓存失效或后续通知。事件类放在发布模块的 `api.event` 中，使用只读 ID 和必要快照。阶段 1 的库存、购物车和订单一致性仍使用同步接口；非关键监听器应在事务提交后执行，失败不能反向破坏已完成业务。

## 8. 测试与架构守卫

- `ApplicationModules.verify()`：验证核心及新增模块的结构、允许依赖和无循环。
- 每个 facade 使用 JUnit 5 编写领域/应用服务单元测试，覆盖正常、异常、边界和权限路径。
- 每个 Controller 使用 MockMvc 验证请求校验、认证、状态码、业务码和 JSON 契约。
- 使用数据库集成测试验证账号唯一约束、购物车唯一约束、乐观锁和库存原子更新。
- 下单与取消必须有事务回滚、并发库存、重复幂等键和重复取消测试。
- 核心接口覆盖率 100%，关键业务方法覆盖率不低于 90%；需求变化时先补测试，再扩展策略或 facade 实现。
