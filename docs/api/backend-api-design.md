# 轻量级外卖服务平台后端 API 设计

## 1. 契约范围

本文定义阶段 1 后端提供给前端的 HTTP/JSON 契约。基础路径为 `/api/v1`，字符集为 UTF-8。内部 `restaurant`、`item` 模块在外部接口中分别使用 `shops`、`products` 资源名。除文中明确标记为公开的接口外，均要求登录。

业务范围见[功能分析](../feature-analysis.md)，接口背后的模块职责见[后端架构设计](../architecture/backend-architecture-design.md)。

通用数据约定：ID 为正整数；金额为保留两位小数的十进制数，后端使用 `BigDecimal` 计算；时间为 ISO 8601 UTC 字符串，例如 `2026-09-03T08:30:00Z`；PATCH 请求至少包含一个允许修改的字段。未知字段和非法枚举值返回参数错误。

## 2. 认证与权限

登录成功后，前端在受保护请求中携带：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

令牌标识用户 ID 和角色，不包含密码等敏感信息。商家是有效用户关联的经营者身份；需要商家权限的接口还会校验商家状态和资源归属。

| 权限标记 | 含义 |
| --- | --- |
| Public | 无需令牌，可浏览公开资源 |
| User | 需要有效用户令牌，只能操作本人资源 |
| Merchant | 需要有效商家身份，只能管理本人店铺及其资源 |

## 3. 统一响应

所有有响应体的成功请求使用：

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {}
}
```

失败响应使用相同外壳；`data` 可包含字段错误或冲突上下文，不返回异常堆栈：

```json
{
  "code": 1001,
  "msg": "请求参数不合法",
  "data": {
    "fieldErrors": { "quantity": "必须大于 0" }
  }
}
```

分页响应的 `data` 固定为：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 10,
  "total": 0,
  "totalPages": 0
}
```

`page` 从 1 开始，`pageSize` 默认 10、最大 100。默认排序由各列表接口说明；`sortOrder` 仅接受 `asc`、`desc`。

## 4. HTTP 状态与业务错误码

| HTTP | 业务码 | 名称 | 场景 |
| --- | ---: | --- | --- |
| 400 | 1001 | `VALIDATION_ERROR` | 字段缺失、格式错误、空购物车等非法请求 |
| 401 | 1002 | `UNAUTHENTICATED` | 未携带、过期或无效令牌 |
| 403 | 1003 | `FORBIDDEN` | 已登录但角色或资源归属不符 |
| 404 | 1004 | `RESOURCE_NOT_FOUND` | 用户可见范围内的资源不存在 |
| 409 | 1005 | `RESOURCE_CONFLICT` | 唯一约束或并发修改冲突 |
| 409 | 1101 | `ACCOUNT_EXISTS` | 用户账号已存在 |
| 401 | 1102 | `BAD_CREDENTIALS` | 账号或密码错误 |
| 403 | 1103 | `ACCOUNT_DISABLED` | 用户账号被禁用 |
| 409 | 1201 | `MERCHANT_EXISTS` | 当前用户已注册商家 |
| 403 | 1202 | `MERCHANT_SUSPENDED` | 商家已暂停经营权限 |
| 409 | 1301 | `SHOP_NOT_OPEN` | 店铺未营业，不能购物或下单 |
| 409 | 1401 | `PRODUCT_OFF_SALE` | 商品已下架 |
| 409 | 1402 | `INSUFFICIENT_STOCK` | 库存不足或并发扣减失败 |
| 400 | 1501 | `CART_EMPTY` | 未选择有效购物车项 |
| 400 | 1502 | `MIXED_SHOPS` | 一次结算包含多个店铺 |
| 409 | 1601 | `PRICE_CHANGED` | 商品版本或价格已变化，需用户确认 |
| 409 | 1602 | `ORDER_STATE_CONFLICT` | 当前订单状态不允许操作 |
| 409 | 1603 | `IDEMPOTENCY_CONFLICT` | 同一幂等键对应不同请求内容 |
| 500 | 9000 | `INTERNAL_ERROR` | 未预期的服务端错误 |

对无权查看的他人订单或购物车项可返回 404，避免泄漏资源是否存在。

## 5. 资源模型

枚举值：用户状态为 `ACTIVE`、`DISABLED`；商家状态为 `ACTIVE`、`SUSPENDED`；店铺状态为 `OPEN`、`CLOSED`、`TEMPORARILY_CLOSED`；商品状态为 `ON_SALE`、`OFF_SALE`；阶段 1 订单状态为 `PENDING_PAYMENT`、`CANCELLED`，并保留 `PAID`、`PREPARING`、`DELIVERING`、`COMPLETED`。

| 模型 | 响应字段 |
| --- | --- |
| `User` | `id`, `account`, `nickname`, `phone`, `status`, `createdAt`, `updatedAt` |
| `Merchant` | `id`, `userId`, `name`, `phone`, `status`, `createdAt` |
| `Shop` | `id`, `merchantId`, `name`, `description`, `status`, `createdAt`, `updatedAt` |
| `Category` | `id`, `shopId`, `name`, `sortOrder`, `createdAt`, `updatedAt` |
| `Product` | `id`, `shopId`, `categoryId`, `name`, `description`, `price`, `stock`, `status`, `version`, `createdAt`, `updatedAt` |
| `CartItem` | `id`, `product`, `quantity`, `subtotal`, `available`, `unavailableReason`, `createdAt`, `updatedAt` |
| `OrderLine` | `productId`, `productName`, `unitPrice`, `quantity`, `subtotal` |
| `Order` | `id`, `orderNumber`, `userId`, `shopId`, `shopName`, `lines`, `total`, `status`, `createdAt`, `updatedAt`, `cancelledAt` |

`CartItem.product` 是最新 `Product` 摘要；`OrderLine` 是下单时的不可变快照。

## 6. 用户接口

### `POST /users` — 注册用户（Public）

请求：

```json
{
  "account": "alice01",
  "password": "ExamplePass123!",
  "passwordConfirm": "ExamplePass123!",
  "nickname": "Alice",
  "phone": "13800000000"
}
```

成功返回 `201 Created` 和 `User`。`account`、`password`、`passwordConfirm` 必填；密码不得出现在响应中。主要错误：1001、1101。

### `POST /users/login` — 登录（Public）

请求字段为 `account`、`password`。成功返回 `200 OK`：

```json
{
  "code": 0,
  "msg": "登录成功",
  "data": {
    "accessToken": "<token>",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "user": { "id": 1, "account": "alice01", "nickname": "Alice", "phone": "13800000000", "status": "ACTIVE" },
    "roles": ["USER"]
  }
}
```

主要错误：1001、1102、1103。

### 当前用户

| 方法与路径 | 权限 | 请求 | 成功响应 | 主要错误 |
| --- | --- | --- | --- | --- |
| `GET /users/me` | User | 无 | `200`, `User` | 1002, 1103 |
| `PATCH /users/me` | User | 可选 `nickname`, `phone` | `200`, 更新后的 `User` | 1001, 1002, 1103 |

## 7. 商家接口

| 方法与路径 | 权限 | 请求 | 成功响应 | 主要错误 |
| --- | --- | --- | --- | --- |
| `POST /merchants` | User | `name`、`phone` | `201`, `Merchant` | 1001, 1103, 1201 |
| `GET /merchants/me` | User | 无 | `200`, 当前用户的 `Merchant` | 1002, 1004 |

一个用户最多创建一个商家档案。注册成功后重新登录或刷新令牌，`roles` 中应包含 `MERCHANT`。

## 8. 店铺接口

| 方法与路径 | 权限 | 请求或查询参数 | 成功响应 | 主要错误 |
| --- | --- | --- | --- | --- |
| `POST /shops` | Merchant | Body: `name`, `description?` | `201`, 初始状态为 `CLOSED` 的 `Shop` | 1001, 1202 |
| `GET /shops` | Public；`mine=true` 时 Merchant | `page`, `pageSize`, `keyword?`, `status?`, `mine?`, `sortBy?`, `sortOrder?` | `200`, `Shop` 分页 | 1001 |
| `GET /shops/{shopId}` | Public | Path: `shopId` | `200`, `Shop` | 1004 |
| `PATCH /shops/{shopId}` | Merchant | Body 可选 `name`, `description`, `status` | `200`, 更新后的 `Shop` | 1001, 1003, 1004, 1202 |

店铺列表默认按 `createdAt desc`；`sortBy` 白名单为 `name`、`createdAt`。`mine=true` 返回当前商家的全部店铺，供管理页面使用；其他请求返回公开店铺列表。商家只能修改自己拥有的店铺。

## 9. 分类与商品接口

### 商品分类

| 方法与路径 | 权限 | 请求或查询参数 | 成功响应 | 主要错误 |
| --- | --- | --- | --- | --- |
| `POST /shops/{shopId}/categories` | Merchant | Body: `name`, `sortOrder?` | `201`, `Category` | 1001, 1003, 1004, 1005 |
| `GET /shops/{shopId}/categories` | Public | 无 | `200`, `Category[]`，按 `sortOrder asc` | 1004 |
| `PATCH /categories/{categoryId}` | Merchant | Body 可选 `name`, `sortOrder` | `200`, `Category` | 1001, 1003, 1004, 1005 |
| `DELETE /categories/{categoryId}` | Merchant | 无 | `200`, `{ "id": 21, "deleted": true }` | 1003, 1004, 1005 |

删除为逻辑删除；分类仍被有效商品引用时返回 409。

### 商品

| 方法与路径 | 权限 | 请求或查询参数 | 成功响应 | 主要错误 |
| --- | --- | --- | --- | --- |
| `POST /products` | Merchant | Body: `shopId`, `categoryId`, `name`, `description?`, `price`, `stock` | `201`, 初始为 `OFF_SALE` 的 `Product` | 1001, 1003, 1004, 1202 |
| `GET /shops/{shopId}/products` | Public | `categoryId?`, `keyword?`, `page`, `pageSize`, `sortBy?`, `sortOrder?`, `includeOffSale?` | `200`, `Product` 分页 | 1001, 1004 |
| `GET /products/{productId}` | Public；查看下架商品时 Merchant | Path: `productId` | `200`, `Product` | 1003, 1004 |
| `PATCH /products/{productId}` | Merchant | Body 可选 `categoryId`, `name`, `description`, `price`, `stock`, `status`, `version` | `200`, `Product` | 1001, 1003, 1004, 1005, 1202 |

商品列表默认按 `createdAt desc`；`sortBy` 白名单为 `name`、`price`、`createdAt`。Public 请求仅返回 `ON_SALE` 商品；`includeOffSale=true` 仅对店主有效。价格必须大于 0，库存不得小于 0。PATCH 中的 `version` 用于乐观锁，版本过期返回 409。

## 10. 购物车接口

| 方法与路径 | 权限 | 请求 | 成功响应 | 主要错误 |
| --- | --- | --- | --- | --- |
| `POST /cart-items` | User | Body: `productId`, `quantity` | `201`（新建）或 `200`（合并），`CartItem` | 1001, 1004, 1301, 1401, 1402 |
| `GET /cart-items` | User | 无 | `200`, `CartItem[]` 和 `total` | 1002, 1103 |
| `PATCH /cart-items/{cartItemId}` | User | Body: `quantity` | `200`, `CartItem` | 1001, 1004, 1301, 1401, 1402 |
| `DELETE /cart-items/{cartItemId}` | User | 无 | `200`, `{ "id": 31, "deleted": true }` | 1004 |

加入同一商品时增加已有数量，而不是新建重复项。`GET /cart-items` 的 `data` 格式为：

```json
{
  "items": [],
  "total": 0.00
}
```

`total` 仅供展示，实际订单金额在下单时重新计算。

## 11. 订单接口

### `POST /orders` — 创建订单（User）

请求头必须包含唯一的 `X-Idempotency-Key`（建议 UUID）。请求只引用购物车项及前端已确认的商品版本，不接收数量、价格或总金额：

```json
{
  "items": [
    { "cartItemId": 31, "productVersion": 3 },
    { "cartItemId": 32, "productVersion": 7 }
  ]
}
```

成功返回 `201 Created` 和 `Order`。同一用户使用相同幂等键及相同请求重试时返回首次结果；相同键对应不同请求返回 1603。商品版本变化返回 1601，`data.currentItems` 给出最新购物车项供前端确认。库存不足、混合店铺或店铺闭店时整单失败，库存和购物车均保持不变。

### 查询与取消

| 方法与路径 | 权限 | 请求或查询参数 | 成功响应 | 主要错误 |
| --- | --- | --- | --- | --- |
| `GET /orders` | User | `status?`, `page`, `pageSize`, `sortBy?`, `sortOrder?` | `200`, `Order` 摘要分页 | 1001, 1002 |
| `GET /orders/{orderId}` | User | Path: `orderId` | `200`, 完整 `Order` | 1004 |
| `POST /orders/{orderId}/cancel` | User | 无 | `200`, 更新后的 `Order` | 1004, 1602 |

订单列表默认按 `createdAt desc`，`sortBy` 白名单为 `createdAt`、`total`。阶段 1 只允许所有者取消 `PENDING_PAYMENT` 订单；取消成功后库存恢复一次，订单和明细继续保留。

## 12. 前端调用约束

- 前端不得以本地角色、店铺状态、商品状态、库存或金额替代后端校验。
- 收到 401 时清除失效令牌并进入登录流程；403 显示无权限；409 根据业务码提示刷新或重试。
- 创建订单遇到网络超时时，应使用原幂等键重试，不能生成新键后自动重复提交。
- 前端只展示 `msg` 或自身文案，不拼接服务端内部异常信息。
