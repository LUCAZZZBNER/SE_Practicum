# ABC 阶段 1 前后端联调、验收与交付执行文档

> 更新日期：2026-09-08
>
> 当前状态：B 已再次执行 `git pull --ff-only origin develop`，从 `2132dad` 快进到 `1168626 test: complete cart regression evidence`。本地与远程 `develop` 完全一致。最新前端 25 个测试文件、94 个测试全部通过，生产构建成功。`fix/api-alignment` 及其本地评审分支仍不参与本轮。
>
> 当前唯一下一阶段：R1 至 R3 和购物车回归测试已经完成，不再继续补同类前端代码。现在从第 9 节开始，在同一台电脑启动 MySQL、后端和前端，完成 30 个接口的真实联调、证据记录和最终验收。94 个 mock 单元测试全绿仍不能替代真实联调。

本文已经删除建 B 分支、保存初始 Red、创建 ServiceImpl 外壳、编写 V2、逐模块重复提交和反复 Push 等已完成步骤。那些内容可从 Git 历史查看，不再作为待办重复执行。

---

## 1. 什么文档说了算

发生冲突时按以下顺序处理：

1. 根目录课程要求 `26271学期-软件工程综合实践.md`；
2. `docs/software-requirements-specification.md`，简称 SRS，规定阶段 1 必须实现的业务；
3. `docs/api/backend-api-design.md`，规定 HTTP 路径、请求字段、响应字段、权限、状态码和错误码；
4. Controller、Service 接口及自动化测试，作为可执行契约；
5. `docs/architecture/backend-architecture-design.md`，规定四层结构、包边界和调用方向；
6. 本文只规定执行顺序，不能修改或缩小前面的需求。

任何人不得为了让页面或测试通过而擅自修改 SRS/API。发现测试与 SRS/API 相反时，由 A 判断并修正测试；发现实现与 SRS/API 不一致时，修正实现。

### 1.1 已冻结的关键结论

| 主题 | 阶段 1 唯一结论 |
| --- | --- |
| 用户和商家 | 两套独立账号、独立注册、独立登录 |
| 认证 | JWT Bearer：`Authorization: Bearer <accessToken>` |
| 店铺修改 | `PATCH /api/v1/shops/{shopId}` |
| 购物车 | `/api/v1/cart-items`，不是 `/cart` 或 `/cart/items` |
| 商品 | PATCH 必须携带当前 `version`，成功后版本递增 |
| 订单 | 创建必须携带 `X-Idempotency-Key` 和每项 `productVersion` |
| 取消订单 | 只允许 `PENDING_PAYMENT → CANCELLED`，库存只能恢复一次 |
| 数据库 | 保留 V1，通过 V2 对齐契约，禁止修改已发布的 V1 |
| 范围 | 30 个阶段 1 接口；不增加支付、退款、骑手、配送、优惠券和消息功能 |

---

## 2. 当前已经完成，不要重做

截至 2026-09-07，以下内容已经完成：

- Java 17、Spring Boot、Maven Wrapper、MyBatis、Flyway 和 MySQL Driver；
- 简单四层 MVC 结构；
- 6 个 Controller 和 30 个 HTTP 映射；
- 6 个 Service 接口和 6 个 ServiceImpl；
- User、Merchant、Restaurant、Item、Shopping、Order 的 Entity、DAO 和 Mapper XML；
- JWT Bearer、安全拦截器、统一响应及异常映射；
- V1、V2 在测试数据库中成功迁移；
- 用户/商家独立账号、分类逻辑删除、商品乐观锁、原子库存、购物车、订单幂等及取消事务；
- 后端 Controller、Service 契约和 DAO 集成测试全部通过；
- `feature/b-tdd` 已通过合并提交进入远程 `develop`。

2026-09-07，A/C 又将 `7b2274b`、`7b257f8`、`1168626` 三个提交推入 `develop`。B 已从 `2132dad` fast-forward 到 `1168626`。本轮修改 6 个前端文件，完成商品描述、目标购物车数量、下单防重复点击及购物车回归测试。第 5 节已经重新按 `1168626` 审计；此前针对 `83c3f93`、`3aa82ac`、`90fd50c` 或 `fix/api-alignment` 的待办不能代替本次结论。

后端代码当前基线是远程 `develop`。不要再次创建 V2、ServiceImpl 外壳或 `feature/b-tdd`，也不要再次制造最初的 26 个 Red。

### 2.1 当前四层架构

```text
HTTP 请求
  ↓
Controller：路径、参数校验、可信登录主体、响应包装
  ↓
Service / ServiceImpl：业务规则、权限、事务、跨模块编排
  ↓
DAO / MyBatis Mapper：SQL、分页、条件更新、记录映射
  ↓
MySQL：表、索引、唯一约束、外键、CHECK、Flyway 历史
```

每个业务模块内部拥有自己的 `controller/service/dao/entity`。Controller 不写业务规则，DAO 不调用其他模块；跨模块只调用对方 Service 接口。

---

## 3. 必须联调的 30 个接口

| 模块 | 数量 | 接口 |
| --- | ---: | --- |
| User | 4 | `POST /users`、`POST /users/login`、`GET/PATCH /users/me` |
| Merchant | 4 | `POST /merchants`、`POST /merchants/login`、`GET/PATCH /merchants/me` |
| Shop | 4 | `POST /shops`、`GET /shops`、`GET/PATCH /shops/{shopId}` |
| Category | 4 | `POST/GET /shops/{shopId}/categories`、`PATCH/DELETE /categories/{categoryId}` |
| Product | 4 | `POST /products`、`GET /shops/{shopId}/products`、`GET/PATCH /products/{productId}` |
| Cart | 4 | `POST/GET /cart-items`、`PATCH/DELETE /cart-items/{cartItemId}` |
| Order | 6 | `POST/GET /orders`、`GET /orders/{orderId}`、`POST /orders/{orderId}/cancel`、`GET /merchant/orders`、`GET /merchant/orders/{orderId}` |

表中路径均位于 `/api/v1` 下。准确请求和响应字段不得从本表猜测，必须查看 `docs/api/backend-api-design.md`。

---

## 4. 现在怎样继续 TDD

后端第一轮 TDD 已经完成。接下来对“前端接入”和“联调缺陷”继续使用：

```text
Red：先用测试或可复现记录证明旧路径、旧字段或缺陷确实存在
  ↓
Green：只修改让该契约通过的最少代码
  ↓
Refactor：自动测试仍全绿时整理重复代码和命名
```

为了减少无意义操作，本阶段不要求每个小按钮都单独提交。只保留三个有意义的节点：

1. A 提交一组前端接口契约 Red；
2. C 提交前端真实接入 Green；
3. 三人提交联调记录和最终验收结果。

若联调发现新的后端缺陷，则该缺陷单独保留一个 Red 和一个 Green；不能先改实现再补测试，也不能删除断言制造绿色。

---

## 5. `develop@1168626` 最新审计结论

### 5.1 本次拉取的三个提交

| 顺序 | 提交 | 实际作用 |
| ---: | --- | --- |
| 1 | `7b2274b test: add product and cart requirement red coverage` | 先加入商品描述、目标数量和防重复提交的失败测试 |
| 2 | `7b257f8 feat:add product and cart requirement red coverage` | 实际是 Green：修改三个页面让 Red 通过；提交标题写成了 red coverage，但代码内容没有问题 |
| 3 | `1168626 test: complete cart regression evidence` | 补齐删除、刷新和下单成功的回归断言 |

Git 顺序保留了 R1 至 R3 的 `Red → Green`。最后一个提交是对已经存在的购物车刷新实现补回归证据，因此直接 Green 是正常的，不要伪造失败记录。

### 5.2 自动验证结果

B 在当前 `develop@1168626` 实际执行：

```text
npm.cmd run test:run：25 个测试文件通过，94 个测试通过
npm.cmd run build：成功，1710 个模块完成生产构建
```

构建只有大于 500 kB 的 chunk 警告，不影响阶段 1，不做拆包或构建工具升级。MySQL267 当前为 Running，localhost:3306 端口可连接。

### 5.3 R1 至 R3 和回归测试全部完成

| 项目 | 最新证据 | 结论 |
| --- | --- | --- |
| R1 商品描述 | 商家表单能创建、载入和修改 `description`；商品详情展示描述；有新增与编辑测试 | 完成 |
| R2 目标购物车数量 | 每项有 1 至库存上限的数字输入，PATCH 发送用户选择的数量 | 完成 |
| R3 防重复点击 | `submitting` 请求锁和按钮 disabled 同时存在，finally 恢复 | 完成 |
| 删除回归 | 测试真正触发 ConfirmAction 的 confirm，并断言 `removeCartItem(id)` | 完成 |
| 刷新回归 | 修改、删除、下单成功后均断言再次 `getCart()` | 完成 |
| 下单反馈 | 测试断言“订单创建成功”提示；幂等重试测试仍保留 | 完成 |

上次的 P0-1 至 P0-5 也保持已修复。静态检查没有发现会阻止开始真实联调的新 API 契约错误。

### 5.4 现在真正还缺什么

当前缺的不是更多 mock 单元测试，而是以下真实证据：

1. MySQL、Spring Boot、Vite 同时运行时，浏览器是否能完成整个商家建店和用户下单故事；
2. 30 个接口是否真正经过 HTTP、JWT、安全拦截器、Service、DAO 和 MySQL；
3. 401、403、404、409、旧版本、库存不足、幂等冲突和重复取消是否返回规定结果；
4. Flyway V1/V2、后端完整测试、前端 94 项测试和构建结果是否进入最终测试日志；
5. 是否保存 Network 记录、关键页面截图和 Bug/复测证据。

因此现在直接从第 9 节开始，不再等 A 或 C 提交同类前端功能。

### 5.5 联调时重点观察、不要提前扩大开发范围

- `CustomerStoresView.vue` 的“搜索店铺或商品”输入框目前没有绑定，也不发送 `keyword`；它是无效控件。最小处理是联调后删除，或者只接入后端已有的店铺关键字查询，不能宣称能搜索商品。
- 店铺、用户订单、商家订单页面目前只请求固定第一页，没有翻页控件。SRS 写有分页查询；先用少量联调数据验证主流程，随后在最终验收前由 A/C 确认是否需要最小翻页控件，不要现在引入复杂表格框架。
- `/acceptance` 页面仍显示“页面骨架完成、待确认接口”等过期硬编码内容。它不影响业务接口，但演示前应更新为真实结果或从演示入口移除，避免误导验收者。
- CLOSED 店铺在列表中不能进入详情，而 SRS 允许查看指定店铺详情和状态。联调时记录验收意见；后端已经正确阻止闭店加入购物车和下单。
- Axios 与页面 catch 可能重复显示同一失败消息。只有真实联调复现重复提示后才修，不提前重构统一错误层。

---

## 6. 第一步：所有人只同步最新 `develop`

B 已经完成本步骤。A、C 在各自电脑执行相同命令：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git status --short
git fetch origin --prune
git switch develop
git pull --ff-only origin develop
git log -3 --oneline --decorate
git status --short
```

核对结果：

- `HEAD` 和 `origin/develop` 都是 `1168626`；
- 第一行日志是 `1168626 test: complete cart regression evidence`；
- 后面能看到 `7b257f8` Green 和 `7b2274b` Red；
- `git status --short` 没有输出；
- 不复制压缩包，不从 `fix/api-alignment` 取文件，不执行针对该分支的 merge、rebase 或 cherry-pick。

若 `git status --short` 显示个人未提交文件，先停止切分支，让文件所有者确认；不要用 `reset --hard` 或 `checkout --` 丢弃文件。

### 6.1 当前三个人是否还要互相等待

现在不需要再等任何人的前端 Push：

1. A 的 R1 至 R3 Red 已在 `7b2274b`；
2. C 的 Green 已在 `7b257f8`；
3. 购物车补充回归测试已在 `1168626`；
4. B 的 MySQL267 正在运行，3306 可连接；
5. 三人现在直接进入第 9、10 节真实联调。

30 个 Controller 和 B 的 ServiceImpl/DAO 已完成。只有真实联调证明后端有缺陷时，才由 A 补后端 Red、B 修 ServiceImpl/DAO/XML；不要因为前端显示问题直接修改数据库或后端。

### 6.2 B 现在立即执行的环境预检

这一步不需要前端代码再次 Push：

```powershell
Get-Service -Name 'MySQL267'
Test-NetConnection localhost -Port 3306

Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$env:JAVA_HOME = 'D:\Dev\Java\JDK17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
.\mvnw.cmd -version
```

若 `MySQL267` 是 `Stopped`，在“管理员 PowerShell”中执行：

```powershell
Start-Service -Name 'MySQL267'
```

普通 PowerShell 没有权限启动 Windows 服务，不要反复执行。若端口仍不通，先查看服务日志；不要修改 Flyway V1/V2 猜测解决。

B 还可以重新验证当前前端基线：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
node -v
npm.cmd -v
npm.cmd ci
npm.cmd run test:run
npm.cmd run build
```

若 PowerShell 拦截 `npm.ps1`，继续使用 `npm.cmd`。已经安装 Node.js 的电脑不再重复运行 `winget install`。`npm ci` 会依据仓库中的 `package-lock.json` 安装项目依赖，但每台电脑仍需自行安装 Node.js。

---

## 7. 已完成证据：A 的 Red 测试

提交 `7b2274b test: add product and cart requirement red coverage` 已经进入 `develop`，不再执行建分支、提交或 Push。它为下面三个缺失行为先写了会失败的测试：

1. 商品创建、编辑和详情必须处理 `description`；
2. 购物车修改数量必须提交用户选择的目标数量；
3. 创建订单请求未结束时必须禁用按钮，防止重复点击。

这三个 Red 都指向具体缺失行为，不是语法、导入或测试环境错误。它们保留在 Git 历史里，可作为“先写测试、后写实现”的 TDD 证据。

---

## 8. 已完成证据：C 的最小 Green 和购物车回归

提交 `7b257f8` 已完成三个最小实现，提交 `1168626` 已补齐购物车删除、刷新和下单成功提示的回归断言。当前结果如下：

- `MerchantProductsView.vue` 的商品表单、创建请求和修改请求都处理 `description`；
- `ProductDetailView.vue` 显示后端返回的商品描述；
- `CartView.vue` 使用数字输入保存目标数量；
- 创建订单期间按钮处于 loading/disabled，结束后恢复；
- 修改数量、删除商品、创建订单后都会重新读取购物车；
- 最新全量结果为 25 个测试文件、94 个测试全部通过，生产构建成功。

这些修改已经合入 `develop`，不要重复实现，也不要为了这一批已完成内容再次建分支。现在直接从第 9 节开始真实联调。

---

## 9. 现在执行：在同一台联调电脑上启动真实后端和前端

下面两个进程必须在同一台电脑上运行，因为 `localhost` 只代表当前电脑。B 和 C 如果使用不同电脑，C 的 `localhost:8080` 不能访问 B 的后端。

A 的 Red 和 C 的 Green 已进入远程 `develop`，B 也已拉取到 `1168626`。现在就在 B 的电脑同时启动后端和前端进行真实联调，不要再建立等待分支，也不要在未合并的评审分支上做最终验收。

### 9.1 联调机启动 MySQL 和后端

先查看 MySQL：

```powershell
Get-Service -Name 'MySQL267'
Test-NetConnection localhost -Port 3306
```

如果服务停止，必须在管理员 PowerShell 中执行：

```powershell
Start-Service -Name 'MySQL267'
```

然后在普通 PowerShell 中启动开发后端：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$env:JAVA_HOME = 'D:\Dev\Java\JDK17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DELIVERY_DB_USERNAME = 'delivery_app'
$env:DELIVERY_DB_PASSWORD = Read-Host '输入本机 delivery_app 密码' -MaskInput
$env:SPRING_PROFILES_ACTIVE = 'dev'
.\mvnw.cmd spring-boot:run
```

这个窗口保持运行。看到 `Started BackendApplication` 后，后端位于 `http://localhost:8080`。密码只存在当前进程，不写入 Git、文档或截图。

### 9.2 同一台联调机启动前端

另开一个普通 PowerShell：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd ci
npm.cmd run dev
```

浏览器打开 `http://localhost:5173`。浏览器 Network 面板中的业务请求必须到 `/api/v1/...`，并由开发代理转发到 8080。

---

## 10. 第五步：按一个完整故事验证全部 30 个接口

使用新的测试账号，按以下顺序人工操作。每一步记录“接口、预期、实际、是否通过”；关键节点保存截图。不要直接向数据库插入业务数据，因为那样不能证明接口可用。

### 10.1 商家准备商品

1. 商家注册：验证 `POST /merchants`；
2. 商家登录：验证 `POST /merchants/login`，保存 Merchant Token；
3. 查看商家资料：验证 `GET /merchants/me`；
4. 修改商家资料：验证 `PATCH /merchants/me`；
5. 创建店铺：验证 `POST /shops`，新店应为 `CLOSED`；
6. 查询本人店铺：验证 `GET /shops?mine=true`；
7. 查看店铺详情：验证 `GET /shops/{shopId}`；
8. 修改店铺并设为 `OPEN`：验证 `PATCH /shops/{shopId}`；
9. 创建将用于商品的分类：验证 `POST /shops/{shopId}/categories`；
10. 查询分类：验证 `GET /shops/{shopId}/categories`；
11. 修改分类：验证 `PATCH /categories/{categoryId}`；
12. 另外创建一个空分类并删除：验证 `DELETE /categories/{categoryId}`；不要删除已有商品引用的分类；
13. 创建商品：验证 `POST /products`，初始状态应为 `OFF_SALE`；
14. 按店铺查询商品：验证 `GET /shops/{shopId}/products?includeOffSale=true`；
15. 查看商品详情：验证 `GET /products/{productId}`；
16. 携带当前 version 把商品改为 `ON_SALE`：验证 `PATCH /products/{productId}`，返回的新 version 应递增。

### 10.2 用户完成一次下单和取消

17. 用户注册：验证 `POST /users`；
18. 用户登录：验证 `POST /users/login`，保存 User Token；
19. 查看用户资料：验证 `GET /users/me`；
20. 修改用户资料：验证 `PATCH /users/me`；
21. 浏览营业店铺：验证公开 `GET /shops`；
22. 浏览该店商品：再次验证公开商品列表只显示 `ON_SALE`；
23. 加入购物车：验证 `POST /cart-items`；
24. 查询购物车：验证 `GET /cart-items`，金额来自当前商品价格；
25. 修改数量：验证 `PATCH /cart-items/{cartItemId}`；
26. 为验证删除，可删除后重新加入：验证 `DELETE /cart-items/{cartItemId}`；
27. 使用购物车返回的 `product.version` 和新的幂等键创建订单：验证 `POST /orders`；
28. 查询用户订单列表和详情：验证 `GET /orders`、`GET /orders/{orderId}`；
29. 取消待支付订单：验证 `POST /orders/{orderId}/cancel`，状态应为 `CANCELLED`；
30. 切回 Merchant Token，查询商家订单列表和详情：验证 `GET /merchant/orders`、`GET /merchant/orders/{orderId}`。

步骤数量是操作顺序，不是接口计数；其中部分步骤同时验证同一模块的列表和详情。最终必须在记录表中逐项覆盖第 3 节的全部 30 个唯一接口。

### 10.3 必须补的失败场景

最少人工验证以下失败，不需要增加新功能：

- 不带 Token 访问受保护接口得到 401；
- User Token 调用商家接口或 Merchant Token 调用用户接口得到 403；
- 查询别人的资源不能泄露数据；
- 重复用户/商家账号和同店同名分类得到 409；
- 使用旧 product version 更新商品得到 409；
- 库存不足不能创建订单；
- 同一订单请求使用相同幂等键和相同 body 返回原订单；
- 相同幂等键配不同 body 得到 `IDEMPOTENCY_CONFLICT`；
- 第二次取消同一订单得到 `ORDER_STATE_CONFLICT`，库存不能再次增加。

---

## 11. 联调发现问题时三个人怎么处理

1. C 保存浏览器 Network 中的请求方法、URL、请求体、状态码和响应体，隐藏 Token；
2. A 对照 SRS/API 判断是前端错误、后端错误还是测试错误；
3. 如果是前端错误，A/C 先建立失败测试，C 修复；
4. 如果是后端错误，A 先建立最小失败测试并提交 Red，B 从最新 `origin/develop` 建 `fix/b-integration` 修复；
5. B 只修改导致缺陷的 ServiceImpl/DAO/XML，不扩大需求；
6. 修复后同时运行目标测试和完整后端测试；C 重新验证原请求；
7. 把根因、修复提交号和复测结果追加到 `docs/test/test-log.md`。

以下情况不是修改后端的理由：页面仍调用旧路径、页面字段名写错、Token 未发送、MySQL 没启动或 Vite 没配置代理。

---

## 12. 最终自动化验收

### 12.1 后端

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$env:SPRING_PROFILES_ACTIVE = 'test'
$env:DELIVERY_DB_USERNAME = 'delivery_app'
$env:DELIVERY_DB_PASSWORD = Read-Host '输入本机 delivery_app 密码' -MaskInput
.\mvnw.cmd clean test
```

要求：`BUILD SUCCESS`，Failures、Errors、Skipped 都为 0，并生成 `backend/target/site/jacoco/index.html`。

确认 Flyway：

```powershell
mysql -u delivery_app -p delivery_test -e "SELECT installed_rank, version, script, success FROM flyway_schema_history ORDER BY installed_rank;"
```

必须看到 V1、V2 各一条且 `success=1`。

### 12.2 前端

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd ci
npm.cmd run test:run
npm.cmd run build
```

要求：全部 Vitest 测试通过且 Vite 构建成功。当前已经有前端测试体系，不再额外引入另一套大型框架。

### 12.3 证据

至少保留：

- 后端完整测试汇总；
- 前端测试和构建汇总；
- Flyway V1/V2 查询结果；
- 30 接口联调清单；
- 商家建店/上架商品、用户下单/取消、商家查看订单的关键截图；
- 实际出现过的 Bug、Red、Green 和复测记录。

---

## 13. 真实联调发现缺陷后怎样交付

如果第 9、10 节没有发现代码缺陷，不创建空分支，直接补齐测试日志和验收材料。如果发现缺陷：

1. 先记录请求、响应、复现步骤和责任层；
2. A 为确认的缺陷补最小失败测试；
3. 前端缺陷由 C 修，ServiceImpl/DAO/XML 缺陷由 B 修；
4. 分支名按责任使用 `fix/c-integration-<简短问题>` 或 `fix/b-integration-<简短问题>`；
5. 目标测试、前后端全量测试和原失败请求复测都通过后，才合入 `develop`。

合入后的最终操作：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git switch develop
git pull --ff-only origin develop
git status --short
```

然后执行第 12 节最终验收并推送最后的测试日志/验收材料。只有远程 `develop` 包含全部代码和证据、工作区干净，阶段 1 才算完成。个人修复分支等合并成功后再删除。

---

## 14. 每台新电脑仍需自行准备

Git 会同步源码、Maven Wrapper、`pom.xml`、`package-lock.json` 和迁移 SQL，但不会安装本机软件或同步密码。

| 工具 | 是否随 Git 提供 | 要求 |
| --- | --- | --- |
| Git | 否 | `git --version` 可用 |
| JDK 17 | 否 | `java -version` 和 Maven 都显示 17 |
| Maven | Wrapper 提供 | 不用全局安装；首次使用需要联网下载 |
| MySQL Server | 否 | 本机 3306，创建 `delivery_dev`、`delivery_test` 和应用账号 |
| Node.js/npm | 否 | 安装 Node LTS，`node -v`、`npm -v` 可用 |
| IDEA/VS Code | 否 | 可选 |
| `rg` | 否 | 可选，不影响构建 |

真实密码只通过当前终端的 `DELIVERY_DB_PASSWORD` 提供。Flyway 能自动建表和升级表，不能替团队成员安装 MySQL、创建数据库或同步密码。

---

## 15. 最终停止条件

- [x] 后端四层实现完成并合入远程 `develop`；
- [x] V1/V2 和后端完整测试通过；
- [x] P0-1 至 P0-5 的实现修复已进入远程 `develop`；
- [x] 购物车删除、刷新和下单成功已有有效回归测试；
- [x] R1 商品描述、R2 目标数量、R3 防重复点击已按 Red/Green 完成；
- [x] 30 个前端 API 的方法和路径已有自动契约覆盖；
- [x] 当前 `develop@1168626` 的 25 个测试文件、94 个测试和生产构建通过；
- [ ] 30 个接口真实联调全部通过；
- [ ] 必要的 401/403/404/409、乐观锁、幂等和重复取消已验证；
- [ ] 测试日志、Bug 记录和关键截图真实可追踪；
- [ ] A、B、C 的最终代码都已合入并推送远程 `develop`；
- [ ] 三个人从最新 `develop` 能按第 9 节启动并复现核心流程。

全部勾选后停止增加功能，进入课程报告、个人总结、演示脚本和答辩准备。
