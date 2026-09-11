# 轻量级外卖服务平台后端 API 契约

| 文档版本 | V1.2 |
| --- | --- |
| 基线 | `docs/software-requirements-specification.md` V1.2 |
| 用途 | 前后端联调、接口测试、TDD 和交叉验收 |

## 1. 契约范围

本文是基于 SRS V1.2 的前后端联调、测试和验收契约。所有路径均以
`/api/v1` 为前缀，字符集为 UTF-8。普通用户和商家使用独立账号及登录接口。
本文定义字段、状态、权限、错误码和示例；后端实现不得弱化 SRS 中的归属、快照、
库存、事务和幂等规则。

统一约定：

- ID 为正整数；数量、页码和版本号为整数；金额为最多两位小数的 JSON number。
- 时间为 ISO 8601 UTC 字符串，例如 `2026-09-11T08:30:00Z`。
- 未特别说明的字段不可为 `null`；集合无数据时返回 `[]`。
- `PATCH` 至少包含一个允许修改的字段；未知字段和非法枚举值返回 `400`。
- 密码、密码摘要、内部异常堆栈和客户端本地文件路径不得出现在响应中。

## 2. 认证、权限与幂等

受保护请求携带：

```
Authorization: Bearer <accessToken>
Content-Type: application/json
```

权限标记：

| 标记 | 含义 |
| --- | --- |
| `Public` | 无需登录，可访问公开浏览或注册接口 |
| `User` | 有效普通用户令牌，只能操作本人资源 |
| `Merchant` | 有效商家令牌，只能操作本人店铺及其资源 |

以下写操作必须携带唯一 `X-Idempotency-Key`，建议使用 UUID：

```
POST /orders
POST /orders/{orderId}/pay
POST /orders/{orderId}/cancel
POST /orders/{orderId}/confirm-receipt
POST /merchant/orders/{orderId}/prepare
POST /merchant/orders/{orderId}/deliver
```

相同主体使用相同幂等键和相同请求重试时，返回首次业务结果，不重复创建订单、
支付、退款、扣减库存或推进状态。相同幂等键对应不同请求时返回
`IDEMPOTENCY_CONFLICT`。不同幂等键重复操作最终状态时返回
`ORDER_STATE_CONFLICT`，不得再次执行副作用。

## 3. 统一响应和分页

成功响应：

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {}
}
```

失败响应使用相同外壳，`data` 可为 `null` 或包含字段错误、当前版本等上下文：

```json
{
  "code": 1001,
  "msg": "请求参数不合法",
  "data": {
    "fieldErrors": {
      "quantity": "必须大于 0"
    }
  }
}
```

分页 `data` 固定为：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 10,
  "total": 0,
  "totalPages": 0
}
```

`page` 从 1 开始，`pageSize` 为 1 到 100，默认 10；`sortOrder` 只接受
`asc` 和 `desc`。

## 4. HTTP 状态和业务错误码

| HTTP | 业务码 | 名称 | 使用场景 |
| --- | ---: | --- | --- |
| 400 | 1001 | `VALIDATION_ERROR` | 缺字段、格式错误、空购物车、备注超长 |
| 401 | 1002 | `UNAUTHENTICATED` | 未携带、过期或无效令牌 |
| 403 | 1003 | `FORBIDDEN` | 角色错误或资源不属于当前主体 |
| 404 | 1004 | `RESOURCE_NOT_FOUND` | 当前可见范围内不存在 |
| 409 | 1005 | `RESOURCE_CONFLICT` | 唯一约束或通用并发冲突 |
| 409 | 1101 | `ACCOUNT_EXISTS` | 用户账号已存在 |
| 401 | 1102 | `BAD_CREDENTIALS` | 账号或密码错误 |
| 403 | 1103 | `ACCOUNT_DISABLED` | 用户账号被禁用 |
| 409 | 1201 | `MERCHANT_ACCOUNT_EXISTS` | 商家账号已存在 |
| 403 | 1202 | `MERCHANT_SUSPENDED` | 商家已暂停经营权限 |
| 409 | 1301 | `SHOP_NOT_OPEN` | 店铺未营业，不能购物或下单 |
| 409 | 1302 | `SHOP_ADDRESS_REQUIRED` | 店铺无完整经营地址，不能营业 |
| 409 | 1401 | `PRODUCT_OFF_SALE` | 商品下架 |
| 409 | 1402 | `INSUFFICIENT_STOCK` | SKU 库存不足 |
| 409 | 1403 | `SKU_OFF_SALE` | SKU 下架或不可购买 |
| 409 | 1404 | `SKU_VERSION_CONFLICT` | SKU 版本过期 |
| 400 | 1405 | `IMAGE_INVALID` | 图片格式、大小或内容不合法 |
| 409 | 1406 | `IMAGE_REQUIRED` | 上架商品缺少合法主图 |
| 400 | 1501 | `CART_EMPTY` | 未选择有效购物车项 |
| 400 | 1502 | `MIXED_SHOPS` | 一次结算包含多个店铺 |
| 409 | 1503 | `ADDRESS_INVALID` | 地址不存在、不属于用户或已失效 |
| 409 | 1601 | `ORDER_STATE_CONFLICT` | 当前订单状态不允许操作 |
| 409 | 1602 | `IDEMPOTENCY_CONFLICT` | 同一幂等键对应不同请求 |
| 409 | 1603 | `PRICE_OR_VERSION_CHANGED` | 下单确认的 SKU 价格或版本已变化 |
| 409 | 1604 | `REFUND_CONFLICT` | 重复退款或退款状态冲突 |
| 500 | 9000 | `INTERNAL_ERROR` | 未预期服务端错误 |

对无权查看的他人订单、地址或购物车项，可以统一返回 `404`，不得泄漏资源存在性。

## 5. 枚举和状态机

### 5.1 身份、店铺和商品

| 对象 | 枚举 |
| --- | --- |
| 用户状态 | `ACTIVE`、`DISABLED` |
| 商家状态 | `ACTIVE`、`SUSPENDED` |
| 店铺状态 | `CLOSED`、`OPEN`、`TEMPORARILY_CLOSED` |
| 商品/SKU 状态 | `OFF_SALE`、`ON_SALE` |

用户和商家注册初始状态为 `ACTIVE`；新店铺为 `CLOSED`；新商品和新 SKU
为 `OFF_SALE`。店铺进入 `OPEN` 前必须有完整经营地址。只有商品和 SKU
均为 `ON_SALE` 且 SKU 库存大于 0 时可购买。

### 5.2 订单、支付和退款

```
PENDING_PAYMENT --用户模拟支付--> PAID
PAID -----------商家开始制作--> PREPARING
PREPARING ------商家开始配送--> DELIVERING
DELIVERING ------用户确认收货--> COMPLETED

PENDING_PAYMENT ------用户取消--> CANCELLED
PAID -----------------用户取消--> CANCELLED + 全额退款
PREPARING ------------用户取消--> CANCELLED + 全额退款
```

订单创建初始状态为 `PENDING_PAYMENT`。禁止跳过状态、逆向迁移或商家代替用户
确认收货。`DELIVERING`、`COMPLETED`、`CANCELLED` 不允许普通取消。

```
UNPAID --模拟支付成功--> PAID
NOT_REFUNDED --已支付订单取消--> REFUNDING --同步退款成功--> REFUNDED
```

`REFUNDING` 仅是事务内中间状态，对外成功结果为 `REFUNDED`。已支付订单取消
后支付状态仍为 `PAID`，退款状态单独为 `REFUNDED`。

稳定状态组合：

| 订单状态 | 支付状态 | 退款状态 |
| --- | --- | --- |
| `PENDING_PAYMENT` | `UNPAID` | `NOT_REFUNDED` |
| `PAID`、`PREPARING`、`DELIVERING`、`COMPLETED` | `PAID` | `NOT_REFUNDED` |
| `CANCELLED` | `UNPAID` | `NOT_REFUNDED` |
| `CANCELLED` | `PAID` | `REFUNDED` |

## 6. 资源对象

### 6.1 认证对象

```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 7200,
  "user": null,
  "merchant": {
    "id": 1,
    "account": "merchant01",
    "name": "示例快餐店商家",
    "phone": "13900000000",
    "status": "ACTIVE",
    "createdAt": "2026-09-11T08:00:00Z",
    "updatedAt": "2026-09-11T08:00:00Z"
  },
  "roles": ["MERCHANT"]
}
```

用户登录时 `user` 为 `User`、`merchant` 为 `null`、`roles` 为
`["USER"]`；商家登录时相反。资料对象均不得包含密码。

### 6.2 基础资料对象

`User` 字段：`id`、`account`、`nickname`、`phone`、`status`、`createdAt`、
`updatedAt`。`phone` 可为 `null`。

`Merchant` 字段：`id`、`account`、`name`、`phone`、`status`、`createdAt`、
`updatedAt`。

`UserAddress` 字段：

```json
{
  "id": 51,
  "recipient": "张三",
  "phone": "13800000000",
  "region": "浙江省杭州市西湖区",
  "detail": "文三路 1 号 101 室",
  "isDefault": true,
  "createdAt": "2026-09-11T08:00:00Z",
  "updatedAt": "2026-09-11T08:00:00Z"
}
```

首个有效地址自动设为默认地址；删除默认地址后不自动指定其他地址。商家只能
通过订单快照读取用户收货信息。

### 6.3 店铺、图片、分类、商品和 SKU

`Shop` 必须包含 `id`、`merchantId`、`name`、`description`、`region`、`detail`、
`phone`、`status`、`createdAt`、`updatedAt`。未设置经营地址时，`region`、
`detail`、`phone` 可为 `null`；进入 `OPEN` 前必须完整。

`ImageAsset` 字段为 `id`、`url`、`contentType`、`size`、`createdAt`。只接受
JPG、PNG、WebP，单文件不超过 5 MB。被历史订单快照引用的 URL 必须持续可访问。

`Category` 字段为 `id`、`shopId`、`name`、`sortOrder`、`createdAt`、
`updatedAt`。

`Sku` 字段：

```json
{
  "id": 1001,
  "productId": 101,
  "name": "大份",
  "price": 19.8,
  "stock": 20,
  "status": "ON_SALE",
  "version": 3,
  "createdAt": "2026-09-11T08:10:00Z",
  "updatedAt": "2026-09-11T08:20:00Z"
}
```

同一商品内 SKU 名称唯一；价格大于 0；库存大于等于 0；版本用于乐观锁。

`Product` 不再直接承载销售价格和库存，字段为：

```json
{
  "id": 101,
  "shopId": 11,
  "categoryId": 21,
  "name": "招牌牛肉饭",
  "description": "招牌套餐",
  "image": {
    "id": 301,
    "url": "/uploads/products/301.webp",
    "contentType": "image/webp",
    "size": 182736,
    "createdAt": "2026-09-11T08:05:00Z"
  },
  "status": "ON_SALE",
  "minPrice": 18.8,
  "inStock": true,
  "skus": [{
    "id": 1001,
    "productId": 101,
    "name": "默认规格",
    "price": 18.8,
    "stock": 20,
    "status": "ON_SALE",
    "version": 3,
    "createdAt": "2026-09-11T08:10:00Z",
    "updatedAt": "2026-09-11T08:20:00Z"
  }],
  "createdAt": "2026-09-11T08:10:00Z",
  "updatedAt": "2026-09-11T08:20:00Z"
}
```

下架草稿的 `image` 可为 `null`；商品上架前必须有图片和至少一个 SKU。
`minPrice` 是当前返回 SKU 的最低价格，`inStock` 表示至少一个返回 SKU 库存大于
0，二者均为只读派生字段，不是商品级价格或库存。公开列表中的 `skus` 只返回
`ON_SALE` SKU；商家查看自己的商品时可请求全部 SKU。

### 6.4 购物车和订单

`CartItem` 字段为 `id`、`product` 摘要、`sku`、`quantity`、`subtotal`、
`available`、`unavailableReason`、`createdAt`、`updatedAt`。其中 `product` 摘要
固定包含 `id`、`shopId`、`name`、`imageUrl`、`status`，`sku` 使用完整 `Sku`。
购物车项引用 `skuId`，同一用户同一 SKU 原则上只有一项；加入购物车不扣库存。

订单明细 `OrderLine` 必须包含商品和 SKU 快照：

```json
{
  "productId": 101,
  "skuId": 1001,
  "productName": "招牌牛肉饭",
  "skuName": "大份",
  "imageUrl": "/uploads/products/301.webp",
  "unitPrice": 19.8,
  "quantity": 2,
  "subtotal": 39.6
}
```

`Order` 字段至少包括 `id`、`orderNumber`、`userId`、`shopId`、`shopName`、
`shopAddressSnapshot`、`userAddressSnapshot`、`remark`、`lines`、`total`、
`status`、`paymentStatus`、`refundStatus`、`cancelReason`、`cancelledAt`、
`completedAt`、`createdAt`、`updatedAt`。地址、商品、SKU、图片和价格均为下单
快照，源数据后续变化不得影响订单。

`Payment` 字段为 `id`、`orderId`、`paymentNumber`、`amount`、`status`、
`idempotencyKey`、`paidAt`、`createdAt`。

`Refund` 字段为 `id`、`orderId`、`paymentId`、`refundNumber`、`amount`、
`status`、`idempotencyKey`、`completedAt`、`createdAt`。

订单备注和取消原因均可选，去除首尾空格后最多 200 个字符；纯空白按无内容处理。

## 7. 接口目录

| 方法 | 路径 | 权限 | 用途 |
| --- | --- | --- | --- |
| POST | `/users` | Public | 用户注册 |
| POST | `/users/login` | Public | 用户登录 |
| GET/PATCH | `/users/me` | User | 查询/修改个人信息 |
| POST | `/merchants` | Public | 商家注册 |
| POST | `/merchants/login` | Public | 商家登录 |
| GET/PATCH | `/merchants/me` | Merchant | 查询/修改商家资料 |
| POST/GET | `/user-addresses` | User | 新增/查询收货地址 |
| PATCH/DELETE | `/user-addresses/{addressId}` | User | 修改/删除地址 |
| POST | `/files/images` | Merchant | 上传商品图片 |
| POST/GET | `/shops` | Merchant/Public | 创建/查询店铺 |
| GET | `/shops/{shopId}` | Public | 店铺详情 |
| PATCH | `/shops/{shopId}` | Merchant | 修改店铺资料或状态 |
| PATCH | `/shops/{shopId}/address` | Merchant | 修改经营地址 |
| POST/GET | `/shops/{shopId}/categories` | Merchant/Public | 分类增/查 |
| PATCH/DELETE | `/categories/{categoryId}` | Merchant | 分类改/删 |
| POST | `/products` | Merchant | 新增商品及至少一个 SKU |
| GET | `/shops/{shopId}/products` | Public/Merchant | 商品列表 |
| GET | `/products/{productId}` | Public/Merchant | 商品详情 |
| PATCH | `/products/{productId}` | Merchant | 修改商品基础信息 |
| POST | `/products/{productId}/skus` | Merchant | 新增 SKU |
| PATCH | `/skus/{skuId}` | Merchant | 修改 SKU |
| POST/GET | `/cart-items` | User | 加入/查询购物车 |
| PATCH/DELETE | `/cart-items/{itemId}` | User | 修改/删除购物车项 |
| POST/GET | `/orders` | User | 创建/查询订单 |
| GET | `/orders/{orderId}` | User | 订单详情 |
| POST | `/orders/{orderId}/pay` | User | 模拟支付 |
| POST | `/orders/{orderId}/cancel` | User | 取消订单 |
| POST | `/orders/{orderId}/confirm-receipt` | User | 确认收货 |
| GET | `/orders/{orderId}/refund` | User | 查询退款 |
| GET | `/merchant/orders` | Merchant | 商家订单列表 |
| GET | `/merchant/orders/{orderId}` | Merchant | 商家订单详情 |
| POST | `/merchant/orders/{orderId}/prepare` | Merchant | 开始制作 |
| POST | `/merchant/orders/{orderId}/deliver` | Merchant | 开始配送 |

## 8. 接口详细约定

### 8.1 认证与资料

`POST /users` 请求字段：`account`、`password`、`passwordConfirm`、`nickname`、
`phone?`；成功 `201` 返回 `User`。失败：`1001`、`1101`。

`POST /users/login` 和 `POST /merchants/login` 请求字段均为 `account`、`password`；
成功 `200` 返回 `AuthSession`。用户登录失败为 `1102` 或 `1103`；商家登录失败
为 `1102` 或 `1202`。

`POST /merchants` 请求字段：`account`、`password`、`passwordConfirm`、`name`、
`phone`；成功 `201` 返回 `Merchant`。失败：`1001`、`1201`。

`GET/PATCH /users/me` 和 `GET/PATCH /merchants/me` 分别只允许对应主体访问。
PATCH 用户允许 `nickname`、`phone`；PATCH 商家允许 `name`、`phone`。

### 8.2 用户地址

- `POST /user-addresses`：请求 `recipient`、`phone`、`region`、`detail`、
  `isDefault?`，成功 `201` 返回 `UserAddress`。
- `GET /user-addresses`：返回当前用户的 `UserAddress[]`。
- `PATCH /user-addresses/{addressId}`：允许修改上述字段，成功 `200`。
- `DELETE /user-addresses/{addressId}`：成功 `200` 返回
  `{ "id": 51, "deleted": true }`。

仅允许地址所有者访问。字段错误返回 `1001`，资源或归属错误返回 `1004` 或
`1503`。默认地址切换必须原子完成；删除默认地址后不自动指定其他地址。

地址创建、查询、修改、删除的成功状态分别为 `201`、`200`、`200`、`200`。
地址字段校验失败示例：

```json
{
  "code": 1001,
  "msg": "请求参数不合法",
  "data": { "fieldErrors": { "phone": "手机号格式不正确" } }
}
```

### 8.3 店铺、图片和分类

- `POST /shops`：请求 `name`、`description?`，创建为 `CLOSED`。
- `GET /shops`：查询 `page`、`pageSize`、`keyword?`、`status?`、`mine?`、
  `sortBy?`、`sortOrder?`；公开查询只能由 Public 使用，`mine=true` 要求
  Merchant。`sortBy` 白名单为 `name`、`createdAt`，默认 `createdAt desc`。
- `GET /shops/{shopId}`：公开返回店铺详情。
- `PATCH /shops/{shopId}`：允许 `name`、`description`、`status`；进入 `OPEN`
  前检查完整经营地址。
- `PATCH /shops/{shopId}/address`：请求 `region`、`detail`、`phone`，仅店主。
- `POST /files/images`：`multipart/form-data` 字段 `file`，返回 `ImageAsset`。
  服务端检查真实内容、类型和大小，不接受 Base64 或本地路径。
分类接口的具体约定：

新增分类请求 `name`、`sortOrder?`；修改分类可提交 `name`、`sortOrder` 中至少
一项。查询结果按 `sortOrder asc` 排序。

| 方法 | 成功 | 常见失败 |
| --- | --- | --- |
| `POST /shops/{shopId}/categories` | `201` 返回 `Category` | `1001`、`1003`、`1004`、`1005` |
| `GET /shops/{shopId}/categories` | `200` 返回 `Category[]` | `1004` |
| `PATCH /categories/{categoryId}` | `200` 返回更新后的 `Category` | `1001`、`1003`、`1004`、`1005` |
| `DELETE /categories/{categoryId}` | `200` 返回删除结果 | `1003`、`1004`、`1005` |

分类名称在同一店铺内唯一；存在有效商品引用时不能物理删除。店铺接口的成功
响应分别为创建 `201`、查询 `200`、修改 `200`；资源不存在返回 `1004`，越权
返回 `1003`，商家暂停返回 `1202`，无地址营业返回 `1302`。

### 8.4 商品和 SKU

`POST /products` 请求：

```json
{
  "shopId": 11,
  "categoryId": 21,
  "name": "招牌牛肉饭",
  "description": "招牌套餐",
  "imageId": 301,
  "skus": [
    { "name": "默认规格", "price": 18.8, "stock": 20 }
  ]
}
```

`skus` 至少一个；新商品和 SKU 均为 `OFF_SALE`。创建是原子操作。
`PATCH /products/{productId}` 只修改 `categoryId`、`name`、`description`、
`imageId`、`status`；修改商品状态为 `ON_SALE` 时必须有合法图片和 SKU。

`POST /products/{productId}/skus` 请求 `name`、`price`、`stock`，创建为
`OFF_SALE`；`PATCH /skus/{skuId}` 请求至少一个 `name`、`price`、`stock`、
`status` 和必填 `version`。版本不匹配返回 `1404` 和当前 SKU：

```json
{
  "code": 1404,
  "msg": "SKU 版本已变化",
  "data": { "currentSku": { "id": 1001, "version": 4 } }
}
```

商品列表公开只返回商品和 SKU 均可售的内容；商家可通过 `includeOffSale=true`
查看本人商品的完整管理数据。价格、库存和版本均从 SKU 返回，不再从 Product
读取商品级销售价格或库存。

`GET /shops/{shopId}/products` 支持 `categoryId?`、`keyword?`、`page`、
`pageSize`、`sortBy?`、`sortOrder?`、`includeOffSale?`；`sortBy` 白名单为
`name`、`minPrice`、`createdAt`，默认 `createdAt desc`。`GET /products/{productId}`
遵守相同公开/店主可见性规则。

商品和 SKU 接口的成功响应分别为：新增商品 `201`、商品查询 `200`、商品修改
`200`、新增 SKU `201`、修改 SKU `200`。常见失败为 `1001`、`1003`、`1004`、
`1202`、`1405`、`1406`、`1404`；任何失败不得产生半个商品、SKU 或图片引用。

### 8.5 购物车

- `POST /cart-items` 请求 `{ "skuId": 1001, "quantity": 2 }`，成功返回
  `CartItem`；同一用户同一 SKU 合并数量。
- `GET /cart-items` 返回 `{ "items": [], "total": 0.0 }`，`total` 仅供展示。
- `PATCH /cart-items/{itemId}` 请求 `{ "quantity": 3 }`。
- `DELETE /cart-items/{itemId}` 返回删除结果。

加入和修改数量只校验当前库存，不扣库存。SKU 下架、商品下架、店铺不营业或
库存不足时分别返回 `1403`、`1401`、`1301`、`1402`。购物车项只属于创建它的用户。

购物车接口成功状态分别为新增或合并 `201/200`、查询 `200`、修改 `200`、删除
`200`；未登录返回 `1002`，参数错误返回 `1001`，资源不存在或越权返回 `1004`。

加入购物车失败示例：

```json
{
  "code": 1403,
  "msg": "SKU 下架或不可购买",
  "data": { "skuId": 1001, "status": "OFF_SALE" }
}
```

查询购物车的 `data` 固定为 `{ "items": [], "total": 0.0 }`；`total` 为当前
展示价格计算值，不能作为订单成交金额。

### 8.6 订单、支付、取消和退款

`POST /orders` 必须携带 `X-Idempotency-Key`。请求：

```json
{
  "items": [
    { "cartItemId": 31, "skuVersion": 3 }
  ],
  "addressId": 51,
  "remark": "少放辣椒"
}
```

请求不接受客户端价格、金额或订单状态。服务端重新读取购物车、SKU、店铺和地址，
校验单店铺、营业状态、SKU 状态、版本和库存，保存全部快照并在同一事务内扣减
SKU 库存、清理购物车项，成功返回 `201` 的 `PENDING_PAYMENT` 订单。

失败时：空购物车 `1501`、多店铺 `1502`、地址错误 `1503`、版本或价格变化
`1603`、库存不足 `1402`、店铺未营业 `1301`；订单、库存和购物车均不得部分提交。

`GET /orders` 支持 `status?`、`page`、`pageSize`、`sortBy?`、`sortOrder?`，仅返回
当前用户订单摘要；默认按 `createdAt desc`。`GET /orders/{orderId}` 返回本人完整
`Order`。订单摘要字段为 `id`、`orderNumber`、`shopId`、`shopName`、`total`、
`status`、`paymentStatus`、`refundStatus`、`createdAt`。

`POST /orders/{orderId}/pay` 请求体为空，需幂等键；仅允许订单所有者对
`PENDING_PAYMENT` 操作。成功将订单改为 `PAID`，支付状态为 `PAID`。

`POST /orders/{orderId}/cancel` 请求 `{ "reason": "临时有事" }`，需幂等键。
允许 `PENDING_PAYMENT`、`PAID`、`PREPARING`；前者不退款，后两者在同一事务
中恢复库存并完成全额模拟退款。配送中、已完成和已取消返回 `1601`。

`GET /orders/{orderId}/refund` 仅对已取消的已支付订单返回 `Refund`；待支付取消
没有退款记录，返回 `1004`。不提供手工重复退款接口。

`POST /orders/{orderId}/confirm-receipt` 请求体为空，需幂等键；仅允许订单所有者
将 `DELIVERING` 改为 `COMPLETED`。

### 8.7 商家订单履约

`GET /merchant/orders` 支持 `shopId?`、`status?`、分页和排序；只返回当前商家
店铺订单。`GET /merchant/orders/{orderId}` 返回完整订单，包括用户地址快照和备注，
但不能访问其他商家订单。

`POST /merchant/orders/{orderId}/prepare` 和 `/deliver` 请求体为空，均需幂等键。
前者只允许 `PAID -> PREPARING`，后者只允许 `PREPARING -> DELIVERING`。不能
跳过状态、越权操作或代替用户确认收货。

用户和商家订单列表的 `sortBy` 白名单均为 `createdAt`、`total`，默认
`createdAt desc`。

## 9. 完整联调示例

以下示例均省略重复的 `Authorization`，路径已包含 `/api/v1`。

### 9.0 注册和独立登录

用户注册请求：

```json
{
  "account": "alice01",
  "password": "ExamplePass123!",
  "passwordConfirm": "ExamplePass123!",
  "nickname": "Alice",
  "phone": "13800000000"
}
```

商家注册请求使用相同密码字段，并使用 `name` 替代 `nickname`。用户和商家登录
均只提交账号密码，但分别调用 `/users/login` 和 `/merchants/login`：

```json
{
  "account": "alice01",
  "password": "ExamplePass123!"
}
```

登录成功返回 6.1 节的 `AuthSession`。账号或密码错误示例：

```json
{
  "code": 1102,
  "msg": "账号或密码错误",
  "data": null
}
```

商家被暂停时返回 `1202`，普通用户被禁用时返回 `1103`，不得创建有效会话。

### 9.1 创建订单

请求：

```http
POST /api/v1/orders
X-Idempotency-Key: order-20260911-0001
Content-Type: application/json
```

```json
{
  "items": [{ "cartItemId": 31, "skuVersion": 3 }],
  "addressId": 51,
  "remark": "少放辣椒"
}
```

成功 `201`：

```json
{
  "code": 0,
  "msg": "订单创建成功",
  "data": {
    "id": 10001,
    "orderNumber": "ORD202609110001",
    "userId": 1,
    "shopId": 11,
    "shopName": "示例快餐店",
    "userAddressSnapshot": {
      "recipient": "张三",
      "phone": "13800000000",
      "region": "浙江省杭州市西湖区",
      "detail": "文三路 1 号 101 室"
    },
    "shopAddressSnapshot": {
      "region": "浙江省杭州市西湖区",
      "detail": "学院路 2 号",
      "phone": "05710000000"
    },
    "remark": "少放辣椒",
    "lines": [{
      "productId": 101,
      "skuId": 1001,
      "productName": "招牌牛肉饭",
      "skuName": "默认规格",
      "imageUrl": "/uploads/products/301.webp",
      "unitPrice": 18.8,
      "quantity": 2,
      "subtotal": 37.6
    }],
    "total": 37.6,
    "status": "PENDING_PAYMENT",
    "paymentStatus": "UNPAID",
    "refundStatus": "NOT_REFUNDED",
    "cancelReason": null,
    "cancelledAt": null,
    "completedAt": null,
    "createdAt": "2026-09-11T09:00:00Z",
    "updatedAt": "2026-09-11T09:00:00Z"
  }
}
```

失败 `409`：

```json
{
  "code": 1402,
  "msg": "SKU 库存不足",
  "data": {
    "items": [{ "skuId": 1001, "requested": 2, "available": 1 }]
  }
}
```

### 9.2 模拟支付

请求：

```http
POST /api/v1/orders/10001/pay
X-Idempotency-Key: pay-20260911-0001
Content-Type: application/json
```

```json
{}
```

成功 `200`：

```json
{
  "code": 0,
  "msg": "模拟支付成功",
  "data": {
    "orderId": 10001,
    "orderStatus": "PAID",
    "paymentStatus": "PAID",
    "payment": {
      "id": 7001,
      "orderId": 10001,
      "paymentNumber": "PAY202609110001",
      "amount": 37.6,
      "status": "PAID",
      "idempotencyKey": "pay-20260911-0001",
      "paidAt": "2026-09-11T09:02:00Z",
      "createdAt": "2026-09-11T09:02:00Z"
    }
  }
}
```

失败 `409`：

```json
{
  "code": 1601,
  "msg": "当前订单状态不允许支付",
  "data": { "currentStatus": "COMPLETED" }
}
```

### 9.3 商家推进和用户确认收货

`POST /api/v1/merchant/orders/10001/prepare` 成功返回订单状态 `PREPARING`；
`POST /api/v1/merchant/orders/10001/deliver` 成功返回 `DELIVERING`。二者请求体
均为 `{}`，分别使用独立幂等键。非法跳转示例：

```json
{
  "code": 1601,
  "msg": "当前订单状态不允许开始配送",
  "data": { "currentStatus": "PAID", "expectedStatus": "PREPARING" }
}
```

用户确认：

```http
POST /api/v1/orders/10001/confirm-receipt
X-Idempotency-Key: receipt-20260911-0001
Content-Type: application/json
```

成功响应中的 `data.orderStatus` 为 `COMPLETED`，`data.paymentStatus` 为
`PAID`，`data.refundStatus` 为 `NOT_REFUNDED`。

### 9.4 已支付订单取消和退款

请求：

```http
POST /api/v1/orders/10002/cancel
X-Idempotency-Key: cancel-20260911-0001
Content-Type: application/json
```

```json
{ "reason": "临时有事" }
```

成功 `200`：

```json
{
  "code": 0,
  "msg": "订单已取消并完成模拟退款",
  "data": {
    "orderId": 10002,
    "orderStatus": "CANCELLED",
    "paymentStatus": "PAID",
    "refundStatus": "REFUNDED",
    "refund": {
      "id": 7002,
      "orderId": 10002,
      "paymentId": 7003,
      "refundNumber": "REF202609110001",
      "amount": 37.6,
      "status": "REFUNDED",
      "idempotencyKey": "cancel-20260911-0001",
      "completedAt": "2026-09-11T09:10:00Z",
      "createdAt": "2026-09-11T09:10:00Z"
    }
  }
}
```

待支付订单取消成功时 `paymentStatus` 为 `UNPAID`、`refundStatus` 为
`NOT_REFUNDED`，且 `refund` 为 `null`。重复或不允许取消：

```json
{
  "code": 1601,
  "msg": "当前订单状态不允许取消",
  "data": { "currentStatus": "DELIVERING" }
}
```

### 9.5 图片、SKU 和地址

上传图片：

```http
POST /api/v1/files/images
Content-Type: multipart/form-data
```

成功 `201` 返回：

```json
{
  "code": 0,
  "msg": "图片上传成功",
  "data": {
    "id": 301,
    "url": "/uploads/products/301.webp",
    "contentType": "image/webp",
    "size": 182736,
    "createdAt": "2026-09-11T08:05:00Z"
  }
}
```

用户地址创建请求：

```json
{
  "recipient": "张三",
  "phone": "13800000000",
  "region": "浙江省杭州市西湖区",
  "detail": "文三路 1 号 101 室",
  "isDefault": true
}
```

SKU 更新请求必须包含版本：

```json
{
  "price": 19.8,
  "stock": 18,
  "status": "ON_SALE",
  "version": 3
}
```

## 10. 前端、测试和验收约束

- 前端以响应中的 `code`、状态和字段为准，不能用本地金额、库存、角色或状态
  替代后端判断。
- 收到 `401` 清理令牌并回到登录；`403` 显示无权限；`409` 按业务码提示刷新、
  重试或展示状态冲突。
- 创建订单网络超时必须使用原 `X-Idempotency-Key` 重试，不能自动生成新键。
- 核心接口测试必须断言：请求字段、HTTP 状态、业务码、状态迁移、快照内容和
  数据一致性，而不能只断言 HTTP 200。
- 必须覆盖：地址归属和默认地址唯一、图片校验、SKU 版本冲突、库存回滚、单店铺
  订单、备注边界、重复支付、三种可取消状态、全额退款、履约顺序和越权访问。
- API 文档示例中的字段名称、枚举和路径是联调基线；变更必须同步更新 SRS、
  前端契约测试、后端接口测试和验收用例。

## 11. 与旧阶段 1 契约的迁移说明

V1.2 不再使用以下旧字段或旧规则：

| 旧契约 | V1.2 契约 |
| --- | --- |
| `Product.price/stock/version` | `Sku.price/stock/version` |
| 购物车 `productId` | 购物车 `skuId` |
| 下单 `productVersion` | 下单 `skuVersion`，并新增 `addressId`、`remark` |
| 仅待支付可取消 | 待支付、已支付、制作中可取消 |
| 无支付/退款/履约接口 | 新增支付、退款查询、制作、配送、确认收货 |
| 无地址接口 | 新增用户地址和店铺经营地址接口 |
| 无商品图片接口 | 新增图片上传和商品主图引用 |

后端实现和前端联调必须以本文 V1.2 字段和状态为准，不得同时兼容两套互相冲突
的字段语义，除非另行建立版本化迁移方案。
