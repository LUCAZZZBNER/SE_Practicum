# 数据库准备记录

- 日期：2026-09-05
- 执行人：B
- 分支：`feature/b-database-foundation`
- MySQL 版本：MySQL Community Server 26.7.0（Windows x86_64）
- 开发库：`delivery_dev`
- 测试库：`delivery_test`
- 迁移：`V1__create_core_tables.sql`
- 当前数据库版本：V1

## 验证结果

- [x] `delivery_test` 有 8 张业务表
- [x] `delivery_dev` 有 8 张业务表
- [x] 两个库均有一条成功的 V1 历史
- [x] 重复运行迁移成功，Flyway 输出 `Schema delivery_test is up to date. No migration necessary.`
- [x] `users.account` 唯一约束有效
- [x] 一个用户最多一个商家，`merchants.user_id` 唯一约束有效
- [x] 一个商家最多一个店铺，`shops.merchant_id` 唯一约束有效
- [x] 同一用户的同一购物车商品唯一，`cart_items(user_id, product_id)` 唯一约束有效
- [x] `price`、`quantity`、`total_amount` 正数约束有效
- [x] 数据库密码没有写入 Git；配置文件只引用环境变量

8 张业务表为：

1. `users`
2. `merchants`
3. `shops`
4. `product_categories`
5. `products`
6. `cart_items`
7. `orders`
8. `order_items`

测试库历史检查结果：

```text
version  script                       success
1        V1__create_core_tables.sql   1
```

开发库启动日志确认：Flyway 从空 Schema 执行 V1 成功，数据库达到版本 V1，随后后端在 8080 端口启动成功。

后端测试摘要：

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 执行命令

以下为实际使用的命令，命令本身不包含密码：

```powershell
mysql -u delivery_app -p delivery_test
mysql -u delivery_app -p delivery_test -e "SELECT version, script, success FROM flyway_schema_history ORDER BY installed_rank;"

$env:SPRING_PROFILES_ACTIVE = 'test'
.\mvnw.cmd test

$env:SPRING_PROFILES_ACTIVE = 'dev'
.\mvnw.cmd spring-boot:run

mysql -u delivery_app -p delivery_dev -e "SHOW TABLES;"
mysql -u delivery_app -p delivery_dev -e "SELECT version, script, success FROM flyway_schema_history ORDER BY installed_rank;"
```

## 问题和处理

- Maven Wrapper 在部分 PowerShell 执行环境中曾触发 `.m2` 普通目录的 `Target[0]` 空值错误；B 的实际终端能够启动 Maven 3.9.16，后续仍需在提交前确认 Wrapper 在新终端可重复运行。
- 第一次测试失败是因为清理环境变量后没有重新设置 `DELIVERY_DB_USERNAME` 和 `DELIVERY_DB_PASSWORD`；在当前 PowerShell 重新设置变量后测试通过。密码没有写入命令、日志或仓库。
- Flyway 提示 MySQL 26.7 高于其已验证的 MySQL 9.4；V1 校验、首次迁移和重复迁移均成功，因此当前只记录警告，不关闭 Flyway，也不增加规避配置。
- MyBatis 提示尚未发现 Mapper；当前仍处于 TDD 前期准备阶段，未编写 Mapper 是预期状态。
- 重复迁移测试日志曾显示 Java 25；随后开发库启动已确认使用 `D:\Dev\Java\JDK17` 的 Java 17.0.20.1。后续 Maven 测试继续固定使用 JDK 17。
