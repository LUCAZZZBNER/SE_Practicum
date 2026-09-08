---
title: SE_Practicum 项目交接记录
date: 2026-09-02
status: 进行中
tags:
  - 软件工程实践
  - 项目交接
  - Spring-Boot
---

# SE_Practicum 项目交接记录

> 更新时间：2026-09-02 13:05（Asia/Shanghai）  
> 当前阶段：需求基线和后端架构骨架已完成，业务开发尚未开始。  
> 本文档可直接使用 Markdown 或 Obsidian 阅读。

## 1. 项目概况

本项目是一个轻量级外卖服务平台课程实践项目，主要包含普通用户、商家、店铺、商品、购物车和订单功能。项目强调前后端分离、模块边界、TDD、Git 过程证据和需求变更后的回归测试。

需求范围和验收条件以[软件需求规格说明书](../software-requirements-specification.md)为准。

## 2. 团队分工

| 成员 | 当前职责 | 近期重点 |
| --- | --- | --- |
| 本人 | 后端业务与数据库 | 数据库设计、业务实现、Mapper/持久化、Service、事务、接口联调 |
| A | 前端 | 初始化 Vue 3、页面与组件、Axios、接口联调 |
| B | 后端架构、TDD 和测试 | 后端公共框架、模块规范、测试先行、接口和架构测试 |

协作原则：B 先根据用例写失败测试，本人完成最少业务实现使测试通过，之后共同重构；A 根据已经确认的接口文档开发，不自行猜测请求和响应字段。

## 3. Git 状态

核对时的仓库状态：

```text
当前分支：develop
本地 develop：ed3a05d
远程 origin/develop：ed3a05d
工作区：干净
```

近期提交：

| 提交 | 内容 |
| --- | --- |
| `ed3a05d` | 添加仓库根目录 IDE 忽略规则 |
| `7550675` | 添加 Spring Boot / Spring Modulith 后端骨架 |
| `a2f12fb` | 建立基础目录与文档结构 |
| `e10fe3d` | 初始化仓库 |

当前 `develop` 比 `main` 多提交 `ed3a05d`；后续合并分支时需要注意这一差异。

## 4. 已完成内容

### 4.1 需求与目录

- 已完成 V1.0 软件需求规格说明书。
- 已明确 `/api/v1` 接口前缀、统一响应结构、核心业务规则和验收标准。
- 已建立 `backend`、`frontend`、`docs` 和 `e2e` 目录框架。
- 已添加仓库根目录 `.gitignore`，忽略 `.idea/`、`*.iml` 等个人 IDE 文件。

### 4.2 后端骨架

后端已经可以启动，启动入口为：

```text
backend/src/main/java/com/delivery/backend/BackendApplication.java
```

当前技术配置：

| 项目 | 当前值 |
| --- | --- |
| Java 编译目标 | Java 17 |
| Spring Boot | 4.1.1 |
| 构建工具 | Maven / Maven Wrapper |
| Web | Spring Web MVC |
| 模块管理 | Spring Modulith 2.1.1 |
| 数据库驱动 | MySQL Connector/J |
| 辅助库 | Lombok、DevTools |

已经建立以下模块：

| 包 | 负责内容 | 当前完成度 |
| --- | --- | --- |
| `user` | 用户注册、登录、个人资料 | 仅骨架和简单状态判断 |
| `merchant` | 商家账号与权限 | 仅骨架 |
| `restaurant` | 店铺信息与营业状态 | 仅骨架 |
| `item` | 商品、价格、状态和库存 | 仅骨架 |
| `shopping` | 购物车 | 仅骨架 |
| `order` | 下单、查询和状态迁移 | 仅骨架 |

模块目录是已经提交到 Git 的源代码，不是运行程序后自动生成的。每个模块通过 `package-info.java` 中的 `@ApplicationModule` 声明边界，具体说明见[架构设计](../architecture/README.md)。

### 4.3 本地开发环境

本人电脑目前已经完成：

- 安装 Microsoft OpenJDK 17.0.20；
- IDEA 已加载 `backend/pom.xml`；
- Spring Boot 后端已在 Java 17 下成功启动；
- 服务默认监听 `http://localhost:8080`；
- MySQL Server 已安装并配置；
- IDEA 数据库工具已使用 `root@localhost:3306` 连接 MySQL；
- 数据目录位于 `D:\Database\MySQLData`。

注意：数据库密码属于个人敏感信息，不得写入本仓库或提交到 Git。

### 4.4 测试验证

2026-09-02 已运行当前后端测试：

```text
Tests run: 2
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

现有测试只验证：

1. Spring 上下文能够加载；
2. Spring Modulith 模块边界能够通过基础校验。

这些不是实际业务测试。目前注册、登录、商品、购物车和订单都没有测试。

## 5. 尚未完成内容

### 5.1 后端

- 尚未配置 Spring Boot 数据源；
- 尚未确定并加入 MyBatis、MyBatis-Plus 或其他持久化方案；
- 尚未加入数据库迁移工具或 SQL 迁移目录；
- 尚未实现统一响应体和全局异常处理；
- 尚未实现参数校验、认证和权限控制；
- 尚未实现 Controller、业务 Service 和数据库持久化；
- 尚未实现任何 `/api/v1` 实际接口；
- 尚未实现密码加密；
- 尚未实现订单事务、库存一致性和防重复提交。

### 5.2 数据库

- 本机 MySQL 已可用，但项目数据库尚未创建；
- 数据库设计文档仍为空白占位；
- 用户、商家、店铺、商品、购物车、订单、订单明细表均未设计；
- 没有建表或迁移脚本；
- 没有测试数据脚本。

### 5.3 前端

- `frontend` 仍只有说明文件；
- Vue 3、Vite、路由、Axios、状态管理和页面均未初始化。

### 5.4 文档与测试材料

- API 文档仍为空白占位；
- 测试用例、测试日志和缺陷列表仍为空白占位；
- 尚无接口测试、业务单元测试、数据库测试和端到端测试；
- 尚无覆盖率报告和阶段验收记录。

## 6. 当前关键风险与待决策事项

在开始业务代码前，三人需要确认：

1. 持久化采用 MyBatis 还是 MyBatis-Plus；
2. 是否使用 Flyway 管理数据库迁移；
3. 登录采用 JWT 还是会话；
4. 普通用户和商家是共用账号表，还是分表；
5. 业务命名统一使用需求中的 `shop/product`，还是现有代码中的 `restaurant/item`；
6. 第一阶段是否保留 Spring Boot 4.1.1，团队成员的开发 JDK 是否统一为 17；
7. 统一响应、错误码、分页格式、金额格式和时间格式；
8. 数据库名称及项目专用数据库账号。

特别注意：需求文档使用“店铺/商品”，现有 Java 模块使用 `restaurant/item`。如果不提前统一，API、前端、数据库和代码的名称会长期混乱。

## 7. 建议的下一步执行顺序

### 第一步：三人冻结最小技术约定

负责人：B 主导，全员确认。

输出：更新架构、数据库和 API 文档，至少确定第 6 节中的事项。

### 第二步：本人完成数据库第一版设计

先设计核心表及关系：

```text
users
merchants
shops/restaurants
product_categories
products/items
cart_items
orders
order_items
```

每张表明确主键、外键、唯一约束、状态、金额、库存、时间字段和逻辑删除策略。设计先写入[数据库设计](../database/README.md)，评审通过后再编写迁移脚本。

### 第三步：以用户注册作为第一个 TDD 切片

1. 确认 `POST /api/v1/users` 请求、响应和错误码；
2. B 先写注册成功、账号重复、参数为空和密码不合法测试；
3. 运行测试并保留失败证据；
4. 本人实现用户表、持久化、密码加密和注册业务；
5. 测试通过后重构；
6. A 根据已确认接口开发注册页面；
7. 更新 API 文档、测试日志和开发日志。

### 第四步：按纵向业务链继续

推荐顺序：

```text
注册/登录
→ 创建店铺
→ 新增与浏览商品
→ 购物车
→ 创建和查询订单
→ 取消订单
```

不要先把所有 Entity 写完再集中写 Service；每次完成一个可以测试和演示的小闭环。

## 8. 本人接手后的第一批任务

- [ ] 与 B 确认持久化、迁移、认证和模块命名方案；
- [ ] 完成数据库实体关系草图；
- [ ] 完成用户表字段、索引和约束设计；
- [ ] 完成注册接口字段草案；
- [ ] 等 B 提交注册失败测试后实现注册业务；
- [ ] 每个步骤分别保留文档、测试失败、实现、测试通过的 Git 提交证据；
- [ ] 不提交 `.idea/`、数据库密码、个人配置或 `target/`。

## 9. 常用操作

### 更新本地代码

```bash
git switch develop
git pull origin develop
```

### 启动后端

在 IDEA 中运行 `BackendApplication.main()`，成功标志：

```text
Tomcat started on port 8080
Started BackendApplication
```

### 运行测试

在 `backend` 目录执行：

```powershell
.\mvnw.cmd test
```

也可以在 IDEA 中右键 `src/test/java` 运行全部测试。

### 提交前检查

```bash
git status
```

只暂存本次任务相关文件，不提交个人 IDE 配置和密码。提交信息示例：

```text
docs(database): add initial user table design
test(user): add user registration scenarios
feat(user): implement user registration
```

## 10. 交接结论

当前项目不是“功能已开发”，而是“需求和后端模块骨架已准备好”。后端能够启动，基础架构测试通过，本地 MySQL 环境可用；但数据库、接口、业务、前端和业务测试均尚未实现。

接下来最重要的不是继续增加空目录，而是先统一技术和命名约定，然后以“用户注册”为第一个 TDD 纵向切片完成数据库、测试、后端接口和前端页面的闭环。
