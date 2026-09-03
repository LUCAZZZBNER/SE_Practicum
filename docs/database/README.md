+# 轻量级外卖平台数据库设计

> 项目：SE_Practicum  
> 数据库负责人：B  
> 架构与测试审查：A  
> 前端字段审查：C  
> 当前版本：V1.0  
> 文档状态：待 A、C 评审  
> 最后更新：2026-09-03

---

## 1. 文档目的

本文档定义第一阶段数据库结构，是 Entity、MyBatis Mapper、Service、Flyway、接口和测试数据的共同依据。

实施顺序：B 完成设计 → A 检查约束、事务和测试 → C 检查页面字段 → 三人评审 → A 先提交失败测试 → B 编写 Flyway SQL。

正式表结构只能通过 Flyway 修改；禁止在 IDEA/Workbench 中手工修改正式表；禁止在本文档或 Git 中记录密码。

## 2. 固定技术约定

| 项目 | 固定值 |
| --- | --- |
| 数据库 | MySQL 8 |
| 开发库 | `delivery_dev` |
| 测试库 | `delivery_test` |
| 应用账号 | `delivery_app` |
| 字符集/排序 | `utf8mb4` / `utf8mb4_unicode_ci` |
| 迁移/持久化 | Flyway / MyBatis |
| 主键 | `BIGINT UNSIGNED AUTO_INCREMENT` |
| 金额 | `DECIMAL(10,2)`，Java 使用 `BigDecimal` |
| 时间 | `DATETIME(3)`，应用时区 `Asia/Shanghai` |
| 逻辑删除 | 长期业务表使用 `deleted` |
| 状态 | 大写英文字符串，与 Java 枚举一致 |

API、数据库和前端使用 `shop/product`；第一阶段 Java 内部仍可使用现有 `restaurant/item` 模块。

## 3. 数据模型总览

| 表 | 含义 |
| --- | --- |
| `users` | 普通用户与商家的统一登录账号 |
| `merchants` | 与账号一对一关联的商家资料 |
| `shops` | 商家的店铺 |
| `product_categories` | 店铺内商品分类 |
| `products` | 商品、价格、库存与状态 |
| `cart_items` | 用户购物车项 |
| `orders` | 订单主信息 |
| `order_items` | 下单时商品快照 |

~~~mermaid
erDiagram
    USERS ||--o| MERCHANTS : "扩展为商家"
    USERS ||--o{ CART_ITEMS : "拥有"
    USERS ||--o{ ORDERS : "创建"
    MERCHANTS ||--o{ SHOPS : "经营"
    SHOPS ||--o{ PRODUCT_CATEGORIES : "包含"
    SHOPS ||--o{ PRODUCTS : "销售"
    SHOPS ||--o{ ORDERS : "接收"
    PRODUCT_CATEGORIES ||--o{ PRODUCTS : "归类"
    PRODUCTS ||--o{ CART_ITEMS : "被选择"
    PRODUCTS ||--o{ ORDER_ITEMS : "形成快照"
    ORDERS ||--|{ ORDER_ITEMS : "包含"
~~~

关系规则：

- 一个账号最多一条商家资料；
- 商家角色由后端查询 `merchants` 判断；
- 商品的分类必须属于同一店铺；
- 同一用户同一商品最多一条购物车记录；
- 第一阶段一个订单只属于一个店铺；
- 订单明细保存成交快照。

## 4. 通用规则

- 表和字段使用小写蛇形命名，表名用复数；
- 主键叫 `id`，外键叫 `资源_id`；
- 长期表使用 `created_at`、`updated_at`；
- `deleted=0` 有效，`deleted=1` 已删除；
- 普通查询必须过滤 `deleted=0`；
- users、merchants、shops、product_categories、products、orders 使用逻辑删除；
- cart_items 是临时数据，允许物理删除；
- order_items 是历史快照，不提供单独删除；
- 用户账号即使逻辑删除，也不能被另一人复用。

状态：

| 资源 | 状态 |
| --- | --- |
| 用户 | `ACTIVE`、`DISABLED` |
| 商家 | `ACTIVE`、`SUSPENDED` |
| 店铺 | `OPEN`、`CLOSED`、`TEMPORARILY_CLOSED` |
| 分类 | `ACTIVE`、`DISABLED` |
| 商品 | `ON_SALE`、`OFF_SALE` |
| 订单 | `PENDING_PAYMENT`、`PAID`、`PREPARING`、`DELIVERING`、`COMPLETED`、`CANCELLED` |

## 5. 表结构

### 5.1 `users`

用途：保存全部登录主体。

| 字段 | 类型 | 可空 | 默认 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 用户 ID |
| `account` | `VARCHAR(50)` | 否 | 无 | 唯一 | 登录账号 |
| `password_hash` | `VARCHAR(100)` | 否 | 无 | 无 | BCrypt 摘要 |
| `nickname` | `VARCHAR(50)` | 是 | NULL | 无 | 昵称 |
| `phone` | `VARCHAR(20)` | 是 | NULL | 无 | 联系电话 |
| `status` | `VARCHAR(20)` | 否 | ACTIVE | 状态检查 | 账号状态 |
| `created_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 创建时间 |
| `updated_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 更新时间 |
| `deleted` | `TINYINT(1)` | 否 | 0 | 索引 | 逻辑删除 |

索引：`uk_users_account(account)`、`idx_users_status_deleted(status, deleted)`。

规则：账号统一转小写后保存；密码只存 BCrypt 摘要；响应 DTO 不得含密码；DISABLED 或已删除账号不能登录。

### 5.2 `merchants`

| 字段 | 类型 | 可空 | 默认 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 商家 ID |
| `user_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键、唯一 | 用户账号 |
| `merchant_name` | `VARCHAR(100)` | 否 | 无 | 无 | 商家名称 |
| `contact_phone` | `VARCHAR(20)` | 否 | 无 | 无 | 联系方式 |
| `status` | `VARCHAR(20)` | 否 | ACTIVE | 状态检查 | 商家状态 |
| `created_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 创建时间 |
| `updated_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 更新时间 |
| `deleted` | `TINYINT(1)` | 否 | 0 | 索引 | 逻辑删除 |

约束：`user_id → users.id`；`uk_merchants_user(user_id)`；`idx_merchants_status_deleted(status,deleted)`。

规则：注册商家时 users 和 merchants 在同一事务创建；失败整体回滚；SUSPENDED 商家不能管理资源。

### 5.3 `shops`

| 字段 | 类型 | 可空 | 默认 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 店铺 ID |
| `merchant_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 所属商家 |
| `name` | `VARCHAR(100)` | 否 | 无 | 索引 | 店铺名称 |
| `description` | `VARCHAR(500)` | 是 | NULL | 无 | 简介 |
| `status` | `VARCHAR(30)` | 否 | CLOSED | 状态检查 | 营业状态 |
| `created_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 创建时间 |
| `updated_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 更新时间 |
| `deleted` | `TINYINT(1)` | 否 | 0 | 索引 | 逻辑删除 |

约束：`merchant_id → merchants.id`；索引 `(merchant_id,deleted)`、`(status,deleted)`、`name`。

规则：新店固定 CLOSED；只有 OPEN 可接单；商家只能修改自己的店；状态变化不能删除历史数据。

### 5.4 `product_categories`

| 字段 | 类型 | 可空 | 默认 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 分类 ID |
| `shop_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 所属店铺 |
| `name` | `VARCHAR(100)` | 否 | 无 | 业务唯一 | 分类名 |
| `sort_order` | `INT` | 否 | 0 | 无 | 显示顺序 |
| `status` | `VARCHAR(20)` | 否 | ACTIVE | 状态检查 | 分类状态 |
| `created_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 创建时间 |
| `updated_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 更新时间 |
| `deleted` | `TINYINT(1)` | 否 | 0 | 索引 | 逻辑删除 |

约束：`shop_id → shops.id`；索引 `(shop_id,status,deleted)`、`(shop_id,sort_order)`。

规则：同店有效分类名由 Service 保证不重复；有商品引用时不能物理删除；按 sort_order、id 排序。

### 5.5 `products`

| 字段 | 类型 | 可空 | 默认 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 商品 ID |
| `shop_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 店铺 |
| `category_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 分类 |
| `name` | `VARCHAR(100)` | 否 | 无 | 索引 | 商品名 |
| `description` | `VARCHAR(1000)` | 是 | NULL | 无 | 描述 |
| `price` | `DECIMAL(10,2)` | 否 | 无 | CHECK > 0 | 价格 |
| `stock` | `INT UNSIGNED` | 否 | 0 | CHECK >= 0 | 库存 |
| `status` | `VARCHAR(20)` | 否 | OFF_SALE | 状态检查 | 上下架 |
| `version` | `INT UNSIGNED` | 否 | 0 | 无 | 并发版本 |
| `created_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 创建时间 |
| `updated_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 更新时间 |
| `deleted` | `TINYINT(1)` | 否 | 0 | 索引 | 逻辑删除 |

外键：shop_id → shops.id；category_id → product_categories.id。索引：`(shop_id,status,deleted)`、`(category_id,status,deleted)`、`name`。

规则：新商品 OFF_SALE；分类必须属于相同店铺；商家只能管理自己的商品；下架或零库存不能购买；改价不改变订单快照。

原子扣库存必须带条件：

~~~sql
UPDATE products
SET stock = stock - #{quantity},
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE id = #{productId}
  AND stock >= #{quantity}
  AND status = 'ON_SALE'
  AND deleted = 0;
~~~

受影响行数必须为 1，否则作为库存或状态冲突。

### 5.6 `cart_items`

| 字段 | 类型 | 可空 | 默认 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 购物车项 ID |
| `user_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 用户 |
| `product_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 商品 |
| `quantity` | `INT UNSIGNED` | 否 | 无 | CHECK > 0 | 数量 |
| `created_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 创建时间 |
| `updated_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 更新时间 |

约束：user_id → users.id；product_id → products.id；`UNIQUE(user_id,product_id)`；索引 `user_id`。

规则：只能操作本人购物车；重复加入时累加数量；累加后不能超过库存；删除购物车不修改库存。

### 5.7 `orders`

| 字段 | 类型 | 可空 | 默认 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 订单 ID |
| `order_number` | `VARCHAR(40)` | 否 | 无 | 唯一 | 订单编号 |
| `user_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 用户 |
| `shop_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 店铺 |
| `idempotency_key` | `VARCHAR(64)` | 否 | 无 | 与用户组合唯一 | 防重复键 |
| `total_amount` | `DECIMAL(10,2)` | 否 | 无 | CHECK >= 0 | 总金额 |
| `status` | `VARCHAR(30)` | 否 | PENDING_PAYMENT | 状态检查 | 状态 |
| `version` | `INT UNSIGNED` | 否 | 0 | 无 | 并发版本 |
| `created_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 创建时间 |
| `updated_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 更新时间 |
| `cancelled_at` | `DATETIME(3)` | 是 | NULL | 无 | 取消时间 |
| `deleted` | `TINYINT(1)` | 否 | 0 | 索引 | 逻辑删除 |

约束：order_number 唯一；`UNIQUE(user_id,idempotency_key)`；user_id → users.id；shop_id → shops.id。索引 `(user_id,created_at)`、`(user_id,status,created_at)`、`(shop_id,created_at)`。

规则：金额由服务端计算；初始 PENDING_PAYMENT；只有 PENDING_PAYMENT 可取消；幂等键避免重复订单；状态更新必须带原状态或 version 条件。

### 5.8 `order_items`

| 字段 | 类型 | 可空 | 默认 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 明细 ID |
| `order_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 订单 |
| `product_id` | `BIGINT UNSIGNED` | 否 | 无 | 索引 | 原商品 ID |
| `product_name_snapshot` | `VARCHAR(100)` | 否 | 无 | 无 | 商品名快照 |
| `unit_price_snapshot` | `DECIMAL(10,2)` | 否 | 无 | CHECK > 0 | 成交单价 |
| `quantity` | `INT UNSIGNED` | 否 | 无 | CHECK > 0 | 数量 |
| `subtotal` | `DECIMAL(10,2)` | 否 | 无 | CHECK > 0 | 小计 |
| `created_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 创建时间 |

约束：order_id → orders.id；索引 `order_id`、`product_id`。

规则：快照在下单时写入；subtotal 由服务端计算；商品改名、改价、下架或删除均不能改变历史明细；不提供单独修改/删除接口。

## 6. 外键删除策略

优先使用 `ON DELETE RESTRICT` 或默认限制，禁止可能删除历史记录的 `ON DELETE CASCADE`。

| 父表 | 子表 | 处理 |
| --- | --- | --- |
| users | merchants/orders | 使用逻辑删除，不删历史 |
| users | cart_items | Service 可清理临时数据 |
| merchants | shops | 使用逻辑删除 |
| shops | categories/products/orders | 不物理级联 |
| categories | products | 有商品时禁止物理删除 |
| products | order_items | 绝不删除历史快照 |
| orders | order_items | 正常业务不物理删除 |

## 7. 订单事务

创建订单必须在一个 Service 事务内：

~~~text
验证用户 → 读取购物车 → 检查非空和单店铺
→ 检查店铺 OPEN → 检查商品 ON_SALE 和库存
→ 读取服务端价格 → 计算金额
→ 写 orders → 写 order_items
→ 原子扣库存 → 清理购物车 → 提交
~~~

任何一步失败都必须回滚订单、明细、库存和购物车。

取消规则：仅 `PENDING_PAYMENT → CANCELLED`。因创建订单已扣库存，取消成功必须在同一事务中条件更新订单、恢复库存、填写 cancelled_at；并发取消只能一次成功，库存只能恢复一次。

## 8. 校验职责

| 规则 | C 前端 | B Service | 数据库 |
| --- | --- | --- | --- |
| 必填 | 提示 | 必须校验 | NOT NULL |
| 账号唯一 | 可提示 | 检查并处理冲突 | UNIQUE |
| 密码 | 基础格式 | BCrypt | 只存摘要 |
| 价格/库存 | 输入限制 | 必须校验 | CHECK |
| 资源归属 | 隐藏按钮 | 必须校验 | 外键不能代替权限 |
| 店铺/商品状态 | 展示 | 操作时重新校验 | 状态约束 |
| 订单金额 | 展示 | 服务端计算 | DECIMAL |
| 防重复订单 | 禁用按钮 | 幂等判断 | 组合唯一键 |

## 9. 查询与索引

| 查询 | 索引 |
| --- | --- |
| 登录查账号 | users.account |
| 商家查店铺 | shops(merchant_id,deleted) |
| 用户浏览店铺 | shops(status,deleted) |
| 店铺商品 | products(shop_id,status,deleted) |
| 分类商品 | products(category_id,status,deleted) |
| 用户购物车 | cart_items(user_id) |
| 用户订单 | orders(user_id,status,created_at) |
| 订单明细 | order_items(order_id) |

sortBy 不能直接拼 SQL。白名单：

- 店铺：createdAt、name；
- 商品：createdAt、price、name；
- 订单：createdAt、totalAmount；
- sortOrder：asc、desc。

未知排序字段第一阶段返回 400。

## 10. Flyway 约定

目录：

~~~text
backend/src/main/resources/db/migration/
~~~

第一阶段文件：

| 文件 | 内容 |
| --- | --- |
| `V1__create_core_tables.sql` | 创建 8 张表、外键、约束和索引 |
| `V2__add_query_indexes.sql` | 仅在评审发现缺失索引时新增 |

规则：

1. 迁移编号递增；
2. 合并 develop 且执行过的旧迁移禁止修改；
3. 修复只能新增迁移；
4. 演示数据不进入正式迁移；
5. 测试数据放 `backend/src/test/resources/db/test-data/`；
6. 不删除或手工修改 `flyway_schema_history`；
7. 先在 delivery_test 验证，再在 delivery_dev 验证。

## 11. MyBatis 约定

~~~text
backend/src/main/java/com/delivery/backend/<module>/mapper/
backend/src/main/resources/mapper/<module>/
~~~

- Mapper 只做数据访问；
- 参数使用 `#{...}`；
- 禁止用 `${...}` 拼接用户输入；
- 排序使用 Java 白名单；
- Entity、请求 DTO、响应 DTO 分开；
- Controller 不直接调用 Mapper；
- 更新必须检查受影响行数；
- 查询必须过滤 deleted=0。

## 12. 测试清单

### 结构与约束

- [ ] 8 张表由 Flyway 创建；
- [ ] 账号、merchant user_id、购物车组合键、订单号、幂等键唯一；
- [ ] 价格、库存、数量、金额约束有效；
- [ ] 外键阻止无效关联；
- [ ] 逻辑删除数据不出现在普通查询。

### 事务和并发

- [ ] 下单成功写订单、明细、扣库存、清购物车；
- [ ] 任一失败全部回滚；
- [ ] 并发库存不为负；
- [ ] 相同幂等键不重复下单；
- [ ] 并发取消只恢复一次库存。

### 历史一致性

- [ ] 商品改名/改价不改变订单快照；
- [ ] 商品下架不删除订单明细；
- [ ] 店铺关闭不删除订单；
- [ ] 取消不删除订单和明细。

## 13. A、B、C 评审

### B 自检

- [ ] 8 张表字段完整；
- [ ] 外键、状态、金额、快照、幂等和并发方案明确；
- [ ] 没有密码和密钥；
- [ ] 删除不会破坏历史数据。

### A 评审

- [ ] 核心规则可测试；
- [ ] 事务范围完整；
- [ ] 原子扣库存；
- [ ] 并发取消不重复恢复；
- [ ] 测试库与开发库隔离；
- [ ] 错误可映射到正确 HTTP 状态码。

### C 评审

- [ ] 用户、店铺、分类、商品字段满足页面；
- [ ] 购物车可显示数量、价格和小计；
- [ ] 订单可显示编号、状态、总额、时间和明细快照；
- [ ] 分页 DTO 能提供 items、page、pageSize、total、totalPages。

### 评审记录

| 项目 | 内容 |
| --- | --- |
| 评审日期 | 待填写 |
| B 自检 | 待填写 |
| A 评审 | 待填写 |
| C 评审 | 待填写 |
| 问题 | 待填写 |
| 修改结果 | 待填写 |
| 最终结论 | 待填写 |
| 关联 PR | 待填写 |

最终结论写“通过”前，B 不编写正式 V1 SQL。

## 14. 提交顺序

第一次提交：

~~~powershell
git status
git diff -- docs/database/README.md
git add docs/database/README.md
git commit -m "docs(database): design core data model"
git push -u origin feature/b-database
~~~

A、C 评审修改后第二次提交：

~~~powershell
git add docs/database/README.md
git commit -m "docs(database): apply schema review feedback"
git push
~~~

然后由 A 先提交失败测试，B 再提交 Flyway 实现，A 最后提交 Green 和回归日志。

## 15. 当前进度

- [x] MySQL 8、开发库、测试库和 delivery_app 已准备；
- [x] Windows 环境变量已设置；
- [x] IDEA 两个数据库连接已建立；
- [x] 数据库 V1.0 初稿已完成；
- [ ] B 自检；
- [ ] A、C 评审；
- [ ] 三人确认通过；
- [ ] A 提交失败测试；
- [ ] B 配置 MyBatis/Flyway 并编写 V1 迁移；
- [ ] 测试库和开发库迁移成功；
- [ ] A 保存 Green 和回归记录。
