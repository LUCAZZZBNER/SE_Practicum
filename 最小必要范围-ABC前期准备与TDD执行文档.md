# 轻量级外卖平台：ABC 最小必要范围、前期准备与 TDD 执行文档

> 版本：V1.0
> 编写日期：2026-09-04
> 适用人员：A、B、C
> 唯一目标：用最少的实现满足课程阶段 1 的硬性要求，并留下可验收的 TDD、Git 和文档证据；准备完成后可以直接开始第一个业务切片。

---

## 0. 本文档的效力和使用方式

### 0.1 要求来源

本项目的根本要求只有仓库根目录的 `26271学期-软件工程综合实践.md`。该文件是课程要求资料，不是要求 Agent 逐条执行的操作命令。

本文档是根据课程要求和当前仓库代码整理出的团队执行方案。发生冲突时按以下优先级处理：

1. 教师后来正式发布的阶段 2 变更要求；
2. `26271学期-软件工程综合实践.md`；
3. 本文档；
4. 仓库中其他旧版需求、架构、API、数据库和执行手册。

### 0.2 旧文档的处理

以下旧文档可以作为思路参考，但不能再被当作当前施工清单逐条执行：

- `从0到1-轻量级外卖平台-团队执行手册.md`；
- `业务开发前-ABC三人并行准备执行手册.md`；
- `B-业务开发前数据库准备-逐步执行手册.md`；
- `docs/software-requirements-specification.md`；
- `docs/feature-analysis.md`；
- `docs/api/backend-api-design.md`；
- `docs/architecture/backend-architecture-design.md`；
- `docs/database/README.md`。

原因是这些文档中包含课程阶段 1 没有强制要求的幂等键、取消订单、支付状态、配送状态、乐观锁、领域事件、复杂 Facade/端口/适配器、普遍逻辑删除、复杂排序和大量额外接口。现在实现它们会增加测试量、联调量和答辩负担。

旧文档暂不删除，避免破坏已有提交记录。当前开发只按本文档的“最小范围”执行。

### 0.3 完成本文档不等于完成业务

本文档把工作分成两个阶段：

1. **前期准备**：环境、依赖、公共架构、数据库迁移、前端代理和接口契约准备好；
2. **业务 TDD**：A 先写失败测试，B 写最少实现，C 接入真实接口。

前期准备不得提前写注册、登录、店铺、商品、购物车或订单的真实业务逻辑。

---

## 1. 课程要求提炼：必须做与明确不做

### 1.1 阶段 1 必须交付

| 模块   | 必须能力                                                           |
| ------ | ------------------------------------------------------------------ |
| 用户   | 注册、登录、本人信息查询、本人信息修改、账号密码和用户状态校验     |
| 商家   | 商家注册                                                           |
| 店铺   | 创建店铺、切换营业/关店/临时闭店状态、店铺列表、店铺详情、状态展示 |
| 商品   | 商品分类、商品新增/修改、上下架、价格、库存、列表、详情            |
| 购物车 | 加入、查询、修改数量、删除                                         |
| 订单   | 从购物车创建订单、本人订单列表、本人订单详情和状态展示             |

同时必须满足：

- Spring Boot 前后端分离；
- Entity、Mapper/Dao、Service、Controller 四层职责清楚；
- `/api/v1` 前缀、资源名 URI、正确的 HTTP 方法和状态码；
- 统一响应、全局异常处理；
- 核心接口先测试后实现；
- 测试包含成功、失败和边界场景；
- 核心接口覆盖率 100%，关键业务方法覆盖率至少 90%；
- 留存 Red、Green、回归、覆盖率和 Git 过程证据；
- 阶段 2 收到教师变更后再补测试和实现，不提前猜题。

### 1.2 当前明确不做

除非教师阶段 2 明确要求，否则不要实现：

- 支付、退款、优惠券、配送、骑手、评价、通知；
- 订单取消、商家接单、订单状态流转；
- 幂等键、分布式锁、消息队列、缓存、微服务；
- 图片上传、地图、定位、短信、验证码；
- 管理员后台；
- HATEOAS、复杂动态排序、导出报表；
- 领域事件、六边形架构、Repository Port、独立 Facade 层；
- 全表通用逻辑删除和通用 CRUD 框架；
- 前端动画、主题系统、复杂状态管理和自动化前端测试框架；
- 为尚未发布的阶段 2 需求预留大量字段或接口。

原则：课程要求没有点名、当前验收流程用不到、阶段 2 尚未正式发布的功能，一律不做。

---

## 2. 当前仓库代码审计结论

审计时间为 2026-09-04，检查了 `backend`、`frontend`、`docs`、根目录配置和现有 Git 状态。

### 2.1 后端现状

当前已有：

- Spring Boot 4.1.1、Java 17 编译目标、Spring Modulith；
- `user`、`merchant`、`restaurant`、`item`、`shopping`、`order` 六个模块包；
- 六个模块的状态枚举、只读 record 和简单占位 `*Module` 类；
- 一个 Spring 上下文测试和一个 Modulith 边界测试。

当前没有：

- 任意真实 REST Controller；
- 请求 DTO、响应 DTO、统一返回体；
- 全局异常处理、参数校验、登录态拦截；
- Entity、Mapper/Dao、Service 接口和 ServiceImpl；
- MyBatis、Flyway 依赖和 Mapper XML；
- 数据源配置、开发/测试 Profile、建表迁移；
- 任意业务测试、MockMvc 接口测试和覆盖率配置。

结论：现有 Java 类只是可保留的模块骨架，不能算任何业务功能已经完成。不要继续围绕现有 `*Module` 占位方法扩写业务；开始对应切片时再按四层结构替换。

### 2.2 前端现状

当前已有：

- Vue 3、Vite、Vue Router、Element Plus、Axios 和 Pinia 依赖；
- 登录/注册、店铺、商品、购物车、订单、个人信息页面骨架；
- 公共布局、侧边栏、确认操作、空状态和 Axios 封装；
- 路由按普通用户、商家和登录页面拆分。

当前问题：

- 页面主要使用写死的示例数据；
- 登录和注册只显示“占位”消息，没有调用接口；
- 多数按钮没有提交行为；
- API 路径存在 `/stores`、`/cart/items`、`/merchants/login` 等未冻结写法；
- Axios 当前按 Bearer Token 设计，但后端认证方式尚未实现；
- Vite 没有 `/api` 代理；
- 没有前端自动化测试，本阶段也不新增，采用接口测试加人工联调证据即可。

结论：前端脚手架和页面骨架可以复用，不重写 UI。C 的主要工作是去掉假数据并接入冻结后的 API。

### 2.3 文档现状

现有文档数量多、内容完整，但已经超过课程最小范围。尤其是下列内容会显著扩大工作量：

- 订单幂等、取消订单和库存恢复；
- `PAID/PREPARING/DELIVERING/COMPLETED` 全状态流；
- 所有业务表逻辑删除；
- 乐观锁、并发版本字段；
- 复杂分页、排序白名单；
- Facade、Command、View、Repository Port、领域事件；
- 大量并非阶段 1 强制的接口。

这些内容本轮不删除，但不进入开发任务和测试清单。

### 2.4 本机与构建现状

实际验证结果：

| 项目     | 当前结果                                                         | 准备阶段处理                                              |
| -------- | ---------------------------------------------------------------- | --------------------------------------------------------- |
| Git      | A+B 共同 Red 基线已发布到远程 `develop`，合并提交 `8723448`   | 三人拉取 `develop` 后分别创建个人 TDD 分支                 |
| Java     | 已安装 `D:\Dev\Java\JDK17`，已用 17 完成编译和测试           | 本机不重复安装；其他成员各自确认 JDK 17                   |
| Maven    | Wrapper 已固定自动下载 Maven 3.9.16                            | 三人统一使用 `backend/mvnw.cmd`，不要求全局安装 Maven     |
| Node     | 能找到 Node 24.19.0                                              | C 改用带 npm 的 Node LTS 环境，版本统一即可               |
| npm      | 当前命令不可用                                                   | C 修复后执行`npm ci` 和 `npm run build`               |
| MySQL    | 客户端/服务已可用，版本 26.7；V1 已在开发库和测试库验证         | 其他成员各自创建空库、账号和本机环境变量                  |
| 后端测试 | 169 个已运行：143 个通过，26 个缺少 ServiceImpl 的预期 Error    | 作为三人共同 TDD Red 基线                                 |
| 前端构建 | 因 npm 不可用，当前无法运行                                      | C 修复后重新验证                                          |

因此，后端共同前期基线已经达到可以开始业务 TDD 的状态；前端仍需由 C 补齐带 npm 的 Node 环境后再构建。

---

## 3. 三人固定分工

用户指定的“1+4、2、3”解释如下，并作为固定边界：

| 成员 | 对应课程分工 | 固定负责                                                                                                                                   |
| ---- | ------------ | ------------------------------------------------------------------------------------------------------------------------------------------ |
| A    | 1 + 4        | 后端架构、公共配置、Controller、Service 接口、请求/响应 DTO、全部接口测试、核心业务测试设计、MockMvc、覆盖率、回归、架构边界、Bug 复现测试 |
| B    | 2            | 数据表、Flyway、Entity、Mapper/Dao、Mapper XML、ServiceImpl、事务、数据库约束、数据校验和后端联调                                          |
| C    | 3            | Vue 页面、路由、Axios、登录态交互、前后端联调、人工验收、测试报告、开发日志和最终材料                                                      |

2026-09-05 执行例外：为避免数据库准备继续等待，B 临时接管 JDK 17、Maven Wrapper、MyBatis/Flyway 依赖和测试 Profile，只负责把数据库迁移运行起来。A 仍负责后续 Controller、Service 接口、业务 Red 测试、MockMvc、覆盖率和回归测试；本次例外不表示 B 可以跳过 Red 直接写业务实现。

### 3.1 边界规则

- A 不替 B 写真实业务判断和 SQL；A 可以建立 `ServiceImpl` 空壳，使测试能够编译并明确失败。
- B 不先写业务实现。B 可以先做表设计、迁移和 Mapper 公共配置，但具体 Mapper/ServiceImpl 必须等对应 Red 测试提交后再写。
- C 不等待所有后端完成。C 先按冻结契约整理页面字段和 API 方法，但不伪造“联调完成”。
- Controller 只负责接收参数、取得当前用户、调用 Service 和返回结果。
- ServiceImpl 负责业务规则和事务。
- Mapper 只负责 SQL，不做业务判断。
- Entity 只映射表，不直接作为接口响应。
- 一个人不能通过一次提交同时加入测试和使该测试通过的完整业务代码。

---

## 4. 冻结的最小技术方案

### 4.1 后端结构

保留现有六个业务模块，不新增业务模块。模块内部只使用课程要求的四层结构：

```text
com.delivery.backend
├─ common
│  ├─ ApiResponse.java
│  ├─ BusinessException.java
│  ├─ GlobalExceptionHandler.java
│  └─ LoginInterceptor.java
├─ user
│  ├─ controller
│  ├─ dto
│  ├─ entity
│  ├─ mapper
│  └─ service
│     └─ impl
├─ merchant
├─ restaurant
├─ item
├─ shopping
└─ order
```

对外资源命名统一为：

- Java 模块 `restaurant`，HTTP/数据库使用 `shops`；
- Java 模块 `item`，HTTP/数据库使用 `products`；
- Java 模块 `shopping`，HTTP/数据库使用 `cart-items`。

Spring Modulith 只保留现有模块边界测试，不再扩展命名接口、事件或复杂模块 API。

### 4.2 最少依赖

A 只补充以下后端依赖/插件：

- Bean Validation；
- MyBatis Spring Boot Starter；
- Flyway Core 和 MySQL 支持；
- `spring-security-crypto`，只用于 BCrypt 密码摘要，不启用完整 Spring Security；
- JaCoCo Maven Plugin，用于覆盖率报告。

保留现有 WebMVC、MySQL Driver、JUnit/MockMvc 和 Modulith。不要加入 JWT、Redis、MapStruct、Swagger、MyBatis-Plus、Testcontainers 或代码生成器。

前端不增加依赖，继续使用现有 Vue、Router、Axios、Element Plus。Pinia 当前没有必要，可以留在依赖中但不要为了“用上它”新增状态层。

### 4.3 最小登录方案

采用服务端 `HttpSession`：

1. 登录成功后在 Session 保存 `USER_ID`；
2. `LoginInterceptor` 保护需要登录的接口；
3. 商家接口根据 `USER_ID` 查询 `merchants` 判断身份和资源归属；
4. Vite 把 `/api` 代理到后端，因此本地开发不额外处理 CORS；
5. C 删除 Bearer Token 逻辑，不把用户 ID 当作可信身份提交。

这已经满足登录、本人资源和权限校验。阶段 1 不实现 JWT、刷新令牌和权限框架。

### 4.4 统一响应和错误

成功响应统一为：

```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

错误只保留四类，避免维护大规模错误码表：

| HTTP |  code | 含义                   |
| ---- | ----: | ---------------------- |
| 400  | 40000 | 参数或业务规则不满足   |
| 401  | 40100 | 未登录或登录失效       |
| 403  | 40300 | 无权操作该资源         |
| 404  | 40400 | 资源不存在             |
| 409  | 40900 | 唯一键、库存或状态冲突 |
| 500  | 50000 | 未预期的服务端错误     |

不要为每一个提示创建独立数字错误码。具体原因写在 `msg`，测试断言 HTTP 状态、code 和必要消息即可。

---

## 5. B 可立即开始的最小数据库设计

B 可以在 A 完成公共架构前并行设计以下 8 张表。字段以支持阶段 1 为限。

| 表                     | 最少字段                                                                                      | 必要约束                                                            |
| ---------------------- | --------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| `users`              | `id, account, password_hash, nickname, phone, status, created_at, updated_at`               | account 唯一；status 为`ACTIVE/DISABLED`                          |
| `merchants`          | `id, user_id, name, created_at`                                                             | user_id 外键且唯一                                                  |
| `shops`              | `id, merchant_id, name, description, status, created_at, updated_at`                        | merchant_id 外键且唯一；status 为`OPEN/CLOSED/TEMPORARILY_CLOSED` |
| `product_categories` | `id, shop_id, name`                                                                         | shop_id 外键；同店分类名唯一                                        |
| `products`           | `id, shop_id, category_id, name, description, price, stock, status, created_at, updated_at` | price > 0；stock >= 0；status 为`ON_SALE/OFF_SALE`                |
| `cart_items`         | `id, user_id, product_id, quantity, created_at, updated_at`                                 | quantity > 0；`user_id + product_id` 唯一                         |
| `orders`             | `id, order_no, user_id, shop_id, total_amount, status, created_at`                          | order_no 唯一；初始状态`PENDING_PAYMENT`                          |
| `order_items`        | `id, order_id, product_id, product_name, unit_price, quantity`                              | 保存下单快照；unit_price > 0；quantity > 0                          |

本阶段不要加入：

- `idempotency_key`；
- `version`；
- `cancelled_at`；
- 所有表的 `deleted`；
- 配送地址快照、支付流水、退款字段；
- 阶段 2 尚未确认的预留字段。

数据库必须有两个独立库：

- `delivery_dev`：人工联调；
- `delivery_test`：自动化测试。

表结构只通过 `V1__create_core_tables.sql` 建立。开发过程中需要修正已共享的 V1 时新增 V2，不改已执行迁移。密码只通过环境变量提供。

---

## 6. 冻结的最小 API

以下 22 个接口是阶段 1 的完整清单。没有列出的接口不做。

### 6.1 用户和商家

| 方法  | 路径                    | 登录 | 用途                                  |
| ----- | ----------------------- | ---- | ------------------------------------- |
| POST  | `/api/v1/users`       | 否   | 普通用户注册                          |
| POST  | `/api/v1/merchants`   | 否   | 商家注册，同时创建 users 和 merchants |
| POST  | `/api/v1/users/login` | 否   | 普通用户和商家共用登录                |
| GET   | `/api/v1/users/me`    | 是   | 查询本人信息                          |
| PATCH | `/api/v1/users/me`    | 是   | 修改 nickname、phone                  |

### 6.2 店铺、分类和商品

| 方法  | 路径                                  | 登录 | 用途                                   |
| ----- | ------------------------------------- | ---- | -------------------------------------- |
| POST  | `/api/v1/shops`                     | 商家 | 创建本人唯一店铺，初始`CLOSED`       |
| PATCH | `/api/v1/shops/{shopId}/status`     | 店主 | 修改三种店铺状态                       |
| GET   | `/api/v1/shops`                     | 否   | 店铺列表并展示状态，不做复杂分页排序   |
| GET   | `/api/v1/shops/{shopId}`            | 否   | 店铺详情                               |
| POST  | `/api/v1/shops/{shopId}/categories` | 店主 | 新增分类                               |
| GET   | `/api/v1/shops/{shopId}/categories` | 否   | 分类列表                               |
| POST  | `/api/v1/products`                  | 商家 | 新增商品，初始`OFF_SALE`             |
| PATCH | `/api/v1/products/{productId}`      | 店主 | 修改分类、名称、描述、价格、库存、状态 |
| GET   | `/api/v1/shops/{shopId}/products`   | 否   | 店铺商品列表                           |
| GET   | `/api/v1/products/{productId}`      | 否   | 商品详情                               |

### 6.3 购物车和订单

| 方法   | 路径                                | 登录 | 用途                           |
| ------ | ----------------------------------- | ---- | ------------------------------ |
| POST   | `/api/v1/cart-items`              | 用户 | 加入商品或累加同商品数量       |
| GET    | `/api/v1/cart-items`              | 用户 | 查询本人购物车                 |
| PATCH  | `/api/v1/cart-items/{cartItemId}` | 本人 | 修改数量                       |
| DELETE | `/api/v1/cart-items/{cartItemId}` | 本人 | 物理删除临时购物车项           |
| POST   | `/api/v1/orders`                  | 用户 | 使用本人整个购物车创建一个订单 |
| GET    | `/api/v1/orders`                  | 用户 | 本人订单列表，响应含状态       |
| GET    | `/api/v1/orders/{orderId}`        | 本人 | 本人订单详情和明细快照         |

说明：课程模块拆开后无法在不丢失必需能力的情况下继续减少；不再添加取消订单等接口。

---

## 7. A 发给 B 的最小业务逻辑需求单

本节就是 A 的正式交接内容。A 冻结字段名后把本节链接发给 B；B 的 ServiceImpl 和 Mapper 只实现这里列出的规则。

### 7.1 Service 接口清单

| Service             | 方法               | 最少输入                                                          | 最少输出                   |
| ------------------- | ------------------ | ----------------------------------------------------------------- | -------------------------- |
| `UserService`     | `register`       | account、password、passwordConfirm、nickname、phone               | 非敏感用户信息             |
|                     | `login`          | account、password、HttpSession                                    | 非敏感用户信息和是否为商家 |
|                     | `getCurrent`     | userId                                                            | 本人信息                   |
|                     | `updateCurrent`  | userId、nickname、phone                                           | 更新后的本人信息           |
| `MerchantService` | `register`       | account、password、passwordConfirm、nickname、phone、merchantName | 商家信息；内部同时创建用户 |
| `ShopService`     | `create`         | userId、name、description                                         | 店铺信息                   |
|                     | `changeStatus`   | userId、shopId、status                                            | 更新后的店铺               |
|                     | `list`           | 无或 keyword                                                      | 店铺列表                   |
|                     | `get`            | shopId                                                            | 店铺详情                   |
| `CategoryService` | `create`         | userId、shopId、name                                              | 分类信息                   |
|                     | `listByShop`     | shopId                                                            | 分类列表                   |
| `ProductService`  | `create`         | userId、shopId、categoryId、name、description、price、stock       | 商品信息                   |
|                     | `update`         | userId、productId、可修改字段                                     | 更新后的商品               |
|                     | `listByShop`     | shopId、可选 categoryId                                           | 商品列表                   |
|                     | `get`            | productId                                                         | 商品详情                   |
| `CartService`     | `add`            | userId、productId、quantity                                       | 购物车项                   |
|                     | `listMine`       | userId                                                            | 本人购物车和展示总额       |
|                     | `changeQuantity` | userId、cartItemId、quantity                                      | 更新后的购物车项           |
|                     | `remove`         | userId、cartItemId                                                | 无                         |
| `OrderService`    | `createFromCart` | userId                                                            | 订单详情                   |
|                     | `listMine`       | userId                                                            | 本人订单列表               |
|                     | `getMine`        | userId、orderId                                                   | 本人订单详情               |

### 7.2 必须实现的规则

#### 用户与商家

- account 去除首尾空格后不能为空且全局唯一；
- password 至少 6 位，passwordConfirm 必须一致；
- 密码只保存 BCrypt 摘要，任何响应都不能出现摘要；
- 新用户状态固定为 `ACTIVE`；
- 登录时校验账号存在、密码正确、状态为 `ACTIVE`；
- 商家注册在一个事务内创建 users 和 merchants，任一步失败全部回滚；
- 一个 user 最多一个 merchant。

#### 店铺、分类与商品

- 一个商家阶段 1 最多一个店铺；
- 店铺初始 `CLOSED`，只接受 `OPEN/CLOSED/TEMPORARILY_CLOSED`；
- 只有店主可以修改状态、分类和商品；
- 同一店铺分类名不能重复；
- 商品分类必须属于商品的 shop；
- price 必须大于 0，stock 必须大于等于 0；
- 商品初始 `OFF_SALE`；
- 普通浏览只返回 `ON_SALE` 商品，店铺详情仍展示店铺当前状态。

#### 购物车

- 只能操作本人的购物车项；
- quantity 必须为正整数；
- 加入或修改时重新校验店铺 `OPEN`、商品 `ON_SALE` 和库存；
- 同一用户同一商品再次加入时累加数量，不新建重复记录；
- 为降低订单复杂度，一个用户购物车只允许同一店铺商品；
- 购物车展示总额可以计算，但不能作为订单成交金额直接入库。

#### 订单

- 创建订单必须在一个事务中完成；
- 购物车不能为空；
- 下单时重新查询店铺状态、商品状态、价格和库存；
- 金额由后端按当前价格计算；
- 写入 orders 和 order_items 商品名称/单价/数量快照；
- 扣减库存并清空本人购物车；
- 任一步失败，订单、明细、库存和购物车全部回滚；
- 初始状态固定 `PENDING_PAYMENT`，阶段 1 不实现后续状态变化；
- 只能查询本人的订单。

### 7.3 B 不需要实现的内容

- 不写 Controller、MockMvc 和测试用例；
- 不写 JWT、登录过滤器或全局异常；
- 不做订单取消、支付、配送或幂等；
- 不做通用 BaseService、BaseMapper 封装；
- 不做动态 SQL 排序框架；
- 不做缓存和并发压测；
- 不扩展本文档之外的字段和接口。

---

## 8. 前期准备：三人并行执行顺序

### 8.1 共同起点

三人先完成：

```powershell
git switch develop
git pull --ff-only
git status --short
```

除本次新增的执行文档外，必须确认没有来源不明的改动。B 继续使用 `feature/b-database-foundation`，但文档、数据库配置和迁移要分开提交。

分支固定为：

- A：`feature/a-backend-foundation`；
- B：`feature/b-database-foundation`（当前仓库已经在该分支，继续使用，不再另建重复分支）；
- C：`feature/c-frontend-foundation`。

### 8.2 A：后端公共架构准备

按顺序完成：

1. 选择已有 JDK 17，设置命令行和 IDEA Project SDK；
2. 修复 Maven Wrapper，确保 `backend\mvnw.cmd -version` 和 `backend\mvnw.cmd test` 可运行；
3. 在 `pom.xml` 只加入第 4.2 节依赖和 JaCoCo；
4. 创建 `ApiResponse`、`BusinessException`、`GlobalExceptionHandler`；
5. 创建 HttpSession 登录拦截器和最小 Web 配置；
6. 为六个模块建立 `controller/dto/entity/mapper/service/impl` 目录规则；
7. 建立第 7.1 节的 Service 接口、DTO 和可编译但未实现业务的 Controller/ServiceImpl 空壳；
8. 保留并运行 Modulith 边界测试；
9. 给所有后端测试统一使用 `@ActiveProfiles("test")`，防止测试误连开发库；
10. 添加一个公共响应 MockMvc 冒烟测试和 JaCoCo 报告命令；
11. 把第 7 节交给 B，冻结方法签名。

A 的准备验收：

```powershell
cd backend
.\mvnw.cmd clean test
.\mvnw.cmd verify
```

要求：测试通过，`target/site/jacoco/index.html` 可生成；此时业务空壳可以返回 501 或抛出明确的“未实现”异常，但不能伪装成功。

### 8.3 B：数据库和持久化准备（逐步操作版）

本节是 B 的完整操作手册。按照 2026-09-05 的新分工，B 可以从上到下连续执行数据库运行准备，不再等待 A。命令默认在 Windows PowerShell 中运行；包含密码的内容只能在本人电脑输入，不得复制到聊天、截图、文档或 Git。

#### 8.3.0 开始前先看：当前到底能做到哪一步

当前仓库已知状态：

```text
当前分支：feature/b-database-foundation
MySQL 客户端：已安装
MySQL 服务：需要按下面命令再次确认
pom.xml：还没有 MyBatis 和 Flyway
Maven Wrapper：目前不能运行
Java：JDK 17 已装在 D:\Dev\Java\JDK17，但当前终端默认仍指向 Java 25
```

因此 B 现在分两段执行。

第一段现在立刻可以完成：

```text
1. 保存本执行文档
2. 确认 MySQL 服务
3. 创建两个空数据库
4. 创建 delivery_app 账号
5. 设置本机环境变量
6. 确认 8 张表设计
7. 创建 V1__create_core_tables.sql
8. 把完整建表 SQL 写入 V1
9. 人工检查 V1
10. 提交“设计 + V1”，但不执行 V1
```

原计划这里等待 A 完成公共运行环境。2026-09-05 团队决定不再等待，改由 B 临时接管以下准备：

```text
1. 把项目切换到 JDK 17
2. 修复 Maven Wrapper
3. 在 pom.xml 加入 MyBatis、Flyway Core、Flyway MySQL
4. 给后端测试增加 test Profile
```

B 完成接管后，直接执行第二段：

```text
1. 启用已有的 D:\Dev\Java\JDK17（不下载、不重装）
2. 修复 Maven Wrapper
3. 由 B 在 pom.xml 加入 MyBatis 和 Flyway
4. 由 B 给测试固定 test Profile
5. 用 test Profile 运行 Maven 测试
6. Flyway 自动在 delivery_test 创建 8 张表
7. 检查表、约束和 Flyway 历史
8. 重复运行迁移
9. 用 dev Profile 让 Flyway 创建 delivery_dev
10. 保存证据并提交 PR
```

最重要的停止线：

```text
现在可以“设计表”和“写建表 SQL”。
现在不可以在 delivery_dev/delivery_test 手工执行 CREATE TABLE。
当前仍不可以写 UserMapper、UserServiceImpl 等业务实现；B 只接管数据库运行基础，不接管 A 的业务 Red 测试。
```

如果只想知道下一条应该执行什么，从 8.3.2 开始，一次只复制一个代码块，看到“通过后继续”再执行下一块。

#### 8.3.1 B 的允许范围和停止线

B 在准备阶段只交付：

```text
MySQL 开发库和测试库
→ 项目数据库账号
→ 数据库环境变量
→ application-dev/test.properties
→ MyBatis 公共配置
→ Flyway V1 建表迁移
→ 8 张表的约束验证记录
→ Entity/Mapper/ServiceImpl 空目录或空壳
```

B 在 A 提交对应 Red 测试前不得编写：

- 注册、登录、商品、购物车、订单的 ServiceImpl 业务代码；
- 业务 Mapper 方法和 Mapper XML 查询；
- Controller；
- 为了演示而直接向正式迁移写入测试数据；
- V1 之外的预想功能字段。

#### 8.3.2 第 1 步：确认仓库起点

打开 PowerShell，进入项目根目录：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git status --short
git branch --show-current
```

`git status --short` 必须没有输出。如果有自己未提交的文件，先确认和提交；如果是别人的改动，不要删除、覆盖或执行 `git reset --hard`。

当前仓库已经停在 B 的正确分支 `feature/b-database-foundation`。先看第二条命令输出。

如果输出已经是：

```text
feature/b-database-foundation
```

就不要切换分支，继续下一步。

如果不是该分支，先检查它是否已经存在：

```powershell
git branch --list feature/b-database-foundation
```

如果有输出，切换过去：

```powershell
git switch feature/b-database-foundation
```

如果没有输出，才从最新 `develop` 创建：

```powershell
git switch develop
git pull --ff-only
git switch -c feature/b-database-foundation
```

最后统一验证：

```powershell
git branch --show-current
git log -5 --oneline --decorate
git status --short
```

期望分支输出：

```text
feature/b-database-foundation
```

当前 `git status --short` 可能显示：

```text
?? 最小必要范围-ABC前期准备与TDD执行文档.md
```

这是本次生成的执行文档，不是异常。先把它作为单独的文档提交保存：

```powershell
git add -- '最小必要范围-ABC前期准备与TDD执行文档.md'
git diff --cached --stat
git commit -m "docs: add minimal abc preparation and tdd guide"
git status --short
```

期望最后 `git status --short` 没有输出。若还有其他文件，不要直接提交，先确认来源。

#### 8.3.3 第 2 步：确认 MySQL 客户端和服务

检查客户端位置和版本：

```powershell
Get-Command mysql
mysql --version
```

当前电脑曾检测到 MySQL Community Server 26.7。课程不要求固定小版本，只要能正常执行本项目 SQL 即可。

列出 MySQL Windows 服务：

```powershell
Get-Service | Where-Object {
  $_.Name -like '*MySQL*' -or $_.DisplayName -like '*MySQL*'
} | Select-Object Name, DisplayName, Status
```

如果服务状态不是 `Running`，使用上一条命令得到的真实服务名启动。例如服务名是 `MySQL267` 时：

```powershell
Start-Service -Name 'MySQL267'
```

不要直接照抄 `MySQL267`；必须替换为本机实际服务名。检查 3306 端口：

```powershell
Test-NetConnection -ComputerName localhost -Port 3306
```

期望：

```text
TcpTestSucceeded : True
```

连接管理员账号，只使用 `-p` 让 MySQL 安全询问密码，不要把密码写在命令后面：

```powershell
mysql -u root -p
```

进入 `mysql>` 后执行：

```sql
SELECT VERSION();
SELECT CURRENT_USER();
SHOW VARIABLES LIKE 'character_set_server';
```

退出：

```sql
exit
```

若无法登录，先解决服务、端口或管理员密码问题，不要继续创建项目配置。

#### 8.3.4 第 3 步：安全检查现有数据库

再次进入 MySQL：

```powershell
mysql -u root -p
```

检查两个库是否已经存在：

```sql
SHOW DATABASES LIKE 'delivery_dev';
SHOW DATABASES LIKE 'delivery_test';
```

如果已存在，先检查其中的表：

```sql
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema IN ('delivery_dev', 'delivery_test')
ORDER BY table_schema, table_name;
```

处理规则：

- 两个库不存在：进入下一步创建；
- 库存在但没有业务表：可以继续；
- 已有 `flyway_schema_history` 和业务表：先与 A 核对迁移版本；
- 存在来源不明的数据：停止，不执行 `DROP DATABASE`、`DROP TABLE` 或清空操作。

退出：

```sql
exit
```

#### 8.3.5 第 4 步：创建开发库、测试库和项目账号

进入 MySQL 管理员会话：

```powershell
mysql -u root -p
```

创建两个数据库：

```sql
CREATE DATABASE IF NOT EXISTS delivery_dev
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS delivery_test
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

创建只允许本机连接的项目账号。下面的 `<在本机输入强密码>` 必须替换成 B 自己生成的密码，不要把真实密码写回本文档：

```sql
CREATE USER IF NOT EXISTS 'delivery_app'@'localhost'
  IDENTIFIED BY '<在本机输入强密码>';
```

如果账号以前已经存在并且需要更换密码：

```sql
ALTER USER 'delivery_app'@'localhost'
  IDENTIFIED BY '<在本机输入新的强密码>';
```

只授予两个项目库的权限：

```sql
GRANT ALL PRIVILEGES ON delivery_dev.*
  TO 'delivery_app'@'localhost';

GRANT ALL PRIVILEGES ON delivery_test.*
  TO 'delivery_app'@'localhost';

FLUSH PRIVILEGES;
SHOW GRANTS FOR 'delivery_app'@'localhost';
```

确认库的字符集：

```sql
SELECT schema_name, default_character_set_name, default_collation_name
FROM information_schema.schemata
WHERE schema_name IN ('delivery_dev', 'delivery_test');
```

退出管理员会话：

```sql
exit
```

分别验证项目账号可以连接：

```powershell
mysql -u delivery_app -p -D delivery_dev -e "SELECT DATABASE(), CURRENT_USER();"
mysql -u delivery_app -p -D delivery_test -e "SELECT DATABASE(), CURRENT_USER();"
```

两条命令都必须成功。

#### 8.3.6 第 5 步：设置数据库环境变量

固定变量名：

```text
DELIVERY_DB_USERNAME
DELIVERY_DB_PASSWORD
```

用户名可以直接设置：

```powershell
[Environment]::SetEnvironmentVariable(
  'DELIVERY_DB_USERNAME',
  'delivery_app',
  'User'
)
$env:DELIVERY_DB_USERNAME = 'delivery_app'
```

密码使用安全输入，不在 PowerShell 命令历史中出现明文：

```powershell
$deliverySecurePassword = Read-Host '输入 delivery_app 数据库密码' -AsSecureString
$deliveryPasswordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($deliverySecurePassword)
try {
  $deliveryPlainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($deliveryPasswordPointer)
  [Environment]::SetEnvironmentVariable(
    'DELIVERY_DB_PASSWORD',
    $deliveryPlainPassword,
    'User'
  )
  $env:DELIVERY_DB_PASSWORD = $deliveryPlainPassword
}
finally {
  [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($deliveryPasswordPointer)
  Remove-Variable deliveryPlainPassword -ErrorAction SilentlyContinue
  Remove-Variable deliverySecurePassword -ErrorAction SilentlyContinue
  Remove-Variable deliveryPasswordPointer -ErrorAction SilentlyContinue
}
```

只检查变量是否存在，不输出密码：

```powershell
if ([string]::IsNullOrWhiteSpace($env:DELIVERY_DB_USERNAME)) {
  Write-Error 'DELIVERY_DB_USERNAME 未设置'
} else {
  Write-Output 'DELIVERY_DB_USERNAME 已设置'
}

if ([string]::IsNullOrWhiteSpace($env:DELIVERY_DB_PASSWORD)) {
  Write-Error 'DELIVERY_DB_PASSWORD 未设置'
} else {
  Write-Output 'DELIVERY_DB_PASSWORD 已设置'
}
```

设置用户级环境变量后要重启 IDEA，IDEA 才能读取新值。严禁执行以下形式：

```text
mysql -u delivery_app -p真实密码
spring.datasource.password=真实密码
```

#### 8.3.7 第 6 步：先确定数据库设计，不创建正式表

这一阶段容易混淆三件事：

| 动作                      | 现在能否做                 | 正确做法                                  |
| ------------------------- | -------------------------- | ----------------------------------------- |
| 设计表结构                | 可以                       | 确定 8 张表、字段、主外键和约束           |
| 编写建表 SQL              | 可以                       | 把 SQL 写入`V1__create_core_tables.sql` |
| 在正式开发库/测试库创建表 | 完成第 10 步运行基础后再做 | 由 B 启动测试，让 Flyway 自动执行 V1      |

一句话路线：

```text
现在：创建两个空数据库和账号
→ 设计 8 张表
→ 写好 V1 文件但不手工执行
→ B 补齐 JDK 17、MyBatis/Flyway 并修好 Maven
→ 使用 test Profile 启动测试
→ Flyway 自动在 delivery_test 创建表
→ 验证通过后再让 Flyway创建 delivery_dev
```

禁止在 `delivery_dev` 或 `delivery_test` 中执行以下做法：

```text
手工逐条粘贴 CREATE TABLE
mysql> SOURCE V1__create_core_tables.sql
使用 IDEA/Workbench 点击创建正式表
```

原因是手工创建不会生成 `flyway_schema_history`。后续 Flyway 看到一个已经非空、却没有迁移历史的数据库时会拒绝启动。现在可以创建两个**空数据库**，但表必须等待 Flyway 创建。

如果团队已经认可本文档第 5 节的最小设计，B 不需要重新发明一套表结构，只需要理解和核对。随后打开：

打开：

```text
docs/database/README.md
```

将旧设计文档的阶段 1 实施范围收敛到第 5 节的 8 张表。逐项删除或标记“阶段 1 不实施”：

- `idempotency_key`；
- `version`；
- `cancelled_at`；
- 全表 `deleted`；
- 取消订单和恢复库存；
- 配送、支付、退款字段；
- 复杂排序和并发扩展。

检查修改范围：

```powershell
git diff -- docs/database/README.md
```

搜索是否仍把额外字段写成阶段 1 必做：

```powershell
rg -n "idempotency|cancelled_at|DELIVERING|退款|配送|取消订单|乐观锁" docs/database/README.md
```

允许保留“明确不做”的说明，但不能再出现在 V1 表字段或阶段 1 验收项中。

#### 8.3.8 第 7 步：确认依赖缺口，由 B 接管数据库运行基础

先检查当前 `pom.xml`：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
rg -n "mybatis|flyway|mysql-connector" backend/pom.xml
```

B 现在负责在当前数据库准备分支加入：

- MyBatis Starter；
- Flyway Core；
- Flyway MySQL；
- MySQL Driver。

当前已验证结果是只存在 MySQL Driver，MyBatis 和 Flyway 尚未加入。

判断方法：

- 找到 MyBatis、Flyway 和 MySQL Driver：依赖已经补齐，可以继续验证；
- 只找到 MySQL Driver：这是当前已知状态，由 B 按第 10 步补齐；
- 命令完全没有输出：先检查是否位于项目根目录。

第 8 步和第 9 步已经完成。B 接下来不再等待 A，而是按第 10 步补齐 JDK 17、Maven Wrapper、MyBatis、Flyway 和测试 Profile。

先提交 B 已完成的纯设计修改：

```powershell
git status --short
git add -- docs/database/README.md
git diff --cached
git commit -m "docs(database): freeze minimal phase-one schema"
```

数据库设计已提交为 `a5a4306`。配置和 V1 已提交为 `5cb8b86`。后续公共依赖由 B 在新的独立提交中修改，提交信息必须说明这是为数据库迁移补齐运行基础，不能夹带业务实现。

#### 8.3.9 第 8 步：创建数据库配置文件

创建目录；已存在时不会覆盖：

```powershell
New-Item -ItemType Directory -Force -Path 'backend/src/main/resources/db/migration'
New-Item -ItemType Directory -Force -Path 'backend/src/main/resources/mapper'
```

用 IDEA 创建或修改 `backend/src/main/resources/application.properties`，只保留公共配置：

```properties
spring.application.name=backend
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}

spring.datasource.username=${DELIVERY_DB_USERNAME}
spring.datasource.password=${DELIVERY_DB_PASSWORD}

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true

mybatis.mapper-locations=classpath*:mapper/**/*.xml
mybatis.configuration.map-underscore-to-camel-case=true
```

创建 `backend/src/main/resources/application-dev.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/delivery_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
```

创建 `backend/src/main/resources/application-test.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/delivery_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
```

检查三个配置文件：

```powershell
Get-Content 'backend/src/main/resources/application.properties'
Get-Content 'backend/src/main/resources/application-dev.properties'
Get-Content 'backend/src/main/resources/application-test.properties'
```

检查是否误写密码：

```powershell
rg -n "spring\.datasource\.(username|password)" backend/src/main/resources
```

期望 password 行只出现：

```text
spring.datasource.password=${DELIVERY_DB_PASSWORD}
```

#### 8.3.10 第 9 步：创建 V1 迁移

用 IDEA 创建：

```text
backend/src/main/resources/db/migration/V1__create_core_tables.sql
```

文件名必须严格保持两个下划线。V1 使用以下最小结构；B 必须逐行理解主键、唯一键、外键和 CHECK 后再提交：

```sql
CREATE TABLE `users` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `account` VARCHAR(50) NOT NULL,
    `password_hash` VARCHAR(100) NOT NULL,
    `nickname` VARCHAR(50) NOT NULL,
    `phone` VARCHAR(20) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_users_account` UNIQUE (`account`),
    CONSTRAINT `chk_users_status`
        CHECK (`status` IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE `merchants` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_merchants_user_id` UNIQUE (`user_id`),
    CONSTRAINT `fk_merchants_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE `shops` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `merchant_id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500) NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'CLOSED',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_shops_merchant_id` UNIQUE (`merchant_id`),
    CONSTRAINT `chk_shops_status`
        CHECK (`status` IN ('OPEN', 'CLOSED', 'TEMPORARILY_CLOSED')),
    CONSTRAINT `fk_shops_merchant`
        FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE `product_categories` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `shop_id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_categories_shop_name` UNIQUE (`shop_id`, `name`),
    CONSTRAINT `fk_categories_shop`
        FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE `products` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `shop_id` BIGINT UNSIGNED NOT NULL,
    `category_id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(1000) NULL,
    `price` DECIMAL(10, 2) NOT NULL,
    `stock` INT UNSIGNED NOT NULL DEFAULT 0,
    `status` VARCHAR(20) NOT NULL DEFAULT 'OFF_SALE',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    INDEX `idx_products_shop_status` (`shop_id`, `status`),
    INDEX `idx_products_category` (`category_id`),
    CONSTRAINT `chk_products_price` CHECK (`price` > 0),
    CONSTRAINT `chk_products_stock` CHECK (`stock` >= 0),
    CONSTRAINT `chk_products_status`
        CHECK (`status` IN ('ON_SALE', 'OFF_SALE')),
    CONSTRAINT `fk_products_shop`
        FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_products_category`
        FOREIGN KEY (`category_id`) REFERENCES `product_categories` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE `cart_items` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `product_id` BIGINT UNSIGNED NOT NULL,
    `quantity` INT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_cart_items_user_product` UNIQUE (`user_id`, `product_id`),
    INDEX `idx_cart_items_user` (`user_id`),
    CONSTRAINT `chk_cart_items_quantity` CHECK (`quantity` > 0),
    CONSTRAINT `fk_cart_items_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_cart_items_product`
        FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE `orders` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(40) NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `shop_id` BIGINT UNSIGNED NOT NULL,
    `total_amount` DECIMAL(10, 2) NOT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_orders_order_no` UNIQUE (`order_no`),
    INDEX `idx_orders_user_created` (`user_id`, `created_at`),
    CONSTRAINT `chk_orders_total` CHECK (`total_amount` > 0),
    CONSTRAINT `chk_orders_status`
        CHECK (`status` IN ('PENDING_PAYMENT')),
    CONSTRAINT `fk_orders_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_orders_shop`
        FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE `order_items` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT UNSIGNED NOT NULL,
    `product_id` BIGINT UNSIGNED NOT NULL,
    `product_name` VARCHAR(100) NOT NULL,
    `unit_price` DECIMAL(10, 2) NOT NULL,
    `quantity` INT UNSIGNED NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_order_items_order` (`order_id`),
    CONSTRAINT `chk_order_items_price` CHECK (`unit_price` > 0),
    CONSTRAINT `chk_order_items_quantity` CHECK (`quantity` > 0),
    CONSTRAINT `fk_order_items_order`
        FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_order_items_product`
        FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);
```

人工检查建表顺序必须是：

```text
users
→ merchants
→ shops
→ product_categories
→ products
→ cart_items
→ orders
→ order_items
```

检查 V1 没有额外范围：

```powershell
rg -n "idempotency|cancelled|DELIVERING|PAID|PREPARING|COMPLETED|deleted|version" backend/src/main/resources/db/migration
```

正常情况下这条命令不应有输出。

到这里，B 当前不依赖 A 的工作已经全部完成。检查并保存配置和 V1：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git status --short
git diff -- docs/database/README.md
git diff -- backend/src/main/resources/application.properties
git diff -- backend/src/main/resources/application-dev.properties
git diff -- backend/src/main/resources/application-test.properties
git diff -- backend/src/main/resources/db/migration/V1__create_core_tables.sql
```

注意：`git diff` 默认不会显示新建且尚未暂存的文件内容，所以还要暂存后检查：

```powershell
git add -- docs/database/README.md
git add -- backend/src/main/resources/application.properties
git add -- backend/src/main/resources/application-dev.properties
git add -- backend/src/main/resources/application-test.properties
git add -- backend/src/main/resources/db/migration/V1__create_core_tables.sql
git diff --cached --stat
git diff --cached
```

人工确认没有密码、没有多余表和业务实现后提交：

```powershell
git commit -m "feat(database): add minimal schema and flyway migration"
git status --short
```

配置和 V1 已提交为 `5cb8b86`。按照 2026-09-05 的新分工，B 不再暂停等待 A，直接进入第 10 步补齐运行环境。仍然不要手工执行 V1；只有 Flyway 可以在正式开发库和测试库执行它。

#### 8.3.11 第 10 步：B 接管运行基础并执行第一次测试库迁移

本步骤是 B 现在的下一步。它分为 10.1～10.8，必须依次执行。B 只接管数据库运行基础，不编写任何业务 Mapper 或 ServiceImpl。

当前机器在 2026-09-05 的实测状态：

```text
分支：feature/b-database-foundation
V1 已提交并推送：5cb8b86
Java：已安装 D:\Dev\Java\JDK17；当前终端默认仍是 25
JAVA_HOME：未设置
Maven：系统没有 mvn
Maven Wrapper：存在，但因脚本空值错误不能启动
MySQL267 服务：Running
localhost:3306：可连接
DELIVERY_DB_USERNAME：未设置
DELIVERY_DB_PASSWORD：未设置
pom.xml：只有 MySQL Driver，没有 MyBatis/Flyway
后端测试：尚未固定 test Profile
```

##### 10.1 先保存本执行文档

进入项目根目录：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git branch --show-current
git status --short
```

确认分支为：

```text
feature/b-database-foundation
```

先把总执行文档和数据库文档中的新分工说明一起提交：

```powershell
git add -- '最小必要范围-ABC前期准备与TDD执行文档.md'
git add -- 'docs/database/README.md'
git diff --cached --stat
git commit -m "docs: add minimal abc preparation and tdd guide"
git status --short
```

该提交只应包含这两份文档。如果还有别的修改，不要混入这个提交。

##### 10.2 启用已经安装的 JDK 17（不下载、不重装）

已经确认本机的 JDK 17 位于 `D:\Dev\Java\JDK17`，版本为 Microsoft OpenJDK 17.0.20.1。现在默认的 `java` 显示 25，仅仅因为终端仍指向 Oracle Java 25，并且 `JAVA_HOME` 没有设置；不代表电脑没有 JDK 17。

先验证现有安装：

```powershell
$deliveryJdk17Path = 'D:\Dev\Java\JDK17'
Test-Path -LiteralPath "$deliveryJdk17Path\bin\java.exe"
Test-Path -LiteralPath "$deliveryJdk17Path\bin\javac.exe"
& "$deliveryJdk17Path\bin\java.exe" -version
& "$deliveryJdk17Path\bin\javac.exe" -version
```

两个 `Test-Path` 应输出 `True`，版本应显示 17。随后把现有 JDK 17 设置为当前用户的 `JAVA_HOME`：

```powershell
[Environment]::SetEnvironmentVariable(
  'JAVA_HOME',
  'D:\Dev\Java\JDK17',
  'User'
)
$env:JAVA_HOME = 'D:\Dev\Java\JDK17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
```

两条版本命令都显示 17 即完成。Java 25 不用卸载，它可以和 JDK 17 共存。

IDEA 中选择 `File → Project Structure → Project SDK → Add SDK / JDK`，路径填 `D:\Dev\Java\JDK17`，language level 选择 17。再到 `Settings → Build Tools → Maven → Runner`，把 JRE 设为 `Project SDK (17)`。

当前电脑不执行下面的下载和安装流程；以下内容仅供将来在另一台完全没有 JDK 17 的电脑上备用：

###### 10.2A 备用安装流程（当前电脑跳过）

如果 `D:\Dev\Java\JDK17` 不存在，才使用 Eclipse Adoptium 官方 API 下载当前最新的 Temurin 17 Windows x64 MSI。先创建临时下载目录：

```powershell
$deliveryToolsDirectory = Join-Path $env:TEMP 'se-practicum-tools'
New-Item -ItemType Directory -Force -Path $deliveryToolsDirectory
$deliveryJdkInstaller = Join-Path $deliveryToolsDirectory 'OpenJDK17.msi'
```

下载安装文件：

```powershell
curl.exe --fail --location `
  'https://api.adoptium.net/v3/installer/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' `
  --output $deliveryJdkInstaller
```

确认文件存在且不是空文件：

```powershell
Get-Item -LiteralPath $deliveryJdkInstaller |
  Select-Object FullName, Length, LastWriteTime
```

`Length` 应明显大于 0。检查数字签名：

```powershell
Get-AuthenticodeSignature -LiteralPath $deliveryJdkInstaller |
  Select-Object Status, StatusMessage, SignerCertificate
```

只有签名状态为 `Valid` 才继续。如果下载失败或签名无效，删除该临时 MSI 并停止，不要安装。

打开安装程序：

```powershell
Start-Process -FilePath 'msiexec.exe' `
  -ArgumentList "/i `"$deliveryJdkInstaller`"" `
  -Wait
```

安装界面中确保启用：

```text
Add to PATH
Set JAVA_HOME variable
```

安装结束后关闭当前 PowerShell，重新打开一个 PowerShell，再进入项目：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
```

查找安装目录：

```powershell
$deliveryJdk17 = Get-ChildItem `
  -LiteralPath 'C:\Program Files\Eclipse Adoptium' `
  -Directory `
  -Filter 'jdk-17*' `
  -ErrorAction SilentlyContinue |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 1

$deliveryJdk17 | Select-Object FullName
```

如果没有输出，说明安装未完成，停止并重新检查安装程序。

把 JDK 17 设置为当前用户的 `JAVA_HOME`，并立即在当前终端生效：

```powershell
[Environment]::SetEnvironmentVariable(
  'JAVA_HOME',
  $deliveryJdk17.FullName,
  'User'
)
$env:JAVA_HOME = $deliveryJdk17.FullName
$env:Path = "$($env:JAVA_HOME)\bin;$env:Path"
```

验证 JDK 17：

```powershell
& "$env:JAVA_HOME\bin\java.exe" -version
& "$env:JAVA_HOME\bin\javac.exe" -version
```

两条输出都必须是 17。若直接执行 `java -version` 仍显示 25，不影响当前检查，但要关闭并重新打开 PowerShell；后续以 `JAVA_HOME` 和 Maven 输出为准。

IDEA 中再设置：

```text
File
→ Project Structure
→ Project SDK
→ 选择刚安装的 JDK 17
→ Project language level 选择 17
```

##### 10.3 修复 Maven Wrapper 的 Windows 空值错误

进入后端目录并再次复现：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$env:MVNW_VERBOSE = 'true'
.\mvnw.cmd -version
```

当前已知错误是：

```text
Cannot index into a null array
Cannot start maven from wrapper
```

原因是 `mvnw.cmd` 把普通 `.m2` 目录当成符号链接，并直接读取空的 `Target[0]`。

用 IDEA 或记事本打开脚本：

```powershell
notepad.exe .\mvnw.cmd
```

找到以下旧代码：

```powershell
$MAVEN_WRAPPER_DISTS = $null
if ((Get-Item $MAVEN_M2_PATH).Target[0] -eq $null) {
  $MAVEN_WRAPPER_DISTS = "$MAVEN_M2_PATH/wrapper/dists"
} else {
  $MAVEN_WRAPPER_DISTS = (Get-Item $MAVEN_M2_PATH).Target[0] + "/wrapper/dists"
}
```

完整替换为：

```powershell
$MAVEN_WRAPPER_DISTS = $null
$MAVEN_M2_ITEM = Get-Item $MAVEN_M2_PATH
if ($null -eq $MAVEN_M2_ITEM.Target -or $MAVEN_M2_ITEM.Target.Count -eq 0) {
  $MAVEN_WRAPPER_DISTS = "$MAVEN_M2_PATH/wrapper/dists"
} else {
  $MAVEN_WRAPPER_DISTS = $MAVEN_M2_ITEM.Target[0] + "/wrapper/dists"
}
```

保存并关闭。检查修改范围：

```powershell
git diff -- mvnw.cmd
```

只应看到上面的空值处理修改。再次执行：

```powershell
.\mvnw.cmd -version
Remove-Item Env:MVNW_VERBOSE -ErrorAction SilentlyContinue
```

第一次执行会从 `maven-wrapper.properties` 指定的地址下载 Maven 3.9.16，可能需要几分钟。成功输出必须同时包含：

```text
Apache Maven 3.9.16
Java version: 17
```

2026-09-05 已在 B 的电脑实测通过：Maven Wrapper 输出 `Apache Maven 3.9.16`，并使用 `D:\Dev\Java\JDK17` 中的 Java 17.0.20.1。因此第 10.3 步已经完成，不再修改 Wrapper，也不再处理 JDK。

如果下载失败，先检查网络，不要把 Maven 压缩包提交进仓库。

##### 10.4 在 pom.xml 加入最小数据库依赖

回到项目根目录：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
notepad.exe .\backend\pom.xml
```

在 `<dependencies>` 内、现有 MySQL Driver 前加入：

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>4.0.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

为什么使用这些依赖：

- MyBatis Starter 4.0.0 官方要求 Spring Boot 4.0+ 和 Java 17+，与当前项目匹配；
- Spring Boot 4.1.1 官方提供 `spring-boot-starter-flyway`；
- Flyway 的 MySQL 支持需要单独加入 `flyway-mysql`；
- Flyway 版本由 Spring Boot 4.1.1 统一管理，不手写版本。

不要加入 JPA、MyBatis-Plus、H2、Testcontainers 或 Flyway Maven Plugin。

保存后检查：

```powershell
Select-String `
  -Path '.\backend\pom.xml' `
  -Pattern 'mybatis|starter-flyway|flyway-mysql|mysql-connector'
```

上面的 `Select-String` 是 Windows PowerShell 自带命令，不要求安装 `rg`。然后单独执行：

```powershell
git diff -- backend/pom.xml
```

确认 Maven 能解析依赖：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
.\mvnw.cmd dependency:tree "-Dincludes=org.mybatis.spring.boot:mybatis-spring-boot-starter,org.flywaydb:flyway-core,org.flywaydb:flyway-mysql,com.mysql:mysql-connector-j"
```

注意：先等 `Set-Location` 执行完并重新出现 `PS ...\backend>` 提示符，再粘贴下一条 Maven 命令。不要把 `git diff`、`Set-Location` 和 Maven 命令粘在同一行。

第一次解析需要联网下载依赖。命令必须以 `BUILD SUCCESS` 结束。

2026-09-05 已由 B 实测完成，解析结果为：MyBatis Starter 4.0.0、Flyway Core/MySQL 12.4.0、MySQL Connector/J 9.7.0，最终输出 `BUILD SUCCESS`。因此第 10.4 步完成，不再增加其他持久化依赖。

##### 10.5 把后端测试固定到 test Profile

打开测试类：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
notepad.exe .\backend\src\test\java\com\delivery\backend\BackendApplicationTests.java
```

在 import 区加入：

```java
import org.springframework.test.context.ActiveProfiles;
```

在 `@SpringBootTest` 下面、类声明上面加入：

```java
@ActiveProfiles("test")
```

最终类开头应该是：

```java
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {
```

检查：

```powershell
Get-ChildItem -LiteralPath '.\backend\src\test' -Recurse -Filter '*.java' |
  Select-String -Pattern 'SpringBootTest|ActiveProfiles'
git diff -- backend/src/test/java/com/delivery/backend/BackendApplicationTests.java
```

这样即使有人忘记设置 `SPRING_PROFILES_ACTIVE`，测试仍优先连接 `delivery_test`，不会误用 `delivery_dev`。

2026-09-05 已由 B 完成：唯一的 Spring Boot 测试类已加入 `@ActiveProfiles("test")`，并通过 Java 17 的 `mvn test-compile` 验证，输出 `BUILD SUCCESS`。这里只完成编译，没有启动测试上下文或连接数据库。

##### 10.6 检查数据库、账号和环境变量

确认 MySQL 服务和端口：

```powershell
Get-Service -Name 'MySQL267' |
  Select-Object Name, Status

Test-NetConnection -ComputerName localhost -Port 3306 |
  Select-Object ComputerName, RemotePort, TcpTestSucceeded
```

期望 `Status` 为 `Running`、`TcpTestSucceeded` 为 `True`。

验证项目账号以及两个数据库；命令会安全询问密码：

```powershell
mysql -u delivery_app -p -e "SHOW DATABASES LIKE 'delivery_dev'; SHOW DATABASES LIKE 'delivery_test';"
```

必须同时看到 `delivery_dev` 和 `delivery_test`。如果账号不存在或库不存在，回到第 4 步创建，不要继续。

为当前 PowerShell 设置变量：

```powershell
$env:DELIVERY_DB_USERNAME = 'delivery_app'
$env:DELIVERY_DB_PASSWORD = Read-Host '输入 delivery_app 数据库密码' -MaskInput
$env:SPRING_PROFILES_ACTIVE = 'test'
```

只检查是否设置，不打印真实值：

```powershell
if ([string]::IsNullOrWhiteSpace($env:DELIVERY_DB_USERNAME)) {
  throw 'DELIVERY_DB_USERNAME 未设置'
}
if ([string]::IsNullOrWhiteSpace($env:DELIVERY_DB_PASSWORD)) {
  throw 'DELIVERY_DB_PASSWORD 未设置'
}
if ($env:SPRING_PROFILES_ACTIVE -ne 'test') {
  throw '当前不是 test Profile'
}
Write-Output '数据库变量和 test Profile 已准备'
```

##### 10.7 提交 B 接管的公共运行基础

在真正迁移前先检查所有代码改动：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git status --short
git diff --check
git diff -- backend/mvnw.cmd
git diff -- backend/pom.xml
git diff -- backend/src/test/java/com/delivery/backend/BackendApplicationTests.java
```

本提交只能包含：

```text
backend/mvnw.cmd
backend/pom.xml
backend/src/test/java/com/delivery/backend/BackendApplicationTests.java
```

提交：

```powershell
git add -- backend/mvnw.cmd
git add -- backend/pom.xml
git add -- backend/src/test/java/com/delivery/backend/BackendApplicationTests.java
git diff --cached --stat
git diff --cached --check
git commit -m "build(database): add mybatis flyway and test runtime"
```

不要提交 JDK、Maven 下载目录、`.m2`、数据库密码或 `target`。

##### 10.8 第一次执行测试库迁移

进入后端目录：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
```

最后确认 Maven 使用 Java 17：

```powershell
.\mvnw.cmd -version
```

然后执行：

```powershell
.\mvnw.cmd clean test
```

应用上下文启动时将发生：

```text
读取 application-test.properties
→ 连接 delivery_test
→ Flyway 扫描 classpath:db/migration
→ 执行 V1__create_core_tables.sql
→ 创建 8 张业务表
→ 创建 flyway_schema_history
→ 运行上下文测试和模块边界测试
```

必须看到 Maven 最终输出：

```text
BUILD SUCCESS
```

运行结束后清理当前终端中的敏感变量：

```powershell
Remove-Item Env:DELIVERY_DB_PASSWORD -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
```

用户名可以保留，也可以清理：

```powershell
Remove-Item Env:DELIVERY_DB_USERNAME -ErrorAction SilentlyContinue
```

失败时按报错分类处理：

| 报错关键词                           | 先检查                                                                  |
| ------------------------------------ | ----------------------------------------------------------------------- |
| `Access denied`                    | DELIVERY_DB_USERNAME/PASSWORD 是否正确；delivery_app 是否有 test 库权限 |
| `Unknown database`                 | `delivery_test` 是否存在                                              |
| `Communications link failure`      | MySQL267 服务和 3306 端口                                               |
| `Unsupported Database: MySQL 26.7` | 记录完整错误；检查实际 Flyway 版本；不要关闭 Flyway或手工建表           |
| `No Flyway database plugin found`  | `flyway-mysql` 是否存在于依赖树                                       |
| `Table already exists`             | test 库是否被手工建过表；不要直接删除，先确认数据来源                   |
| SQL syntax error                     | V1 报错行、逗号、约束名和 MySQL 语法                                    |
| `Cannot start maven from wrapper`  | 10.3 的 Wrapper 修复是否准确                                            |
| Maven 显示 Java 25                   | JAVA_HOME 未指向 JDK 17，重新设置并打开新终端                           |

如果出现 `Unsupported Database: MySQL 26.7`，先执行：

```powershell
.\mvnw.cmd dependency:tree `
  "-Dincludes=org.flywaydb:flyway-core,org.flywaydb:flyway-mysql"
```

把完整版本和错误记录下来，再评估是否将两个 Flyway 组件同步升级。禁止通过 `spring.flyway.enabled=false`、手工执行 V1 或设置 baseline 来掩盖问题。

官方依据：

- MyBatis Spring Boot Starter 兼容表：[https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)；
- Spring Boot 4.1.1 Starter 清单：[https://docs.spring.io/spring-boot/reference/using/build-systems.html](https://docs.spring.io/spring-boot/reference/using/build-systems.html)；
- Flyway MySQL 独立模块说明：[https://documentation.red-gate.com/fd/mysql-277579322.html](https://documentation.red-gate.com/fd/mysql-277579322.html)；
- Maven Wrapper 官方说明：[https://maven.apache.org/tools/wrapper/](https://maven.apache.org/tools/wrapper/)。

#### 8.3.12 第 11 步：检查测试库迁移结果

连接测试库：

```powershell
mysql -u delivery_app -p delivery_test
```

在 `mysql>` 执行：

```sql
SHOW TABLES;

SELECT `version`, `description`, `type`, `script`, `success`
FROM `flyway_schema_history`
ORDER BY `installed_rank`;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'delivery_test'
  AND table_name <> 'flyway_schema_history'
ORDER BY table_name;
```

必须正好看到 8 张业务表：

```text
cart_items
merchants
order_items
orders
product_categories
products
shops
users
```

检查列：

```sql
SHOW CREATE TABLE users;
SHOW CREATE TABLE products;
SHOW CREATE TABLE cart_items;
SHOW CREATE TABLE orders;
SHOW CREATE TABLE order_items;
```

检查外键：

```sql
SELECT table_name, constraint_name, referenced_table_name
FROM information_schema.referential_constraints
WHERE constraint_schema = 'delivery_test'
ORDER BY table_name, constraint_name;
```

检查唯一约束和索引：

```sql
SELECT table_name, index_name,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_index,
       non_unique
FROM information_schema.statistics
WHERE table_schema = 'delivery_test'
GROUP BY table_name, index_name, non_unique
ORDER BY table_name, index_name;
```

退出：

```sql
exit
```

#### 8.3.13 第 12 步：做最小约束验证

连接测试库：

```powershell
mysql -u delivery_app -p delivery_test
```

先写一组可回滚的基础数据：

```sql
START TRANSACTION;

INSERT INTO users(account, password_hash, nickname, phone)
VALUES ('b_schema_test', 'not-a-real-login-hash', 'B Test', NULL);

SET @test_user_id = LAST_INSERT_ID();

INSERT INTO merchants(user_id, name)
VALUES (@test_user_id, 'B Test Merchant');

SET @test_merchant_id = LAST_INSERT_ID();

INSERT INTO shops(merchant_id, name, status)
VALUES (@test_merchant_id, 'B Test Shop', 'CLOSED');

SET @test_shop_id = LAST_INSERT_ID();

INSERT INTO product_categories(shop_id, name)
VALUES (@test_shop_id, 'B Test Category');

SET @test_category_id = LAST_INSERT_ID();

INSERT INTO products(shop_id, category_id, name, price, stock)
VALUES (@test_shop_id, @test_category_id, 'B Test Product', 1.00, 1);

SET @test_product_id = LAST_INSERT_ID();
```

逐条执行以下错误数据，并确认每条都被数据库拒绝。某条失败后如果当前客户端仍可继续，再执行下一条：

```sql
INSERT INTO users(account, password_hash, nickname)
VALUES ('b_schema_test', 'another-hash', 'Duplicate Account');

INSERT INTO cart_items(user_id, product_id, quantity)
VALUES (@test_user_id, @test_product_id, 0);

INSERT INTO products(shop_id, category_id, name, price, stock)
VALUES (@test_shop_id, @test_category_id, 'Invalid Price', 0.00, 1);

INSERT INTO shops(merchant_id, name, status)
VALUES (@test_merchant_id, 'Second Shop', 'OPEN');

INSERT INTO orders(order_no, user_id, shop_id, total_amount)
VALUES ('B-INVALID-TOTAL', @test_user_id, @test_shop_id, 0.00);
```

最后清理本事务的合法测试数据：

```sql
ROLLBACK;

SELECT COUNT(*) AS remaining_test_users
FROM users
WHERE account = 'b_schema_test';

exit
```

期望 `remaining_test_users` 为 0。注意：这些 SQL 只验证数据库底线；分类是否属于同一店铺、购物车是否同一店铺等跨行规则仍由后续 ServiceImpl 校验。

#### 8.3.14 第 13 步：验证迁移可重复运行

在后端目录再次运行完全相同的测试：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$env:SPRING_PROFILES_ACTIVE = 'test'
.\mvnw.cmd test
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
```

第二次运行必须成功，Flyway 不能重复创建表。再次检查历史：

```powershell
mysql -u delivery_app -p delivery_test -e "SELECT version, script, success FROM flyway_schema_history ORDER BY installed_rank;"
```

应只有一条 V1 成功记录，而不是重复 V1。

#### 8.3.15 第 14 步：迁移开发库

使用 dev Profile 启动一次后端：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$env:SPRING_PROFILES_ACTIVE = 'dev'
.\mvnw.cmd spring-boot:run
```

看到应用成功启动和 Flyway 成功后按 `Ctrl+C` 停止，再清理本终端变量：

```powershell
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
```

检查开发库：

```powershell
mysql -u delivery_app -p delivery_dev -e "SHOW TABLES;"
mysql -u delivery_app -p delivery_dev -e "SELECT version, script, success FROM flyway_schema_history ORDER BY installed_rank;"
```

比较两个库的业务表名：

```powershell
mysql -u delivery_app -p -N -e "SELECT table_schema, table_name FROM information_schema.tables WHERE table_schema IN ('delivery_dev','delivery_test') AND table_name <> 'flyway_schema_history' ORDER BY table_schema, table_name;"
```

两个库都必须是相同的 8 张业务表。

#### 8.3.16 第 15 步：建立持久化目录，不写业务实现

回到根目录：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
```

创建各模块准备目录：

```powershell
$deliveryModules = @('user', 'merchant', 'restaurant', 'item', 'shopping', 'order')
foreach ($deliveryModule in $deliveryModules) {
  New-Item -ItemType Directory -Force -Path "backend/src/main/java/com/delivery/backend/$deliveryModule/entity"
  New-Item -ItemType Directory -Force -Path "backend/src/main/java/com/delivery/backend/$deliveryModule/mapper"
  New-Item -ItemType Directory -Force -Path "backend/src/main/java/com/delivery/backend/$deliveryModule/service/impl"
  New-Item -ItemType Directory -Force -Path "backend/src/main/resources/mapper/$deliveryModule"
}
```

Git 不跟踪空目录。若 A 已创建接口和空壳，B 直接复用；若没有，不要为了保留空目录加入无意义 Java 类。等第一个 Red 切片再创建实际 Entity、Mapper 和 ServiceImpl。

检查当前阶段不应出现业务实现：

```powershell
$persistenceMatches = Get-ChildItem `
  -LiteralPath '.\backend\src\main\java' `
  -Recurse `
  -Filter '*.java' |
  Select-String -Pattern 'class\s+\w*ServiceImpl|interface\s+\w*Mapper|@(Insert|Update|Delete|Select)\b'

if ($persistenceMatches) {
  $persistenceMatches
} else {
  Write-Output '未发现提前编写的持久化业务实现'
}
```

如果只看到 A 明确交接的空壳可以保留；若出现真实注册、购物车、订单逻辑，停止并确认是否违反 Red 先行顺序。

2026-09-05 已由 B 完成：`user`、`merchant`、`restaurant`、`item`、`shopping`、`order` 六个模块的 `entity`、`mapper`、`service/impl` 和资源 Mapper 目录共 24 个均已存在；检查未发现 ServiceImpl、Mapper 接口或 MyBatis SQL 注解。目录为空，所以 Git 没有新增文件，这是预期结果，不创建 `.gitkeep` 或无意义 Java 类。

#### 8.3.17 第 16 步：保存数据库准备证据

创建 `docs/database/preparation-log.md`，至少填写：

```markdown
# 数据库准备记录

- 日期：
- 执行人：B
- 分支：feature/b-database-foundation
- MySQL 版本：
- 开发库：delivery_dev
- 测试库：delivery_test
- 迁移：V1__create_core_tables.sql

## 验证结果

- [ ] delivery_test 有 8 张业务表
- [ ] delivery_dev 有 8 张业务表
- [ ] 两个库均有一条成功的 V1 历史
- [ ] 重复运行迁移成功
- [ ] account 唯一约束有效
- [ ] 一个用户最多一个商家
- [ ] 一个商家最多一个店铺
- [ ] 同一购物车商品唯一
- [ ] price、quantity、total_amount 正数约束有效
- [ ] 没有密码进入 Git

## 执行命令

记录实际运行的命令，不记录密码。

## 问题和处理

记录实际错误及处理方法；没有则写“无”。
```

最终运行后端测试并把结果摘要写入该日志：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$env:SPRING_PROFILES_ACTIVE = 'test'
.\mvnw.cmd clean test
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
```

不要提交 `target/`、本地日志文件或数据库导出文件。

2026-09-05 已由 B 完成：已创建 `docs/database/preparation-log.md`，记录 MySQL 26.7.0、两个数据库、V1 历史、8 张业务表、约束验证、重复迁移、后端测试摘要和问题处理；敏感信息扫描未发现密码值，未创建数据库导出，也未把 `target/` 加入 Git。

#### 8.3.18 第 17 步：敏感信息和改动范围检查

回到根目录：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git status --short
git diff --stat
git diff
```

检查不应提交的文件：

```powershell
git status --short --ignored
```

检查配置只引用环境变量：

```powershell
rg -n "spring\.datasource\.(username|password)" backend/src/main/resources
rg -n "DELIVERY_DB_PASSWORD" backend docs
```

人工打开 `git diff` 再确认：

- 没有真实密码；
- 没有 root 账号；
- 没有个人绝对路径；
- 没有测试账号密码；
- 没有 `target/`；
- 没有业务 ServiceImpl/Mapper；
- 没有第 5、7 节以外的字段和规则。

#### 8.3.19 第 18 步：提交和推送

配置和迁移已经在第 9 步结束时提交。先确认该提交存在：

```powershell
git log -10 --oneline --grep "minimal schema and flyway migration"
```

期望至少看到一条 `feat(database): add minimal schema and flyway migration`。这里不重复提交配置。

现在提交迁移后的验证证据：

```powershell
git add -- docs/database/preparation-log.md
git diff --cached
git commit -m "test(database): record migration and constraint verification"
```

最终检查：

```powershell
git status --short
git log -5 --oneline --decorate
```

工作区必须干净。推送：

```powershell
git push -u origin feature/b-database-foundation
```

PR 标题：

```text
feat(database): prepare minimal MySQL and Flyway foundation
```

PR 描述必须写明：

```markdown
## 完成

- 配置 delivery_dev 和 delivery_test
- 使用环境变量读取数据库账号和密码
- Flyway V1 创建 8 张阶段 1 核心表
- 验证外键、唯一约束和数值 CHECK
- 重复运行迁移成功

## 验证

- `SPRING_PROFILES_ACTIVE=test .\mvnw.cmd clean test`
- 测试库和开发库均有 8 张业务表
- Flyway V1 历史成功且不重复

## 本 PR 明确未实现

- 未实现任何业务 Mapper
- 未实现任何 ServiceImpl 业务逻辑
- 未实现注册、登录、商品、购物车和订单接口
```

#### 8.3.20 第 19 步（2026-09-05 最终方案）：把共同 Red 基线合入 develop，三人从同一版本开始 TDD

当前 B 已推送至 `4ee5265`，A 的最新远程提交是 `09ad704`，远程 `develop` 基线是 `b4b6548`。A 已新增 Controller、Service 接口和 26 个预期失败的 Service 契约测试。A+B 已在 `feature/abc-pre-tdd-integration` 完成集成，合并提交为 `6b2d78e`。

团队最终决定把这套共同 Red 基线直接合入 `develop`，让 A、B、C 都从同一个远程版本创建各自的 TDD 分支。合入前必须确认：Java 17 编译成功，143 个 Controller/公共层测试全绿，完整测试的 26 个 Error 全部且仅仅来自尚未提供 ServiceImpl。合入后 `develop` 暂时红灯是团队明确接受的 TDD 起点，不得把这 26 个已知 Error 当成环境故障，也不得要求某一个人单独把其他人的模块一起补完。

A 的契约与 V1 已知存在四类差异：独立商家账号、分类排序和逻辑删除、商品乐观锁版本、订单幂等与取消状态。这些差异必须在 A 的 Red 进入历史后通过新的 V2 迁移解决，禁止修改已经执行并推送的 V1。

详细命令、冲突处理、合入 `develop`、其他成员首次安装和拉取步骤见本节“19A：共同 Red 基线发布操作手册”。

##### 团队共同约定（合入 develop 前必须读）

本次允许 `develop` 暂时包含 26 个预期 Error，原因是它们就是六个业务模块的 TDD Red 契约。三个人必须在自己的分支开发，不能直接在 `develop` 写业务代码。每个人只负责分配给自己的模块，并至少保证自己的模块测试变绿；三人汇总后再以 169 个测试全部通过作为阶段完成标准。

共同 Red 基线发布后，每个人更新本地 `develop`：

```powershell
git switch develop
git pull --ff-only
git status --short
```

做最后一次测试库验证：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$env:SPRING_PROFILES_ACTIVE = 'test'
.\mvnw.cmd clean test
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
```

此时运行完整测试会得到 `143 Passed + 26 Errors`，不是全部通过。B 给 A、C 的交接消息改为：

```text
B 数据库准备和 A 的接口契约已经合入 develop：Flyway V1、8 张表、
公共配置、Controller、Service 接口和契约测试均已同步。当前完整测试
固定为 143 个通过、26 个 Service Red。请从最新 develop 创建个人 TDD
分支，不要直接在 develop 写业务代码或修改已发布的 V1。
```

B 不再等待 A；A、B、C 按任务分配，从最新 `develop` 分别创建自己的业务分支并开始 TDD。需要调整数据库时新增 V2，禁止修改已经发布并在 B 电脑执行过的 V1。

B 的最终准备验收：

- [ ] `delivery_dev` 和 `delivery_test` 均存在；
- [ ] 项目账号只拥有两个项目库的权限；
- [ ] 密码仅存在于本机环境变量；
- [ ] 三个 application 配置文件不含密码；
- [ ] V1 只创建 8 张最小业务表；
- [ ] 两个库迁移结果一致；
- [ ] V1 重复检查成功且不重复执行；
- [ ] 必要外键、唯一约束和 CHECK 生效；
- [ ] 后端 143 个公共层/Controller 测试通过，完整测试仅保留 26 个约定的 Service Red；
- [ ] 准备日志已提交；
- [ ] 没有提前提交业务 Mapper 或 ServiceImpl；
- [ ] 共同 Red 基线已发布到远程 `develop`，本地工作区干净。

#### 8.3.20A：共同 Red 基线发布操作手册

##### 19A.1 当前状态和执行文档提交

当前已位于 `feature/abc-pre-tdd-integration`，A+B 代码合并提交 `6b2d78e` 已完成；本执行文档已从 stash 恢复，但仍是未跟踪文件。先把最终方案写入版本历史：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git branch --show-current
git status --short
git add -- '最小必要范围-ABC前期准备与TDD执行文档.md'
git diff --cached --check
git diff --cached --stat
git commit -m 'docs: publish shared red baseline workflow'
```

第一条必须输出 `feature/abc-pre-tdd-integration`。提交完成后执行：

```powershell
git status --short
git log -3 --oneline --decorate
```

`git status --short` 必须没有输出。原 stash 暂时保留作备份，确认远程 `develop` 已包含本文档后再决定是否删除。

##### 19A.2 把最新 develop 拿到集成分支

```powershell
git merge --no-ff origin/develop -m 'merge: sync latest develop before pre-tdd integration'
git status --short
```

命令是在集成分支执行，只会修改集成分支，不会修改 `develop`。发生冲突时先执行 `git status`；若要完全撤销本次合并，执行 `git merge --abort`。

##### 19A.3 合并 A 的最新分支并停在提交前

```powershell
git merge --no-ff --no-commit origin/feature/backend-architecture
git status
git diff --name-only --diff-filter=U
```

已预检到重点重叠文件是 `backend/pom.xml`、`backend/src/main/resources/application.properties` 和 `BackendApplicationTests.java`。不要对前两个文件整份选择 ours 或 theirs。

##### 19A.4 冲突解决规则

`pom.xml` 最终必须同时保留：Java 17、WebMVC、Validation、JaCoCo、MyBatis、Flyway Starter、Flyway MySQL、MySQL Driver、Lombok 和测试依赖。MyBatis 只保留一份，采用 A 冻结的 4.1.0。按 A 的简单四层 MVC 决定接受移除 Spring Modulith 及其 BOM。

`application.properties` 最终必须同时保留：

```properties
spring.application.name=backend
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
spring.datasource.username=${DELIVERY_DB_USERNAME}
spring.datasource.password=${DELIVERY_DB_PASSWORD}
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
mybatis.mapper-locations=classpath*:mapper/**/*.xml
mybatis.configuration.map-underscore-to-camel-case=true
spring.jackson.deserialization.fail-on-unknown-properties=true
security.jwt.secret=${JWT_SECRET:local-development-secret-change-before-production}
security.jwt.expiration-seconds=${JWT_EXPIRATION_SECONDS:7200}
```

接受 A 删除 `BackendApplicationTests.java`、旧 `*Module`、旧顶层快照类和 Modulith `package-info.java`，使用 A 新增的 Controller、Service 接口与测试。必须保留 B 的两个 Profile、V1 迁移、数据库 README 和 preparation log。

本次合并禁止创建 ServiceImpl、Mapper、Entity 或 V2，也禁止改写 V1。

解决所有冲突后执行：

```powershell
git diff --name-only --diff-filter=U
git diff --check
git status --short
git add --all
git diff --cached --stat
git commit -m 'merge: integrate backend contracts with database foundation'
```

未解决冲突文件列表必须为空，`git diff --check` 必须无错误。

##### 19A.5 验证绿色基础与预期 Red

在同一个 PowerShell 中设置 Java、数据库账号和 test Profile：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$env:JAVA_HOME = 'D:\Dev\Java\JDK17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DELIVERY_DB_USERNAME = 'delivery_app'
$env:DELIVERY_DB_PASSWORD = Read-Host '输入 delivery_app 数据库密码' -MaskInput
$env:SPRING_PROFILES_ACTIVE = 'test'
.\mvnw.cmd -version
.\mvnw.cmd clean test-compile
.\mvnw.cmd "-Dtest=*ControllerTests,DefaultJwtTokenServiceTests,AuthenticationInterceptorTests,GlobalExceptionHandlerTests" test
```

Maven 必须使用 Java 17；编译必须成功；A 的 Controller、安全和异常测试应为 143 个全部通过。再验证两个库仍只有一条成功 V1：

```powershell
mysql -u delivery_app -p delivery_test -e "SELECT version, script, success FROM flyway_schema_history ORDER BY installed_rank;"
mysql -u delivery_app -p delivery_dev -e "SELECT version, script, success FROM flyway_schema_history ORDER BY installed_rank;"
```

然后运行完整测试，建立正式 Red 基线：

```powershell
.\mvnw.cmd clean test
```

此时完整测试应发现 169 个测试，其中 143 个公共层/Controller 测试通过，26 个 Service 契约测试仅因 ServiceImpl 尚不存在而失败。这个 `BUILD FAILURE` 是下一阶段 TDD 的预期 Red，不是前期集成失败。

若发生编译失败、数据库/Flyway 失败、Controller 测试失败、Java 不是 17、V1 checksum 变化，或者失败原因不是缺少 Service 实现，则不能进入 TDD。

##### 19A.6 推送集成分支并正式合入 develop

2026-09-05 已执行完成：共同 Red 基线已直接推送到远程 `develop`，合并提交为 `8723448`。远程 `origin/develop` 已包含 A 的接口契约、B 的数据库基础、143 个绿色基础测试、26 个预期 Service Red 以及本执行文档。没有额外推送远程集成分支，因为最终共享入口已经统一为 `develop`。

以下命令保留为本次发布记录；当前电脑不要重复执行这次合并：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git status --short
git fetch origin --prune
git switch develop
git pull --ff-only origin develop
git merge --no-ff feature/abc-pre-tdd-integration -m 'merge: publish shared pre-tdd red baseline'
git push origin develop
```

如果将来重新发布新的共同基线，合并最新 `origin/develop` 后必须重新执行 19A.5 的编译、143 个基础测试和完整测试。结果仍须为 `143 Passed + 26 Errors`，且 26 个 Error 只能由缺少 ServiceImpl 引起。

团队已明确接受 Red 基线。若在另一份克隆中重做同样的发布流程，可执行：

```powershell
git switch develop
git pull --ff-only origin develop
git merge --no-ff feature/abc-pre-tdd-integration -m 'merge: publish shared pre-tdd red baseline'
git status --short
git log -5 --oneline --decorate
git push origin develop
```

若 `git push origin develop` 提示分支受保护，不要强推。改为在远程仓库创建从 `feature/abc-pre-tdd-integration` 指向 `develop` 的普通 PR，在 PR 说明中明确写：

```text
共同 TDD Red 基线：Java 17 编译成功；143 个基础测试通过；
26 个 Service 契约测试因 ServiceImpl 尚未实现而预期报错。
团队已同意将该 Red 基线发布到 develop，供 A、B、C 创建个人分支。
```

PR 合并后，本地执行：

```powershell
git switch develop
git pull --ff-only origin develop
git status --short
```

##### 19A.7 A、B、C 拉取后的统一开发起点

每个人都必须先更新共同基线，再创建自己的分支。不要在 `develop` 直接开发：

```powershell
Set-Location '自己的项目根目录'
git fetch origin --prune
git switch develop
git pull --ff-only origin develop
git status --short
```

确认工作区干净后创建个人分支。分支名按实际任务替换：

```powershell
# A 执行
git switch -c feature/a-tdd

# B 执行
git switch -c feature/b-tdd

# C 执行
git switch -c feature/c-tdd
```

三段命令只能由对应成员执行一段。创建后用以下命令确认起点：

```powershell
git branch --show-current
git log -3 --oneline --decorate
```

##### 19A.8 别人的电脑能否拉取后直接使用

结论：代码和项目依赖版本可以自动同步，但操作系统级工具、数据库实例和密码不能通过 Git 自动同步。第一次在一台新电脑运行时，仍需完成一次本机环境准备。

必须在每台电脑安装或确认：

| 本机项目 | 是否随 Git 下载 | 最低要求 | 用途 |
| --- | --- | --- | --- |
| Git | 否 | 能执行 `git --version` | 拉取、建分支、提交 |
| JDK | 否 | JDK 17，`java -version` 和 Maven 都显示 17 | 编译、测试、运行后端 |
| Maven | 不必手工安装 | 使用仓库里的 `backend/mvnw.cmd` | Wrapper 首次联网自动下载 Maven 3.9.16 |
| MySQL Server | 否 | 本机运行，监听 `localhost:3306`；建议与团队使用相同版本 | 提供 `delivery_dev`、`delivery_test` |
| MySQL 命令行或 Workbench | 否，二选一即可 | 能创建数据库和授权 | 只用于首次创建空库/账号及排错 |
| Node.js 与 npm | 否 | 安装包含 npm 的 Node LTS | 安装和运行前端依赖 |
| IDEA/VS Code | 否，也不是强制 | 任意可用版本 | 编辑代码；不能代替 JDK/MySQL/Node |
| `rg` | 否，也不是必需 | 可不安装 | 仅用于快速搜索，不影响项目运行 |

后端 Java 依赖不需要逐个安装。`backend/pom.xml` 已固定 Spring Boot、MyBatis、Flyway、MySQL Driver、Validation、Lombok 和测试依赖；第一次运行 Maven Wrapper 时会下载到每个人自己的 Maven 缓存。前端依赖也不需要逐个安装，`frontend/package-lock.json` 已固定依赖树，执行 `npm ci` 会自动下载。

每台新电脑的首次后端准备：

```powershell
java -version
git --version
Set-Location '自己的项目根目录\backend'
.\mvnw.cmd -version
```

两条 Java 版本都必须显示 17。如果机器装了多个 JDK，在当前 PowerShell 临时指定 JDK 17：

```powershell
$env:JAVA_HOME = '这台电脑实际的JDK17目录'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
.\mvnw.cmd -version
```

不得照抄 B 电脑的 `D:\Dev\Java\JDK17`，除非另一台电脑确实安装在完全相同的目录。

MySQL 只会由 Flyway 自动创建“表”，不会自动安装 MySQL Server，也不能在目标数据库尚不存在时自动创建 `delivery_dev` 和 `delivery_test`。每台电脑第一次需要用管理员账号创建两个空库和本机应用账号；SQL 与第 8.3.4 节完全一致。随后设置各自电脑的环境变量，真实密码不能提交到 Git：

```powershell
[Environment]::SetEnvironmentVariable('DELIVERY_DB_USERNAME', 'delivery_app', 'User')
[Environment]::SetEnvironmentVariable('DELIVERY_DB_PASSWORD', '这台电脑为delivery_app设置的密码', 'User')
$env:DELIVERY_DB_USERNAME = 'delivery_app'
$env:DELIVERY_DB_PASSWORD = Read-Host '输入本机 delivery_app 密码' -MaskInput
```

数据库准备好后，在 `backend` 目录运行：

```powershell
$env:SPRING_PROFILES_ACTIVE = 'test'
.\mvnw.cmd clean test
```

Flyway 会按照仓库中的 `V1__create_core_tables.sql` 自动建立表和迁移历史。共同 Red 基线阶段，预期仍是 143 个通过、26 个因缺少 ServiceImpl 而报错；如果是 `Access denied`、`Unknown database`、连接拒绝或 Flyway checksum 错误，则是本机数据库没有正确配置，不能算预期 Red。

每台新电脑的首次前端准备：

```powershell
node -v
npm -v
Set-Location '自己的项目根目录\frontend'
npm ci
npm run build
```

`npm ci` 和 Maven Wrapper 第一次执行都需要联网，之后大部分依赖会使用本机缓存。当前 B 电脑虽然能找到 Node 24.19.0，但 `npm` 命令不可用；要运行前端，仍需安装或修复一个自带 npm 的 Node LTS 环境。

第一个需要数据库变更的 Green 提交应新增 `V2__align_schema_with_service_contracts.sql`，补齐独立商家账号、分类排序/逻辑删除、商品版本、订单幂等及取消字段；所有人不得修改已经发布的 V1。

### 8.4 C：前端和联调准备

按顺序完成：

1. 安装带 npm 的 Node LTS，验证 `node -v`、`npm -v`；
2. 执行 `npm ci` 和 `npm run build`；
3. 在 Vite 中把 `/api` 代理到 `http://localhost:8080`；
4. Axios 的 `baseURL` 固定 `/api/v1`，删除 Bearer Token 逻辑；
5. 把 API 路径全部改成第 6 节：`shops`、`cart-items`、共用登录；
6. 保留现有页面布局，不重新设计；
7. 建立统一 loading、错误提示和空数据处理的最小用法；
8. 列出每个页面所需字段，与 A 的 DTO 对齐；
9. 页面仍可暂时显示示例数据，但必须标记“未联调”，不能记录为功能完成。

C 的准备验收：

```powershell
cd frontend
npm ci
npm run build
npm run dev
```

要求：构建成功、路由可打开、访问 `/api/v1/...` 会通过 Vite 代理到后端；即使后端返回 404/501，也不能出现跨域错误。

### 8.5 合并顺序

```text
A 公共后端依赖与架构
        ↓
B 数据库配置与 V1 迁移
        ↓
C 前端代理与 API 路径
        ↓
三人从最新 develop 做一次联合验收
```

每个 PR 只合并准备内容，不夹带真实业务实现。

---

## 9. “可以开始 TDD”的唯一判定

以下全部满足后才能开始用户注册：

- [ ] 三人都能拉取并运行最新 `develop`；
- [ ] 命令行和 IDEA 都使用 JDK 17；
- [ ] Maven Wrapper 可用，后端现有测试全绿；
- [ ] JaCoCo 能生成报告；
- [ ] npm 可用，`npm ci` 和 `npm run build` 全绿；
- [ ] MySQL 开发库、测试库分离；
- [ ] Flyway 自动建立 8 张最小表；
- [ ] 数据库密码不在代码、文档和 Git 中；
- [ ] 统一响应、全局异常、参数校验和 Session 拦截骨架可运行；
- [ ] 六个模块目录和 Service 方法签名冻结；
- [ ] 前端 `/api` 代理生效；
- [ ] 第 6 节 API 与前端 API 文件完全一致；
- [ ] 第 7 节已经由 A、B 共同确认；
- [ ] `git status --short` 为空；
- [ ] 三个准备 PR 已合并到 `develop`。

只要有一项未满足，就继续修准备问题，不进入业务代码。

---

## 10. 业务阶段的最小 TDD 流程

### 10.1 固定切片顺序

按依赖从小到大执行：

1. 普通用户注册；
2. 共用登录；
3. 本人信息查询/修改；
4. 商家注册；
5. 店铺创建和状态修改；
6. 店铺列表/详情；
7. 分类新增/列表；
8. 商品新增/修改；
9. 商品列表/详情；
10. 购物车增删改查；
11. 创建订单；
12. 订单列表/详情。

不要同时铺开多个后端业务切片。

### 10.2 每个切片的固定闭环

```text
A 与 B 确认本切片规则
→ A 写成功、失败、边界测试
→ A 运行并保存失败结果（Red）
→ A 提交 test(...): ... [RED]
→ B 写刚好让测试通过的 Mapper + ServiceImpl
→ B 运行并保存通过结果（Green）
→ B 提交 feat(...): ... [GREEN]
→ A/B 只在全绿前提下去重和改名（Refactor）
→ C 接真实 API，删除对应页面假数据
→ 三人联调并执行全部回归
→ C 更新日志、测试记录和接口状态
→ 合并 develop
```

### 10.3 A 每个切片最少测试

每个接口至少有：

- 1 个成功场景；
- 1 个参数/边界失败场景；
- 1 个身份、归属、状态或资源不存在场景（适用时）；
- 对写操作检查数据库最终状态；
- 对事务操作检查失败回滚。

不要为了凑数量重复同一断言。覆盖率是结果，不代替规则测试。

### 10.4 Git 证据

推荐提交信息：

```text
test(user): define registration behavior [RED]
feat(user): implement registration [GREEN]
refactor(user): simplify registration validation
feat(frontend): connect customer registration
docs(test): record registration red-green result
```

Red 记录至少包含：测试类、执行命令、失败测试名和失败原因。Green 记录至少包含：同一命令、通过测试数和提交哈希。不要只保存截图，文本日志也要入库。

---

## 11. 阶段 1 完成判定

只有同时满足以下条件，模块才算完成：

- 接口与第 6 节一致；
- A 的成功、失败、边界测试先提交；
- B 的实现只覆盖第 7 节规则；
- C 已删除对应页面假数据并真实联调；
- HTTP 状态码和统一响应正确；
- 数据库结果正确，事务失败能回滚；
- 全量回归测试通过；
- 核心接口覆盖率 100%，关键业务方法覆盖率不低于 90%；
- 测试日志、覆盖率、开发日志和 AI 使用点已记录；
- 三个人都能解释该切片的规则、测试、SQL 和页面行为。

阶段 1 完成后停止加功能，等待教师发布阶段 2 变更。不要自行实现“可能会考”的扩展。

---

## 12. 当前第一步

三人下一次共同工作时只做以下事情：

1. A 修复 JDK 17 和 Maven Wrapper；
2. B 验证 MySQL 两个数据库并把旧表设计裁剪为第 5 节；
3. C 修复 npm 并验证当前前端构建；
4. 三人确认第 6、7 节，不讨论额外功能；
5. 建立三个准备分支并按第 8 节提交；
6. 准备验收清单全绿后，A 才开始“普通用户注册”的 Red 测试。

这条顺序是当前最短路径。
