# 轻量级外卖服务平台后端架构设计

## 1. 架构目标与范围

后端是 Java 17、Spring Boot 单体应用，采用简单 MVC 分层和单数据库部署。六个业务模块 `user`、`merchant`、`restaurant`、`item`、`shopping`、`order` 保持独立目录、数据所有权和用例边界；这六个模块是阶段 1 的固定基线。支付、优惠、通知等后续能力在具有独立职责和数据所有权时可以增加模块。

本设计与[后端 API 设计](../api/backend-api-design.md)配套：API 文档定义 `/api/v1` 路径、资源名称、权限、字段、状态码和错误码，本文件定义代码组织和调用规则；两者冲突时以 API 契约为准。本文只描述目标架构，不表示功能已经实现。

不使用 Spring Modulith。项目不声明 `@ApplicationModule`、`@NamedInterface` 或 `ApplicationModules.verify()`，也不依赖 Modulith 的模块检测和事件边界。模块隔离通过包结构、依赖约定、代码评审和测试实现。

### 1.1 技术栈

| 层次 | 选型与约束 |
| --- | --- |
| 语言与运行时 | Java 17 |
| 应用框架 | Spring Boot 4.1.1、Spring MVC（`spring-boot-starter-webmvc`） |
| 构建工具 | Maven，使用仓库提供的 `./mvnw` |
| 持久化与数据库连接 | MyBatis 负责 Entity/数据库记录映射和 SQL 执行，MySQL Connector/J 提供数据库连接；每个模块只使用自己的 Mapper/DAO |
| JSON 与校验 | Spring MVC 的 JSON 转换和 Bean Validation；请求校验在 Controller 层完成 |
| 测试 | JUnit 5、Spring Boot Test、MockMvc；数据库集成测试使用项目配置的数据源 |
| 开发辅助 | Lombok（仅用于减少样板代码，不承载业务规则） |
| 认证 | HS256 JWT Bearer 访问令牌；令牌解析和当前主体由 `security` 基础设施提供，用户与商家角色独立 |

Spring Modulith 不属于目标技术栈；即使构建文件或本地环境暂时保留相关旧依赖，也不得在新代码或架构判断中使用它。

## 2. 总体结构

请求经过统一的 Web 和安全基础设施后进入对应业务模块：

```text
HTTP /api/v1
    ↓
Controller（请求校验、认证、响应包装）
    ↓
Service 接口 → ServiceImpl（业务规则、事务、跨模块编排）
    ↓
DAO 接口 → DAO 实现（SQL、分页、锁和持久化）
    ↓
Database
```

推荐的包布局如下；每个模块都必须有自己的 Controller、Service（接口和实现）以及 DAO，不建立全局的 `controller`、`service` 或 `dao` 大目录：

```text
com.delivery.backend/
├── common/                         # ApiResponse、Page、异常、结果映射
├── security/                       # token 解析、CurrentPrincipal、权限拦截器
├── config/                         # 数据源、事务、JSON 和 Bean Validation 配置
├── user/
│   ├── controller/                 # UserController
│   ├── service/                    # UserService + UserServiceImpl
│   ├── dao/                        # UserDao + 实现
│   ├── entity/                     # UserEntity
│   └── dto/                        # Request/Response/内部传输对象
├── merchant/                       # 同样的 controller/service/dao/entity/dto
├── restaurant/
├── item/
├── shopping/
└── order/
```

Controller 只负责 HTTP 方法、路径参数、Bean Validation、当前主体和 `ApiResponse<T>`；不得写库存、状态迁移或归属判断。Service 接口是模块的业务入口，`ServiceImpl` 实现规则、事务和对其他模块 Service 接口的调用。DAO/Mapper 只负责 MyBatis 持久化，不调用其他模块，也不返回 HTTP 对象。Entity 只在所属模块内部使用；跨模块传递不可变 DTO 或标量 ID，禁止共享 Entity、DAO 或数据库 Mapper。

统一异常处理（例如 `@RestControllerAdvice`）将参数、认证、权限、资源不存在、冲突和未预期异常映射为 API 文档规定的 `code`、`msg`、`data` 和 HTTP 状态；不得返回堆栈、密码、令牌或数据库细节。

`security` 基础设施使用 HS256 验证 JWT 签名及 `exp`，读取 `sub` 和 `role` 后生成不可变 `CurrentPrincipal`。拦截器只处理 `/api/v1/**`：公开接口可跳过认证，可选认证接口在令牌存在时验证令牌，其余接口要求 Bearer 令牌并检查 `USER` 或 `MERCHANT` 角色。验证后的主体通过请求属性 `currentPrincipal` 交给 Controller；Controller 只把其中的可信 ID 传给 Service，不接受请求体中的主体 ID 代替认证。签名密钥由 `JWT_SECRET` 配置，默认有效期为 7200 秒并可由 `JWT_EXPIRATION_SECONDS` 调整；部署环境必须覆盖开发占位密钥。

## 3. 模块依赖与协作规则

模块间允许调用对方公开的 Service 接口和 DTO，禁止直接引用对方的 Controller、ServiceImpl、DAO、Entity 或包内实现。依赖方向按业务需要保持无环：

```text
merchant   （独立账号，无业务模块依赖）
restaurant → merchant
item       → merchant, restaurant
shopping   → user, restaurant, item
order      → user, restaurant, item, shopping
```

商家注册和登录使用自己的账号表与 `MerchantService`，不要求先创建普通用户；因此商家和用户的认证会话、角色和密码摘要相互独立。通用认证解析属于 `security` 基础设施，不构成商家对用户业务模块的依赖。

新增模块必须说明单一业务职责、拥有的表和公开 Service 接口；调用方只依赖该接口，并在文档中补充依赖方向。不得为了复用 DAO 或 Entity 而增加反向依赖或循环依赖。

## 4. 六个核心模块

### 4.1 `user` 用户模块

数据所有权：`users`，包括账号、昵称、联系方式、密码摘要、状态和时间字段。

Controller 对应：`POST /users`、`POST /users/login`、`GET /users/me`、`PATCH /users/me`。`UserService`/`UserServiceImpl` 负责注册、登录、当前资料查询和局部更新，并签发 `USER` 会话。对外只返回 API 定义的 `User` 和 `AuthSession`，绝不返回密码摘要。

可供其他模块调用的只读方法应返回 `UserSnapshot`（ID、状态等必要字段），用于校验用户存在且为 `ACTIVE`。`UserDao` 负责账号唯一查询、按 ID 查询和保存；密码哈希、令牌签发等可由本模块 Service 调用安全基础设施完成。

### 4.2 `merchant` 商家模块

数据所有权：`merchants`，账号与普通用户独立，`account` 全局唯一，状态为 `ACTIVE` 或 `SUSPENDED`。

Controller 对应：`POST /merchants`、`POST /merchants/login`、`GET /merchants/me`、`PATCH /merchants/me`。`MerchantService`/`MerchantServiceImpl` 负责独立注册、登录、资料查询和更新；注册成功返回 `201`，登录会话带 `MERCHANT` 角色。暂停商家不能管理店铺、分类或商品，但历史数据可读。

公开的 `requireActiveMerchant(merchantId)` 或同等方法只返回商家快照，供店铺和商品模块进行经营权限校验。`MerchantDao` 只处理账号/ID 查询、唯一性检查和保存。

### 4.3 `restaurant` 店铺模块

数据所有权：`shops`，每条记录保存所属 `merchantId`、名称、简介、状态和时间字段；状态为 `OPEN`、`CLOSED` 或 `TEMPORARILY_CLOSED`。

Controller 对应：`POST /shops`、`GET /shops`、`GET /shops/{shopId}`、`PATCH /shops/{shopId}`。`RestaurantService`/`RestaurantServiceImpl` 校验商家状态和店铺归属，处理公开列表、`mine=true` 列表、详情和状态更新。列表分页从 1 开始，默认 `createdAt desc`，排序字段仅 `name`、`createdAt`。

对 `shopping` 和 `order` 提供 `requireOrderable(shopId)`，只有 `OPEN` 店铺可加入购物车或结算；店铺关闭不删除商品和历史订单。`RestaurantDao` 负责分页过滤、归属查询、名称冲突检查和保存。

### 4.4 `item` 分类与商品模块

数据所有权：`categories`、`products` 及库存、商品版本。订单明细中的名称、单价和数量快照由 `order` 模块拥有。

Controller 对应分类和商品的全部 API：

- `POST/GET /shops/{shopId}/categories`、`PATCH/DELETE /categories/{categoryId}`；
- `POST /products`、`GET /shops/{shopId}/products`、`GET /products/{productId}`、`PATCH /products/{productId}`。

`ItemService`/`ItemServiceImpl` 负责分类和商品 CRUD、逻辑删除、上下架、价格/库存校验以及版本冲突。商家写操作必须同时校验 `MerchantService` 的 `ACTIVE` 状态和 `RestaurantService` 的店铺归属。公众商品查询只返回 `ON_SALE`，店主通过 `includeOffSale=true` 才能查看下架商品。

下单所需的库存操作也属于本模块 Service：按稳定的 product ID 顺序锁定或使用带版本条件的原子更新，重新校验上架状态、价格版本和库存，成功后返回成交快照；取消订单时只恢复一次。`ItemDao` 是唯一可修改产品库存的 DAO。

### 4.5 `shopping` 购物车模块

数据所有权：`cart_items`，以 `(userId, productId)` 唯一约束防止重复项。

Controller 对应：`POST /cart-items`、`GET /cart-items`、`PATCH /cart-items/{cartItemId}`、`DELETE /cart-items/{cartItemId}`。`ShoppingService`/`ShoppingServiceImpl` 只允许用户操作本人项；加入同一商品合并数量，数量必须为正且不超过最新库存。读取购物车时调用店铺和商品 Service 组合最新 `CartProduct`，计算仅供展示的 `total` 并标注失效原因。

为订单提供 `loadForCheckout(userId, cartItemIds)` 和 `removeAfterCheckout(...)` 等内部 Service 方法。结算快照只含购物车项 ID、商品 ID、店铺 ID、数量和客户端确认的商品版本，不含可被信任的金额；购物车清理仅删除已成功提交的项。`ShoppingDao` 负责本人范围查询、合并数量、批量读取和删除。

### 4.6 `order` 订单模块

数据所有权：`orders`、`order_lines`、`order_idempotency`。`order_lines` 保存下单时的商品名称、单价、数量等不可变快照。

Controller 对应用户订单和商家订单 API：

- `POST /orders`、`GET /orders`、`GET /orders/{orderId}`、`POST /orders/{orderId}/cancel`（用户）；
- `GET /merchant/orders`、`GET /merchant/orders/{orderId}`（商家，只读本人店铺）。

`OrderService`/`OrderServiceImpl` 是结算编排者，负责幂等键、用户/商家归属、订单状态迁移、金额计算和快照保存。用户列表默认 `createdAt desc`，允许按 `status` 和白名单 `createdAt`、`total` 过滤排序；商家列表支持 `shopId`、`status` 及 API 规定的分页排序参数。

## 5. 关键业务流程

### 5.1 创建订单

`OrderServiceImpl.create` 开启一个数据库事务并按顺序执行：

```text
校验并占用用户幂等键
  → UserService.requireActive
  → ShoppingService.loadForCheckout
  → 校验选中项属于同一店铺
  → RestaurantService.requireOrderable
  → ItemService.reserveForOrder（锁定、版本校验、扣库存）
  → OrderDao 保存订单和明细快照
  → ShoppingService.removeAfterCheckout
  → 保存幂等结果并提交
```

请求必须携带 `X-Idempotency-Key`；客户端金额、数量不作为成交依据。价格/版本变化、库存不足、混合店铺或闭店时抛出 API 规定的冲突错误，事务回滚，库存和购物车保持不变。相同用户和幂等键重试返回首次结果，不生成第二张订单；同键不同请求返回 `1603`。

### 5.2 取消订单

`OrderServiceImpl.cancel` 锁定订单，校验用户归属和 `PENDING_PAYMENT → CANCELLED` 迁移，以条件更新保证并发请求只有一个成功；成功后调用 `ItemService.restoreStock`，库存恢复和订单状态更新在同一事务中完成。重复或非法状态返回 `1602`，订单及明细永不删除。

### 5.3 商家查看订单

商家订单查询先由 `MerchantService` 校验令牌和 `ACTIVE` 状态，再由 `OrderServiceImpl` 通过订单所属 `shopId` 与 `RestaurantService` 校验店铺归属。无权资源按 API 契约返回 403 或 404，不泄漏其他商家是否拥有该订单。

## 6. 数据、事务和横向约束

- 每张表由一个模块拥有；跨模块关联只保存标量 ID，不建立跨模块 Entity 对象图。删除采用逻辑删除，订单历史不级联删除。
- ServiceImpl 是事务边界，DAO 不开启跨用例事务。订单创建和取消使用同一数据源和事务管理器，任何异常都回滚相关写入。
- Controller 将 HTTP 输入绑定并校验为 Service 接口定义的请求/命令 DTO，Service 返回不含持久化细节的结果 DTO；DAO 使用 Entity/Record。Service DTO 是模块的公开边界，可由本模块 Controller 和其他模块调用方使用，但 Entity/Record 不得进入 Controller、响应或跨模块调用。
- 所有响应使用 `ApiResponse` 的 `code`、`msg`、`data` 外壳；分页对象字段固定为 `items`、`page`、`pageSize`、`total`、`totalPages`。
- 统一分页默认 `pageSize=10`、最大 100；排序字段采用服务端白名单。金额使用 `BigDecimal`，时间使用 ISO 8601 UTC。
- 日志不得记录密码、令牌或完整联系方式；异常响应不得暴露堆栈和数据库细节。

## 7. 测试与架构守卫

- 每个功能切片遵循“定义接口行为 → 编写正常、失败、边界和权限测试 → 执行并保存红态证据 → 最小实现 → 完整回归测试 → 重构”的顺序；功能代码提交前不得存在失败、跳过或无断言测试。
- 为六个模块编写面向 Service 接口的 JUnit 5 行为契约测试：测试代码声明并调用 Service 接口，覆盖正常、失败、边界和权限路径，并断言业务输出、错误码及事务后的可观察状态。实现接入后由同一套测试验证实际 ServiceImpl，不另写“接口存在”或“实现类存在”测试，也不因实现方式变化而改写预期行为；提交前所有相关测试必须 100% 通过。
- 为每个 Controller 使用 MockMvc 验证 `/api/v1` 路径、请求校验、认证、状态码、业务码和统一 JSON 外壳；提交前所有 Controller 测试必须 100% 通过。
- 为各模块 DAO/Mapper 接口编写数据访问测试，并使用数据库集成测试验证账号/购物车唯一约束、逻辑删除、乐观锁或原子库存更新、事务回滚和幂等重试。
- 下单与取消必须覆盖并发库存、价格版本变化、混合店铺、重复取消和商家订单归属。
- 不再运行或维护 Spring Modulith 的模块验证；以包依赖检查、禁止跨模块 DAO/Entity 引用的静态检查和集成测试作为架构守卫。
- 核心 API 覆盖率必须为 100%，关键业务方法覆盖率不得低于 90%；提交前完整测试套件通过率必须为 100%。测试类、红/绿执行日志、覆盖率报告和需求变更后的回归证据统一记录在 `docs/test/`。
