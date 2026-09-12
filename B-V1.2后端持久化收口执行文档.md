# B-V1.2 后端持久化收口执行文档

更新时间：2026-09-12  
依据：`docs/software-requirements-specification.md`、`docs/api/backend-api-design.md`  
远程基线：`origin/develop@4fa815e`  
工作分支：`codex/b-v12-green`

## 1. 当前状态

本分支直接从 A 已推送的最新 `develop` 创建，没有合入旧的 `codex/v12-backend-tdd`，因此不包含此前代 A 编写的 Red/Controller 提交。

已自动验证：

```text
不连接数据库的 Controller/安全/异常处理测试：164 个
Failures: 0
Errors: 0
BUILD SUCCESS
```

没有执行 Flyway、MySQL 和 `@SpringBootTest`，没有修改任何数据库。

## 2. B 只负责什么

B 接下来只改这些范围：

```text
backend/src/main/java/**/entity
backend/src/main/java/**/dao
backend/src/main/java/**/service/impl
backend/src/main/resources/mapper
backend/src/main/resources/db/migration
后端数据库、Service、事务和并发测试
```

默认不改 Controller、Service 接口 DTO 和 A 已提交的 Controller 测试。若接口层与最新 API 文档冲突，只记录并交给 A 确认，不能为了让数据库实现方便擅自改变接口。

## 3. 当前必须先解决的问题

### 3.1 迁移链不能直接视为完成

V3 至 V9 已经进入 `develop`，从现在开始按已发布迁移处理，不直接修改这些文件。新增修复统一写入：

```text
backend/src/main/resources/db/migration/V10__harden_v12_persistence.sql
```

V10 至少要处理：

1. 为阶段1已有 Product 回填默认 SKU；
2. 把旧购物车的 `product_id` 映射为默认 `sku_id`；
3. 回填完成后再给新业务必填列增加非空约束；
4. 为同一用户“最多一个有效默认地址”增加数据库唯一保障；
5. 给图片增加上传商家归属，支持商品图片归属校验；
6. 修正幂等键唯一范围，不能让不同用户互相占用一个全局 key，也不能允许同一主体用同一动作 key 操作不同订单；
7. 给支付、退款、订单状态和常用归属查询补必要约束和索引；
8. 所有回填必须兼容已有 V1/V2 数据，不能要求删库重建。

### 3.2 地址默认值存在并发漏洞

当前代码用“先查询是否为空，再清除默认，再插入”的方式决定首地址。两个并发请求可能同时认为自己是首地址。B 需要：

1. DAO 增加用户级锁或锁定该用户全部有效地址；
2. ServiceImpl 在同一事务中完成锁定、清除旧默认和写入新默认；
3. 数据库增加有效默认地址唯一约束；
4. 增加并发测试，最终只能有一个默认地址。

### 3.3 图片没有商家归属

当前 `images` 表没有 `merchant_id`，商品实现只检查图片是否存在，任意商家可能引用别人的图片。B 需要：

1. V10 为图片增加商家归属；
2. 上传图片时写入当前 merchantId；
3. DAO 提供 `findOwnedById(merchantId, imageId)`；
4. 创建/修改商品必须验证图片属于当前商家；
5. 增加“引用他人图片失败”的 Service/DAO 测试。

### 3.4 Product 和 SKU 仍有双重价格库存来源

当前 Product 仍写入 `price/stock/version`，购物车查询也同时读取 Product 和 SKU。最新 SRS/API 明确以 SKU 的价格、库存和版本为准。B 需要：

1. 新业务的加购、改数量、下单、扣库存、恢复库存全部只使用 SKU；
2. Product 的 `minPrice/inStock` 由 SKU 查询结果派生；
3. 不再用 Product version 判断 SKU 价格是否变化；
4. 旧 Product 列是否删除必须先完成回填并确认 A 的 DTO 已不再依赖；在确认前可以保留兼容列，但不能作为业务真值。

### 3.5 取消和退款存在确定性错误

当前 `OrderServiceImpl` 在“有幂等键”时，不区分订单是否已支付就创建退款记录。因此待支付订单取消也可能产生 Refund，违反 SRS。

B 必须实现：

- `PENDING_PAYMENT`：取消、恢复 SKU 库存，不创建 Refund；
- `PAID/PREPARING`：取消、恢复一次库存、根据唯一 Payment 创建唯一全额 Refund；
- `DELIVERING/COMPLETED/CANCELLED`：返回1601，无副作用；
- 找不到已支付订单对应 Payment：整个事务回滚；
- 重复取消不得重复恢复库存或退款。

### 3.6 幂等不能只在订单列里记一个 key

当前 pay/prepare/deliver/receipt key 只是写入订单列，没有主体+动作+请求指纹的完整唯一范围。同一个 key 操作另一张订单时可能不会返回1602。

建议 V10 新增统一幂等表，至少包含：

```text
actor_type
actor_id
action_name
idempotency_key
request_fingerprint
order_id
created_at
```

唯一键为：

```text
(actor_type, actor_id, action_name, idempotency_key)
```

每个动作必须先锁订单，再检查幂等记录，再做条件状态更新和副作用，最后在同一事务写入幂等结果。

### 3.7 下单幂等和备注规范化顺序不正确

当前实现先验证地址是否仍存在，再查询旧幂等订单；地址后来被删除时，同键重试无法返回首次订单。备注长度也按 trim 前判断，指纹使用未规范化的 remark。

B 需要按以下顺序改：

1. 校验并规范化幂等键；
2. 将 remark trim，纯空白变 null，trim 后检查最多200字符；
3. 使用规范化后的 addressId、items、remark 计算指纹；
4. 查询已有幂等订单：同指纹直接返回，不再依赖当前地址/购物车/SKU状态；
5. 没有旧订单时才验证地址、购物车、店铺、SKU并创建订单。

## 4. B 的执行顺序

### 第一步：只写数据库/Service Red

在现有数据库测试中增加以下失败场景，不新增 Controller 测试：

- 两个并发首地址只能留下一个默认地址；
- 阶段1 Product/购物车经过迁移后都有合法默认 SKU；
- 商家不能引用其他商家的图片；
- 待支付取消没有退款记录；
- 已支付取消恰好一个退款并只恢复一次库存；
- 同一主体、动作、key 换订单返回1602；
- 下单成功后删除原地址，同 key 重试仍返回原订单；
- remark 按 trim 后长度和内容生成幂等指纹。

先提交一个可解释的数据库/Service Red，不动 A 的 Controller 测试。

### 第二步：新增 V10

只新增 V10，不编辑 V1 至 V9。先加可空列和新表，再回填，再增加非空/唯一/外键约束。完成后先审查 SQL，再在备份过的测试库执行。

### 第三步：修改 DAO 和 Mapper

增加：地址锁、图片归属查询、SKU 回填/查询支持、订单行锁、支付退款唯一查询、统一幂等记录读写。所有库存和状态变化必须使用带条件的 SQL 更新。

### 第四步：修改 ServiceImpl

按第3节依次修复地址、图片、SKU购物车、下单、支付/退款/履约。一个事务只负责一个完整业务动作，失败必须整体回滚。

### 第五步：验证

不需要数据库密码的快速测试：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
.\mvnw.cmd test -Dtest=AuthenticationInterceptorTests,DefaultJwtTokenServiceTests,GlobalExceptionHandlerTests,ImageControllerTests,ItemControllerTests,MerchantControllerTests,OrderControllerTests,RestaurantControllerTests,ShoppingControllerTests,UserAddressControllerTests,UserControllerTests
```

需要人工输入测试库账号密码后再运行：

```powershell
$env:DB_USERNAME = Read-Host '输入测试库用户名'
$env:DB_PASSWORD = Read-Host '输入测试库密码' -MaskInput
.\mvnw.cmd flyway:validate
.\mvnw.cmd test
```

成功标准：V10 只执行一次、Flyway history success=1、全量测试 Failures/Errors 为0、旧数据无需删库即可升级。

## 5. 当前不要做

- 不把旧 `codex/v12-backend-tdd` 合入当前分支；
- 不修改 A 已完成的 Controller/Red，除非 API 文档确定冲突并由 A 确认；
- 不修改 V1 至 V9 的已发布内容；
- 不运行 `flyway repair`；
- 不删除数据库重建来掩盖迁移问题；
- 不开发优惠券、评价、骑手、真实支付、地图和管理员后台。

## 6. 当前停止点

现在已经可以开始第4节第一步。数据库尚未执行迁移，因此 B 的下一件事不是联调，而是先补数据库/Service Red，再写 V10 和对应 DAO/ServiceImpl Green。
