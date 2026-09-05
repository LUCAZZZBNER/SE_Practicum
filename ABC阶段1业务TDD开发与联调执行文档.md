# ABC 阶段 1 业务 TDD 开发与联调执行文档

> 当前阶段：前期架构、接口契约、数据库 V1 和共同 Red 基线已经发布；现在进入业务 TDD、数据库 V2、前端接入和联调阶段。
>
> 当前共同基线：远程 `develop`；2026-09-05 发布时为 169 个测试，其中 143 个公共层/Controller 测试通过，26 个 Service 契约测试因实现尚不存在而处于预期 Red。
>
> 本文替代原《最小必要范围-ABC前期准备与TDD执行文档》。旧文档中的 22 接口、共用登录、HttpSession、`/shops/{shopId}/status`、排除订单取消/幂等/乐观锁等结论全部作废。

---

## 1. 先确定什么说了算

出现冲突时，严格按以下优先级处理：

1. 根目录课程要求 `26271学期-软件工程综合实践.md`；
2. `docs/software-requirements-specification.md`，决定阶段 1 必须实现的业务；
3. `docs/api/backend-api-design.md`，决定 HTTP 路径、字段、权限、状态码和错误码；
4. 已冻结的 Controller、Service 接口和 Service 契约测试，作为可执行契约；
5. `docs/architecture/backend-architecture-design.md`，决定四层结构、包边界和调用方向；
6. 本执行文档，决定三个人按什么顺序操作；
7. 数据库设计、准备日志和旧执行手册，只记录实现或历史，不能缩小前六项已经确定的需求。

如果 Service 测试与 SRS/API 冲突，A 先修正测试和接口并提交新的 Red，B 不得通过修改业务含义来迁就错误测试。如果数据库 V1 与 SRS/API 冲突，保留 V1，通过 V2 修正，禁止重写已经发布的 V1。

### 1.1 已确认的冲突及最终结论

| 主题 | 已作废的旧结论 | 当前唯一结论 |
| --- | --- | --- |
| 商家账号 | 商家依附 `users`，与用户共用登录 | 商家使用独立账号、密码、状态和登录接口 |
| 用户登录 | 用户和商家共用 `POST /users/login` | 用户使用 `POST /users/login`，商家使用 `POST /merchants/login` |
| 认证 | `HttpSession` | JWT Bearer：`Authorization: Bearer <accessToken>` |
| 店铺修改 | `PATCH /shops/{shopId}/status` | `PATCH /shops/{shopId}`，可局部修改名称、简介和状态 |
| 购物车路径 | `/cart/items` 或其他写法 | `/api/v1/cart-items` |
| 接口数量 | 22 个最小接口 | 30 个阶段 1 HTTP 接口 |
| 分类 | 只新增和查询 | 还必须修改、排序和逻辑删除 |
| 商品 | 不做版本控制 | PATCH 必须使用 `version` 做乐观锁 |
| 订单 | 不做幂等和取消 | 创建必须使用 `X-Idempotency-Key`；必须支持取消待支付订单 |
| 订单状态 | 只有 `PENDING_PAYMENT` | 模型支持全部文档状态；阶段 1 实际状态变化至少包含 `PENDING_PAYMENT → CANCELLED` |
| V1 | 被当作最终业务数据库 | V1 只是已发布的初始结构，业务实现前必须新增 V2 对齐契约 |

---

## 2. 当前项目已经完成什么

已经完成：

- Java 17、Spring Boot、Maven Wrapper；
- Spring MVC 四层代码骨架；
- MyBatis、Flyway、MySQL Driver；
- `delivery_dev`、`delivery_test` 本机数据库和 V1 迁移；
- 6 个 Controller、6 个 Service 接口；
- JWT Bearer、安全拦截器、统一响应和异常映射；
- 143 个公共层/Controller 绿色测试；
- 26 个 Service 业务契约 Red 测试；
- A+B 基线已经合入远程 `develop`。

尚未完成：

- ServiceImpl；
- Entity/Record；
- DAO/MyBatis Mapper 和 XML SQL；
- 对齐 SRS/API 的 V2 数据库迁移；
- DAO/数据库/事务测试；
- 30 个接口的真实业务响应；
- 前端按 JWT 和 30 个接口接入；
- 全量测试全绿和最终联调证据。

### 2.1 当前架构是不是四层

是简单四层 MVC：

```text
HTTP
  ↓
Controller：路径、参数校验、读取可信登录主体、包装响应
  ↓
Service 接口 / ServiceImpl：业务规则、权限、事务、跨模块编排
  ↓
DAO/MyBatis Mapper：SQL、分页、条件更新、数据库记录映射
  ↓
MySQL：表、索引、唯一约束、外键、CHECK、Flyway 历史
```

每个业务模块内部拥有自己的 `controller/service/dao/entity`，禁止建立全局大目录。Controller 不能写业务判断；DAO 不能调用别的模块；跨模块只能调用对方 Service 接口，不能直接调用对方 DAO 或使用对方 Entity。

---

## 3. 30 个必须实现的 HTTP 接口

原文计数必须区分两种口径，任何人不得擅自修改原作者文档：

- `backend-api-design.md` 明确定义 30 个 HTTP 接口；
- SRS 第 8.4 节“核心接口清单”原文列出 26 个；
- 但 SRS 的 FR-PRODUCT-001 正文另外明确写出 4 个分类接口，因此整份 SRS 中出现的唯一接口路径合计仍为 30 个；
- 这 4 个分类接口同时存在于 API 文档、现有 `ItemController` 和契约测试。

因此当前实现以 `backend-api-design.md` 的 30 个 HTTP 接口为准，SRS 用于对齐业务规则；SRS 第 8.4 节的 26 行保持原样，只把它视为汇总漏项。如果团队不接受这一解释，由 SRS/API 原作者 A 明确确认后再修改源文档；B 不自行删接口或替原作者补写 SRS。

### 3.1 用户 4 个

| 方法 | 路径 | 权限 | 负责人链路 |
| --- | --- | --- | --- |
| POST | `/api/v1/users` | Public | A 测试/契约，B 实现，C 注册页 |
| POST | `/api/v1/users/login` | Public | A 测试/契约，B 实现，C 用户登录页 |
| GET | `/api/v1/users/me` | User | A 测试/契约，B 实现，C 个人中心 |
| PATCH | `/api/v1/users/me` | User | A 测试/契约，B 实现，C 资料修改 |

### 3.2 商家 4 个

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| POST | `/api/v1/merchants` | Public |
| POST | `/api/v1/merchants/login` | Public |
| GET | `/api/v1/merchants/me` | Merchant |
| PATCH | `/api/v1/merchants/me` | Merchant |

### 3.3 店铺 4 个

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| POST | `/api/v1/shops` | Merchant |
| GET | `/api/v1/shops` | Public；`mine=true` 时 Merchant |
| GET | `/api/v1/shops/{shopId}` | Public |
| PATCH | `/api/v1/shops/{shopId}` | Merchant |

### 3.4 分类 4 个

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| POST | `/api/v1/shops/{shopId}/categories` | Merchant |
| GET | `/api/v1/shops/{shopId}/categories` | Public |
| PATCH | `/api/v1/categories/{categoryId}` | Merchant |
| DELETE | `/api/v1/categories/{categoryId}` | Merchant |

### 3.5 商品 4 个

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| POST | `/api/v1/products` | Merchant |
| GET | `/api/v1/shops/{shopId}/products` | Public/可选 Merchant |
| GET | `/api/v1/products/{productId}` | Public/可选 Merchant |
| PATCH | `/api/v1/products/{productId}` | Merchant |

### 3.6 购物车 4 个

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| POST | `/api/v1/cart-items` | User |
| GET | `/api/v1/cart-items` | User |
| PATCH | `/api/v1/cart-items/{cartItemId}` | User |
| DELETE | `/api/v1/cart-items/{cartItemId}` | User |

### 3.7 用户订单 4 个、商家订单 2 个

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| POST | `/api/v1/orders` | User |
| GET | `/api/v1/orders` | User |
| GET | `/api/v1/orders/{orderId}` | User |
| POST | `/api/v1/orders/{orderId}/cancel` | User |
| GET | `/api/v1/merchant/orders` | Merchant |
| GET | `/api/v1/merchant/orders/{orderId}` | Merchant |

不得为了少写代码删除任何一项，也不得增加支付、退款、骑手、配送调度、优惠券、消息通知等阶段 1 之外的功能。

---

## 4. TDD 在本项目中怎样执行

TDD 固定为：

```text
Red：测试先存在，并确认因目标行为未实现而失败
  ↓
Green：只写让当前测试通过的最少代码
  ↓
Refactor：测试保持全绿时整理重复代码和命名
```

当前 26 个 Service 契约测试已经在实现之前进入 Git 历史，因此第一轮 Red 证据已经存在。B 不用等待 A 重新写相同测试，可以开始 Green；但如果发现 SRS/API 中的重要规则没有测试，先通知 A 补一个独立 Red 提交，再由 B 实现。

每个业务切片都必须保留两个独立提交：

```text
test(user): cover duplicate registration [RED]
feat(user): implement duplicate registration rule [GREEN]
```

严禁把新增测试和使它通过的完整实现放在同一个提交里。严禁删除断言、改成跳过或降低正确要求来制造绿色。

### 4.1 当前 Red 的特殊问题

现有契约测试使用 `@SpringBootTest`，而所有 Controller 都要求 6 个 Service Bean。当前第一个错误是缺少 `ItemService`，会遮住目标模块的真实失败。

B 的第一个实现提交应当为六个接口建立最小可注入外壳：

```text
user/service/impl/UserServiceImpl.java
merchant/service/impl/MerchantServiceImpl.java
restaurant/service/impl/RestaurantServiceImpl.java
item/service/impl/ItemServiceImpl.java
shopping/service/impl/ShoppingServiceImpl.java
order/service/impl/OrderServiceImpl.java
```

每个类使用 `@Service` 并实现对应接口；尚未进入当前切片的方法先明确抛出 `UnsupportedOperationException`，不能返回伪造成功对象。这个提交只让 Spring 上下文可启动，不算任何业务 Green。

---

## 5. 三个人的固定职责

### 5.1 A：契约、Controller、测试和评审

A 负责：

1. 维护 SRS/API 与 Controller/Service 签名一致；
2. 每个切片开始前确认成功、失败、边界、权限和事务规则；
3. 缺测试时先新增 Red 并提交 Red 日志；
4. 维护 Controller、统一错误码、JWT 和 MockMvc 测试；
5. 评审 B 是否把业务放在 ServiceImpl、把 SQL 放在 DAO；
6. 每次 Green 后运行相关契约测试和公共回归；
7. 最终生成 JaCoCo 报告并记录覆盖率。

A 不替 B 编写数据库 SQL、DAO 或完整 ServiceImpl。

### 5.2 B：V2、Entity、DAO、ServiceImpl 和事务

B 负责：

1. 新增 V2，不修改 V1；
2. 每个模块建立 Entity/Record；
3. 每个模块建立 MyBatis DAO/Mapper 接口和 XML；
4. 编写 DAO 数据库集成测试；
5. 实现 6 个 ServiceImpl；
6. 完成密码摘要、JWT 签发、权限、归属、状态和唯一性规则；
7. 完成订单创建/取消事务、幂等、库存扣减/恢复；
8. 让现有 26 个 Service 契约测试逐步变绿。

MyBatis 的 Mapper 接口本身就是 DAO。除非确实需要组合多个 Mapper，不要再加一层只转发调用的 `DaoImpl`。

### 5.3 C：前端、真实接口接入和联调记录

C 负责：

1. 安装带 npm 的 Node LTS，执行 `npm ci`；
2. Axios `baseURL` 使用 `/api/v1`；
3. 分开实现用户登录和商家登录；
4. 保存 Bearer Token，在受保护请求中发送 `Authorization`；
5. 按 30 个接口修正所有路径和字段；
6. 只在对应后端切片 Green 后移除该页面假数据；
7. 验证 401/403/404/409 提示；
8. 保存每个切片的构建、联调和人工验收记录。

C 不得继续按 HttpSession、共用登录、`/cart/items` 或 `/shops/{id}/status` 开发。

---

## 6. 所有人开工前的 Git 操作

每个人先保存自己的未提交内容。工作区不是空的就停止，不要覆盖别人的文件。

```powershell
Set-Location '自己的项目根目录'
git status --short
git fetch origin --prune
git switch develop
git pull --ff-only origin develop
git status --short
```

然后分别创建自己的分支，只执行属于自己的一条：

```powershell
# A
git switch -c feature/a-tdd

# B
git switch -c feature/b-tdd

# C
git switch -c feature/c-tdd
```

确认：

```powershell
git branch --show-current
git log -3 --oneline --decorate
```

任何人都不直接在 `develop` 写业务代码。每次准备合并前都先拉取最新 `origin/develop` 到个人分支并回归。

---

## 7. B 的第一阶段：建立可开发的数据库和 ServiceImpl 基础

### 7.1 建立 B 分支并保存正式 Red

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git switch develop
git pull --ff-only origin develop
git switch -c feature/b-tdd

Set-Location '.\backend'
$env:JAVA_HOME = 'D:\Dev\Java\JDK17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DELIVERY_DB_USERNAME = 'delivery_app'
$env:DELIVERY_DB_PASSWORD = Read-Host '输入本机 delivery_app 密码' -MaskInput
$env:SPRING_PROFILES_ACTIVE = 'test'
.\mvnw.cmd -version
.\mvnw.cmd clean test
```

预期基线仍是 169 个测试、143 个通过、26 个 Error，且根因只能是找不到 Service Bean。把命令、时间、提交号和结果追加到 `docs/test/test-log.md`，不得伪造全绿。

### 7.2 新增六个 ServiceImpl 外壳

逐个实现现有 Service 接口，加入 `@Service`。暂未实现的方法抛出：

```java
throw new UnsupportedOperationException("Pending TDD implementation");
```

然后运行：

```powershell
.\mvnw.cmd clean test
```

预期上下文能够创建，测试应失败在明确的未实现方法。提交：

```powershell
Set-Location '..'
git add -- backend/src/main/java
git diff --cached --check
git commit -m 'chore(service): add injectable tdd implementation shells'
```

### 7.3 增加密码摘要的最小依赖

用户和商家密码必须保存摘要，不能明文保存。若 `pom.xml` 尚无密码编码器，只添加 `spring-security-crypto`，不要引入整套 Spring Security Web 认证来替换现有 JWT 拦截器。

修改后验证：

```powershell
Set-Location '.\backend'
.\mvnw.cmd dependency:tree "-Dincludes=org.springframework.security:spring-security-crypto"
.\mvnw.cmd test-compile
```

### 7.4 编写 V2，不修改 V1

文件固定为：

```text
backend/src/main/resources/db/migration/V2__align_schema_with_api_contract.sql
```

V2 至少完成以下对齐：

| 表 | 必须变更 | 原因 |
| --- | --- | --- |
| `merchants` | 去掉对 `users.user_id` 的账号依附；增加独立 `account`、`password_hash`、`phone`、`status`、`updated_at`；账号唯一 | 商家独立注册和登录 |
| `shops` | 把“每个商家只能一店”的唯一约束改为同一商家店名唯一；根据查询规则保留有效性/逻辑删除字段 | `mine=true` 和同商家店名冲突规则 |
| `product_categories` | 增加 `sort_order`、`created_at`、`updated_at`、逻辑删除标记 | 分类排序、修改、逻辑删除 |
| `products` | 增加非空 `version`，初始为 1；更新时做条件更新并递增 | 商品 PATCH 乐观锁和价格确认 |
| `orders` | 增加 `idempotency_key`、请求指纹、`shop_name` 快照、`updated_at`、`cancelled_at`；扩展状态约束；建立 `(user_id,idempotency_key)` 唯一约束 | 幂等、历史快照和取消 |
| `order_items` | 保留商品名称、单价、数量快照；需要的查询索引补齐 | 历史订单不受商品修改影响 |

规则：

- 先确认开发库和测试库当前没有真实业务数据；若有数据，停止并先制定回填方式；
- V2 必须能从已经执行过 V1 的数据库升级；
- 不在 MySQL 中手工改表后再补 SQL；
- 不删除 `flyway_schema_history`；
- 不修改 V1 的任何字符，否则已执行环境会 checksum 失败；
- V2 在 `delivery_test` 通过后再用于 `delivery_dev`。

验证测试库：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$env:SPRING_PROFILES_ACTIVE = 'test'
.\mvnw.cmd test-compile
mysql -u delivery_app -p delivery_test -e "SELECT installed_rank, version, script, success FROM flyway_schema_history ORDER BY installed_rank;"
```

应看到 V1、V2 各一条且 `success=1`。再重复启动一次，确认 V2 不会重复执行。然后切换 dev Profile 验证开发库。

V2 与 DAO 基础建议使用单独提交：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git add -- backend/src/main/resources/db/migration backend/pom.xml
git diff --cached --check
git commit -m 'feat(database): align schema with api contract [GREEN]'
```

---

## 8. B 的六个模块实现顺序

固定依赖顺序：

```text
user → merchant → restaurant → item → shopping → order
```

不要同时铺开六个模块。每完成一个模块，先运行该模块测试，再运行公共测试，提交后再进入下一个模块。

### 8.1 User

建立：

```text
user/entity/UserEntity.java
user/dao/UserDao.java
resources/mapper/user/UserDao.xml
user/service/impl/UserServiceImpl.java
```

DAO 最少提供：按 account 查询、按 id 查询、插入、局部更新。ServiceImpl 最少完成：

- 注册字段校验和两次密码一致；
- 账号唯一，冲突映射 `ACCOUNT_EXISTS`；
- BCrypt 摘要保存；
- 默认 `ACTIVE`；
- 登录校验摘要和状态，签发 `USER` JWT；
- 本人资料查询/局部更新；
- `requireActive`；
- 永不返回 `password_hash`。

测试：

```powershell
.\mvnw.cmd "-Dtest=UserServiceContractTests,UserControllerTests" test
```

通过后提交：

```powershell
git commit -m 'feat(user): implement user persistence and service [GREEN]'
```

### 8.2 Merchant

建立 MerchantEntity、MerchantDao、XML 和 MerchantServiceImpl。最少完成：

- 与 users 完全独立的账号、摘要和状态；
- 独立注册、独立 `/merchants/login`；
- 默认 `ACTIVE`，`SUSPENDED` 拒绝登录和写操作；
- 签发 `MERCHANT` JWT；
- 本人资料查询/局部更新；
- `requireActive`；
- 账号冲突映射 `MERCHANT_ACCOUNT_EXISTS`。

测试：

```powershell
.\mvnw.cmd "-Dtest=MerchantServiceContractTests,MerchantControllerTests" test
```

### 8.3 Restaurant

建立 ShopEntity、RestaurantDao、XML 和 RestaurantServiceImpl。最少完成：

- 创建前调用 `MerchantService.requireActive`；
- 新店默认 `CLOSED`；
- 同一商家同名店铺冲突；
- 公开列表只显示可公开店铺，`mine=true` 只显示当前商家店铺；
- 分页从 1 开始、pageSize 最大 100；
- 排序白名单只有 `name`、`createdAt`；
- 只有店主可 PATCH 名称、简介、状态；
- `requireOwned` 和 `requireOrderable` 正确报错。

```powershell
.\mvnw.cmd "-Dtest=RestaurantServiceContractTests,RestaurantControllerTests" test
```

### 8.4 Item

建立 CategoryEntity、ProductEntity、ItemDao/必要的分类与商品 Mapper、XML 和 ItemServiceImpl。最少完成：

- 分类属于店主店铺，名称唯一，按 `sortOrder asc` 查询；
- 分类 PATCH 和逻辑删除；被有效商品引用时拒绝删除；
- 商品创建校验店铺、分类、价格和库存，默认 `OFF_SALE`、version=1；
- 公众只看到 `ON_SALE`，店主可查看下架商品；
- PATCH 使用 `WHERE id=? AND version=?` 原子更新并递增 version；
- 版本过期返回 `RESOURCE_CONFLICT`；
- 预留库存检查状态、版本和数量，库存不能变负；
- 恢复库存每次只执行调用要求的数量。

```powershell
.\mvnw.cmd "-Dtest=ItemServiceContractTests,ItemControllerTests" test
```

必须补 DAO 测试验证乐观锁和原子扣库存，不仅依赖 Java 先查询再更新。

### 8.5 Shopping

建立 CartItemEntity、ShoppingDao、XML 和 ShoppingServiceImpl。最少完成：

- 仅允许用户操作本人项；
- `(user_id,product_id)` 唯一；
- 同商品再次加入时累加数量而不是插入第二行；
- 每次新增/修改重新检查用户、店铺、商品、库存；
- 查询使用最新商品价格计算展示 subtotal/total；
- 删除购物车项是物理删除，不影响商品库存；
- `loadForCheckout` 只返回指定且属于当前用户的项；
- `removeAfterCheckout` 只删除成功结算项。

```powershell
.\mvnw.cmd "-Dtest=ShoppingServiceContractTests,ShoppingControllerTests" test
```

### 8.6 Order

建立 OrderEntity、OrderItemEntity、OrderDao/Mapper、XML 和 OrderServiceImpl。创建订单必须在一个 `@Transactional` 事务中按顺序执行：

```text
校验用户和 X-Idempotency-Key
→ 读取本人选中购物车项
→ 确认只能来自一个店铺
→ 确认店铺 OPEN
→ 按商品 ID 稳定顺序校验 version 并原子扣库存
→ 服务端按数据库价格计算金额
→ 保存订单、店铺名称快照和商品明细快照
→ 删除已结算购物车项
→ 提交事务
```

任何一步失败，订单、明细、库存和购物车全部回滚。相同用户、相同幂等键、相同请求返回首次结果；相同键不同请求返回 `IDEMPOTENCY_CONFLICT`。

取消订单必须：

- 只允许订单本人；
- 只允许 `PENDING_PAYMENT → CANCELLED`；
- 状态条件更新保证并发取消只有一次成功；
- 在同一事务恢复库存并写 `cancelled_at`；
- 第二次取消返回 `ORDER_STATE_CONFLICT`，不能再次恢复库存；
- 订单和明细永不删除。

用户只能查询本人订单；商家只能查询本人店铺订单。

```powershell
.\mvnw.cmd "-Dtest=OrderServiceContractTests,OrderControllerTests" test
```

订单必须补事务回滚、幂等重试、重复取消和库存恢复数据库测试。

---

## 9. 每个模块的固定提交和合并流程

模块开发完成后：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
.\mvnw.cmd clean test

Set-Location '..'
git status --short
git diff --check
git add -- backend docs/test
git diff --cached --stat
git diff --cached --check
git commit -m 'feat(module): describe completed green slice [GREEN]'
git fetch origin --prune
git merge --no-ff origin/develop -m 'merge: sync develop before module delivery'
```

同步 `develop` 后重新运行目标模块和完整测试。由于其他模块可能仍在 Red，完整测试允许保留已登记的未完成模块错误，但当前模块必须全绿，且不能出现编译、数据库、Flyway、公共测试或已完成模块回归。

然后推送个人分支并创建 PR：

```powershell
git push -u origin HEAD
```

PR 必须写明：

- 本次实现哪些 SRS/接口；
- 对应 Red 提交；
- 目标测试通过数量；
- 完整测试还剩哪些已知 Red；
- 是否包含 V2/数据库变更；
- C 可以开始联调哪些接口。

评审通过后合入 `develop`。其他人执行 `git pull --ff-only origin develop`，不能复制文件手工对齐。

---

## 10. C 的联调顺序

C 与后端 Green 顺序一致：

1. 用户注册、用户登录、个人中心；
2. 商家注册、商家登录、商家资料；
3. 店铺创建、列表、详情、PATCH；
4. 分类新增、列表、修改、删除；
5. 商品新增、列表、详情、PATCH；
6. 购物车增删改查；
7. 下单、用户订单列表/详情/取消；
8. 商家订单列表/详情。

前端首次准备：

```powershell
node -v
npm -v
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm ci
npm run build
```

每接通一个切片：

- 删除该页面对应的假数据；
- 检查请求路径和字段；
- 检查 Bearer Token；
- 检查成功状态码和 `code=0`；
- 人工触发 400、401、403、404、409；
- 保存截图和结果到 `docs/test/test-log.md`；
- 再执行 `npm run build`。

当前项目没有前端自动测试框架。阶段 1 不为了形式额外增加大型前端测试体系；由 A 的后端自动测试、C 的构建和逐接口人工联调共同形成最小证据。若课程明确要求前端自动化测试，再单独加入 Vitest，而不是现在擅自扩展。

---

## 11. 新电脑仍需完成的一次性环境准备

Git 只同步源码、Maven 配置和 npm 锁文件，不会同步本机软件、数据库和密码。

| 工具 | 是否随仓库提供 | 要求 |
| --- | --- | --- |
| Git | 否 | 可执行 `git --version` |
| JDK 17 | 否 | `java -version` 和 `mvnw.cmd -version` 都显示 17 |
| Maven | 是，Wrapper 脚本 | 不用全局安装；首次联网自动下载 Maven 3.9.16 |
| MySQL Server | 否 | 本机 `localhost:3306`；创建 dev/test 空库和应用账号 |
| Node.js/npm | 否 | C 必须安装包含 npm 的 Node LTS |
| IDEA/VS Code | 否 | 可选，不影响构建 |
| `rg` | 否 | 可选，不影响项目运行 |

每台电脑必须设置自己的：

```text
DELIVERY_DB_USERNAME
DELIVERY_DB_PASSWORD
```

Flyway能自动创建和升级表，但不能安装 MySQL、创建数据库实例或替别人保存密码。真实密码不得提交到 Git。

---

## 12. 最终验收标准

业务阶段完成必须同时满足：

- [ ] 30 个接口与 API 文档一致；
- [ ] 用户和商家独立账号、独立登录；
- [ ] JWT Bearer 鉴权和资源归属正确；
- [ ] V1 保持不变，V2 在 dev/test 均成功；
- [ ] 6 个 ServiceImpl、各模块 Entity 和 DAO/Mapper 完成；
- [ ] 分类逻辑删除和排序完成；
- [ ] 商品乐观锁和原子库存完成；
- [ ] 订单幂等、事务、快照和取消恢复库存完成；
- [ ] 169 个现有测试以及新增 DAO/数据库测试全部通过；
- [ ] 0 failures、0 errors、0 skipped；
- [ ] `mvnw.cmd clean test` 成功并生成 JaCoCo 报告；
- [ ] `npm ci`、`npm run build` 成功；
- [ ] C 已删除对应业务假数据并完成 30 接口联调；
- [ ] Red/Green 提交、测试日志、Bug 记录和截图真实可追踪；
- [ ] 三人最终代码已合入 `develop`，工作区干净。

最终命令：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git switch develop
git pull --ff-only origin develop

Set-Location '.\backend'
$env:SPRING_PROFILES_ACTIVE = 'test'
.\mvnw.cmd clean test

Set-Location '..\frontend'
npm ci
npm run build
```

完成以上内容后停止增加功能，进入报告整理和答辩准备。支付、退款、骑手、配送调度、优惠券、消息通知不属于本阶段实现范围。
