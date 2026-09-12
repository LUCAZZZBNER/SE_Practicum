# B-V1.2 后端持久化收口与联调准备执行手册

更新时间：2026-09-12  
需求依据：`docs/software-requirements-specification.md`

接口依据：`docs/api/backend-api-design.md`

远程代码基线：`origin/develop@4fa815e`

B 当前工作分支：`codex/b-v12-green`

> 这是一份“从当前状态继续做”的操作手册，不是新的需求文档。若本文与 SRS 或 API 文档冲突，必须以 SRS 和 API 文档为准。B 不得为了方便数据库实现而擅自改接口路径、请求字段、响应结构或错误码。

---

## 1. 先看结论：B 现在从哪里开始

当前已经完成：

- A 的最新代码已经进入当时的 `origin/develop@4fa815e`；
- 当前分支直接从该版本创建，没有合入旧 `codex/v12-backend-tdd` 中替 A 写的代码；
- 不连接数据库的 Controller、安全和异常处理测试共 164 个，已经全部通过；
- V1 至 V9 已经存在，应当视为已经发布，不能再修改；
- B 尚未写 V10，尚未完成本轮数据库、DAO、Mapper 和 ServiceImpl 收口。

B 现在按下面顺序工作：

```text
0. 核对分支和环境
1. 保存当前测试基线
2. 只补数据库/Service 层测试
3. 新建并验证 V10 数据库迁移
4. 修改 Entity、DAO 和 Mapper
5. 修改 ServiceImpl
6. 运行分模块测试和后端全量测试
7. 同步最新 develop，再回归一次
8. 提交、推送并合入 develop
9. 前后端联调
```

只有第 0 至第 8 步全部完成，才算“B 的代码已经准备好，可以正式联调”。

---

## 2. B 能改什么，不能改什么

### 2.1 B 的主要修改范围

```text
backend/src/main/java/com/delivery/backend/**/entity
backend/src/main/java/com/delivery/backend/**/dao
backend/src/main/java/com/delivery/backend/**/service/impl
backend/src/main/resources/mapper
backend/src/main/resources/db/migration
backend/src/test/java 中的数据库、DAO、Service 和并发测试
```

为了实现图片归属，允许对内部 `ImageService` 增加一个最小的 `requireOwned` 方法，但不能借此修改 Controller 接口、HTTP 路径或对外 DTO。

### 2.2 B 默认不能改的范围

```text
Controller
Controller 测试
HTTP 路径
请求与响应 JSON 字段
统一错误码
A 已确定的公开 Service 方法签名
前端页面
```

如果发现这些内容与 API 文档不一致：先记录文件、行号、文档依据和预期行为，发给 A 确认；B 继续完成不依赖该冲突的数据库工作，未确认前不要擅自改 Controller。

### 2.3 明确禁止

- 不把旧 `codex/v12-backend-tdd` 合入当前分支；
- 不编辑 V1 至 V9，不执行 `flyway repair`；
- 不删除 `delivery_dev` 或 `delivery_test` 来掩盖迁移错误；
- 不使用 `git reset --hard`；
- 不撤销或提交 `ABC阶段1业务TDD开发与联调执行文档.md` 当前属于用户的本地修改；
- 不开发优惠券、评价、骑手、真实支付、地图、管理员后台；
- 不要求每一个小步骤都提交或推送。

---

## 3. 完成标准

### 3.1 B 开发完成标准

- 只新增 `V10__harden_v12_persistence.sql`，V1 至 V9 无变化；
- V9 旧数据可直接迁移到 V10，不需要删库；
- 商品、旧购物车完成 SKU 回填；
- 同一用户最多一个有效默认地址有数据库保障；
- 商品图片能够校验上传商家归属；
- 新购物车、下单、扣库存、恢复库存都以 SKU 为业务真值；
- 待支付取消不产生退款；
- 已支付或制作中取消只恢复一次库存并只创建一条全额退款；
- 支付、取消、制作、配送、确认收货的幂等键按“主体 + 动作”隔离；
- 下单同键重试先返回首次结果，不因地址后来被删除而失败；
- 备注先 trim，纯空白变 `null`，trim 后不超过 200 字符；
- 全量后端测试 `Failures: 0`、`Errors: 0`。

### 3.2 可以联调的额外标准

- B 分支已经合入最新 `develop`；
- 最新 `develop` 后端全量测试仍通过；
- C 的前端也是同一个最新 `develop`；
- 后端连接 `delivery_dev` 正常启动；
- Flyway 历史中 V10 只有一条且 `success=1`；
- 前端实际请求使用 `/api/v1/...` 并能到达后端。

---

## 4. 第 0 步：核对当前分支和工具

### 4.1 进入项目并确认工作区

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git branch --show-current
git status --short --branch
```

预期分支：

```text
codex/b-v12-green
```

允许看到下面这项用户修改，但不要暂存它：

```text
M ABC阶段1业务TDD开发与联调执行文档.md
```

如果分支不对：

```powershell
git switch codex/b-v12-green
```

不要执行 `git add .`。

### 4.2 检查远程 develop 是否更新

```powershell
git fetch origin
git rev-list --left-right --count HEAD...origin/develop
```

两个数字分别表示“当前分支独有提交数”和“远程 develop 独有提交数”。例如 `1 0` 表示可以继续。如果第二个数字不是 0：

```powershell
git log --oneline --decorate HEAD..origin/develop
git merge origin/develop
git status --short
```

若有 Controller/API 冲突，交给 A 确认；不要为了完成数据库代码擅自改变接口。

### 4.3 检查 Java 和 Maven Wrapper

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
java -version
.\mvnw.cmd -version
```

成功标准是 Java 17 和 Maven 3.9.x。已有 JDK 17 不需要重新下载，也不需要单独安装 Maven。

### 4.4 检查 MySQL 服务和端口

```powershell
Get-Service | Where-Object {
    $_.Name -match 'mysql' -or $_.DisplayName -match 'mysql'
}
Test-NetConnection 127.0.0.1 -Port 3306
```

成功标准：`TcpTestSucceeded : True`。

若服务为 `Stopped`，用管理员 PowerShell，并把名字换成实际查到的名字：

```powershell
Start-Service -Name 'MySQL267'
Get-Service -Name 'MySQL267'
```

若启动失败，不要继续数据库测试，更不能靠删库解决。

### 4.5 检查 MySQL 命令行工具

```powershell
Get-Command mysql.exe -ErrorAction SilentlyContinue
Get-Command mysqldump.exe -ErrorAction SilentlyContinue
```

如果没有输出：

```powershell
Get-ChildItem -LiteralPath 'C:\Program Files\MySQL' -Recurse -Filter 'mysql.exe' -ErrorAction SilentlyContinue
Get-ChildItem -LiteralPath 'C:\Program Files\MySQL' -Recurse -Filter 'mysqldump.exe' -ErrorAction SilentlyContinue
```

找到后可用完整路径执行，不必重装数据库。

### 4.6 设置本窗口数据库凭据

项目实际读取 `DELIVERY_DB_USERNAME` 和 `DELIVERY_DB_PASSWORD`，不是旧文档中的 `DB_USERNAME/DB_PASSWORD`。

```powershell
$deliveryCredential = Get-Credential -Message '输入本机 MySQL 测试库账号和密码'
$env:DELIVERY_DB_USERNAME = $deliveryCredential.UserName
$env:DELIVERY_DB_PASSWORD = $deliveryCredential.GetNetworkCredential().Password
$env:SPRING_PROFILES_ACTIVE = 'test'
```

只检查是否设置，不打印密码：

```powershell
$env:DELIVERY_DB_USERNAME
if ([string]::IsNullOrWhiteSpace($env:DELIVERY_DB_PASSWORD)) {
    '数据库密码没有设置'
} else {
    '数据库密码已设置'
}
$env:SPRING_PROFILES_ACTIVE
```

环境变量只在当前 PowerShell 窗口有效。

---

## 5. 第 1 步：保存当前 Green 基线

作用：证明新增 B 测试前，环境和 A 的现有代码是正常的。

### 5.1 不连接数据库的快速测试

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
.\mvnw.cmd "-Dtest=AuthenticationInterceptorTests,DefaultJwtTokenServiceTests,GlobalExceptionHandlerTests,ImageControllerTests,ItemControllerTests,MerchantControllerTests,OrderControllerTests,RestaurantControllerTests,ShoppingControllerTests,UserAddressControllerTests,UserControllerTests" test
```

预期：`Failures: 0`、`Errors: 0`、`BUILD SUCCESS`。

### 5.2 当前数据库和 Service 测试

第一次从其他分支切换回来时，必须先执行 `clean`，避免 `target/classes` 残留旧分支已经删除的 Flyway SQL：

```powershell
.\mvnw.cmd clean "-Dtest=*ServiceContractTests,*ConcurrencyTests,ItemDaoIntegrationTests" test
```

常见错误：

| 错误文字 | 含义 | 处理 |
|---|---|---|
| `Access denied for user` | 用户名或密码错误 | 重做 4.6 |
| `Communications link failure` | MySQL 未启动或端口不通 | 重做 4.4 |
| `Unknown database delivery_test` | 测试库不存在 | 用有权限账号创建测试库 |
| `Validate failed` / checksum | 旧 SQL 被修改 | 不要 repair，检查 V1-V9 diff |
| `Found more than one migration with version 3` 且路径位于 `target/classes` | 切换分支后残留了旧编译副本 | 执行 `\.\mvnw.cmd clean` 后重跑；不要改数据库或 V3 |
| `Table ... doesn't exist` | 迁移失败或 Profile 错 | 确认 Profile 是 test |

### 5.3 记录基线

```powershell
git rev-parse --short HEAD
```

记录日期、分支、提交号、快速测试结果、数据库测试结果。不必为基线单独提交。

---

## 6. 第 2 步：补 B 负责的 Red 测试

Red 是先增加表达规则的测试，并确认旧实现不能满足。Red 阶段允许断言失败，不允许编译失败；不修改 Controller 测试。

### 6.1 地址唯一性

新增：

```text
backend/src/test/java/com/delivery/backend/address/UserAddressConcurrencyTests.java
```

覆盖：

```text
concurrentFirstAddressesLeaveExactlyOneDefaultAddress
databaseRejectsTwoActiveDefaultAddressesForTheSameUser
```

第一个测试让两个线程同时为同一用户创建首地址，最终只允许一个有效默认地址。第二个测试绕过 Service，直接尝试写入第二个有效默认地址，数据库必须拒绝。

当前 Service 已调用 `requireActiveForUpdate` 锁用户行，所以第一个可能已通过；真正预期 Red 的是数据库直接写入场景，因为 V9 尚无唯一约束。

```powershell
.\mvnw.cmd "-Dtest=UserAddressConcurrencyTests" test
```

### 6.2 图片归属

新增：

```text
backend/src/test/java/com/delivery/backend/image/ImageOwnershipServiceTests.java
```

覆盖：

- 商家甲可以使用自己上传的图片；
- 商家乙不能使用商家甲的图片；
- 不存在图片失败；
- 无归属的旧孤立图片不能被新商品引用。

建议方法名：

```text
merchantCanUseAnImageUploadedByItself
merchantCannotUseAnotherMerchantsImage
unownedLegacyImageCannotBeAttachedToANewProduct
```

```powershell
.\mvnw.cmd "-Dtest=ImageOwnershipServiceTests" test
```

当前 `images` 没有 `merchant_id`，跨商家引用测试应为 Red。

### 6.3 SKU 是唯一业务真值

新增专门的 B 测试类，并保留 A 已有测试不动：

```text
backend/src/test/java/com/delivery/backend/item/SkuBusinessTruthTests.java
backend/src/test/java/com/delivery/backend/persistence/PersistenceSchemaContractTests.java
```

覆盖：

- `minPrice` 来自 SKU 最低价；
- `inStock` 来自至少一个有效 SKU；
- 加购校验 SKU，不读取 Product 的旧价格、库存和 version；
- 下单金额使用 SKU 当前价格；
- 扣库存和恢复库存只更新 `product_skus.stock`；
- Product 旧字段与 SKU 不同时，不能影响购物车小计或订单金额。

```powershell
.\mvnw.cmd "-Dtest=SkuBusinessTruthTests,PersistenceSchemaContractTests" test
```

### 6.4 取消与退款

新增专门的 B 测试类，不修改 A 已有的订单测试：

```text
backend/src/test/java/com/delivery/backend/order/OrderPersistenceRedTests.java
```

增加：

```text
pendingPaymentCancellationDoesNotCreateARefund
paidCancellationCreatesExactlyOneFullRefund
preparingCancellationCreatesExactlyOneFullRefund
repeatedCancellationRestoresSkuStockOnlyOnce
paidCancellationRollsBackWhenPaymentRecordIsMissing
deliveringCompletedAndCancelledOrdersCannotBeCancelled
```

除返回值外，使用 `JdbcTemplate` 检查订单三种状态、SKU 库存、Payment 数量、Refund 数量/金额/paymentId。

```powershell
.\mvnw.cmd "-Dtest=OrderPersistenceRedTests" test
```

当前实现只要取消带幂等键就可能建 Refund，因此“待支付取消没有退款”应为 Red。

### 6.5 支付与履约幂等

每个动作覆盖：

1. 同一主体 + 动作 + key + 请求：返回首次结果，无重复副作用；
2. 同一主体 + 动作 + key 换订单：返回 1602；
3. 不同主体使用相同文本 key：互不占用。

动作：`CREATE_ORDER`、`PAY_ORDER`、`CANCEL_ORDER`、`PREPARE_ORDER`、`DELIVER_ORDER`、`CONFIRM_RECEIPT`。

建议方法名：

```text
sameActorAndActionCannotReuseAKeyForAnotherOrder
differentActorsMayUseTheSameKeyText
sameKeyReplayDoesNotRepeatPaymentRefundStockOrTransition
```

```powershell
.\mvnw.cmd "-Dtest=OrderPersistenceRedTests" test
```

### 6.6 下单重试和备注规范化

在 `OrderPersistenceRedTests` 增加：

```text
idempotentReplayReturnsOriginalOrderAfterAddressDeletion
blankRemarkIsStoredAsNull
remarkLengthIsCheckedAfterTrim
normalizedRemarkProducesTheSameFingerprint
differentNormalizedRemarkConflictsForTheSameKey
```

地址重试步骤：创建地址 → 成功下单 → 删除地址 → 用完全相同 key 和请求重试 → 仍返回第一次订单。

备注边界：

- `"   "` 保存为 null；
- trim 后 200 字符成功；
- trim 后 201 字符失败；
- `"  少辣  "` 和 `"少辣"` 的规范化 fingerprint 相同。

```powershell
.\mvnw.cmd "-Dtest=OrderPersistenceRedTests" test
```

### 6.7 一次运行本节全部 18 个测试

当前已新增：

```text
UserAddressConcurrencyTests：2 个
ImageOwnershipServiceTests：2 个
PersistenceSchemaContractTests：3 个
SkuBusinessTruthTests：2 个
OrderPersistenceRedTests：9 个
合计：18 个
```

纯编译已经通过。使用已设置数据库密码的 PowerShell 运行：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$env:SPRING_PROFILES_ACTIVE = 'test'
.\mvnw.cmd clean "-Dtest=UserAddressConcurrencyTests,ImageOwnershipServiceTests,PersistenceSchemaContractTests,SkuBusinessTruthTests,OrderPersistenceRedTests" test
```

预期不是 `BUILD SUCCESS`，而是出现由以下未实现规则造成的测试失败：

- 数据库尚未拒绝第二个有效默认地址；
- images 尚无 merchantId；
- cart_items.sku_id 尚可为空；
- 统一动作幂等表尚不存在；
- 待支付取消错误地产生 Refund；
- 不同用户使用同一支付 key 被全局唯一键互相阻塞；
- 同一商家同一 prepare key 可错误地用于不同订单；
- 地址删除后同键下单重试无法返回原订单；
- 空白 remark 与 null 的 fingerprint 不一致。

如果出现 Java 编译错误、数据库连接错误或 Flyway 启动错误，不算有效 Red；必须先修环境或测试代码。只有业务断言失败才是有效 Red。

#### 6.7.1 2026-09-12 实际执行结果

本节命令已经实际执行，结果是有效 Red：

```text
Tests run: 18, Failures: 8, Errors: 3, Skipped: 0
BUILD FAILURE
```

环境部分正常：成功连接 `delivery_test`，Flyway 成功校验 V1～V9 且数据库当前版本为 V9，项目完成编译并真正进入了测试。日志中 MySQL 版本高于 Flyway 已验证版本的提示只是警告，不是这次失败原因。

11 个失败/错误与待开发范围一一对应：

- 数据库允许同一用户存在两个有效默认地址；
- 商家可以使用其他商家的图片；
- `cart_items.sku_id` 仍允许 `NULL`；
- `images` 缺少图片所属商家字段；
- 订单动作幂等表尚不存在；
- 待支付订单取消时错误创建退款记录；
- 已支付订单缺少支付记录时，取消操作没有抛错并回滚；
- 同一商家可把同一个接单幂等 key 用于不同订单；
- 地址删除后，使用相同下单 key 重试不能返回原订单；
- 空白备注与 `null` 生成了不同幂等指纹；
- 不同用户使用相同支付 key 文本时被全局唯一约束错误阻塞。

因此第 6 节已经完成，不要为了让测试变绿而修改这些 Red 测试；下一阶段应由 V10 和对应 DAO/Service 实现消除这些失败。

### 6.8 检查并提交 Red

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git status --short
git diff -- backend/src/test
```

确认没有 Controller 测试和生产代码变化。所有 Red 写完后可统一提交一次：

```powershell
git add -- 'backend/src/test'
git restore --staged -- 'ABC阶段1业务TDD开发与联调执行文档.md'
git status --short
git commit -m 'test(red): define B V1.2 persistence contracts'
```

如果 ABC 文档本来没有被暂存，`git restore --staged` 提示无变化也没关系。此时不用 push。

---

## 7. 第 3 步：设计并新建 V10

### 7.1 新建文件

V1-V9 已发布，Flyway 已保存 checksum，所以只能新建：

```text
backend/src/main/resources/db/migration/V10__harden_v12_persistence.sql
```

IDEA：展开迁移目录 → 右键 `migration` → `New -> File` → 输入完整文件名。不要复制修改 V9。

### 7.2 先备份测试库

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
$backupDirectory = 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum-db-backups'
New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
$backupFile = Join-Path $backupDirectory ('delivery_test_before_v10_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '.sql')
mysqldump.exe --host=127.0.0.1 --port=3306 --user=$env:DELIVERY_DB_USERNAME --password --single-transaction --routines --triggers --result-file="$backupFile" delivery_test
Get-Item -LiteralPath $backupFile | Select-Object FullName,Length,LastWriteTime
```

成功标准：文件存在且 Length 大于 0。备份放在仓库目录外，因此不会被 Git 提交。

### 7.3 迁移前只读检查

```powershell
mysql.exe --host=127.0.0.1 --port=3306 --user=$env:DELIVERY_DB_USERNAME --password delivery_test
```

```sql
SELECT installed_rank,version,description,success
FROM flyway_schema_history ORDER BY installed_rank;

SELECT p.id,p.name FROM products p
LEFT JOIN product_skus s ON s.product_id=p.id
GROUP BY p.id,p.name HAVING COUNT(s.id)=0;

SELECT id,user_id,product_id,sku_id,sku_version FROM cart_items
WHERE sku_id IS NULL OR sku_version IS NULL;

SELECT user_id,COUNT(*) AS default_count FROM user_addresses
WHERE deleted_at IS NULL AND is_default=TRUE
GROUP BY user_id HAVING COUNT(*)>1;

SELECT i.id AS image_id,COUNT(DISTINCT s.merchant_id) AS merchant_count
FROM images i JOIN products p ON p.image_id=i.id JOIN shops s ON s.id=p.shop_id
GROUP BY i.id HAVING COUNT(DISTINCT s.merchant_id)>1;

exit
```

若最后一条返回数据，同一旧图片被多个商家引用，不能静默猜归属；记录后人工确认。

### 7.4 V10 必做内容与顺序

#### 7.4.1 默认 SKU 回填

为“没有任何 SKU 的 Product”创建一条 `默认规格`，价格/库存/状态/version 来自旧 Product；已有 SKU 的 Product 不重复插入。

#### 7.4.2 旧购物车回填

对 `sku_id IS NULL`：优先匹配该 Product 的默认 SKU，写入 skuId 和当前 skuVersion。再次查询确认无 null 后，才能增加新业务必填约束。绝不能映射到其他 Product 的 SKU。

#### 7.4.3 默认地址唯一性

旧数据若有多个有效默认地址，保留 ID 最小的一条，其余改为非默认。增加生成列：只有 `deleted_at IS NULL AND is_default=TRUE` 时值为 userId，其他为 null；对生成列建唯一索引。这样允许多个普通/已删除地址，但每个用户最多一个有效默认地址。

#### 7.4.4 图片归属

给 `images` 增加可空 `merchant_id`：先加列 → 通过 `products -> shops` 回填已引用图片 → 加索引与外键。无法推导的历史孤立图片保持 null；新上传必须写 merchantId，null 图片不得被新商品引用。

#### 7.4.5 统一幂等表

建议表名 `order_action_idempotency`，字段至少包括：

```text
id, actor_type, actor_id, action_name, idempotency_key,
request_fingerprint, order_id, created_at
```

唯一键：

```text
(actor_type, actor_id, action_name, idempotency_key)
```

并为 orderId 建索引和外键。

#### 7.4.6 修正支付/退款唯一范围

删除 V3/V4 的全局唯一 `uk_payments_key` 和 `uk_refunds_idempotency_key`，动作幂等交给统一表。保留/加强：一个订单最多一条 Payment、一个订单最多一条 Refund、一条 Payment 最多一个 Refund、Refund 引用真实 Payment。

#### 7.4.7 必要索引

确认索引覆盖用户订单、商家订单、user+sku 购物车、product SKU、order Payment、order/payment Refund、merchant 图片、主体+动作幂等。不要增加没有查询对应的装饰性索引。

### 7.5 确认旧迁移没改

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git status --short -- 'backend/src/main/resources/db/migration'
git diff --name-only -- 'backend/src/main/resources/db/migration'
Get-Content -LiteralPath 'backend/src/main/resources/db/migration/V10__harden_v12_persistence.sql' -Encoding UTF8
```

`git status` 必须显示新增 V10；未暂存的新文件不会出现在 `git diff` 中，所以同时用 `Get-Content` 检查其内容。Windows PowerShell 5.1 必须明确指定 `-Encoding UTF8`，否则正确的“默认规格”也可能被显示成乱码。再确认 V1-V9，无输出且退出码为 0 才正确：

```powershell
git diff --exit-code origin/develop -- 'backend/src/main/resources/db/migration/V1__create_core_tables.sql' 'backend/src/main/resources/db/migration/V2__align_schema_with_api_contract.sql' 'backend/src/main/resources/db/migration/V3__frontend_contract.sql' 'backend/src/main/resources/db/migration/V4__transaction_idempotency.sql' 'backend/src/main/resources/db/migration/V5__sku_cart_constraints.sql' 'backend/src/main/resources/db/migration/V6__image_content.sql' 'backend/src/main/resources/db/migration/V7__order_action_idempotency.sql' 'backend/src/main/resources/db/migration/V8__fulfillment_idempotency.sql' 'backend/src/main/resources/db/migration/V9__refund_payment_reference.sql'
```

---

## 8. 第 4 步：Entity、DAO 和 Mapper

### 8.1 地址

涉及 `UserAddressDao.java`、`UserAddressDao.xml`、`UserAddressServiceImpl.java`。

- 保留 `requireActiveForUpdate` 在判断首地址之前；
- 清除旧默认、插入/更新新默认在同一事务；
- 数据库唯一冲突转换为现有业务错误，不直接泄漏 SQL 异常；
- 删除默认地址后不自动选另一条；
- 不再重复设计另一套用户锁。

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
.\mvnw.cmd "-Dtest=UserAddressServiceContractTests,UserAddressConcurrencyTests" test
```

### 8.2 图片

涉及 `ImageEntity`、`ImageDao`、`ImageDao.xml`、`ImageServiceImpl`、`ItemServiceImpl`，以及内部 `ImageService` 的最小方法。

- Entity、INSERT、SELECT/resultMap 都增加 merchantId；
- DAO 增加 `merchantId + imageId` 归属查询；
- 上传时保存当前 merchantId；
- 创建/修改商品主图及上架前验证图片归属；
- 公开图片正文读取仍按 imageId，不改变下载接口。

```powershell
.\mvnw.cmd "-Dtest=ImageOwnershipServiceTests,ItemServiceContractTests" test
```

### 8.3 SKU、商品和购物车

涉及 Item/Sku/Shopping 的 DAO、Mapper 和 ServiceImpl。

- minPrice、inStock 从 SKU 派生；
- 购物车身份是 userId+skuId；
- 购物车价格、库存、状态、version 从 SKU 读；
- 下单锁定和扣减 SKU 行；
- 库存 UPDATE 必须包含 `stock >= quantity`；
- SKU 乐观更新带 version，成功后只加 1；
- Product 旧价格/库存/version 暂留兼容，但不作为业务判断。

```powershell
.\mvnw.cmd "-Dtest=ItemDaoIntegrationTests,ItemServiceContractTests,ShoppingServiceContractTests,ShoppingConcurrencyTests" test
```

### 8.4 订单和幂等

涉及 order/entity、`OrderDao.java`、`OrderDao.xml`、`OrderServiceImpl.java`。

DAO/Mapper 至少提供：用户/商家订单加锁查询、主体+动作+key 查询/写入、带来源状态的条件更新、按订单查询唯一 Payment/Refund、支付退款插入、库存恢复所需查询。

状态更新必须以允许的来源状态作为 WHERE 条件。影响行数不是 1 时返回状态冲突，不继续产生副作用。

```powershell
.\mvnw.cmd -DskipTests compile
```

必须 `BUILD SUCCESS`。

---

## 9. 第 5 步：修改 ServiceImpl 使 Red 变 Green

### 9.1 下单顺序

1. 锁定并确认用户有效；
2. trim 幂等键并校验 1-100 字符；
3. remark trim，纯空白转 null，trim 后检查最多 200；
4. 用排序后的购物车项、skuVersion、addressId、规范化 remark 计算 fingerprint；
5. 先查已有幂等订单：同 fingerprint 直接返回，不同 fingerprint 返回 1602；
6. 只有没有旧订单时才验证地址、购物车、店铺和 SKU；
7. 锁 SKU，校验状态/version/库存；
8. 用 SKU 当前价格算总价并保存所有快照；
9. 扣 SKU 库存、保存订单/明细、删除选中购物车；
10. 同一事务提交，任何失败整体回滚。

### 9.2 支付顺序

校验用户 → 规范化 key → 锁本人订单 → 查 `USER+userId+PAY_ORDER+key` → 同请求重放返回 → key 换订单返回 1602 → 只允许 `PENDING_PAYMENT+UNPAID` → 建唯一 Payment → 更新为 `PAID+PAID+NOT_REFUNDED` → 写幂等记录 → 同事务提交。

失败时必须保持 `PENDING_PAYMENT+UNPAID`，不能留下半条 Payment。

### 9.3 取消顺序

共同步骤：校验用户、规范化 key、锁订单、查幂等、检查归属和状态。

`PENDING_PAYMENT`：改 `CANCELLED`，保持 `UNPAID+NOT_REFUNDED`，恢复一次 SKU，不建 Refund，写幂等。

`PAID/PREPARING`：必须找到 Payment；找不到则整体回滚；改 `CANCELLED+PAID+REFUNDED`，恢复一次 SKU，创建恰好一条引用 Payment 的全额 Refund，写幂等，同事务提交。

`DELIVERING/COMPLETED/CANCELLED`：返回 1601，无状态、库存、Refund 副作用。

### 9.4 履约与收货

仅允许：

```text
PAID --商家 prepare--> PREPARING
PREPARING --商家 deliver--> DELIVERING
DELIVERING --订单用户 confirm-receipt--> COMPLETED
```

每个动作使用不同 actionName；先锁订单和查幂等，再条件更新，最后同事务写幂等。禁止越权、跳状态、逆向迁移和商家代收货。

### 9.5 分模块测试

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
.\mvnw.cmd "-Dtest=UserAddressServiceContractTests,UserAddressConcurrencyTests" test
.\mvnw.cmd "-Dtest=ImageOwnershipServiceTests,ItemDaoIntegrationTests,ItemServiceContractTests" test
.\mvnw.cmd "-Dtest=ShoppingServiceContractTests,ShoppingConcurrencyTests" test
.\mvnw.cmd "-Dtest=OrderServiceContractTests,OrderConcurrencyTests" test
```

每一行都应 `BUILD SUCCESS`。

---

## 10. 第 6 步：验证 V9 旧库升级到 V10

使用新排练库，不修改 `delivery_dev`，也不删除现有 `delivery_test`。

### 10.1 创建排练库

`delivery_app` 是后端应用账号，通常只有指定数据库内的建表、读写权限，不能创建一个全新的数据库。因此创建排练库时要临时使用 MySQL 管理员账号（一般是 `root`），创建完成后再只把这个排练库授权给 `delivery_app`。

先生成并显示本次排练库名：

```powershell
$rehearsalDb = 'delivery_v10_' + (Get-Date -Format 'yyyyMMddHHmmss')
$rehearsalDb
```

记下屏幕显示的数据库名。然后使用 MySQL 管理员账号创建它，并仅向应用账号授予这个排练库的权限：

```powershell
$mysqlAdminUser = Read-Host '输入 MySQL 管理员账号（一般为 root）'
mysql.exe --host=localhost --port=3306 --user=$mysqlAdminUser --password --execute="CREATE DATABASE $rehearsalDb CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; GRANT ALL PRIVILEGES ON $rehearsalDb.* TO 'delivery_app'@'127.0.0.1';"
```

出现 `Enter password:` 时输入的是 **MySQL 管理员密码**。输入时屏幕不显示字符是正常现象。

不要给 `delivery_app` 增加全局 `CREATE DATABASE` 权限；这里只授权一个可随时删除的排练库。

最后改回应用账号，验证它已经能够进入排练库：

```powershell
mysql.exe --host=127.0.0.1 --port=3306 --user=$env:DELIVERY_DB_USERNAME --password --database=$rehearsalDb --execute="SELECT DATABASE() AS current_database;"
```

成功标准：命令没有 `ERROR`，结果中的 `current_database` 等于 `$rehearsalDb` 显示的数据库名。

如果刚刚已经遇到 `ERROR 1044 ... Access denied`，不需要更换数据库名。失败的命令没有创建数据库，当前 PowerShell 窗口里的 `$rehearsalDb` 仍可继续使用；先运行 `$rehearsalDb` 确认它有值，再执行上面的管理员创建与授权命令。如果已经关闭了原 PowerShell 窗口，则把实际名称重新赋值，例如：

```powershell
$rehearsalDb = 'delivery_v10_20260912174442'
```

### 10.2 只迁移到 V9

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$env:SPRING_PROFILES_ACTIVE = 'test'
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://127.0.0.1:3306/${rehearsalDb}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:SPRING_FLYWAY_TARGET = '9'
$env:SPRING_DATASOURCE_URL
.\mvnw.cmd "-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false" "-Dspring-boot.run.arguments=--spring.main.web-application-type=none" spring-boot:run
```

这里必须写成 `${rehearsalDb}`，用花括号明确变量边界。如果写成 `$rehearsalDb?useUnicode`，Windows PowerShell 会错误解析变量名，最终可能尝试连接一个名为 `=true&characterencoding...` 的数据库。

运行 Maven 前，上一条输出必须是类似下面的完整 URL，其中数据库名必须是实际排练库，问号也必须存在：

```text
jdbc:mysql://127.0.0.1:3306/delivery_v10_20260912174442?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
```

成功标准：日志显示迁移到版本 9，应用上下文正常启动并退出。不能只看最后的 `BUILD SUCCESS`；日志中不得出现 `Application run failed`、`Access denied` 或 `ERROR`。关闭 DevTools 自动重启是为了让启动失败能够正确传回 Maven，而不是被最后一行 `BUILD SUCCESS` 掩盖。

如果此前报错中的数据库名是 `=true&characterencoding=utf8&servertimezone=asia/shanghai`，说明只是 URL 被 PowerShell 拼错，排练库没有被这次命令修改。无需重新建库，直接重新执行本节全部命令。

### 10.3 插入 V9 最小旧数据

```powershell
mysql.exe --host=127.0.0.1 --port=3306 --user=$env:DELIVERY_DB_USERNAME --password $rehearsalDb
```

```sql
INSERT INTO users(id,account,password_hash,nickname,status)
VALUES(900001,'legacy-user','not-used','Legacy User','ACTIVE');

INSERT INTO merchants(id,account,password_hash,name,phone,status)
VALUES(900001,'legacy-merchant','not-used','Legacy Merchant','13900000000','ACTIVE');

INSERT INTO shops(id,merchant_id,name,status,address_region,address_detail,address_phone)
VALUES(900001,900001,'Legacy Shop','OPEN','杭州','旧店地址','05711234567');

INSERT INTO product_categories(id,shop_id,name,sort_order)
VALUES(900001,900001,'Legacy Category',0);

INSERT INTO images(id,url,content_type,size,content)
VALUES(900001,'/uploads/products/legacy.webp','image/webp',4,NULL);

INSERT INTO products(id,shop_id,category_id,name,description,price,stock,status,version,image_id)
VALUES(900001,900001,900001,'Legacy Product',NULL,12.50,8,'ON_SALE',3,900001);

INSERT INTO cart_items(id,user_id,product_id,sku_id,sku_version,quantity)
VALUES(900001,900001,900001,NULL,NULL,2);

INSERT INTO user_addresses(id,user_id,recipient,phone,region,detail,is_default)
VALUES
(900001,900001,'张三','13800000000','杭州','旧地址一',TRUE),
(900002,900001,'张三','13800000000','杭州','旧地址二',TRUE);

exit
```

这些数据专门模拟：无 SKU 的旧 Product、无 skuId 的旧购物车、重复默认地址、无 merchantId 的旧图片。

### 10.4 升级到 V10

```powershell
Remove-Item Env:SPRING_FLYWAY_TARGET -ErrorAction SilentlyContinue
.\mvnw.cmd "-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false" "-Dspring-boot.run.arguments=--spring.main.web-application-type=none" spring-boot:run
```

成功标准：出现 V10，无 SQL 错误，不删库，应用正常退出。

### 10.5 查询迁移结果

```powershell
mysql.exe --host=127.0.0.1 --port=3306 --user=$env:DELIVERY_DB_USERNAME --password $rehearsalDb
```

```sql
SELECT installed_rank,version,description,success
FROM flyway_schema_history ORDER BY installed_rank;

SELECT id,product_id,name,price,stock,status,version
FROM product_skus WHERE product_id=900001;

SELECT id,product_id,sku_id,sku_version,quantity
FROM cart_items WHERE id=900001;

SELECT user_id,SUM(is_default=TRUE AND deleted_at IS NULL) AS active_defaults
FROM user_addresses WHERE user_id=900001 GROUP BY user_id;

SELECT id,merchant_id,url FROM images WHERE id=900001;

SHOW CREATE TABLE order_action_idempotency;

exit
```

预期：V10 一条且 success=1；默认 SKU 存在；购物车 skuId/version 非空；有效默认地址为 1；图片 merchantId=900001；幂等表唯一范围正确。

### 10.6 恢复测试库配置

```powershell
Remove-Item Env:SPRING_DATASOURCE_URL -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_FLYWAY_TARGET -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_DEVTOOLS_RESTART_ENABLED -ErrorAction SilentlyContinue
$env:SPRING_PROFILES_ACTIVE = 'test'
$env:SPRING_DATASOURCE_URL
$env:SPRING_FLYWAY_TARGET
$env:SPRING_DEVTOOLS_RESTART_ENABLED
$env:SPRING_PROFILES_ACTIVE
```

成功标准：前三项不输出任何值，最后一项只输出 `test`。这表示后续命令不会继续误用排练库，也不会停留在只迁移到 V9 的模式。

排练库保留到验收完成。以后删除时必须使用记下的准确库名，不能用通配符，不能删除 `delivery_dev` 或 `delivery_test`。

---

## 11. 第 7 步：全量后端验证

```powershell
$env:SPRING_PROFILES_ACTIVE
$env:DELIVERY_DB_USERNAME
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
.\mvnw.cmd test
.\mvnw.cmd clean package
```

两次都必须 `Failures: 0`、`Errors: 0`、`BUILD SUCCESS`。

检查报告：

```powershell
Get-ChildItem -LiteralPath 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend\target\surefire-reports' -Filter '*.txt' |
    Select-String -Pattern 'Tests run:'

Get-ChildItem -LiteralPath 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend\target\surefire-reports' -Filter '*.txt' |
    Select-String -Pattern 'Failures: [1-9]|Errors: [1-9]'
```

第二条成功时无输出。

检查真实测试库 V10：

```powershell
mysql.exe --host=127.0.0.1 --port=3306 --user=$env:DELIVERY_DB_USERNAME --password delivery_test
```

```sql
SELECT version,COUNT(*) AS row_count,SUM(success) AS success_count
FROM flyway_schema_history WHERE version='10' GROUP BY version;
exit
```

预期：`row_count=1`、`success_count=1`。

---

## 12. 第 8 步：检查、同步、提交、推送

### 12.1 检查范围

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git status --short
git diff --stat
git diff --name-only
```

确认无前端/Controller/V1-V9 改动，无数据库备份、密码、IDE 临时文件；ABC 文档没有暂存。

### 12.2 合入开发期间的最新 develop

```powershell
git fetch origin
git log --oneline HEAD..origin/develop
git merge origin/develop
```

若没有远程新增，merge 会提示 Already up to date。若出现提交信息编辑器：Vim 按 `Esc`，输入 `:wq`，回车。

合并后重新全测：

```powershell
Set-Location '.\backend'
.\mvnw.cmd test
```

### 12.3 提交 Green

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git add -- 'backend/src/main/java' 'backend/src/main/resources/mapper' 'backend/src/main/resources/db/migration/V10__harden_v12_persistence.sql' 'backend/src/test/java' 'B-V1.2后端持久化收口执行文档.md'
git restore --staged -- 'ABC阶段1业务TDD开发与联调执行文档.md'
git status --short
git commit -m 'feat(green): complete B V1.2 persistence closeout'
```

可以是 Red、Green 两个提交，也可最终一个提交，不要求每步提交。

### 12.4 推 B 分支

```powershell
git push -u origin codex/b-v12-green
```

这不等于已进入 develop。

### 12.5 合入 develop

只有最新 develop 已同步、全量测试通过、V9→V10 排练通过、A 无接口冲突，才合并。

若团队允许本地合并：

```powershell
git switch develop
git pull --ff-only origin develop
git merge --no-ff codex/b-v12-green
Set-Location '.\backend'
$env:SPRING_PROFILES_ACTIVE = 'test'
.\mvnw.cmd test
Set-Location '..'
git push origin develop
```

若仓库要求 Pull Request，只推 B 分支，然后创建 PR，不直接推 develop。

---

## 13. 第 9 步：正式联调

联调是把真实前端、真实后端和开发数据库一起启动，检查页面发出的 HTTP 请求是否按 API 文档完成完整业务链。

### 13.1 拉取最终 develop

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git switch develop
git pull --ff-only origin develop
git status --short --branch
```

### 13.2 启动后端（窗口 1）

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$deliveryCredential = Get-Credential -Message '输入本机 MySQL 开发库账号和密码'
$env:DELIVERY_DB_USERNAME = $deliveryCredential.UserName
$env:DELIVERY_DB_PASSWORD = $deliveryCredential.GetNetworkCredential().Password
$env:SPRING_PROFILES_ACTIVE = 'dev'
.\mvnw.cmd spring-boot:run
```

成功标准：V10 迁移/校验成功，后端启动完成，窗口保持运行。

### 13.3 启动前端（窗口 2）

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
node -v
npm.cmd -v
npm.cmd ci
npm.cmd run test:run
npm.cmd run build
npm.cmd run dev
```

若 `npm ci` 出现 `EPERM ... esbuild.exe`：关闭前端服务器和占用项目的终端，在任务管理器结束残留 node.exe，再重新运行。

### 13.4 第一轮完整业务链

依次操作：用户/商家注册登录 → 商家建店并填地址 → 上传图片 → 建分类、商品和 SKU → 上架并营业 → 用户建收货地址 → 选 SKU 加购物车 → 下单 → 支付 → 商家制作 → 商家配送 → 用户确认收货。

浏览器 Network 中每一步检查：

- 请求路径以 `/api/v1` 开头；
- HTTP 状态与 API 文档一致；
- Request Payload 正确；
- Response 的 `code/msg/data` 正确；
- 登录请求带 Bearer Token；
- 幂等动作带 `X-Idempotency-Key`。

### 13.5 取消与退款链

至少建三张订单：

1. PENDING_PAYMENT 取消：恢复库存、无 Refund；
2. PAID 取消：恢复一次库存、唯一全额 Refund；
3. PREPARING 取消：恢复一次库存、唯一全额 Refund。

再验证 DELIVERING/COMPLETED 不能取消；相同 key 重放不重复退款或恢复库存；待支付取消查询退款返回文档规定的不存在；已支付取消可查 Payment 引用和退款金额。

### 13.6 归属与异常

- 商家乙不能用商家甲图片；
- 用户甲不能改用户乙地址/订单；
- 商家甲不能推进商家乙订单；
- 商品或 SKU 下架不能加购/下单；
- SKU version 变化后旧确认不能直接下单；
- 库存不足时订单不建、购物车不删、库存不变；
- 纯空白备注为 null，trim 后 200 成功、201 失败。

### 13.7 联调记录模板

```text
测试时间：
develop 提交号：
测试人：
前置数据：
页面操作：
请求方法和路径：
HTTP 状态：
响应 code：
预期结果：
实际结果：
是否通过：
截图或错误摘要：
```

提交号：

```powershell
git rev-parse --short HEAD
```

---

## 14. 最终勾选清单

### B 开发

- [ ] 当前工作在 `codex/b-v12-green`；
- [ ] 没有合入旧 `codex/v12-backend-tdd`；
- [ ] 旧 Green 基线已记录；
- [ ] 数据库/Service Red 已补；
- [ ] 只新增 V10，V1-V9 未改；
- [ ] V9 旧数据迁移排练通过；
- [ ] 地址唯一、图片归属、SKU 真值完成；
- [ ] 取消退款与所有动作幂等完成；
- [ ] 下单重试与 remark 规范化完成；
- [ ] 全量 test 和 package 通过。

### Git 与联调

- [ ] 未提交密码、备份、IDE 文件和用户 ABC 文档修改；
- [ ] 已同步最新 origin/develop 并回归；
- [ ] B 分支已推送并按团队方式合入 develop；
- [ ] 前后端来自同一个最新 develop；
- [ ] 完整下单、履约、取消退款、归属和异常链通过；
- [ ] 联调记录包含提交号、请求、预期、实际和结果。

全部勾选后才能认定：B 的 V1.2 持久化完成，项目完成联调并可作为初版交付候选。

---

## 15. 当前停止点和下一条指令

第 6 节已经完成：5 个测试类、18 个测试方法均已写完并实际运行；结果为 7 个通过、8 个失败、3 个错误。11 个未通过项全部来自预期的业务契约缺口，不存在编译、数据库连接或 Flyway 迁移故障。Red 里程碑已经提交为 `b8ee570`，没有 push。

第 7.1 和 7.4 已经完成：已新建并写完 `V10__harden_v12_persistence.sql`。V10 当前包含默认 SKU 和购物车回填、默认地址唯一约束、图片归属、统一动作幂等表，以及支付/退款约束修正。V1～V9 没有修改，V10 尚未提交。

2026-09-12 的实际测试日志已经证明 `delivery_test` 成功执行 V10：Flyway 显示成功校验 10 个迁移、当前版本为 10、无需继续迁移。

第 8 节及其对应的第 9 节 Green 行为已经完成：地址写操作统一锁用户并转换唯一冲突；图片上传保存 merchantId，商品用图按商家校验；购物车写入必填 skuVersion 并以 SKU 为业务真相；订单写操作锁行并使用主体+动作+key 的统一幂等记录；待支付取消不退款，已支付/制作中取消必须引用真实 Payment 并全额退款；下单重放先于地址重新校验，备注先规范化再计算 fingerprint。

实际验证结果：

```text
第 8 节相关测试：47 个通过，0 失败，0 错误
完整后端测试：225 个通过，0 失败，0 错误
最后一次订单收口复测：24 个通过，0 失败，0 错误
BUILD SUCCESS
```

为满足图片归属契约，原测试夹具只补充了 `merchant_id` 建数参数，没有改变业务断言。V1～V9 没有修改；V10、生产代码和夹具调整目前均未提交、未 push。`ABC阶段1业务TDD开发与联调执行文档.md` 仍是用户原有的独立修改，不纳入 B 的提交。

第 10 节旧库升级排练已经完成。独立排练库 `delivery_v10_20260912174728` 先执行 V1～V9，再插入无 SKU 商品、无 skuId/skuVersion 购物车、重复默认地址及无 merchantId 图片等旧数据，随后成功升级到 V10。查询结果为：V10 `success=1`；默认 SKU 的价格、库存、状态和版本正确；购物车完成 SKU 回填；有效默认地址收敛为 1；图片归属回填为原商家；统一动作幂等表及其唯一约束存在。排练库暂时保留，不影响 `delivery_dev` 和 `delivery_test`。

第 11 节也已经完成：`mvn test` 与 `clean package` 均成功，Surefire 报告合计 225 个测试，0 失败、0 错误、0 跳过；生成的 JAR 时间为 2026-09-12 18:02:46。真实 `delivery_test` 中 V10 查询结果为 `row_count=1`、`success_count=1`。

第 12.1 节范围检查已经完成：当前分支是 `codex/b-v12-green`；没有前端、Controller 或 V1～V9 改动；`git diff --check` 无错误。LF/CRLF 信息只是 Windows 换行提醒。`ABC阶段1业务TDD开发与联调执行文档.md` 是用户已有的独立修改，必须继续排除在 B 的暂存和提交之外。

已经执行 `git fetch origin`；`git log --oneline --decorate HEAD..origin/develop` 没有输出，说明当前不存在需要合入的远程 `develop` 新提交，不执行无意义的 merge。

下一步是只暂存并检查 B 的最终 Green 文件：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git add -- 'backend/src/main/java' 'backend/src/main/resources/mapper' 'backend/src/main/resources/db/migration/V10__harden_v12_persistence.sql' 'backend/src/test/java' 'B-V1.2后端持久化收口执行文档.md'
git restore --staged -- 'ABC阶段1业务TDD开发与联调执行文档.md'
git status --short
git diff --cached --name-only
```

成功标准：所有 B 代码、测试、Mapper、V10 和本执行文档显示在暂存区；`ABC阶段1业务TDD开发与联调执行文档.md` 只能显示为未暂存的 ` M`，不能出现在 `git diff --cached --name-only` 中。确认后才创建最终 Green 提交，不在检查前 push。
