# B：业务开发前数据库准备逐步执行手册

> 使用者：小组成员 B。
>
> 开始位置：当前项目已经有数据库设计文档，本机 `mysql --version` 可以执行，显示 MySQL Community Server 26.7；业务代码尚未开始。
>
> 结束位置：Flyway、MyBatis 基础、开发库、测试库和 8 张核心表全部准备完成，相关 PR 合并到 `develop`。然后停止数据库准备，等待 A 在旧版手册 8.1 中先提交用户注册测试。
>
> 本手册只完成准备工作，不实现用户注册、登录、店铺、商品、购物车和订单业务。

---

## 0. 先看最终路线，不要跳步

B 从现在开始按下面顺序执行：

```text
第 1 步  把两份新手册通过文档分支提交
第 2 步  回到最新 develop，创建 B 的数据库准备分支
第 3 步  验证 MySQL 客户端、服务器和项目账号
第 4 步  检查 delivery_dev、delivery_test 是否存在且是否为空
第 5 步  安全检查 Windows 环境变量
第 6 步  阅读并能解释 8 张表
第 7 步  把 MyBatis/Flyway 依赖需求交给 A
第 8 步  创建数据库配置和迁移目录
第 9 步  编写 application 配置
第 10 步 使用 Agent 辅助生成 V1 建表 SQL
第 11 步 B 人工逐项检查 V1
第 12 步 等 A 的依赖准备 PR 合并后，把 develop 合入 B 分支
第 13 步 使用 test Profile 在 delivery_test 执行 Flyway
第 14 步 检查测试库 8 张表、Flyway 历史和数据库约束
第 15 步 重复迁移，确认不会重复建表
第 16 步 使用 dev Profile 在 delivery_dev 执行 Flyway
第 17 步 检查开发库，并比较两个数据库的结构
第 18 步 运行后端测试，保存准备日志
第 19 步 分三次提交，推送并创建 PR
第 20 步 PR 合并后重新验证，然后停止
```

任何一步失败，都在当前步骤处理，不要抱着“后面可能自己恢复”的想法继续。

---

## 1. 本手册中几个容易混淆的词

### 1.1 MySQL

真正保存用户、商品和订单数据的数据库软件。

### 1.2 MyBatis

以后帮助 Java 调用 SQL 的工具。本轮只把它安装并配置好，不编写具体业务 Mapper。

### 1.3 Flyway

自动执行建表 SQL、记录数据库结构版本的工具。

### 1.4 Migration 迁移

一次数据库结构变更。本轮只有第一版：

```text
V1__create_core_tables.sql
```

### 1.5 Profile

Spring Boot 的环境选择：

```text
test → delivery_test
dev  → delivery_dev
```

### 1.6 本轮为什么不写 Mapper

具体 Mapper 属于具体业务切片。例如 `UserMapper` 是用户注册实现的一部分，必须等 A 先写用户测试。现在只准备 MyBatis 的公共扫描和配置。

---

## 2. MySQL 26.7 的处理规则

学校文件没有强制某个 MySQL 小版本，所以本机 MySQL 26.7 可以继续使用，不需要现在重装。

必须满足：

1. Flyway 能连接并完成 V1；
2. SQL 只使用 MySQL 8 和 MySQL 26.7 都能识别的常规语法；
3. B 把实际服务器版本写进准备日志；
4. 其他组员的 MySQL 也能执行同一份 V1；
5. 如果 Flyway 明确提示 `Unsupported Database`，必须停止并解决，不能绕过 Flyway 手工建表后假装完成。

出现 Flyway 不支持时只有两种正规处理：

- 使用与 MySQL 26.7 兼容的 Flyway 版本；
- 将数据库改为团队原定的 MySQL 8.4 LTS。

不要因为版本数字更大就假定所有工具必然兼容。

---

# 第一部分：先保存手册，再创建 B 的代码分支

## 3. 检查当前 Git 状态

打开 PowerShell，进入项目根目录：

```powershell
cd D:\Projects\SchoolWorks\SW_2609\SE_Practicum
git status --short --branch
```

如果两份准备手册还没有提交，应看到类似：

```text
## develop...origin/develop
?? 业务开发前-ABC三人并行准备执行手册.md
?? B-业务开发前数据库准备-逐步执行手册.md
```

`??` 表示文件存在于电脑上，但 Git 还没有跟踪。

如果还出现其他 `M` 或 `??`，先判断是谁的文件，不要一起提交。

## 4. 创建文档分支

如果尚未创建文档分支，执行：

```powershell
git switch develop
git pull --ff-only origin develop
git switch -c feature/docs-preparation-guides
```

如果提示分支已经存在：

```powershell
git branch
```

找到已经创建的文档分支，再切换过去：

```powershell
git switch feature/docs-preparation-guides
```

不要为了处理分支名重复而执行 `git reset --hard`。

## 5. 只提交两份手册

执行：

```powershell
git add -- "业务开发前-ABC三人并行准备执行手册.md"
git add -- "B-业务开发前数据库准备-逐步执行手册.md"
git status
```

确认 `Changes to be committed` 下只有这两份文档，然后提交：

```powershell
git commit -m "docs: add pre-development preparation guides"
git push -u origin feature/docs-preparation-guides
```

在 GitHub 创建 PR：

```text
base：develop
compare：feature/docs-preparation-guides
```

PR 标题：

```text
docs: add pre-development preparation guides
```

PR 合并后执行：

```powershell
git switch develop
git pull --ff-only origin develop
git status --short --branch
```

成功标志：

- 当前是 `develop`；
- 两份手册存在；
- 没有 `??`；
- 本地与 `origin/develop` 同步。

## 6. 创建 B 的数据库准备分支

```powershell
git switch develop
git pull --ff-only origin develop
git switch -c feature/b-database-runtime
git status --short --branch
```

成功时第一行应为：

```text
## feature/b-database-runtime
```

从现在开始，B 的数据库配置、迁移脚本和日志都放在这个分支。

---

# 第二部分：验证数据库环境

## 7. 验证 MySQL 客户端

在 PowerShell 中执行：

```powershell
mysql --version
```

当前预期类似：

```text
C:\Program Files\MySQL\MySQL Server 26.7\bin\mysql.exe
Ver 26.7.0 for Win64 on x86_64
```

这一步只能证明客户端命令可用，不能证明正在运行的服务器版本和权限正确。

## 8. 验证 MySQL Windows 服务

执行：

```powershell
Get-Service | Where-Object { $_.Name -like "*MySQL*" -or $_.DisplayName -like "*MySQL*" }
```

检查 `Status`：

```text
Running → 服务正在运行
Stopped → 服务已停止
```

如果服务停止，在管理员 PowerShell 中使用实际服务名启动。例如服务名是 `MySQL`：

```powershell
Start-Service -Name "MySQL"
```

不要照抄服务名。必须使用上一步真实显示的 `Name`。

## 9. 确认服务器版本

使用管理员账号登录：

```powershell
mysql -u root -p
```

输入密码时没有星号或字符显示，这是正常现象。输入完成按 Enter。

进入 `mysql>` 后执行：

```sql
SELECT VERSION();
SELECT @@version_comment;
```

把结果记下来。最终准备日志记录的是这里的服务器版本，不只是 `mysql --version` 的客户端版本。

## 10. 检查两个数据库

继续在 MySQL 中执行：

```sql
SHOW DATABASES;
```

必须看到：

```text
delivery_dev
delivery_test
```

依次检查：

```sql
USE delivery_dev;
SELECT DATABASE();
SHOW TABLES;

USE delivery_test;
SELECT DATABASE();
SHOW TABLES;
```

### 10.1 理想结果

两个数据库都存在，而且还没有业务表。

### 10.2 如果已经有业务表

立即停止，不要删除。

把下面命令结果保存下来：

```sql
USE delivery_dev;
SHOW TABLES;

USE delivery_test;
SHOW TABLES;
```

确认这些表是不是之前手工创建的。Flyway V1 需要面对可预测的数据库状态。

不允许在没有确认数据用途的情况下执行：

```sql
DROP DATABASE ...;
DROP TABLE ...;
```

## 11. 验证项目专用账号

退出 root：

```sql
EXIT;
```

使用项目账号登录：

```powershell
mysql -u delivery_app -p
```

如果你创建的账号不是 `delivery_app`，把命令中的账号换成实际项目账号。

登录后执行：

```sql
USE delivery_dev;
SELECT DATABASE();

USE delivery_test;
SELECT DATABASE();

SHOW GRANTS;
```

成功标志：

- 可以进入 `delivery_dev`；
- 可以进入 `delivery_test`；
- 对两个库拥有建表、索引、查询、插入、修改和删除等开发权限；
- 应用不是长期使用 root 账号连接。

如果出现 `Access denied`：

1. 检查账号拼写；
2. 检查密码；
3. 检查用户允许从 `localhost` 登录；
4. 使用 root 检查并授予项目库权限；
5. 权限未解决前不要进入 Spring Boot 配置。

退出：

```sql
EXIT;
```

## 12. 安全检查环境变量

不要把数据库密码或 JWT 密钥直接打印到终端截图里。使用下面的命令只显示 `SET` 或 `MISSING`：

```powershell
$deliveryVariableNames = @(
    "DELIVERY_DB_USERNAME",
    "DELIVERY_DB_PASSWORD",
    "DELIVERY_DB_URL",
    "DELIVERY_JWT_SECRET"
)

foreach ($deliveryVariableName in $deliveryVariableNames) {
    $deliveryVariableValue = [Environment]::GetEnvironmentVariable($deliveryVariableName, "User")

    if ([string]::IsNullOrWhiteSpace($deliveryVariableValue)) {
        Write-Host "$deliveryVariableName = MISSING"
    } else {
        Write-Host "$deliveryVariableName = SET"
    }
}
```

必须全部显示：

```text
SET
```

如果显示 `MISSING`：

1. 打开 Windows“编辑账户的环境变量”；
2. 确认变量创建在“用户变量”中；
3. 点击所有窗口的“确定”；
4. 关闭旧 PowerShell；
5. 打开新 PowerShell重新检查。

## 13. 检查 URL 指向哪里

数据库 URL 不是密码，可以检查：

```powershell
[Environment]::GetEnvironmentVariable("DELIVERY_DB_URL", "User")
```

它应当指向本机 MySQL 开发库，例如包含：

```text
localhost
3306
delivery_dev
```

测试环境不能直接使用这个开发库 URL。后面的测试配置会明确覆盖为 `delivery_test`。

---

# 第三部分：B 必须先理解数据库设计

## 14. 打开数据库设计文档

在 IDEA 或文本编辑器中打开：

```text
docs/database/README.md
```

不要直接跳到 SQL。先逐张表确认下面的问题。

## 15. users 表

B 必须能解释：

- `id` 唯一识别用户；
- `account` 为什么必须唯一；
- 为什么数据库只能保存 `password_hash`；
- `status=DISABLED` 表示账号被禁用；
- `deleted` 表示逻辑删除；
- 被禁用和被逻辑删除不是同一个概念。

## 16. merchants 表

B 必须能解释：

- 商家首先是一个用户；
- `user_id` 指向 `users.id`；
- `user_id` 唯一表示一个用户最多有一个商家档案；
- 商家不单独创建另一套登录密码。

## 17. shops 表

B 必须能解释：

- `merchant_id` 表示店铺属于哪个商家；
- 新店铺默认 `CLOSED`；
- `OPEN` 才能下单；
- `TEMPORARILY_CLOSED` 是临时关闭；
- 店铺逻辑删除不能删除历史订单。

## 18. product_categories 表

B 必须能解释：

- 一个分类属于一个店铺；
- `shop_id` 是外键；
- `sort_order` 控制页面排序；
- 分类被有效商品引用时不能随意删除。

## 19. products 表

B 必须能解释：

- 商品属于店铺和分类；
- 价格必须大于 0；
- 库存不能小于 0；
- 新商品默认 `OFF_SALE`；
- `version` 用于检测并发修改；
- 下单扣库存不能只“先查再无条件修改”。

## 20. cart_items 表

B 必须能解释：

- 购物车项属于用户和商品；
- 数量必须大于 0；
- `(user_id, product_id)` 联合唯一；
- 同一个用户加入同一商品时应该增加数量，不是产生第二行；
- 购物车显示价格不能直接作为订单最终成交价。

## 21. orders 表

B 必须能解释：

- `id` 是数据库主键；
- `order_number` 是给业务和用户看的唯一订单号；
- `user_id` 表示订单所有者；
- `shop_id` 表示订单所属店铺；
- `idempotency_key` 用于防止网络重试产生重复订单；
- `total_amount` 必须由后端计算；
- 新订单默认 `PENDING_PAYMENT`；
- 第一阶段只有 `PENDING_PAYMENT` 可以取消；
- 订单不能因为商品删除而被删除。

## 22. order_items 表

B 必须能解释：

- 一张订单包含多条订单明细；
- 明细保存 `product_id`；
- 还必须保存商品名称快照；
- 还必须保存成交单价快照；
- 商品以后改名或改价不能改变历史订单。

## 23. 理解检查点

如果 B 还不能回答以下问题，不进入下一阶段：

- [ ] 为什么用户和商家分两张表；
- [ ] 为什么购物车要有联合唯一约束；
- [ ] 为什么订单和订单明细分两张表；
- [ ] 为什么订单明细保存价格快照；
- [ ] 为什么测试库不能是开发库；
- [ ] 为什么 V1 合并后不能随意修改；
- [ ] 为什么库存不能先查再无条件扣减。

---

# 第四部分：先让 A 准备依赖

## 24. B 不直接修改 pom.xml

准备阶段规定 `backend/pom.xml` 由 A 统一修改，避免 A、B 同时编辑产生冲突。

B 把下面这段发给 A：

```text
B 的数据库准备需要以下后端依赖：
1. 与 Spring Boot 4.1.1 兼容的 MyBatis Spring Boot Starter；
2. Flyway Core；
3. Flyway MySQL 支持模块；
4. 保留 MySQL Connector/J；
5. 添加后请使用 Java 17 运行 .\mvnw.cmd test；
6. 请只准备依赖和公共基础，不提前实现业务 Mapper、Service、Controller。
B 不会同时修改 pom.xml。
```

等待 A 处理依赖期间，B 可以继续创建配置和 V1 SQL。

此时不要尝试通过 Spring Boot 执行 Flyway，因为依赖可能尚未就绪。

---

# 第五部分：创建数据库配置和迁移脚本

## 25. 回到项目根目录并确认分支

```powershell
cd D:\Projects\SchoolWorks\SW_2609\SE_Practicum
git status --short --branch
```

必须位于：

```text
feature/b-database-runtime
```

如果不是，执行：

```powershell
git switch feature/b-database-runtime
```

## 26. 创建资源目录

执行：

```powershell
New-Item -ItemType Directory -Force "backend/src/main/resources/db/migration"
New-Item -ItemType Directory -Force "backend/src/main/resources/mapper"
New-Item -ItemType Directory -Force "backend/src/test/resources/db/test-data"
```

检查：

```powershell
Get-ChildItem "backend/src/main/resources" -Recurse
Get-ChildItem "backend/src/test/resources" -Recurse
```

注意：Git 不保存空目录。`mapper` 和 `test-data` 在没有实际文件时可能不会出现在 GitHub，这是正常现象。

## 27. 规划配置文件

本轮准备三个配置文件：

```text
backend/src/main/resources/application.properties
backend/src/main/resources/application-dev.properties
backend/src/test/resources/application-test.properties
```

其中：

```text
application.properties       公共设置
application-dev.properties   开发库设置
application-test.properties  测试库设置
```

## 28. 修改公共 application.properties

当前文件只有程序名称。使用 IDEA 打开：

```text
backend/src/main/resources/application.properties
```

准备成下面的内容：

```properties
spring.application.name=backend
spring.profiles.default=dev

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

mybatis.mapper-locations=classpath*:mapper/**/*.xml
mybatis.configuration.map-underscore-to-camel-case=true

spring.modulith.detection-strategy=explicitly-annotated
```

逐项解释：

- `spring.profiles.default=dev`：没有显式选择时使用开发环境；
- `spring.flyway.enabled=true`：启动 Flyway；
- `locations`：去 `db/migration` 找 V1；
- `mapper-locations`：以后去 `mapper` 找 MyBatis XML；
- `map-underscore-to-camel-case`：让 `created_at` 对应 Java 的 `createdAt`；
- Modulith 设置：只把明确声明的包识别为业务模块。

## 29. 创建 application-dev.properties

在 IDEA 中新建：

```text
backend/src/main/resources/application-dev.properties
```

内容：

```properties
spring.datasource.url=${DELIVERY_DB_URL:jdbc:mysql://localhost:3306/delivery_dev?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai}
spring.datasource.username=${DELIVERY_DB_USERNAME}
spring.datasource.password=${DELIVERY_DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

说明：

- 如果 `DELIVERY_DB_URL` 已设置，就使用环境变量；
- 如果未设置，才使用冒号后面的本机开发库地址；
- 用户名和密码没有默认值，必须来自环境变量；
- 文件中不能出现你的真实密码。

## 30. 创建 application-test.properties

在 IDEA 中新建：

```text
backend/src/test/resources/application-test.properties
```

内容：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/delivery_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
spring.datasource.username=${DELIVERY_DB_USERNAME}
spring.datasource.password=${DELIVERY_DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

这里故意明确写 `delivery_test`，不读取可能指向开发库的 `DELIVERY_DB_URL`。

## 31. 第一次敏感信息检查

在项目根目录执行：

```powershell
git diff -- backend/src/main/resources backend/src/test/resources
```

逐行看，不能出现：

- 你的 MySQL 密码；
- root 密码；
- JWT 密钥；
- 真实个人账号；
- 个人电脑用户名目录。

## 32. 创建 V1 文件

在 IDEA 中新建：

```text
backend/src/main/resources/db/migration/V1__create_core_tables.sql
```

注意文件名：

```text
V1 + 两个下划线 + create_core_tables.sql
```

错误示例：

```text
V1_create_core_tables.sql
V1-create-core-tables.sql
create_core_tables.sql
```

## 33. 使用 Agent 生成 SQL 的完整任务描述

把下面任务交给 Agent：

```text
请阅读项目根目录的《26271学期-软件工程综合实践.md》、
docs/database/README.md、docs/api/backend-api-design.md、
docs/architecture/backend-architecture-design.md。

当前任务只完成数据库准备，不实现业务功能。

请创建或完善：
backend/src/main/resources/db/migration/V1__create_core_tables.sql

要求：
1. 严格创建 users、merchants、shops、product_categories、products、
   cart_items、orders、order_items 八张表；
2. 创建顺序必须先父表后子表；
3. 字段、类型、默认值以 docs/database/README.md 为准；
4. 添加主键、外键、唯一约束、CHECK 和查询索引；
5. users.account 唯一；
6. merchants.user_id 唯一；
7. cart_items 的 user_id + product_id 唯一；
8. orders.order_number 唯一；
9. orders 的 user_id + idempotency_key 唯一；
10. 金额使用 DECIMAL(10,2)，商品价格大于 0；
11. 库存不小于 0，数量大于 0；
12. 新店铺默认 CLOSED，新商品默认 OFF_SALE，新订单默认 PENDING_PAYMENT；
13. 需要的表包含 created_at、updated_at、deleted、version；
14. order_items 保存 product_name_snapshot、unit_price_snapshot、quantity、subtotal；
15. 外键不得使用会删除历史订单的级联删除；
16. SQL 使用 MySQL 8 和 MySQL 26.7 都支持的常规语法；
17. 不创建第 9 张业务表；
18. 不创建 Entity、Mapper、Service、Controller；
19. 不添加真实账号、手机号、密码、JWT 密钥或演示数据；
20. 完成后逐表解释字段、外键和约束。
```

Agent 完成后，B 必须继续执行下面的人工检查，不能直接提交。

---

# 第六部分：B 人工检查 V1

## 34. 检查建表顺序

V1 中必须按顺序出现：

```text
users
merchants
shops
product_categories
products
cart_items
orders
order_items
```

父表必须在引用它的子表之前创建。

## 35. 检查字段类型

重点确认：

- ID 类型前后一致；
- 外键字段类型与对应主键一致；
- 金额使用 `DECIMAL(10,2)`；
- 密码字段叫 `password_hash`；
- 时间字段类型统一；
- `deleted` 有明确默认值；
- `version` 有明确初始值；
- 状态字段长度足以容纳 `TEMPORARILY_CLOSED`、`PENDING_PAYMENT` 等值。

## 36. 检查默认状态

必须确认：

```text
shops.status 默认 CLOSED
products.status 默认 OFF_SALE
orders.status 默认 PENDING_PAYMENT
```

## 37. 检查唯一约束

至少存在：

```text
users.account
merchants.user_id
cart_items(user_id, product_id)
orders.order_number
orders(user_id, idempotency_key)
```

## 38. 检查 CHECK

至少保证：

```text
商品价格 > 0
商品库存 >= 0
购物车数量 > 0
订单总额 >= 0
订单明细单价 > 0
订单明细数量 > 0
订单明细小计 >= 0
```

## 39. 检查外键

至少存在：

```text
merchants.user_id → users.id
shops.merchant_id → merchants.id
product_categories.shop_id → shops.id
products.shop_id → shops.id
products.category_id → product_categories.id
cart_items.user_id → users.id
cart_items.product_id → products.id
orders.user_id → users.id
orders.shop_id → shops.id
order_items.order_id → orders.id
order_items.product_id → products.id（或按数据库设计采用保留历史的限制策略）
```

不要对历史订单使用危险的级联删除。

## 40. 检查索引

确认常用查询字段有合适索引，例如：

```text
用户账号
商家 user_id
店铺 merchant_id
分类 shop_id
商品 shop_id、category_id、status
购物车 user_id
订单 user_id、shop_id、status、created_at
订单明细 order_id
```

不要给每个字段都盲目加索引。

## 41. 检查订单快照

`order_items` 必须保存：

```text
product_name_snapshot
unit_price_snapshot
quantity
subtotal
```

不能只保存 `product_id`。

## 42. 检查敏感信息和测试数据

V1 只负责结构，不放演示数据。

搜索可疑内容：

```powershell
git diff --check
git diff -- backend/src/main/resources/db/migration
```

确认没有：

- 明文密码；
- 真实手机号；
- JWT 密钥；
- `INSERT INTO` 演示账号；
- 本机绝对路径。

---

# 第七部分：取得 A 的依赖并执行测试库迁移

## 43. 等 A 的准备 PR 合并

A 的 PR 至少应包含：

```text
MyBatis Spring Boot Starter
Flyway Core
Flyway MySQL 支持
MySQL Connector/J
Java 17 构建配置
```

如果 A 的 PR 尚未合并，B 可以完成 SQL 检查，但不能宣称 Flyway 已验证。

## 44. 把最新 develop 合到 B 分支

在项目根目录执行：

```powershell
git switch feature/b-database-runtime
git fetch origin
git merge origin/develop
```

如果无冲突，继续。

如果 `pom.xml` 冲突：

1. 不使用 `git reset --hard`；
2. 保留 A 已经验证过的依赖配置；
3. B 不用自己的旧版本覆盖；
4. 解决后执行 `git status`；
5. 重新运行 Maven 测试。

## 45. 确认 Java 17 和 Maven

```powershell
cd backend
java -version
.\mvnw.cmd -version
```

必须看到 Java 17。

如果仍然是 Java 25，停止。不要用 Java 25 生成本轮正式测试记录。

## 46. 在当前 PowerShell 强制启用 test

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
$env:SPRING_PROFILES_ACTIVE
```

必须输出：

```text
test
```

这是防止自动化测试误连 `delivery_dev` 的额外保险。

## 47. 第一次执行测试和 Flyway

仍在 `backend` 目录执行：

```powershell
.\mvnw.cmd test
```

观察日志，必须确认：

- 数据库名称是 `delivery_test`；
- Flyway 找到了 V1；
- V1 执行成功；
- Spring 上下文加载成功；
- Modulith 边界测试通过；
- 最终是 `BUILD SUCCESS`。

执行完成后清除当前 PowerShell 的临时 Profile：

```powershell
Remove-Item Env:SPRING_PROFILES_ACTIVE
```

如果变量不存在导致清除命令提示错误，可以忽略清除错误，但要确认后续新终端没有遗留错误 Profile。

## 48. 失败时怎么判断

### 48.1 `Cannot start maven from wrapper`

这是 Maven/Java 环境问题，不是 SQL 问题。把完整错误交给 A 处理。

### 48.2 `Access denied`

数据库用户名、密码或权限错误。检查环境变量和 MySQL Grant。

### 48.3 `Unknown database delivery_test`

测试库不存在或名字拼错。回到 MySQL 环境准备步骤。

### 48.4 `Table already exists`

测试库中可能存在手工创建的旧表。不要继续修改 Flyway 历史，也不要直接删除，先确认现有表来源。

### 48.5 `Unsupported Database: MySQL 26.7`

Flyway 版本不支持当前 MySQL。停止并由 A 调整 Flyway 版本，或者统一改用 MySQL 8.4。

### 48.6 SQL syntax error

查看错误中的：

```text
脚本名
行号
错误附近 SQL
```

只修复对应 SQL，不顺便改数据库设计。

### 48.7 `Communications link failure`

检查：

- MySQL 服务是否 Running；
- 端口是否 3306；
- URL 是否拼错；
- 防火墙或本机连接；
- 数据库服务是否刚刚停止。

---

# 第八部分：检查测试库迁移结果

## 49. 登录项目数据库账号

```powershell
mysql -u delivery_app -p
```

使用你实际的项目账号替换 `delivery_app`。

## 50. 查看测试库表

```sql
USE delivery_test;
SHOW TABLES;
```

必须看到 9 个表：

```text
8 张业务表：
users
merchants
shops
product_categories
products
cart_items
orders
order_items

1 张 Flyway 自己的表：
flyway_schema_history
```

## 51. 检查 Flyway 历史

```sql
SELECT installed_rank, version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

必须能看到 V1，并且 `success` 表示成功。

## 52. 检查真实建表结果

依次执行：

```sql
SHOW CREATE TABLE users;
SHOW CREATE TABLE merchants;
SHOW CREATE TABLE shops;
SHOW CREATE TABLE product_categories;
SHOW CREATE TABLE products;
SHOW CREATE TABLE cart_items;
SHOW CREATE TABLE orders;
SHOW CREATE TABLE order_items;
```

不要只看 `SHOW TABLES`。`SHOW CREATE TABLE` 才能确认约束、外键和索引真的被创建。

## 53. 使用 information_schema 检查表数量

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'delivery_test'
ORDER BY table_name;
```

## 54. 检查约束清单

```sql
SELECT table_name, constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_schema = 'delivery_test'
ORDER BY table_name, constraint_type, constraint_name;
```

检查是否包含：

```text
PRIMARY KEY
FOREIGN KEY
UNIQUE
CHECK
```

## 55. 检查索引

```sql
SELECT table_name, index_name, column_name, non_unique
FROM information_schema.statistics
WHERE table_schema = 'delivery_test'
ORDER BY table_name, index_name, seq_in_index;
```

## 56. 进行最小人工约束测试

人工测试只在 `delivery_test` 中执行。

开始前再次确认：

```sql
SELECT DATABASE();
```

必须返回：

```text
delivery_test
```

根据 V1 的实际非空字段准备最小测试数据，验证：

- 重复用户账号被拒绝；
- 商品价格小于等于 0 被拒绝；
- 商品库存小于 0 被拒绝；
- 购物车数量为 0 被拒绝；
- 不存在的外键被拒绝；
- 同一用户同一商品第二条购物车记录被拒绝；
- 重复订单号被拒绝。

因为各字段最终以 V1 为准，本手册不提供可能与实际字段不一致的整段 INSERT。让 Agent 根据最终 V1 只生成 `delivery_test` 使用的约束验证 SQL，并要求每条测试都说明预期成功还是预期失败。

禁止使用真实个人信息。

## 57. 清理人工测试数据

清理前第三次确认：

```sql
SELECT DATABASE();
```

只清理由本轮人工测试插入的数据。

不要删除表，不要删除数据库，不要修改 `flyway_schema_history`。

退出 MySQL：

```sql
EXIT;
```

---

# 第九部分：重复运行和开发库迁移

## 58. 第二次运行测试库迁移

回到 `backend`：

```powershell
cd D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend
$env:SPRING_PROFILES_ACTIVE = "test"
.\mvnw.cmd test
Remove-Item Env:SPRING_PROFILES_ACTIVE
```

第二次必须仍然 `BUILD SUCCESS`。

Flyway 应识别 V1 已执行，不再重复创建八张表。

## 59. 迁移开发库

测试库完全成功后才执行：

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\mvnw.cmd spring-boot:run
```

观察日志，确认连接的是：

```text
delivery_dev
```

确认 Flyway V1 成功且 Spring Boot 完成启动后，按：

```text
Ctrl + C
```

停止程序。

清除临时 Profile：

```powershell
Remove-Item Env:SPRING_PROFILES_ACTIVE
```

## 60. 检查开发库

```powershell
mysql -u delivery_app -p
```

进入后：

```sql
USE delivery_dev;
SHOW TABLES;

SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

同样必须存在八张业务表和一张 Flyway 历史表。

## 61. 比较开发库和测试库表名

```sql
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema IN ('delivery_dev', 'delivery_test')
ORDER BY table_name, table_schema;
```

每张业务表应同时出现在两个数据库中。

如果一边缺表：

- 检查 Profile；
- 检查 Flyway 历史；
- 检查是否使用了错误 URL；
- 不要手工只补建缺少的表，应通过 Flyway 解决。

退出：

```sql
EXIT;
```

---

# 第十部分：保存证据和提交

## 62. 创建 B 的准备日志

在项目中创建：

```text
docs/dev-log/preparation-b-database.md
```

使用下面模板：

```markdown
# B 数据库准备记录

## 基本信息

- 执行人：B
- 执行日期：
- Git 分支：feature/b-database-runtime
- 当前提交：提交后补充

## 环境

- MySQL 客户端版本：
- MySQL 服务器版本：
- Java 版本：17
- Maven 版本：
- 开发库：delivery_dev
- 测试库：delivery_test

## Flyway

- V1 文件：V1__create_core_tables.sql
- 测试库第一次迁移：成功/失败
- 测试库重复运行：成功/失败
- 开发库迁移：成功/失败
- flyway_schema_history：正常/异常

## 表结构检查

- users：通过/未通过
- merchants：通过/未通过
- shops：通过/未通过
- product_categories：通过/未通过
- products：通过/未通过
- cart_items：通过/未通过
- orders：通过/未通过
- order_items：通过/未通过

## 约束检查

- 主键：
- 外键：
- 唯一约束：
- CHECK：
- 索引：
- 订单快照字段：

## 测试结果

- Maven 测试命令：
- 测试总数：
- 通过：
- 失败：0

## 问题和处理

- 问题：
- 原因：
- 处理：
- 处理后结果：

## 安全检查

- 配置文件无明文密码：是/否
- Git 无 .env：是/否
- 日志无密码和 JWT 密钥：是/否

## 结论

数据库准备完成/未完成。未实现业务 Mapper、Service、Controller，可以进入用户注册 TDD/暂不能进入。
```

必须填写真实结果，不能预先全部写“通过”。

## 63. 运行最终后端测试

```powershell
cd D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend
$env:SPRING_PROFILES_ACTIVE = "test"
.\mvnw.cmd test
Remove-Item Env:SPRING_PROFILES_ACTIVE
```

记录：

- 测试总数；
- 通过数；
- 失败数；
- 执行时间。

失败数必须为 0。

## 64. 回到根目录检查修改范围

```powershell
cd D:\Projects\SchoolWorks\SW_2609\SE_Practicum
git status
git diff --check
git diff
```

本轮合理修改范围通常包括：

```text
backend/src/main/resources/application.properties
backend/src/main/resources/application-dev.properties
backend/src/main/resources/db/migration/V1__create_core_tables.sql
backend/src/test/resources/application-test.properties
docs/dev-log/preparation-b-database.md
```

如果出现以下文件，停止检查原因：

```text
UserController.java
UserService.java
OrderService.java
大量 Mapper
前端页面
.env
target/
```

## 65. 第一次提交：数据库配置

```powershell
git add -- "backend/src/main/resources/application.properties"
git add -- "backend/src/main/resources/application-dev.properties"
git add -- "backend/src/test/resources/application-test.properties"
git status
git commit -m "chore(database): configure development and test datasources"
```

提交前确认暂存区只有三个配置文件。

## 66. 第二次提交：Flyway V1

```powershell
git add -- "backend/src/main/resources/db/migration/V1__create_core_tables.sql"
git status
git commit -m "feat(database): add initial Flyway schema"
```

## 67. 第三次提交：验证记录

```powershell
git add -- "docs/dev-log/preparation-b-database.md"
git status
git commit -m "test(database): record migration verification"
```

## 68. 最终提交检查

```powershell
git status
git log --oneline -6
```

`git status` 应显示没有未提交修改。

日志中应能看到类似：

```text
test(database): record migration verification
feat(database): add initial Flyway schema
chore(database): configure development and test datasources
```

## 69. 推送 B 分支

```powershell
git push -u origin feature/b-database-runtime
```

推送失败时：

- `fetch first`：远程同名分支有新提交，先 `git fetch origin`，检查后合并；
- 认证失败：检查 GitHub 登录或 Token；
- 网络失败：保留本地提交，网络恢复后再推送；
- 不要使用 `--force` 覆盖别人的远程提交。

---

# 第十一部分：创建 PR 和合并后复验

## 70. 创建 Pull Request

GitHub 中选择：

```text
base：develop
compare：feature/b-database-runtime
```

标题：

```text
feat(database): prepare Flyway schema and database runtime
```

正文使用：

```markdown
## 本 PR 完成

- 配置 delivery_dev 和 delivery_test；
- 配置 Flyway 迁移目录；
- 配置 MyBatis 基础路径；
- 添加 V1__create_core_tables.sql；
- 创建 8 张核心业务表；
- 验证测试库和开发库迁移；
- 保存 B 数据库准备记录。

## 实际环境

- MySQL 客户端版本：填写
- MySQL 服务器版本：填写
- Java：17

## 验证

- 测试库第一次迁移：成功/失败
- 测试库重复运行：成功/失败
- 开发库迁移：成功/失败
- Maven 测试：总数 X，通过 X，失败 0
- 敏感信息检查：未发现

## 本 PR 明确未实现

- 业务 Mapper；
- Service；
- Controller；
- 用户注册和登录；
- 店铺、商品、购物车和订单业务。
```

## 71. 合并前检查

即使准备阶段跳过了正式人员评审，也必须进行技术自检：

- [ ] PR 目标是 `develop`，不是 `main`；
- [ ] 没有真实密码；
- [ ] 没有 `.env`；
- [ ] 没有 `target`；
- [ ] 没有提前生成业务代码；
- [ ] V1 只有 8 张核心业务表；
- [ ] 测试库和开发库都迁移成功；
- [ ] Maven 测试失败数为 0；
- [ ] MySQL 26.7 的实际情况已记录；
- [ ] PR 说明包含测试命令和结果。

满足后合并到 `develop`。

## 72. 合并后更新本地 develop

```powershell
git switch develop
git pull --ff-only origin develop
git status --short --branch
```

应看到：

```text
## develop...origin/develop
```

且没有未提交文件。

## 73. 合并后再运行一次

后端：

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "test"
.\mvnw.cmd test
Remove-Item Env:SPRING_PROFILES_ACTIVE
```

数据库：

```powershell
mysql -u delivery_app -p
```

```sql
USE delivery_test;
SHOW TABLES;
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
EXIT;
```

只有合并后的 `develop` 也通过，B 的准备工作才真正完成。

## 74. 删除已合并的准备分支

确认 PR 已合并、`develop` 已包含全部提交后，本地可以执行：

```powershell
git branch -d feature/b-database-runtime
```

远程分支可以在 GitHub PR 页面点击删除。

删除分支不会删除已经合并到 `develop` 的提交。

---

# 第十二部分：B 到这里必须停止

## 75. 不再继续写业务代码

B 完成准备后，不要继续创建：

```text
UserMapper
UserRepository
UserRegistrationService
UserController
LoginService
ShopMapper
ProductMapper
CartItemMapper
OrderMapper
```

这些属于旧版手册第 8 章的业务切片。

## 76. 下一步等待 A 的用户注册 Red

A 应从最新 `develop` 创建：

```text
feature/a-test-user
```

A 先提交用户注册测试并保存失败结果。

B 收到 A 的测试分支后才执行：

```powershell
git fetch origin
git switch -c feature/b-user origin/feature/a-test-user
```

B 此时才开始实现：

```text
User Entity/PO
UserRepository
UserMapper
UserRegistrationService
UserController
BCrypt 密码摘要
账号重复异常
```

## 77. B 准备完成消息

B 向团队发送：

```text
[B 数据库准备完成]
最终分支：develop
数据库准备 PR：填写链接
MySQL 客户端版本：填写
MySQL 服务器版本：填写
测试库迁移：成功
测试库重复执行：成功
开发库迁移：成功
业务表：8 张
Flyway 历史：V1 成功
Maven 测试：总数 X，通过 X，失败 0
敏感信息：未提交
业务 Mapper/Service/Controller：未提前实现
下一步：等待 A 提交 feature/a-test-user 的注册 Red 测试
```

---

# 附录 A：B 每次操作前的安全检查

执行数据库命令前问自己：

- [ ] 当前连接的是 `delivery_test` 还是 `delivery_dev`；
- [ ] 我有没有确认 `SELECT DATABASE()`；
- [ ] 当前操作会不会删除数据；
- [ ] 目标是不是课程项目数据库；
- [ ] SQL 中有没有真实密码；
- [ ] 我是否正在修改已经合并的 V1；
- [ ] 这一步是否已经进入业务实现范围。

执行 Git 命令前问自己：

- [ ] 当前分支是不是自己的功能分支；
- [ ] `git status` 中有没有别人的文件；
- [ ] 是否误加入 `.env`、`target`、日志；
- [ ] 是否准备向 `develop` 而不是 `main` 发 PR；
- [ ] 是否运行并记录了测试。

---

# 附录 B：B 的最终一页勾选表

## 环境

- [ ] `mysql --version` 可执行；
- [ ] MySQL 服务为 Running；
- [ ] 已记录实际服务器版本；
- [ ] 项目账号可以进入两个数据库；
- [ ] 四个环境变量都显示 SET；
- [ ] Java 为 17；
- [ ] Maven Wrapper 可用。

## 配置

- [ ] `application.properties` 完成；
- [ ] `application-dev.properties` 指向 `delivery_dev`；
- [ ] `application-test.properties` 指向 `delivery_test`；
- [ ] 用户名和密码读取环境变量；
- [ ] Flyway 目录正确；
- [ ] MyBatis XML 目录正确；
- [ ] 没有明文密码。

## V1

- [ ] 文件名有两个下划线；
- [ ] 只创建 8 张业务表；
- [ ] 建表顺序正确；
- [ ] 主键完整；
- [ ] 外键完整；
- [ ] 唯一约束完整；
- [ ] CHECK 完整；
- [ ] 索引合理；
- [ ] 默认状态正确；
- [ ] 订单快照字段完整；
- [ ] 不包含演示数据和秘密。

## 验证

- [ ] `delivery_test` 第一次迁移成功；
- [ ] `delivery_test` 重复运行成功；
- [ ] `delivery_dev` 迁移成功；
- [ ] 两个数据库都有 8 张业务表；
- [ ] 两个数据库都有 V1 成功记录；
- [ ] `SHOW CREATE TABLE` 已检查；
- [ ] 约束和索引已检查；
- [ ] 后端测试失败数为 0。

## Git 和交付

- [ ] 配置、V1、日志分开提交；
- [ ] 分支已推送；
- [ ] PR 目标是 `develop`；
- [ ] PR 写了实际测试结果；
- [ ] PR 已合并；
- [ ] 合并后的 `develop` 再次测试通过；
- [ ] 工作区干净；
- [ ] 没有提前实现业务代码；
- [ ] 已通知团队等待 A 的用户注册 Red。

以上全部完成，B 的业务开发前准备才算结束。
