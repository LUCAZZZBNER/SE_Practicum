# 轻量级外卖平台数据库设计（阶段 1 最小版）

> 负责人：B
>
> 评审：A（架构、事务、测试）、C（页面字段）
>
> 版本：V2.0-minimal
>
> 更新日期：2026-09-04
>
> 当前状态：表结构设计已收敛；尚未运行 Flyway、尚未在正式库创建表

## 1. 文档目的和效力

本文档只定义课程阶段 1 必须使用的数据库结构，是 B 编写 Flyway V1、Entity、Mapper 和 ServiceImpl 的依据。

执行范围以根目录 `最小必要范围-ABC前期准备与TDD执行文档.md` 为准。旧版数据库设计中的幂等、取消订单、支付、配送、乐观锁和通用逻辑删除不进入阶段 1。

当前允许 B 完成：

- 确认本设计；
- 创建空的 `delivery_dev`、`delivery_test`；
- 编写 `V1__create_core_tables.sql`；
- 检查 SQL。

当前不允许：

- 在 `delivery_dev` 或 `delivery_test` 手工执行 `CREATE TABLE`；
- 用 IDEA 或 Workbench 手工建立正式表；
- 在 A 的 Red 测试之前实现业务 Mapper 或 ServiceImpl。

按照 2026-09-05 的新分工，B 接管 JDK 17、Maven Wrapper、MyBatis/Flyway 和测试 Profile；完成这些运行基础后，由 Flyway 自动创建正式表。

## 2. 数据库和命名约定

| 项目 | 固定值 |
| --- | --- |
| 数据库 | MySQL |
| 开发库 | `delivery_dev` |
| 测试库 | `delivery_test` |
| 应用账号 | `delivery_app@localhost` |
| 字符集 | `utf8mb4` |
| 排序规则 | `utf8mb4_unicode_ci` |
| 迁移工具 | Flyway |
| 持久化 | MyBatis |
| 主键 | `BIGINT UNSIGNED AUTO_INCREMENT` |
| 金额 | `DECIMAL(10,2)`；Java 使用 `BigDecimal` |
| 时间 | `DATETIME(3)`；应用时区 `Asia/Shanghai` |
| 表名 | 小写复数、snake_case |
| 字段名 | 小写 snake_case |
| 状态值 | 大写英文字符串，与 Java 枚举一致 |

数据库账号和密码只从本机环境变量读取：

```text
DELIVERY_DB_USERNAME
DELIVERY_DB_PASSWORD
```

不得在 Git 中保存真实密码。

## 3. 8 张表和关系

阶段 1 只有以下 8 张业务表：

| 表 | 用途 | 所属模块 |
| --- | --- | --- |
| `users` | 普通用户和商家的统一登录账号 | user |
| `merchants` | 商家身份资料 | merchant |
| `shops` | 商家的唯一店铺 | restaurant |
| `product_categories` | 店铺商品分类 | item |
| `products` | 商品、价格、库存和上下架状态 | item |
| `cart_items` | 用户购物车临时数据 | shopping |
| `orders` | 订单主信息 | order |
| `order_items` | 下单时的商品快照 | order |

关系：

```text
users 1 ── 0..1 merchants
merchants 1 ── 0..1 shops
shops 1 ── N product_categories
shops 1 ── N products
product_categories 1 ── N products
users 1 ── N cart_items
products 1 ── N cart_items
users 1 ── N orders
shops 1 ── N orders
orders 1 ── N order_items
products 1 ── N order_items
```

阶段 1 的最小限制：

- 一个账号最多有一个商家身份；
- 一个商家最多有一个店铺；
- 同一店铺的分类名不能重复；
- 同一用户和同一商品最多有一条购物车项；
- 一个订单只属于一个店铺；
- 订单明细保存成交时的商品名称和单价。

## 4. 表结构

### 4.1 `users`

用途：保存所有登录账号。普通用户注册只写本表；商家注册在同一事务中写 `users` 和 `merchants`。

| 字段 | 类型 | 可空 | 默认值 | 约束 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 用户 ID |
| `account` | `VARCHAR(50)` | 否 | 无 | 唯一 | 登录账号 |
| `password_hash` | `VARCHAR(100)` | 否 | 无 | 无 | BCrypt 密码摘要 |
| `nickname` | `VARCHAR(50)` | 否 | 无 | 无 | 昵称 |
| `phone` | `VARCHAR(20)` | 是 | NULL | 无 | 联系电话 |
| `status` | `VARCHAR(20)` | 否 | `ACTIVE` | CHECK | 用户状态 |
| `created_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 创建时间 |
| `updated_at` | `DATETIME(3)` | 否 | 当前时间 | 自动更新时间 | 更新时间 |

约束：

- `PRIMARY KEY (id)`；
- `UNIQUE (account)`；
- `status IN ('ACTIVE', 'DISABLED')`。

业务规则：

- account 去除首尾空格后不能为空；
- account 全局唯一；
- 密码只保存 BCrypt 摘要；
- 响应不能返回 `password_hash`；
- 只有 `ACTIVE` 用户可以登录和执行业务操作。

### 4.2 `merchants`

用途：标识某个用户拥有商家身份。

| 字段 | 类型 | 可空 | 默认值 | 约束 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 商家 ID |
| `user_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键、唯一 | 对应用户 |
| `name` | `VARCHAR(100)` | 否 | 无 | 无 | 商家名称 |
| `created_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 创建时间 |

约束：

- `PRIMARY KEY (id)`；
- `UNIQUE (user_id)`；
- `user_id REFERENCES users(id)`；
- 删除策略为 `RESTRICT`。

业务规则：

- 一个用户最多注册一次商家；
- 商家注册时 users 和 merchants 必须在同一事务中创建；
- 任一步失败都回滚。

### 4.3 `shops`

用途：保存商家的唯一店铺及营业状态。

| 字段 | 类型 | 可空 | 默认值 | 约束 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 店铺 ID |
| `merchant_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键、唯一 | 所属商家 |
| `name` | `VARCHAR(100)` | 否 | 无 | 无 | 店铺名称 |
| `description` | `VARCHAR(500)` | 是 | NULL | 无 | 店铺简介 |
| `status` | `VARCHAR(30)` | 否 | `CLOSED` | CHECK | 营业状态 |
| `created_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 创建时间 |
| `updated_at` | `DATETIME(3)` | 否 | 当前时间 | 自动更新时间 | 更新时间 |

约束：

- `PRIMARY KEY (id)`；
- `UNIQUE (merchant_id)`；
- `merchant_id REFERENCES merchants(id)`；
- `status IN ('OPEN', 'CLOSED', 'TEMPORARILY_CLOSED')`；
- 删除策略为 `RESTRICT`。

业务规则：

- 一个商家阶段 1 最多一个店铺；
- 新店铺初始状态为 `CLOSED`；
- 只有店主可以修改店铺状态；
- 只有 `OPEN` 店铺允许加入购物车和创建订单。

### 4.4 `product_categories`

用途：保存店铺内的商品分类。

| 字段 | 类型 | 可空 | 默认值 | 约束 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 分类 ID |
| `shop_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 所属店铺 |
| `name` | `VARCHAR(100)` | 否 | 无 | 与 shop_id 组合唯一 | 分类名 |

约束：

- `PRIMARY KEY (id)`；
- `UNIQUE (shop_id, name)`；
- `shop_id REFERENCES shops(id)`；
- 删除策略为 `RESTRICT`。

业务规则：

- 分类名称不能为空；
- 同一店铺内分类名不能重复；
- 只有店主可以新增分类。

阶段 1 不实现分类删除和排序字段。

### 4.5 `products`

用途：保存商品资料、当前价格、库存和上下架状态。

| 字段 | 类型 | 可空 | 默认值 | 约束 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 商品 ID |
| `shop_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 所属店铺 |
| `category_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 所属分类 |
| `name` | `VARCHAR(100)` | 否 | 无 | 无 | 商品名称 |
| `description` | `VARCHAR(1000)` | 是 | NULL | 无 | 商品描述 |
| `price` | `DECIMAL(10,2)` | 否 | 无 | CHECK > 0 | 当前价格 |
| `stock` | `INT UNSIGNED` | 否 | 0 | CHECK >= 0 | 当前库存 |
| `status` | `VARCHAR(20)` | 否 | `OFF_SALE` | CHECK | 上下架状态 |
| `created_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 创建时间 |
| `updated_at` | `DATETIME(3)` | 否 | 当前时间 | 自动更新时间 | 更新时间 |

约束：

- `PRIMARY KEY (id)`；
- `shop_id REFERENCES shops(id)`；
- `category_id REFERENCES product_categories(id)`；
- `price > 0`；
- `stock >= 0`；
- `status IN ('ON_SALE', 'OFF_SALE')`；
- 索引 `(shop_id, status)`；
- 索引 `(category_id)`；
- 删除策略为 `RESTRICT`。

业务规则：

- 新商品初始状态为 `OFF_SALE`；
- category 必须属于同一个 shop，该规则由 ServiceImpl 校验；
- 普通用户只浏览 `ON_SALE` 商品；
- `OFF_SALE` 或库存不足时不能加入购物车或下单；
- 商品后续改名、改价不能改变已生成的订单明细快照。

### 4.6 `cart_items`

用途：保存用户尚未结算的购物车项。

| 字段 | 类型 | 可空 | 默认值 | 约束 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 购物车项 ID |
| `user_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 所属用户 |
| `product_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 商品 |
| `quantity` | `INT UNSIGNED` | 否 | 无 | CHECK > 0 | 数量 |
| `created_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 创建时间 |
| `updated_at` | `DATETIME(3)` | 否 | 当前时间 | 自动更新时间 | 更新时间 |

约束：

- `PRIMARY KEY (id)`；
- `UNIQUE (user_id, product_id)`；
- `user_id REFERENCES users(id)`；
- `product_id REFERENCES products(id)`；
- `quantity > 0`；
- 索引 `(user_id)`；
- 删除策略为 `RESTRICT`。

业务规则：

- 只能操作本人的购物车；
- 重复加入同一商品时更新原记录的数量；
- 阶段 1 一个用户的购物车只允许同一店铺商品，该规则由 ServiceImpl 校验；
- 加入和修改数量时重新检查店铺、商品和库存；
- 删除购物车项使用物理删除，因为它只是临时数据。

### 4.7 `orders`

用途：保存订单主信息。

| 字段 | 类型 | 可空 | 默认值 | 约束 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 订单 ID |
| `order_no` | `VARCHAR(40)` | 否 | 无 | 唯一 | 展示用订单编号 |
| `user_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 下单用户 |
| `shop_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 订单店铺 |
| `total_amount` | `DECIMAL(10,2)` | 否 | 无 | CHECK > 0 | 后端计算的总额 |
| `status` | `VARCHAR(30)` | 否 | `PENDING_PAYMENT` | CHECK | 订单状态 |
| `created_at` | `DATETIME(3)` | 否 | 当前时间 | 无 | 创建时间 |

约束：

- `PRIMARY KEY (id)`；
- `UNIQUE (order_no)`；
- `user_id REFERENCES users(id)`；
- `shop_id REFERENCES shops(id)`；
- `total_amount > 0`；
- 阶段 1 只允许 `status = 'PENDING_PAYMENT'`；
- 索引 `(user_id, created_at)`；
- 删除策略为 `RESTRICT`。

业务规则：

- 订单金额由后端根据商品当前价格计算；
- 创建订单时重新检查店铺状态、商品状态和库存；
- 阶段 1 不实现支付、取消和后续状态流转；
- 只能查询本人的订单。

### 4.8 `order_items`

用途：保存下单时的商品快照。

| 字段 | 类型 | 可空 | 默认值 | 约束 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 | 明细 ID |
| `order_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 所属订单 |
| `product_id` | `BIGINT UNSIGNED` | 否 | 无 | 外键 | 原商品 ID |
| `product_name` | `VARCHAR(100)` | 否 | 无 | 无 | 商品名称快照 |
| `unit_price` | `DECIMAL(10,2)` | 否 | 无 | CHECK > 0 | 成交单价快照 |
| `quantity` | `INT UNSIGNED` | 否 | 无 | CHECK > 0 | 成交数量 |

约束：

- `PRIMARY KEY (id)`；
- `order_id REFERENCES orders(id)`；
- `product_id REFERENCES products(id)`；
- `unit_price > 0`；
- `quantity > 0`；
- 索引 `(order_id)`；
- 删除策略为 `RESTRICT`。

业务规则：

- 商品名称和单价在创建订单时写入；
- 商品之后改名、改价或下架，不修改历史明细；
- 阶段 1 不提供订单明细的修改和删除接口。

## 5. 必要索引

只建立当前接口实际使用的索引：

| 查询 | 索引/约束 |
| --- | --- |
| 按账号登录 | `users(account)` 唯一索引 |
| 按用户找商家 | `merchants(user_id)` 唯一索引 |
| 按商家找店铺 | `shops(merchant_id)` 唯一索引 |
| 店铺分类 | `product_categories(shop_id, name)` 唯一索引 |
| 店铺在售商品 | `products(shop_id, status)` |
| 分类商品 | `products(category_id)` |
| 用户购物车 | `cart_items(user_id)` |
| 防止重复购物车项 | `cart_items(user_id, product_id)` 唯一索引 |
| 用户订单列表 | `orders(user_id, created_at)` |
| 订单明细 | `order_items(order_id)` |

阶段 1 不做复杂排序索引和性能预优化。

## 6. 创建订单事务边界

后续由 B 在 `OrderServiceImpl.createFromCart` 中使用一个事务完成：

```text
检查用户 ACTIVE
→ 查询本人购物车并检查非空
→ 确认购物车只有一个店铺
→ 检查店铺 OPEN
→ 重新查询商品状态、价格和库存
→ 计算 total_amount
→ 写 orders
→ 写 order_items 快照
→ 扣减 products.stock
→ 删除本人 cart_items
→ 提交事务
```

任一步失败必须回滚全部修改。

这只是后续业务需求说明。A 提交创建订单的 Red 测试之前，B 不实现该事务。

## 7. 数据库和 Service 的校验边界

| 规则 | 数据库负责 | ServiceImpl 负责 |
| --- | --- | --- |
| 必填字段 | NOT NULL | 参数校验和错误消息 |
| 账号唯一 | UNIQUE | 注册前检查并处理冲突 |
| 状态合法 | CHECK | 状态和操作规则 |
| 数值范围 | CHECK | 提前校验并返回 400/409 |
| 外键存在 | FOREIGN KEY | 资源不存在时返回 404 |
| 资源归属 | 不能代替权限校验 | 必须校验当前用户是否拥有资源 |
| 分类属于同一店铺 | 当前最小表不能直接保证 | 必须校验 |
| 购物车只含同店商品 | 当前最小表不能直接保证 | 必须校验 |
| 订单金额 | DECIMAL 保存结果 | 必须按服务端价格计算 |
| 订单事务 | 数据库支持事务 | 必须声明事务并正确编排 |

## 8. Flyway V1 规则

迁移文件固定为：

```text
backend/src/main/resources/db/migration/V1__create_core_tables.sql
```

建表顺序固定为：

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

规则：

1. V1 只创建上述 8 张表、必要约束和必要索引；
2. V1 不插入演示数据；
3. V1 不包含数据库账号或密码；
4. V1 未经 Flyway 执行前，不手工在正式库建表；
5. V1 一旦合并且执行过，后续修改结构使用 V2，不能改写历史迁移；
6. 先迁移 `delivery_test`，验证后再迁移 `delivery_dev`；
7. 第二次运行必须成功且不能重复创建表。

完整 V1 模板见根目录执行文档第 8.3.10 节。

## 9. 阶段 1 明确不实施

以下内容不出现在 V1，也不进入阶段 1 测试：

- 订单幂等键；
- 订单取消和库存恢复；
- 支付、退款、配送、骑手；
- `PAID`、`PREPARING`、`DELIVERING`、`COMPLETED`、`CANCELLED` 状态流；
- products/orders 乐观锁 version；
- 全表 deleted 逻辑删除；
- cancelled_at；
- 图片表、地址表、支付表、日志表；
- 为阶段 2 猜测的预留字段；
- 复杂分页、排序和性能索引。

如果教师阶段 2 正式要求其中某项，再先由 A 增加测试，然后新增迁移和实现。

## 10. B 的设计自检

### 10.1 表和字段

- [x] 只有 8 张阶段 1 业务表；
- [x] 表名和字段名统一使用 snake_case；
- [x] 金额统一使用 `DECIMAL(10,2)`；
- [x] 密码字段只保存摘要；
- [x] 订单明细保存名称和单价快照；
- [x] 不含幂等、取消、支付、配送和乐观锁字段。

### 10.2 关系和约束

- [x] account 唯一；
- [x] 一个用户最多一个商家；
- [x] 一个商家最多一个店铺；
- [x] 同店分类名唯一；
- [x] 同用户同商品购物车项唯一；
- [x] 必要外键均为 RESTRICT；
- [x] 价格、库存、数量和订单金额有 CHECK；
- [x] 必要查询已有索引。

### 10.3 尚未完成

- [ ] B 补齐并验证 MyBatis/Flyway 依赖；
- [ ] A 确认 Service 方法和 DTO 字段；
- [ ] B 创建并人工检查 V1；
- [ ] Flyway 在 `delivery_test` 创建表；
- [ ] B 验证外键、唯一键和 CHECK；
- [ ] Flyway 在 `delivery_dev` 创建表；
- [ ] A、B、C 完成最终字段对齐。

## 11. 本步骤完成判定

第六步只要求数据库设计收敛，不要求表已经创建。

本步骤完成条件：

- [x] 数据库设计文档只包含 8 张阶段 1 表；
- [x] 每张表字段、类型、主键、外键、唯一键和 CHECK 已明确；
- [x] 订单事务边界已说明；
- [x] 数据库和 Service 校验职责已区分；
- [x] 额外功能已明确移出阶段 1；
- [x] 当前状态明确记录为“尚未运行 Flyway、尚未创建正式表”。

下一步：B 检查 A 的依赖进度，然后创建数据库配置文件和 `V1__create_core_tables.sql`。在 Flyway 可运行之前不要手工建表。
