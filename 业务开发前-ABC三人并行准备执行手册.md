# 轻量级外卖平台：业务开发前 ABC 三人并行准备执行手册

> 文档定位：本手册是《从0到1-轻量级外卖平台-团队执行手册》的“准备阶段补充手册”。
>
> 使用范围：从当前仓库状态开始，一直执行到三个人可以直接进入旧版执行手册第 8 章“阶段 1：按纵向切片实现功能”。
>
> 最高标准：如本手册与《26271学期-软件工程综合实践.md》冲突，以学校文件为准；如与旧版执行手册中的准备步骤冲突，以“不提前实现核心业务、保留 TDD 证据”为准。

---

## 0. 执行完本手册后，你们应当得到什么

本手册执行完以后，项目应当处于下面的状态：

```text
三个人的电脑都能正常拉取 develop
              ↓
后端固定使用 Java 17，Maven Wrapper 可以运行
              ↓
前端 Node、npm、Vite、Vitest 可以运行
              ↓
MySQL 开发库和测试库分开且都能连接
              ↓
Flyway 可以自动创建 8 张核心表
              ↓
MyBatis 已经安装和配置，但还没有提前写完业务 Mapper
              ↓
后端统一响应、错误码、异常处理和测试基础已经准备好
              ↓
前端 API 基础地址、目录、依赖和测试工具已经准备好
              ↓
API、数据库、状态值和目录规则不存在互相矛盾
              ↓
develop 上所有已有测试通过，工作区干净
              ↓
直接进入旧版手册 8.1：用户注册 TDD
```

执行完本手册不代表用户注册、登录、店铺、商品、购物车或订单已经完成。

本手册只负责把“施工场地、工具、地基、公共零件和验收工具”准备好。真正的业务功能必须在旧版手册第 8 章中按照以下顺序开发：

```text
A 先写测试
→ 运行并确认失败（Red）
→ B 写最少业务实现
→ 运行并确认通过（Green）
→ A、B 重构（Refactor）
→ C 接入页面
→ 三人联调
→ 更新文档
→ PR 合并 develop
```

---

## 1. 当前起点

编写本手册时，仓库的真实起点是：

- 当前集成分支为 `develop`；
- 本地 `develop` 与 `origin/develop` 同步；
- 需求规格说明书、功能分析、数据库设计、后端架构设计和 API 设计已经存在；
- 后端只有 Spring Boot、Spring Modulith 和六个模块的骨架；
- 后端还没有 Controller、真实业务 Service、MyBatis Mapper 和数据库迁移脚本；
- 前端 Vue 3 脚手架、路由、布局和静态页面已经存在；
- 前端页面大部分仍使用写死的示例数据；
- 前端 API 文件中还有 `/stores`、`/cart/items`、`/merchants/login` 等与正式契约不一致的路径；
- `frontend/src/stores` 和前端测试目录仍为空；
- 当前终端曾检测到 Java 25，而项目要求 Java 17；
- 当前 Maven Wrapper 曾出现 `Cannot start maven from wrapper`；
- 当前终端曾检测到 Node，但找不到 `npm`；
- MySQL、开发库、测试库和本机环境变量已经按数据库文档记录为已准备，但仍要以实际命令验证为准。

因此，任何人都不能直接宣称“环境准备已经全部完成”。必须以本手册中的验收命令为准。

---

## 2. 三个人在准备阶段可以做什么、不能做什么

### 2.1 可以提前完成的准备工作

以下工作不属于某个具体业务接口，可以在正式用户注册 TDD 前完成：

- 安装和验证 Java、Maven、Node、npm、MySQL、IDEA；
- 添加项目确定使用的基础依赖；
- 配置开发环境和测试环境；
- 创建 Flyway 迁移脚本；
- 创建 8 张核心表、外键、唯一约束、CHECK 和索引；
- 创建统一响应对象；
- 创建统一错误码和全局异常处理；
- 创建分页公共对象；
- 配置测试框架和覆盖率工具；
- 配置前端 Vite、Vitest、Axios 基础地址；
- 修正文档和前端 API 文件中已经确认的命名错误；
- 编写不包含真实业务的冒烟测试；
- 更新 README、准备日志和验收清单。

### 2.2 不能提前闷头完成的业务工作

在旧版手册第 8 章开始前，任何人都不得提前完成：

- 用户注册 Controller、Service、Mapper；
- 用户登录、密码校验和 JWT 业务流程；
- 个人资料查询和修改；
- 商家注册；
- 创建和修改店铺；
- 商品分类和商品增删改查；
- 购物车增删改查；
- 创建、查询和取消订单；
- 订单扣库存、清购物车、恢复库存事务；
- 一次性生成六个模块的完整 Entity、Mapper、Service、Controller；
- 一次性把所有静态页面改成未经测试的真实业务页面。

判断方法：

```text
如果这段代码可以直接完成某个 UC/FR 用例，属于业务实现，必须等测试 Red。

如果这段代码只是让项目能编译、连接、迁移、统一返回或运行测试，属于准备工作，可以现在完成。
```

### 2.3 准备阶段也不能出现红灯

准备工作可以不对每一行配置机械地执行 TDD，但合并到 `develop` 前必须满足：

- 后端现有测试全部通过；
- 前端构建和准备阶段测试全部通过；
- Flyway 能在测试库完成迁移；
- 不能为了通过测试而删除测试；
- 不能把失败测试单独合并到 `develop`；
- 不能提交真实密码、JWT 密钥和个人配置。

---

## 3. 准备阶段的人员职责和文件所有权

为了让三个人可以各自工作而不频繁修改同一个文件，准备阶段固定如下分工。

| 成员 | 准备阶段主要任务 | 主要负责文件 |
| --- | --- | --- |
| A | 后端构建、公共架构、测试基础、架构一致性 | `backend/pom.xml`、后端公共技术包、后端测试基础、`docs/architecture/` |
| B | MySQL、Flyway、数据库配置、建表 SQL、迁移验证 | 数据源配置、`db/migration/`、MyBatis 配置、`docs/database/` |
| C | Node/npm、前端依赖、Vite、Vitest、API 路径基础、README | `frontend/`、根 `README.md`、准备阶段前端记录 |

### 3.1 准备阶段避免同时修改的文件

以下文件只能由指定成员主改：

| 文件 | 主改人 | 其他人如何提出修改 |
| --- | --- | --- |
| `backend/pom.xml` | A | B、C 把依赖需求发给 A，不同时编辑 |
| `backend/src/main/resources/application*.properties` | B | A 把非数据库配置项发给 B |
| `backend/src/test/resources/application-test.properties` | B | A 提出测试配置需求，B 写入 |
| `frontend/package.json`、`package-lock.json` | C | A、B 不编辑 |
| 根目录 `README.md` | C | A、B 把更新内容发给 C |
| `docs/architecture/*` | A | B、C 提意见，由 A 修改 |
| `docs/database/*` | B | A、C 提意见，由 B 修改 |

### 3.2 每人使用独立准备分支

本轮使用三个新的准备分支：

```text
feature/a-preparation-foundation
feature/b-database-runtime
feature/c-frontend-readiness
```

不要三个人都直接在 `develop` 上修改。

---

## 4. 开始前的 20 分钟共同确认

三个人开始闷头工作前，只开一次简短确认，不讨论实现细节，只确认不会做出三个不同版本。

### 4.1 三人共同打开的文件

依次阅读：

```text
26271学期-软件工程综合实践.md
docs/software-requirements-specification.md
docs/architecture/backend-architecture-design.md
docs/api/backend-api-design.md
docs/database/README.md
从0到1-轻量级外卖平台-团队执行手册.md
```

### 4.2 必须口头确认的固定结论

三人逐条回答“确认”：

- 后端使用 Java 17、Spring Boot、Spring Modulith；
- 数据访问使用 MyBatis；
- 数据库迁移使用 Flyway；
- 数据库使用 MySQL 8；
- 开发库为 `delivery_dev`；
- 测试库为 `delivery_test`；
- 前端使用 Vue 3、Vite、Vue Router、Pinia、Element Plus、Axios、Vitest；
- 外部统一使用 `shop`、`product`，内部暂时保留 `restaurant`、`item`；
- 新店铺为 `CLOSED`；
- 新商品为 `OFF_SALE`；
- 新订单为 `PENDING_PAYMENT`；
- 第一阶段只有 `PENDING_PAYMENT` 订单允许取消；
- 登录统一使用 `POST /api/v1/users/login`；
- 购物车统一使用 `/api/v1/cart-items`；
- 成功响应统一使用 `code/msg/data`；
- 删除接口统一使用 `204 No Content`，API 文档中现有的 `200 + 删除结果`需要由 A 在准备阶段改为 204；
- 数据库保持 8 张核心表，幂等键字段存放在 `orders`，准备阶段不新增第 9 张业务表；
- 后端模块内统一使用 `api/web/application/domain/infrastructure/persistence`；
- 上述目录分别对应学校所说的 Controller、Service、Entity、Mapper/DAO 分层；
- 所有真实接口和核心业务方法必须先测试、后实现。

### 4.3 只允许记录，不允许临时扩展需求

准备阶段不要增加：

- 真实支付；
- 优惠券；
- 配送员；
- 地图；
- 短信验证码；
- 图片上传；
- 微服务；
- Docker 或 Kubernetes 强制要求；
- 不在课程需求中的后台管理系统。

发现想法时只记录到“后续可选”，不能打断第一阶段。

---

# 5. A 的闷头准备手册：后端公共基础与测试基础

## A-0. A 的最终交付目标

A 完成后必须交付：

```text
Java 17 和 Maven Wrapper 可用
后端依赖完整且能下载
统一响应对象
分页公共对象
统一错误码
业务异常基类
全局异常处理器
参数校验基础
Modulith 边界测试
公共组件测试
覆盖率工具入口
架构和 API 冲突已统一
准备日志
```

A 在准备阶段不实现用户注册、登录和订单等业务。

## A-1. 创建 A 的准备分支

在项目根目录执行：

```powershell
git status
git switch develop
git pull --ff-only origin develop
git switch -c feature/a-preparation-foundation
git status --short --branch
```

成功标志：

```text
## feature/a-preparation-foundation
```

如果提示分支已经存在：

1. 先执行 `git branch` 查看；
2. 如果该分支是未完成工作，使用 `git switch feature/a-preparation-foundation`；
3. 不要执行 `git reset --hard`；
4. 不确定时停止并检查旧分支是否已经合并。

## A-2. 固定 Java 17

A 在 PowerShell 中执行：

```powershell
java -version
$env:JAVA_HOME
Get-Command java
```

必须看到 Java 17。只看到 Java 25 不算通过。

在 IDEA 中检查：

```text
File
→ Project Structure
→ Project SDK
→ 选择 JDK 17
```

再检查：

```text
Settings
→ Build, Execution, Deployment
→ Build Tools
→ Maven
→ Runner
→ JRE 选择 Project SDK 17
```

关闭并重新打开 PowerShell 后再次运行：

```powershell
java -version
```

失败处理：

- `java` 找不到：检查 JDK 17 的 `bin` 是否进入 Path；
- 显示 Java 25：检查 Path 中 Java 25 是否排在 Java 17 前面；
- IDEA 是 17、终端是 25：说明 IDEA 和 Windows 使用了不同 Java，需要分别修正；
- 不要删除不确定用途的 JDK，先只调整项目使用版本。

## A-3. 修复 Maven Wrapper

进入后端目录：

```powershell
cd backend
.\mvnw.cmd -version
```

成功标志：

- 输出 Maven 版本；
- 输出 Java 17；
- 不再出现 `Cannot start maven from wrapper`；
- 不再出现 `Cannot index into a null array`。

然后执行：

```powershell
.\mvnw.cmd test
```

如果 Wrapper 仍然失败：

1. 检查 `backend/.mvn/wrapper/maven-wrapper.properties` 是否存在；
2. 检查能否访问 Maven 依赖源；
3. 检查脚本是否被杀毒软件或 PowerShell 策略拦截；
4. 记录完整错误，不要只截最后一行；
5. 可以让 Agent 只诊断 Wrapper 启动错误，不要让 Agent顺便重写业务代码；
6. 修复前不能进入正式 Red/Green。

## A-4. 统一管理 pom.xml

准备阶段只有 A 修改 `backend/pom.xml`。

B 向 A 提交以下依赖需求：

```text
MyBatis Spring Boot Starter
Flyway Core
Flyway MySQL 支持
MySQL Connector/J
```

A 自己负责以下依赖或插件：

```text
Spring Web MVC
Bean Validation
Spring Modulith Core/Test
JUnit 5 / Spring 测试
Mockito
安全密码摘要所需组件
JWT 实现所需依赖（只安装，登录功能阶段再实现）
JaCoCo 覆盖率插件
```

操作规则：

1. 不凭记忆乱填版本；
2. 优先由 Spring Boot 的依赖管理提供版本；
3. 必须确认第三方 Starter 与 Spring Boot 4.1.1 兼容；
4. 每增加一组依赖就运行一次 Maven 测试；
5. 不要一次粘贴一个未经理解的巨大 `pom.xml`；
6. 不删除现有 Spring Modulith 测试依赖。

每次检查：

```powershell
.\mvnw.cmd test
```

成功标志：

```text
BUILD SUCCESS
```

## A-5. 创建公共技术目录

在 `backend/src/main/java/com/delivery/backend/` 下准备：

```text
web/
config/
security/
```

准备阶段允许在 `web/` 中实现：

```text
ApiResponse.java
PageResponse.java
ErrorCode.java
BusinessException.java
GlobalExceptionHandler.java
```

准备阶段允许在 `config/` 中放置公共配置，但数据库相关的 MyBatis 配置由 B 负责。

`security/` 在准备阶段只允许放置不会提前完成登录业务的基础契约或配置说明。JWT 签发、登录认证和用户上下文的真实行为放到旧手册 8.2，在 A 先写登录/JWT 测试后实现。

## A-6. 公共响应和异常也采用小步验证

A 不需要给每个 Getter 写测试，但至少要验证：

- 成功响应包含 `code/msg/data`；
- 成功业务码为 0；
- 分页包含 `items/page/pageSize/total/totalPages`；
- 参数错误能够映射为 HTTP 400；
- 业务冲突能够映射为 HTTP 409；
- 未知异常不会把堆栈返回给前端；
- 响应中不出现密码、数据库密码和 JWT 密钥。

建议测试目录：

```text
backend/src/test/java/com/delivery/backend/web/
├── ApiResponseTest.java
└── GlobalExceptionHandlerTest.java
```

公共组件测试可以由 A 自己执行一个小型 Red/Green：

```text
先写公共组件预期
→ 确认测试失败
→ 写最少公共实现
→ 确认通过
```

这不会取代第 8 章的业务 TDD，只是确保公共地基可靠。

## A-7. 准备后端测试基础

A 检查并保留：

```text
BackendApplicationTests.contextLoads
BackendApplicationTests.moduleBoundariesAreValid
```

准备创建：

```text
backend/src/test/java/com/delivery/backend/support/
```

这个目录以后可以放：

- MockMvc 公共配置；
- 测试用户构造工具；
- JSON 读取工具；
- 测试数据清理工具；
- 测试时钟；
- 通用断言。

准备阶段不要提前在这里放真实用户账号、真实手机号或真实密码。

## A-8. 统一架构和 API 文档冲突

A 负责修改文档，使以下内容完全一致：

1. 删除成功统一为 HTTP 204，无响应正文；
2. `restaurant` 只作为 Java 内部模块名，对外统一 `shop/shops`；
3. `item` 只作为 Java 内部模块名，对外统一 `product/products`；
4. 店铺临时关闭统一为 `TEMPORARILY_CLOSED`；
5. 订单状态保留 `DELIVERING`；
6. 第一阶段只有 `PENDING_PAYMENT` 可取消；
7. 后端模块内采用 `api/web/application/domain/infrastructure/persistence`；
8. 数据库表名为 `product_categories` 和 `order_items`；
9. 幂等键放在 `orders` 表；
10. 前端和后端登录统一调用 `/users/login`。

A 修改前必须执行：

```powershell
git diff
```

修改后搜索旧值：

```powershell
rg "/stores|/cart/items|/merchants/login|TEMP_CLOSED" docs frontend/src/api
```

如果搜索仍有结果，逐个判断是历史说明还是未修正契约。不要不看内容就批量替换整个仓库。

## A-9. A 的本地验收

A 在 `backend` 目录执行：

```powershell
.\mvnw.cmd test
```

如果配置了覆盖率，再执行项目确定的覆盖率命令，并确认报告能够生成。

A 检查：

- [ ] Java 是 17；
- [ ] Maven Wrapper 可以运行；
- [ ] `BUILD SUCCESS`；
- [ ] Modulith 边界测试通过；
- [ ] 公共响应测试通过；
- [ ] 全局异常测试通过；
- [ ] `pom.xml` 不包含无来源的任意版本；
- [ ] 没有用户注册、登录、商品、购物车或订单业务实现；
- [ ] 文档中的关键契约已经统一；
- [ ] 没有真实密码或密钥。

## A-10. A 提交和推送

在项目根目录执行：

```powershell
git status
git diff
git add backend/pom.xml backend/src/main backend/src/test docs/architecture docs/api
git status
git commit -m "chore(backend): prepare shared architecture and test foundation"
git push -u origin feature/a-preparation-foundation
```

PR 中写明：

```text
本 PR 只准备公共架构和测试基础，不实现业务用例。
测试命令：cd backend; .\mvnw.cmd test
测试结果：填写实际通过数量
重点检查：Java 17、依赖兼容、统一响应、异常映射、契约一致性
```

---

# 6. B 的闷头准备手册：数据库、Flyway 和 MyBatis 运行基础

> B 实际操作时，优先按照根目录的《B-业务开发前数据库准备-逐步执行手册.md》从头到尾执行。该手册把本章内容重新排成一条不需要来回跳转的线性流程，并包含当前 MySQL 26.7 环境、配置文件内容、验证命令、提交顺序和停止位置。

## B-0. B 的最终交付目标

B 完成后必须交付：

```text
MySQL 8 实际可连接
delivery_dev 与 delivery_test 完全分开
数据源只读取环境变量中的账号和密码
Flyway 能从空结构创建 8 张核心表
重复启动不会重复创建表
MyBatis 基础配置可被 Spring 加载
外键、唯一约束、CHECK、索引真实存在
数据库迁移日志和人工检查记录
B 能解释每张表和关键约束
```

B 在准备阶段不编写六个模块的完整 Mapper、Service 和 Controller。

## B-1. 创建 B 的准备分支

在项目根目录执行：

```powershell
git status
git switch develop
git pull --ff-only origin develop
git switch -c feature/b-database-runtime
git status --short --branch
```

注意：B 不编辑 `backend/pom.xml`。需要的依赖名称发给 A，由 A 统一加入。

## B-2. 验证 MySQL 和两个数据库

重新打开一个 PowerShell，执行：

```powershell
mysql --version
```

必须为 MySQL 8，且建议不低于 8.0.16，因为数据库设计使用有效 CHECK 约束。

检查四个环境变量是否存在：

```powershell
[Environment]::GetEnvironmentVariable("DELIVERY_DB_USERNAME", "User")
[Environment]::GetEnvironmentVariable("DELIVERY_DB_PASSWORD", "User")
[Environment]::GetEnvironmentVariable("DELIVERY_DB_URL", "User")
[Environment]::GetEnvironmentVariable("DELIVERY_JWT_SECRET", "User")
```

检查时只确认是否有值，不要截图或复制密码内容到群聊、文档和 GitHub。

登录项目数据库账号后执行：

```sql
SELECT VERSION();
SHOW DATABASES;
USE delivery_dev;
SELECT DATABASE();
USE delivery_test;
SELECT DATABASE();
```

成功标志：

- MySQL 版本正确；
- 两个数据库都存在；
- 项目账号可以切换到两个数据库；
- 没有使用 root 作为应用长期运行账号。

## B-3. 再读一次数据库设计并人工解释

B 打开 `docs/database/README.md`，逐张表用自己的话回答：

### users

- 为什么账号必须唯一；
- 为什么只保存 `password_hash`；
- `status` 和 `deleted` 有什么区别。

### merchants

- 为什么 `user_id` 唯一；
- 为什么商家是用户身份的扩展，而不是另一套登录账号。

### shops

- 怎样通过 `merchant_id` 找到店主；
- 为什么新店铺默认 `CLOSED`。

### product_categories

- 为什么分类属于某一个店铺；
- 为什么有效分类名称需要检查重复。

### products

- 为什么价格用 `DECIMAL(10,2)`；
- 为什么库存不能小于 0；
- `version` 如何防止多人同时修改。

### cart_items

- 为什么 `(user_id, product_id)` 必须唯一；
- 为什么购物车总价不能直接成为成交价。

### orders

- `order_number` 与数据库主键有什么区别；
- `idempotency_key` 怎样避免重复下单；
- 为什么订单不能物理删除。

### order_items

- 为什么必须保存商品名称和成交单价快照；
- 为什么查询历史订单不能用商品当前价格覆盖快照。

如果其中任何一项说不清，先查文档或让 Agent 用最简单的例子解释，再继续写 SQL。

## B-4. 创建 Flyway 目录和配置文件

准备目录：

```text
backend/src/main/resources/db/migration/
backend/src/test/resources/db/test-data/
```

建议配置文件：

```text
backend/src/main/resources/application.properties
backend/src/main/resources/application-dev.properties
backend/src/test/resources/application-test.properties
```

配置原则：

- 公共文件只放所有环境通用内容；
- 开发配置连接 `delivery_dev`；
- 测试配置连接 `delivery_test`；
- 用户名、密码和 JWT 密钥读取环境变量；
- 不提交 `.env`；
- 可以提交不含秘密值的 `.env.example`；
- 测试配置绝不能继承并连接 `delivery_dev`。

配置后执行：

```powershell
git diff -- backend/src/main/resources backend/src/test/resources
```

逐行检查是否出现真实密码。

## B-5. 编写 V1 建表迁移

创建：

```text
backend/src/main/resources/db/migration/V1__create_core_tables.sql
```

创建顺序必须先父表、后子表：

```text
1. users
2. merchants
3. shops
4. product_categories
5. products
6. cart_items
7. orders
8. order_items
```

V1 至少包含：

- 8 张表的主键；
- 用户账号唯一约束；
- `merchants.user_id` 唯一约束；
- 购物车 `user_id + product_id` 联合唯一约束；
- 订单编号唯一约束；
- 用户与幂等键的唯一约束；
- 所有必要外键；
- 金额大于或等于 0 的 CHECK；
- 商品价格大于 0 的 CHECK；
- 库存大于或等于 0 的 CHECK；
- 购物车和订单明细数量大于 0 的 CHECK；
- 状态字段；
- 创建和更新时间；
- 需要逻辑删除的 `deleted` 字段；
- 商品和订单版本字段；
- 列表和归属查询需要的索引；
- 订单明细的商品名称、单价、数量和小计快照。

禁止在 V1 中加入：

- 真实账号；
- 真实手机号；
- 真实密码；
- JWT 密钥；
- 个人电脑绝对路径；
- 与课程无关的第 9、第 10 张业务表。

## B-6. 配置 MyBatis 基础

B 只配置 MyBatis 能被 Spring 找到，不提前写业务 CRUD。

允许准备：

```text
com/delivery/backend/config/MyBatisConfig.java
backend/src/main/resources/mapper/
```

如果 `MyBatisConfig.java` 与 A 的 `config/` 工作发生冲突，B 保留数据库配置内容，合并时由 A 检查包位置，不要复制出两个重复配置类。

准备阶段可以确认：

- Mapper 扫描包路径正确；
- Mapper XML 路径正确；
- 下划线字段和 Java 驼峰字段的映射规则明确；
- 所有参数将使用 `#{...}`；
- 不会使用 `${...}` 拼接用户输入。

准备阶段不要创建：

```text
UserMapper
ShopMapper
ProductMapper
CartItemMapper
OrderMapper
```

这些文件在对应业务切片中，由 A 先写 Mapper/Service 测试后再逐个增加。

## B-7. 在测试库验证迁移

第一轮必须使用 `delivery_test`。

先确认当前配置确实指向测试库，再运行后端测试或启动 Flyway。不能只看文件名，必须确认实际 URL 中是 `delivery_test`。

迁移后进入 MySQL：

```sql
USE delivery_test;
SHOW TABLES;
SELECT * FROM flyway_schema_history;
```

必须看到：

```text
users
merchants
shops
product_categories
products
cart_items
orders
order_items
flyway_schema_history
```

再执行：

```sql
SHOW CREATE TABLE users;
SHOW CREATE TABLE products;
SHOW CREATE TABLE cart_items;
SHOW CREATE TABLE orders;
SHOW CREATE TABLE order_items;
```

检查唯一约束、CHECK、外键和索引是否真的存在。

如果迁移失败：

1. 记录失败版本和完整 SQL 错误；
2. 尚未合并且只有自己执行过时，可以修正 V1 后重新验证；
3. V1 一旦合并并被全组执行，不再修改 V1，只能增加 V2；
4. 不要手工修改 `flyway_schema_history`；
5. 不要在没确认数据库名称时执行删除命令。

## B-8. 在开发库验证迁移

测试库成功后，再使用开发配置连接 `delivery_dev`。

执行相同检查：

```sql
USE delivery_dev;
SHOW TABLES;
SELECT * FROM flyway_schema_history;
```

成功标志：

- 两个数据库结构一致；
- 两边都有一条成功的 V1 历史；
- 重复启动不会重复创建表；
- 没有把测试数据写进开发迁移。

## B-9. 做最小约束人工验证

准备阶段至少确认以下约束确实有效：

- 重复用户账号被拒绝；
- 不存在的用户不能创建商家记录；
- 商品价格小于等于 0 被拒绝；
- 商品库存小于 0 被拒绝；
- 同一用户同一商品的第二条购物车记录被拒绝；
- 重复订单号被拒绝；
- 订单明细可以保存名称和成交单价快照。

人工验证只使用专门的测试数据，不使用真实个人信息。

验证后清理测试数据时，只操作已经确认的 `delivery_test`。不要对不确定的数据库执行批量删除。

这些人工检查不取代后续自动化测试。第 8 章开始后，A 仍要在对应功能实现前编写 Mapper、Service 和事务测试。

## B-10. 保存数据库准备记录

新建或更新一份准备记录，至少写：

```text
执行日期
执行人 B
MySQL 版本
测试库名称
开发库名称
Flyway 版本
V1 执行结果
8 张表检查结果
约束检查结果
失败及处理方式
是否发现敏感信息
对应提交
```

不要把控制台中的密码复制到日志。

## B-11. B 的本地验收

B 逐项检查：

- [ ] MySQL 8 可以连接；
- [ ] `delivery_dev` 可以连接；
- [ ] `delivery_test` 可以连接；
- [ ] 两个数据库用途没有混淆；
- [ ] Flyway V1 能创建 8 张核心表；
- [ ] `flyway_schema_history` 正常；
- [ ] 关键唯一约束、CHECK、外键和索引存在；
- [ ] 配置文件没有真实密码；
- [ ] MyBatis 基础扫描配置正确；
- [ ] 没有提前创建六个模块的完整 Mapper；
- [ ] 没有提前实现任何业务 Service 或 Controller；
- [ ] B 能用自己的话解释 8 张表。

## B-12. B 提交和推送

```powershell
git status
git diff
git add backend/src/main/resources backend/src/test/resources backend/src/main/java/com/delivery/backend/config docs/database
git status
git commit -m "feat(database): add Flyway schema and database runtime configuration"
git push -u origin feature/b-database-runtime
```

PR 中写明：

```text
本 PR 只完成数据库和 MyBatis 运行基础，不实现业务 Mapper/Service/Controller。
测试库迁移：填写实际结果
开发库迁移：填写实际结果
创建表数量：8 张业务表 + flyway_schema_history
敏感信息检查：未提交真实用户名、密码和 JWT 密钥
```

---

# 7. C 的闷头准备手册：前端运行、契约和测试基础

## C-0. C 的最终交付目标

C 完成后必须交付：

```text
Node 和 npm 可以运行
npm ci 或 npm install 成功
前端可以构建和启动
Element Plus 图标依赖明确
Vitest 和 Vue 测试工具可以运行
Vite 开发代理或 API 基础地址明确
前端 API 路径与正式契约一致
现有静态页面被明确标注为占位，不假装业务已完成
README 更新为真实状态
```

C 在准备阶段不需要等待后端接口完成，但不能伪造“联调成功”。

## C-1. 创建 C 的准备分支

```powershell
git status
git switch develop
git pull --ff-only origin develop
git switch -c feature/c-frontend-readiness
git status --short --branch
```

## C-2. 验证 Node 和 npm

执行：

```powershell
node --version
npm --version
Get-Command node
Get-Command npm
```

成功标志：

- Node 使用当前 LTS 版本；
- npm 能显示版本；
- `Get-Command npm` 能找到实际路径。

如果 Node 有版本但 npm 找不到：

1. 检查 Node 安装是否完整；
2. 检查 Node 安装目录是否进入 Windows Path；
3. 关闭并重新打开 PowerShell；
4. 重新运行 `npm --version`；
5. 不要从不明网站单独下载一个 `npm.exe`。

## C-3. 安装并核对前端依赖

进入前端目录：

```powershell
cd frontend
npm ci
```

如果 `npm ci` 因锁文件与 `package.json` 不一致而失败，先记录错误，再确认确实需要更新依赖时使用：

```powershell
npm install
```

准备阶段需要明确安装：

```text
Vue 3
Vue Router
Pinia
Axios
Element Plus
@element-plus/icons-vue
Vite
Vitest
Vue Test Utils
测试 DOM 环境
覆盖率工具
```

安装运行时图标依赖可使用：

```powershell
npm install @element-plus/icons-vue
```

安装测试开发依赖时，根据项目采用的 Vitest 配置执行项目确定的安装命令。不得删除 `package-lock.json` 来掩盖依赖问题。

## C-4. 补充前端脚本

`frontend/package.json` 至少应当提供：

```text
npm run dev
npm run build
npm run test
npm run test:coverage
```

具体脚本由 C 使用当前安装的 Vitest 版本配置。

修改后执行：

```powershell
npm run build
npm run test
```

准备阶段至少要有一个最小测试，证明 Vitest 能找到并运行测试文件。

## C-5. 配置前端 API 基础地址

允许提交：

```text
frontend/.env.example
```

示例内容只能包含非秘密配置，例如：

```text
VITE_API_BASE_URL=/api/v1
```

不要在前端环境变量中放：

- 数据库密码；
- JWT 签名密钥；
- MySQL 用户名；
- 任何只允许后端知道的秘密。

`frontend/src/api/http.js` 保持统一 Axios 实例。所有业务 API 文件通过它请求，不直接重复创建 Axios。

如果使用 Vite 开发代理，统一把 `/api` 转发到本机后端端口，例如 `http://localhost:8080`。C 要能解释代理只是开发工具，不是后端接口本身。

## C-6. 修正前端 API 路径，但不提前完成业务页面

C 根据正式 API 文档检查：

### 用户

```text
POST /users
POST /users/login
GET /users/me
PATCH /users/me
POST /merchants
```

商家登录页面仍可以单独存在，但它也必须调用 `/users/login`，不能调用不存在的 `/merchants/login`。

### 店铺

```text
POST /shops
GET /shops
GET /shops/{shopId}
PATCH /shops/{shopId}
```

不再使用 `/stores` 和 `/stores/{id}/status`。

### 商品

```text
GET /shops/{shopId}/products
GET /products/{productId}
POST /products
PATCH /products/{productId}
```

### 购物车

```text
POST /cart-items
GET /cart-items
PATCH /cart-items/{cartItemId}
DELETE /cart-items/{cartItemId}
```

不再使用 `/cart` 和 `/cart/items`。

### 订单

```text
POST /orders
GET /orders
GET /orders/{orderId}
POST /orders/{orderId}/cancel
```

C 可以修正 `frontend/src/api/*.js` 中的路径和函数名称，但不要在准备阶段把所有页面改成假装已经联调完成。

## C-7. 固定前端业务字段名称

准备阶段只统一字段契约，不提交真实用户数据：

```text
username → account
store → shop
storeId → shopId
item → product
itemId → productId（购物车项自身 ID 仍叫 cartItemId）
TEMP_CLOSED → TEMPORARILY_CLOSED
```

注册页面后续需要的字段：

```text
account
password
passwordConfirm
nickname
phone
```

但注册按钮的真实调用和完整交互仍在旧手册 8.1 中完成，避免先业务、后测试。

## C-8. 准备前端测试目录

固定：

```text
frontend/src/tests/unit/
frontend/src/tests/integration/
```

准备阶段可以增加：

- 应用能挂载的冒烟测试；
- 路由配置能加载的测试；
- Axios 基础地址测试；
- API 包装函数是否使用正确 URL 的测试。

准备阶段暂不编写“注册成功”“下单成功”等假测试，因为后端真实业务尚未完成。

Pinia 已安装，但准备阶段不需要提前写完所有 Store。`auth`、`cart` Store 在对应业务切片中根据测试逐步实现。

## C-9. 标记现有页面的真实状态

C 检查当前页面：

```text
登录和注册按钮仍是占位行为
店铺、商品、购物车和订单使用示例数据
没有真实接口联调
没有真实登录状态
```

README 必须如实写：

```text
前端脚手架、路由和静态页面骨架已完成；真实业务接口联调未开始。
```

不能写成：

```text
用户、商品、购物车、订单功能已经完成。
```

## C-10. C 的本地验收

在 `frontend` 目录执行：

```powershell
npm ci
npm run build
npm run test
```

然后启动：

```powershell
npm run dev
```

浏览器检查：

- [ ] 首页能够打开；
- [ ] 普通用户布局能够打开；
- [ ] 商家布局能够打开；
- [ ] 控制台没有依赖找不到错误；
- [ ] 刷新子页面不会立即崩溃；
- [ ] 没有宣称后端接口已经联调；
- [ ] `/stores`、`/cart/items`、`/merchants/login` 已从正式 API 封装中清除；
- [ ] `npm run build` 成功；
- [ ] `npm run test` 成功；
- [ ] `.env` 和 `node_modules` 没有加入 Git。

## C-11. C 提交和推送

```powershell
git status
git diff
git add frontend README.md
git status
git commit -m "chore(frontend): prepare build api contract and test foundation"
git push -u origin feature/c-frontend-readiness
```

PR 中写明：

```text
本 PR 只准备前端运行和接口契约，不声称真实业务已联调。
构建命令：npm run build
测试命令：npm run test
结果：填写实际结果
重点检查：依赖、API 路径、环境变量、README 真实状态
```

---

# 8. 三个准备分支的合并顺序

三个人可以并行工作，但为了减少冲突，最终按以下顺序合并。

## 8.1 先合并 A 的公共依赖和架构基础

原因：B 的 Flyway/MyBatis 运行依赖 `pom.xml` 中的依赖。

合并前由 B 快速确认 MyBatis/Flyway 依赖存在，由 C 无需检查后端实现。

合并到：

```text
feature/a-preparation-foundation → develop
```

## 8.2 B 把最新 develop 合入自己的分支

A 合并后，B 执行：

```powershell
git switch feature/b-database-runtime
git fetch origin
git merge origin/develop
```

出现冲突时：

1. 不使用 `git reset --hard`；
2. 不覆盖 A 的 `pom.xml`；
3. 保留 B 的数据源和 Flyway 配置；
4. 重新运行后端测试和数据库迁移；
5. 确认成功后提交冲突解决结果。

然后合并：

```text
feature/b-database-runtime → develop
```

## 8.3 合并 C 的前端准备

C 在合并前更新自己的分支：

```powershell
git switch feature/c-frontend-readiness
git fetch origin
git merge origin/develop
npm run build
npm run test
```

然后合并：

```text
feature/c-frontend-readiness → develop
```

## 8.4 删除已合并的准备分支

确认 GitHub PR 已合并、提交已出现在 `develop` 后，才能删除准备分支。

删除分支不会删除已经合并的提交历史。

---

# 9. 三人联合准备验收

三个准备分支全部合并后，三个人都重新取得最新 `develop`。

```powershell
git switch develop
git pull --ff-only origin develop
git status --short --branch
```

应该看到：

```text
## develop...origin/develop
```

且下面没有 `M`、`??` 等未提交文件。

## 9.1 A 执行后端验收

```powershell
cd backend
java -version
.\mvnw.cmd -version
.\mvnw.cmd test
```

A 记录：

- Java 版本；
- Maven 版本；
- 测试总数；
- 通过数；
- 失败数；
- Modulith 边界结果；
- 测试时间；
- 当前提交号。

失败数必须为 0。

## 9.2 B 执行数据库验收

B 确认后端测试配置连接 `delivery_test`，再检查：

```sql
USE delivery_test;
SHOW TABLES;
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

B 再切换开发库检查：

```sql
USE delivery_dev;
SHOW TABLES;
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

两个数据库都必须迁移成功。

## 9.3 C 执行前端验收

```powershell
cd frontend
npm ci
npm run build
npm run test
```

C 记录：

- Node 版本；
- npm 版本；
- 构建结果；
- 测试总数；
- 通过数；
- 失败数；
- 当前提交号。

失败数必须为 0。

## 9.4 三人共同进行敏感信息检查

在项目根目录检查：

```powershell
git status
git diff --check
git ls-files
```

确认没有提交：

- `.env`；
- IDEA 个人配置；
- `node_modules`；
- `target`；
- 数据库真实密码；
- JWT 真实密钥；
- 真实个人测试账号；
- 包含密码的日志。

## 9.5 三人共同做文档一致性检查

三人逐项确认：

- [ ] API 全部使用 `/api/v1`；
- [ ] 店铺对外统一叫 `shop`；
- [ ] 商品对外统一叫 `product`；
- [ ] 购物车统一叫 `cart-items`；
- [ ] 商家登录也使用 `/users/login`；
- [ ] 新店铺为 `CLOSED`；
- [ ] 新商品为 `OFF_SALE`；
- [ ] 新订单为 `PENDING_PAYMENT`；
- [ ] 第一阶段只有待支付订单可取消；
- [ ] 删除接口统一使用 204；
- [ ] 分页字段一致；
- [ ] 数据库表名一致；
- [ ] 后端目录规则一致；
- [ ] README 没有把静态页面写成已完成功能。

---

# 10. “准备阶段完成”的唯一判定表

下面所有项目都打勾，才能宣布准备阶段结束。

## 10.1 A 的完成条件

- [ ] Java 17；
- [ ] Maven Wrapper 正常；
- [ ] 后端依赖完整；
- [ ] 统一响应可用；
- [ ] 分页对象可用；
- [ ] 错误码和业务异常可用；
- [ ] 全局异常处理可用；
- [ ] 参数校验基础可用；
- [ ] Modulith 边界测试通过；
- [ ] 公共组件测试通过；
- [ ] 覆盖率命令或入口准备好；
- [ ] 文档冲突已经统一；
- [ ] 没有提前实现真实业务。

## 10.2 B 的完成条件

- [ ] MySQL 8 可用；
- [ ] 开发库和测试库分开；
- [ ] 环境变量可读取；
- [ ] 数据源配置没有明文密码；
- [ ] Flyway 已配置；
- [ ] V1 创建 8 张业务表；
- [ ] 测试库迁移成功；
- [ ] 开发库迁移成功；
- [ ] 关键约束真实存在；
- [ ] MyBatis 基础配置可加载；
- [ ] 没有提前写完整业务 Mapper；
- [ ] 没有提前写 Service 和 Controller；
- [ ] B 能解释数据库设计。

## 10.3 C 的完成条件

- [ ] Node 可用；
- [ ] npm 可用；
- [ ] 依赖安装成功；
- [ ] Element Plus 图标依赖完整；
- [ ] `npm run build` 成功；
- [ ] Vitest 可运行；
- [ ] `npm run test` 成功；
- [ ] API 基础地址明确；
- [ ] 前端 API 路径与正式文档一致；
- [ ] `.env.example` 不含秘密；
- [ ] README 真实描述当前状态；
- [ ] 没有伪造接口联调完成。

## 10.4 团队共同完成条件

- [ ] 三个准备 PR 都已合并 `develop`；
- [ ] `develop` 与 `origin/develop` 同步；
- [ ] `develop` 工作区干净；
- [ ] 后端测试失败数为 0；
- [ ] 前端测试失败数为 0；
- [ ] 前端构建成功；
- [ ] 两个数据库迁移成功；
- [ ] Git 中没有秘密和构建产物；
- [ ] 三个人知道下一步是用户注册，而不是同时开发六个模块；
- [ ] 三个人理解测试提交必须早于业务实现提交。

只要有一项未完成，就在准备分支中修复，不进入旧手册第 8 章。

---

# 11. 准备完成后，怎样无缝进入旧版手册第 8.1

所有准备条件通过后，三个人正式开始第一个业务切片：用户注册。

## 11.1 A 先创建用户测试分支

```powershell
git switch develop
git pull --ff-only origin develop
git switch -c feature/a-test-user
```

A 先写：

```text
合法注册
空账号
空密码
确认密码不一致
密码强度错误
重复账号
数据库失败回滚
响应不含密码
```

A 运行测试并确认因为业务尚未实现而失败，记录 Red，然后提交：

```powershell
git add backend/src/test docs/test
git commit -m "test(user): add registration scenarios"
git push -u origin feature/a-test-user
```

注意：只有失败测试的分支暂时不要单独合并 `develop`。

## 11.2 B 从 A 的测试分支开始业务实现

B 不从旧的 `develop` 单独创建一个不含测试的分支，而是取得 A 的测试提交：

```powershell
git fetch origin
git switch -c feature/b-user origin/feature/a-test-user
```

这样 Git 历史天然是：

```text
A 的测试提交
→ B 的业务实现提交
```

B 此时才开始增加：

```text
RegisterUserRequest/Command
User Entity/PO
UserRepository
UserMapper
UserRegistrationService
UserController
BCrypt 密码摘要
账号重复异常
```

B 只实现让注册测试通过的最少代码，不顺便实现登录、店铺、商品或订单。

## 11.3 A、B 确认 Green 和重构

B 实现后运行测试。A 检查：

- 所有注册测试通过；
- 数据库只保存密码摘要；
- 响应没有密码；
- Controller 没有复杂业务；
- Mapper 没有业务判断；
- 测试库没有污染开发库。

然后提交：

```text
feat(user): implement registration
test(user): record registration green result
refactor(user): simplify registration flow
```

每次重构后重新运行测试。

## 11.4 C 接入注册页面

C 从包含可用注册接口的最新分支或 `develop` 开始：

```text
补 account/password/passwordConfirm/nickname/phone
调用 POST /users
增加加载状态
增加重复提交保护
显示字段错误
显示账号重复错误
注册成功后跳转登录
```

## 11.5 三人完成第一个真实闭环

共同验证：

```text
页面填写注册信息
→ Axios 发送请求
→ Controller 接收
→ Service 检查规则
→ Mapper 写入 delivery_dev
→ 数据库保存 password_hash
→ 返回 code/msg/data
→ 页面显示成功
```

再验证重复账号失败。

这一闭环通过后，再按照旧版手册第 8 章依次开发：

```text
登录和个人信息
→ 商家和店铺
→ 分类和商品
→ 购物车
→ 订单
```

---

# 12. 准备阶段常见错误处理

## 12.1 A 已经改了 pom，B 也改了 pom

处理：

1. 暂停合并；
2. B 把自己需要的依赖列表发给 A；
3. A 在自己的分支统一修改；
4. B 撤销自己尚未提交的重复修改时，只针对明确的 `pom.xml`，不要重置整个仓库；
5. A 合并后，B 再合入最新 `develop`。

## 12.2 测试连接到了 delivery_dev

立即停止测试。

检查：

- `application-test.properties`；
- 当前激活的 Spring Profile；
- 实际数据源 URL；
- 环境变量是否把开发 URL 覆盖到了测试配置。

确认连接 `delivery_test` 后才能继续。

## 12.3 Flyway V1 在一个人电脑成功，另一个人失败

依次比较：

- MySQL 版本；
- V1 文件校验值；
- 数据库是否真的是空结构；
- 是否有人手工创建过同名表；
- `flyway_schema_history` 内容；
- 字符集和排序规则。

不要直接删除别人的数据库，也不要手工伪造 Flyway 历史。

## 12.4 前端能启动但不能构建

开发服务器能打开不代表构建成功。

执行：

```powershell
npm run build
```

根据第一条真正的报错检查：

- 缺少依赖；
- 导入路径拼错；
- 文件名大小写；
- Vue 模板语法；
- 未定义变量。

## 12.5 页面显示示例数据，看起来像完成了

必须检查页面脚本是否仍有：

```javascript
const products = [
  // 示例商品
]
```

如果是写死数据，只能标为“页面骨架完成”，不能标为“商品功能完成”。

## 12.6 Agent 一次生成了整个后端

不要直接提交。

处理：

1. 只保留当前准备任务需要的文件；
2. 删除提前生成的业务 Controller、Service、Mapper；
3. 逐文件理解剩余代码；
4. 运行测试；
5. 在 AI 使用记录中写明“删除了提前生成的业务代码，只保留准备阶段所需内容”，不把未理解的代码混入提交。

## 12.7 develop 出现失败测试

准备阶段的 `develop` 必须保持绿色。

如果失败测试来自尚未实现的业务：

- 不应将它单独合并 `develop`；
- 应在功能分支链上由 B 完成实现；
- 测试与实现一起通过后再合并。

---

# 13. 每个人向团队发送的完成消息模板

## A 完成消息

```text
[A 准备完成]
分支：feature/a-preparation-foundation
提交：填写提交号
Java：17
Maven Wrapper：通过/失败
后端测试：总数 X，通过 X，失败 0
已完成：依赖、统一响应、异常、测试基础、架构/API 一致性
未实现：任何业务接口
需要 B 注意：数据源配置文件和 MyBatis 配置位置
需要 C 注意：最终 API 路径和删除响应
PR：填写链接
```

## B 完成消息

```text
[B 准备完成]
分支：feature/b-database-runtime
提交：填写提交号
MySQL：填写版本
测试库迁移：成功/失败
开发库迁移：成功/失败
业务表：8 张
Flyway 历史：V1 成功
约束检查：填写结果
敏感信息：未提交
未实现：业务 Mapper、Service、Controller
PR：填写链接
```

## C 完成消息

```text
[C 准备完成]
分支：feature/c-frontend-readiness
提交：填写提交号
Node：填写版本
npm：填写版本
前端构建：成功/失败
前端测试：总数 X，通过 X，失败 0
API 路径：已与正式契约统一
页面状态：仍是骨架，未伪报联调完成
敏感信息：未提交
PR：填写链接
```

---

# 14. 本手册结束点

当第 10 章全部打勾后，在开发日志中写入：

```text
准备阶段完成。
后端、数据库、前端和测试工具均已通过验收。
develop 为绿色且与远程同步。
团队从下一次提交开始执行旧版手册第 8.1“用户注册”，严格遵循测试 → Red → 实现 → Green → 重构 → 前端 → 联调 → 文档 → PR。
```

从这一刻开始：

- A 不再提前为所有模块一次性生成测试；
- B 不再提前为所有模块一次性生成实现；
- C 不再提前把所有页面伪接到不存在的接口；
- 三个人一次只完成一个可以演示、可以测试、可以合并的纵向业务切片。

第一个切片固定为：

```text
UC-USER-001 用户注册
```

完成用户注册后，才进入登录；完成登录后，才进入后续模块。
