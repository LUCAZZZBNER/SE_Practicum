# ABC 阶段 1 前后端联调、验收与交付执行文档

> 更新日期：2026-09-08
>
> 当前状态：B 已执行 `git pull --ff-only origin develop`，从 `2203eea` 快进到 `312b6fd fix(frontend): clear auth state on logout [GREEN]`。本地代码与远程 `develop` 一致；三份联调文档仍保留为本地未提交修改。最新前端25个测试文件、95个测试全部通过，生产构建成功。
>
> 当前唯一下一阶段：30个接口正常与异常联调均已通过，BUG-FE-001的Red、Green、自动测试和浏览器复测也已完成。现在直接执行第12节最终自动化验收、补齐证据并统一提交。

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

| 主题       | 阶段 1 唯一结论                                                  |
| ---------- | ---------------------------------------------------------------- |
| 用户和商家 | 两套独立账号、独立注册、独立登录                                 |
| 认证       | JWT Bearer：`Authorization: Bearer <accessToken>`              |
| 店铺修改   | `PATCH /api/v1/shops/{shopId}`                                 |
| 购物车     | `/api/v1/cart-items`，不是 `/cart` 或 `/cart/items`        |
| 商品       | PATCH 必须携带当前`version`，成功后版本递增                    |
| 订单       | 创建必须携带`X-Idempotency-Key` 和每项 `productVersion`      |
| 取消订单   | 只允许`PENDING_PAYMENT → CANCELLED`，库存只能恢复一次         |
| 数据库     | 保留 V1，通过 V2 对齐契约，禁止修改已发布的 V1                   |
| 范围       | 30 个阶段 1 接口；不增加支付、退款、骑手、配送、优惠券和消息功能 |

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

| 模块     | 数量 | 接口                                                                                                                                             |
| -------- | ---: | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| User     |    4 | `POST /users`、`POST /users/login`、`GET/PATCH /users/me`                                                                                  |
| Merchant |    4 | `POST /merchants`、`POST /merchants/login`、`GET/PATCH /merchants/me`                                                                      |
| Shop     |    4 | `POST /shops`、`GET /shops`、`GET/PATCH /shops/{shopId}`                                                                                   |
| Category |    4 | `POST/GET /shops/{shopId}/categories`、`PATCH/DELETE /categories/{categoryId}`                                                               |
| Product  |    4 | `POST /products`、`GET /shops/{shopId}/products`、`GET/PATCH /products/{productId}`                                                        |
| Cart     |    4 | `POST/GET /cart-items`、`PATCH/DELETE /cart-items/{cartItemId}`                                                                              |
| Order    |    6 | `POST/GET /orders`、`GET /orders/{orderId}`、`POST /orders/{orderId}/cancel`、`GET /merchant/orders`、`GET /merchant/orders/{orderId}` |

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

## 5. `develop@312b6fd` 最新审计结论

### 5.1 已验证的最新提交

| 顺序 | 提交                                                            | 实际作用                                                                               |
| ---: | --------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
|    1 | `7b2274b test: add product and cart requirement red coverage` | 先加入商品描述、目标数量和防重复提交的失败测试                                         |
|    2 | `7b257f8 feat:add product and cart requirement red coverage`  | 实际是 Green：修改三个页面让 Red 通过；提交标题写成了 red coverage，但代码内容没有问题 |
|    3 | `1168626 test: complete cart regression evidence`             | 补齐删除、刷新和下单成功的回归断言                                                     |
|    4 | `b607c38 fix(frontend): label product price and stock fields` | 给商品价格、库存输入框补明确标签和对应测试 |
|    5 | `77e3226 test(frontend): cover logout behavior [RED]` | 正式保存退出不清登录状态的失败测试 |
|    6 | `312b6fd fix(frontend): clear auth state on logout [GREEN]` | 清除Token和角色并返回首页 |

Git 顺序保留了 R1 至 R3 的 `Red → Green`。最后一个提交是对已经存在的购物车刷新实现补回归证据，因此直接 Green 是正常的，不要伪造失败记录。

### 5.2 自动验证结果

B 在当前 `develop@312b6fd` 实际执行：

```text
npm.cmd run test:run -- AppHeader.spec.js：2 个测试通过
npm.cmd run test:run：25 个测试文件通过，95 个测试通过
npm.cmd run build：成功，1710 个模块完成生产构建
```

构建只有大于 500 kB 的 chunk 警告，不影响阶段 1，不做拆包或构建工具升级。MySQL267 当前为 Running，localhost:3306 端口可连接。

### 5.3 R1 至 R3 和回归测试全部完成

| 项目              | 最新证据                                                                      | 结论 |
| ----------------- | ----------------------------------------------------------------------------- | ---- |
| R1 商品描述       | 商家表单能创建、载入和修改`description`；商品详情展示描述；有新增与编辑测试 | 完成 |
| R2 目标购物车数量 | 每项有 1 至库存上限的数字输入，PATCH 发送用户选择的数量                       | 完成 |
| R3 防重复点击     | `submitting` 请求锁和按钮 disabled 同时存在，finally 恢复                   | 完成 |
| 删除回归          | 测试真正触发 ConfirmAction 的 confirm，并断言`removeCartItem(id)`           | 完成 |
| 刷新回归          | 修改、删除、下单成功后均断言再次`getCart()`                                 | 完成 |
| 下单反馈          | 测试断言“订单创建成功”提示；幂等重试测试仍保留                              | 完成 |

上次的 P0-1 至 P0-5 也保持已修复。静态检查没有发现会阻止开始真实联调的新 API 契约错误。

### 5.4 现在真正还缺什么

正常业务、异常业务、前端自动测试、后端最终完整测试和 Flyway 确认均已通过。当前代码与自动化验收已经完成；本轮只需把最终结果写入日志并将三份本地文档统一提交、推送。仓库中尚未发现联调关键截图，因此课程最终交付前仍需按第12.3节补齐最少截图；A、C 还应各自在最新 `develop` 上完成一次拉取和启动确认。

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

- `HEAD` 和 `origin/develop` 都是 `312b6fd`；
- 第一行日志是 `312b6fd fix(frontend): clear auth state on logout [GREEN]`；
- 后面能看到 `77e3226` Red 和 `b607c38` 商品标签修复；
- `git status --short` 没有输出；
- 不复制压缩包，不从 `fix/api-alignment` 取文件，不执行针对该分支的 merge、rebase 或 cherry-pick。

若 `git status --short` 显示个人未提交文件，先停止切分支，让文件所有者确认；不要用 `reset --hard` 或 `checkout --` 丢弃文件。

### 6.1 当前三个人是否还要互相等待

现在不需要再等任何人的前端 Push：

1. A 的 R1 至 R3 Red 已在 `7b2274b`；
2. C 的 Green 已在 `7b257f8`；
3. 购物车补充回归测试已在 `1168626`；
4. B 的 MySQL267 正在运行，3306 可连接；
5. 正常和异常联调已经完成，现在执行10.7的浏览器复测和第12节最终验收。

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
$dbCredential = Get-Credential -UserName 'delivery_app' -Message '输入本机 delivery_app 数据库密码'
$env:DELIVERY_DB_PASSWORD = $dbCredential.GetNetworkCredential().Password
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

本节不是继续写代码，而是用全新的商家和用户数据，通过前端页面完成一次真实业务流程，证明请求确实经过 `前端 → HTTP → Controller → ServiceImpl → DAO → MySQL`。不要直接向数据库插入业务数据，因为那不能证明接口可用。

> 2026-09-08 进度：B 已在本机完成 10.3.1 至 10.4.8，30 个接口正常路径全部通过；10.5 的认证、角色、资源归属、冲突、乐观锁、库存、幂等和订单状态异常也全部通过。唯一发现的问题是退出只跳回首页、不清除本地登录状态，已登记为 `BUG-FE-001`。下一步先修复该缺陷，再执行第 12 节最终自动化验收，不重复本节联调。

### 10.1 先准备浏览器和记录表

1. 确认 MySQL、后端和前端仍在运行；后端窗口已经出现 `Started BackendApplication`。
2. 浏览器打开 `http://localhost:5173`。
3. 按 `F12` 打开开发者工具，选择 `Network`，再选择 `Fetch/XHR`。
4. 勾选 `Preserve log`，使页面跳转后请求不会消失。
5. 点击 Network 的清除按钮，清掉启动阶段的旧请求。
6. 每做一步，就在 Network 中点击对应请求，依次查看 `Headers`、`Payload` 和 `Response`。

普通成功请求必须同时满足：

- 新建资源通常是 HTTP `201`；登录、查询、修改和删除通常是 HTTP `200`；
- Response JSON 的 `code` 为 `0`；
- `data` 中的 ID、字段、状态和刚才的操作一致；
- 刷新页面后数据仍存在，证明不是前端临时假数据；
- 后端控制台没有异常堆栈或新的 `ERROR`。

每个接口都追加到 `docs/test/test-log.md`，推荐使用下面的表：

| 编号 | 页面操作 | 方法和路径                 | HTTP | code | 页面/数据结果             | 结论 |
| ---: | -------- | -------------------------- | ---: | ---: | ------------------------- | ---- |
|    1 | 商家注册 | `POST /api/v1/merchants` |  201 |    0 | 返回新商家且状态为 ACTIVE | 通过 |

截图中不得出现完整 `Authorization`、`accessToken`、数据库密码。正常流程尽量连续完成，中途失败时按 10.6 停止并留证。

### 10.2 准备一套不会重复的测试数据

第一次可以使用：

```text
商家账号：merchant090801
用户账号：customer090801
统一密码：ExamplePass123!
商家手机号：13900000001
用户手机号：13800000001
店铺名称：联调测试餐厅0908
店铺简介：用于阶段1联调
正式分类：主食
待删除分类：待删除分类
商品名称：招牌牛肉饭
商品描述：联调测试商品
价格：18.8
库存：20
```

如果这些账号或店铺名已经使用过，把末尾的 `01` 改成 `02`、`03`，不要删除旧数据库记录来强行复用。测试过程中记录实际的 `shopId`、`categoryId`、`productId`、`cartItemId`、`product.version` 和 `orderId`。

### 10.3 商家准备店铺和商品

#### 10.3.1 商家注册

首页点击“商家注册”，填写 10.2 的商家数据并点击“提交注册”。检查：

```text
POST /api/v1/merchants
```

成功标准：HTTP `201`、`code = 0`；`data.account` 为新账号；`data.status = ACTIVE`；响应中没有密码；页面跳转到商家登录页。

#### 10.3.2 商家登录

输入新商家账号和密码，点击“登录”。检查：

```text
POST /api/v1/merchants/login
```

成功标准：HTTP `200`、`code = 0`；`data.accessToken` 有值；`data.roles` 包含 `MERCHANT`；页面进入“店铺管理”。Token 只在浏览器中使用，不复制进日志。

#### 10.3.3 查看和修改商家资料

进入左侧“个人信息”。页面应自动发出：

```text
GET /api/v1/merchants/me
```

成功标准：HTTP `200`、`code = 0`，账号、商家名称、手机号和状态正确。修改名称或手机号后点击“保存修改”，检查：

```text
PATCH /api/v1/merchants/me
```

成功标准：HTTP `200`、`code = 0`；Response 返回新值；刷新页面后新值仍存在。

#### 10.3.4 创建并查询本人店铺

进入“店铺管理”，填写店铺名称和简介，点击“创建店铺”。检查：

```text
POST /api/v1/shops
```

成功标准：HTTP `201`、`code = 0`；返回 `shopId`；名称和简介正确；初始 `status` 必须为 `CLOSED`。随后检查页面自动产生的：

```text
GET /api/v1/shops?mine=true...
GET /api/v1/shops/{shopId}
```

两个请求都应为 HTTP `200`、`code = 0`，并返回刚创建的店铺。

#### 10.3.5 把店铺设为营业

选择“营业”，点击“保存店铺”，检查：

```text
PATCH /api/v1/shops/{shopId}
```

成功标准：HTTP `200`、`code = 0`；返回 `status = OPEN`；刷新后仍显示营业。若不设为 `OPEN`，用户加入购物车和下单应被后端拒绝。

#### 10.3.6 创建、查询、修改和删除分类

进入“商品管理”，创建正式分类“主食”，检查：

```text
POST /api/v1/shops/{shopId}/categories
GET  /api/v1/shops/{shopId}/categories
```

成功标准：POST 为 HTTP `201`，GET 为 HTTP `200`，二者 `code = 0`；分类有 ID；页面列表出现“主食”。把它改名为“热销主食”并点击“保存分类”，检查：

```text
PATCH /api/v1/categories/{categoryId}
```

成功标准：HTTP `200`、`code = 0`；返回名称为“热销主食”；刷新后仍存在。

再创建“待删除分类”，确认它还没有商品引用，然后点击删除并确认，检查：

```text
DELETE /api/v1/categories/{categoryId}
```

成功标准：HTTP `200`、`code = 0`、`data.deleted = true`；页面不再显示该分类。不要删除马上要被商品使用的“热销主食”。

#### 10.3.7 创建、查询和上架商品

在“商品管理”填写分类、名称、描述、价格 `18.8` 和库存 `20`，点击“新增商品”。检查：

```text
POST /api/v1/products
```

成功标准：HTTP `201`、`code = 0`；商品有 ID；名称、描述、价格和库存正确；初始 `status = OFF_SALE`；记录当前 `version`，通常为 1。

商品管理页还应产生：

```text
GET /api/v1/shops/{shopId}/products?includeOffSale=true...
GET /api/v1/products/{productId}
```

成功标准：HTTP `200`、`code = 0`；店主能看到刚创建的下架商品及其描述。点击“上架”并确认，检查：

```text
PATCH /api/v1/products/{productId}
```

Payload 必须携带当前 `version`。成功标准：HTTP `200`、`code = 0`；`status = ON_SALE`；返回的新 `version` 比修改前增加；刷新后仍为上架状态。

### 10.4 用户完成购物车、下单和取消

#### 10.4.1 用户注册和登录

返回首页，点击“用户注册”，填写 10.2 的用户数据并提交。检查：

```text
POST /api/v1/users
```

成功标准：HTTP `201`、`code = 0`；账号正确；`status = ACTIVE`；响应无密码。随后登录并检查：

```text
POST /api/v1/users/login
```

成功标准：HTTP `200`、`code = 0`；`data.accessToken` 有值；`data.roles` 包含 `USER`；页面进入店铺列表。新登录会覆盖浏览器中的商家 Token，不要手工修改 Token。

#### 10.4.2 查看和修改用户资料

进入“个人信息”，检查：

```text
GET   /api/v1/users/me
PATCH /api/v1/users/me
```

成功标准：两个请求均为 HTTP `200`、`code = 0`；页面显示当前账号；修改昵称或手机号后返回新值；刷新后修改仍存在。

#### 10.4.3 浏览营业店铺和上架商品

进入“浏览店铺”，检查：

```text
GET /api/v1/shops
```

成功标准：HTTP `200`、`code = 0`；能看到刚才创建的 `OPEN` 店铺。点击“进入店铺”，检查：

```text
GET /api/v1/shops/{shopId}
GET /api/v1/shops/{shopId}/categories
GET /api/v1/shops/{shopId}/products
```

成功标准：三个请求均为 HTTP `200`、`code = 0`；页面能看到“招牌牛肉饭”、描述、价格和库存；普通用户列表只显示 `ON_SALE` 商品。

#### 10.4.4 加入和查询购物车

点击“加入购物车”，检查：

```text
POST /api/v1/cart-items
```

成功标准：首次加入为 HTTP `201`，重复加入也可能为 `200`；`code = 0`；`quantity` 正确；`available = true`；`subtotal = price × quantity`。进入“购物车”，检查：

```text
GET /api/v1/cart-items
```

成功标准：HTTP `200`、`code = 0`；商品存在；单项小计和总价计算正确。

#### 10.4.5 修改和删除购物车项

把数量改成 `2`，点击“修改数量”，检查：

```text
PATCH /api/v1/cart-items/{cartItemId}
```

成功标准：HTTP `200`、`code = 0`；`quantity = 2`；若单价为 18.8，则 `subtotal = 37.6`；修改后页面再次发送 `GET /cart-items` 并显示新数量。

点击“删除”并在确认框中确认，检查：

```text
DELETE /api/v1/cart-items/{cartItemId}
```

成功标准：HTTP `200`、`code = 0`、`data.deleted = true`；随后再次出现 `GET /cart-items`；页面中该项消失。完成删除验证后回店铺重新加入商品，否则无法继续创建订单。

#### 10.4.6 创建订单

购物车至少有一个有效商品时点击“创建订单”，检查：

```text
POST /api/v1/orders
```

在 Request Headers 中确认存在 `X-Idempotency-Key`；Payload 的每项包含实际 `cartItemId` 和购物车返回的 `productVersion`。成功标准：

- HTTP `201`、`code = 0`；
- 返回 `orderId` 和 `orderNumber`；
- `status = PENDING_PAYMENT`；
- 店铺、商品、数量和总金额正确；
- 请求未结束时“创建订单”按钮不可重复点击；
- 成功后显示提示并再次读取购物车。

记录本次请求的幂等键，但截图和日志不要记录 Authorization。

#### 10.4.7 查询和取消用户订单

进入“我的订单”，点击刚创建的订单，检查：

```text
GET /api/v1/orders
GET /api/v1/orders/{orderId}
```

成功标准：两个请求均为 HTTP `200`、`code = 0`；订单号、商品、数量和金额正确；状态为 `PENDING_PAYMENT`。点击“取消订单”并检查：

```text
POST /api/v1/orders/{orderId}/cancel
```

成功标准：HTTP `200`、`code = 0`；状态变成 `CANCELLED`；刷新后仍为 `CANCELLED`。

#### 10.4.8 商家查看用户订单

返回首页，用原商家账号重新登录，进入“订单管理”，检查：

```text
GET /api/v1/merchant/orders
GET /api/v1/merchant/orders/{orderId}
```

成功标准：两个请求均为 HTTP `200`、`code = 0`；商家只能看到自己店铺的订单；刚才订单的商品、数量、金额与用户端一致，状态为 `CANCELLED`。

操作步骤数量不等于唯一接口数量，因为一个页面动作可能自动产生列表和详情两个请求。最终记录表必须逐项覆盖第 3 节全部 30 个唯一接口。

### 10.5 已完成记录：异常场景验证

> 2026-09-08，以下场景已经全部得到预期结果并写入 `docs/test/test-log.md`。本节命令只保留为可复现证据，不要立即重复执行。以后只有修复相关缺陷或验收者要求复测时才使用。

异常测试分成两部分：10.5.1 至 10.5.3 直接通过页面完成；10.5.4 起在一个新的 PowerShell 窗口运行请求。所有命令都请求本机后端 `http://localhost:8080`，运行前必须保持 MySQL 和后端在线。

#### 10.5.1 页面测试：重复商家账号

先清除当前登录状态：

1. 按 `F12`；
2. 打开 `Application`；
3. 左侧打开 `Local Storage → http://localhost:5173`；
4. 删除 `access_token` 和 `user_role`；
5. 刷新 `http://localhost:5173/`。

点击“商家注册”，使用10.3.1已经注册成功的同一商家账号再次提交。手机号、名称和密码也可以保持不变。在 Network 中点击 `merchants` 请求并检查 Response。

通过标准：

```text
POST /api/v1/merchants
HTTP 409
code = 1201
msg = 商家账号已存在
```

这次页面没有跳到登录页是正常的。若得到201，说明重复账号约束失效；若得到500，按10.6记录为后端异常。

#### 10.5.2 页面测试：重复用户账号

返回首页，点击“用户注册”，使用10.4.1已经成功注册的同一用户账号再次提交。在 Network 中点击 `users`。

通过标准：

```text
POST /api/v1/users
HTTP 409
code = 1101
msg = 用户账号已存在
```

#### 10.5.3 页面测试：同一店铺创建同名分类

使用原商家账号重新登录，进入“商品管理”。在分类输入框再次输入已经存在的分类名，例如“热销主食”，点击“新增分类”。

通过标准：

```text
POST /api/v1/shops/{shopId}/categories
HTTP 409
code = 1005
msg = 资源冲突
```

刷新页面后只能存在原来的一个分类，不能出现两个同名分类。

#### 10.5.4 为后续接口异常测试准备 PowerShell

另开一个普通 PowerShell。不要关闭正在运行后端和前端的两个窗口。复制下面整段，建立一个显示 HTTP 和 Response 的辅助函数：

```powershell
$apiBase = 'http://localhost:8080/api/v1'

function Invoke-ApiCheck {
    param(
        [Parameter(Mandatory)] [string] $Method,
        [Parameter(Mandatory)] [string] $Path,
        [hashtable] $Headers = @{},
        [object] $Body = $null
    )

    $request = @{
        Uri = "$apiBase$Path"
        Method = $Method
        Headers = $Headers
        UseBasicParsing = $true
    }

    if ($null -ne $Body) {
        $request.ContentType = 'application/json; charset=utf-8'
        $request.Body = $Body | ConvertTo-Json -Depth 10
    }

    $status = 0
    $content = ''
    try {
        $response = Invoke-WebRequest @request
        $status = [int] $response.StatusCode
        $content = $response.Content
    }
    catch {
        $errorResponse = $_.Exception.Response
        if ($null -eq $errorResponse) {
            throw
        }

        $status = [int] $errorResponse.StatusCode
        $reader = New-Object System.IO.StreamReader($errorResponse.GetResponseStream())
        try {
            $content = $reader.ReadToEnd()
        }
        finally {
            $reader.Dispose()
        }
    }

    $json = $null
    if ($content) {
        $json = $content | ConvertFrom-Json
    }

    Write-Host "HTTP $status"
    if ($null -ne $json) {
        Write-Host ($json | ConvertTo-Json -Depth 20)
    }

    [pscustomobject]@{
        Status = $status
        Body = $json
    }
}
```

然后输入正常流程使用的账号。密码通过提示输入，只保存在当前窗口变量中：

```powershell
$merchantAccount = Read-Host '输入原商家账号'
$userAccount = Read-Host '输入原用户账号'
$testCredential = Get-Credential -UserName $merchantAccount -Message '输入商家和用户共用的测试密码'
$testPassword = $testCredential.GetNetworkCredential().Password

$merchantLogin = Invoke-ApiCheck -Method POST -Path '/merchants/login' -Body @{
    account = $merchantAccount
    password = $testPassword
}

$userLogin = Invoke-ApiCheck -Method POST -Path '/users/login' -Body @{
    account = $userAccount
    password = $testPassword
}

$merchantHeaders = @{
    Authorization = "Bearer $($merchantLogin.Body.data.accessToken)"
}

$userHeaders = @{
    Authorization = "Bearer $($userLogin.Body.data.accessToken)"
}
```

两个登录都必须显示 HTTP 200 和 `code = 0`。随后输入正常联调时记录的ID：

```powershell
$shopId = [long](Read-Host '输入原商家的 shopId')
$productId = [long](Read-Host '输入已上架商品的 productId')
```

如果ID忘记了：商家登录后可从商品管理页 Network 的 URL 和 Response 找到 `shopId`、`productId`。不要把 Token 写进文档。

#### 10.5.5 无 Token 和错误角色

首先完全不发送 Token：

```powershell
$noTokenResult = Invoke-ApiCheck -Method GET -Path '/users/me'
```

通过标准：

```text
HTTP 401
code = 1002
```

再让 User Token 调商家接口，以及让 Merchant Token 调用户接口：

```powershell
$userCallsMerchant = Invoke-ApiCheck -Method GET -Path '/merchants/me' -Headers $userHeaders
$merchantCallsUser = Invoke-ApiCheck -Method GET -Path '/users/me' -Headers $merchantHeaders
```

后两项都应为 HTTP 403、`code = 1003`。若错误角色返回200，就是权限漏洞；立即按10.6记录。

#### 10.5.6 资源归属：第二个商家不能修改第一个商家的店铺

回到页面，注册一个全新的第二商家，例如把账号末尾改成 `02`。第二商家不需要创建店铺。在刚才的 PowerShell 执行：

```powershell
$otherMerchantAccount = Read-Host '输入第二商家账号'
$otherMerchantCredential = Get-Credential -UserName $otherMerchantAccount -Message '输入第二商家密码'
$otherMerchantPassword = $otherMerchantCredential.GetNetworkCredential().Password

$otherMerchantLogin = Invoke-ApiCheck -Method POST -Path '/merchants/login' -Body @{
    account = $otherMerchantAccount
    password = $otherMerchantPassword
}

$otherMerchantHeaders = @{
    Authorization = "Bearer $($otherMerchantLogin.Body.data.accessToken)"
}

$ownershipBody = @{ description = '越权修改测试，不应保存' }
$ownershipResult = Invoke-ApiCheck -Method PATCH -Path "/shops/$shopId" -Headers $otherMerchantHeaders -Body $ownershipBody
```

通过标准：HTTP 403、`code = 1003`。随后用原商家刷新店铺，简介不能变成“越权修改测试，不应保存”。如果实现用404隐藏资源存在，也可以记为通过，但必须确认没有修改数据。

#### 10.5.7 使用旧 version 修改商品

先读取当前商品：

```powershell
$productBefore = Invoke-ApiCheck -Method GET -Path "/products/$productId" -Headers $merchantHeaders
$currentVersion = [long] $productBefore.Body.data.version
$oldVersion = $currentVersion - 1
Write-Host "当前版本=$currentVersion；本次故意使用旧版本=$oldVersion"
```

若 `oldVersion` 小于1，先在商家页面正常修改一次商品描述，再重新执行上面四行。然后故意用旧版本修改：

```powershell
$oldVersionBody = @{
    description = '这次旧版本修改不应成功'
    version = $oldVersion
}
$oldVersionResult = Invoke-ApiCheck -Method PATCH -Path "/products/$productId" -Headers $merchantHeaders -Body $oldVersionBody
```

通过标准：HTTP 409、`code = 1005`。重新查询商品，描述不能变成“这次旧版本修改不应成功”。

#### 10.5.8 准备一个新购物车项

确保店铺为 `OPEN`、商品为 `ON_SALE` 且库存至少为2，然后执行：

```powershell
$addCartBody = @{
    productId = $productId
    quantity = 2
}
$addCartResult = Invoke-ApiCheck -Method POST -Path '/cart-items' -Headers $userHeaders -Body $addCartBody
$cartItemId = [long] $addCartResult.Body.data.id
$cartVersion = [long] $addCartResult.Body.data.product.version
Write-Host "cartItemId=$cartItemId；购物车记录的商品版本=$cartVersion"
```

通过标准：HTTP 201或200、`code = 0`，并成功打印 `cartItemId` 和 `cartVersion`。

#### 10.5.9 下单时商品版本已变化

商家先正常修改商品描述，使版本递增：

```powershell
$productNow = Invoke-ApiCheck -Method GET -Path "/products/$productId" -Headers $merchantHeaders
$versionBeforeChange = [long] $productNow.Body.data.version
$changeBody = @{
    description = '用于价格版本冲突测试'
    version = $versionBeforeChange
}
$changeProduct = Invoke-ApiCheck -Method PATCH -Path "/products/$productId" -Headers $merchantHeaders -Body $changeBody
```

再用购物车之前保存的旧 `$cartVersion` 下单：

```powershell
$priceChangedKey = [guid]::NewGuid().ToString()
$priceChangedHeaders = $userHeaders + @{ 'X-Idempotency-Key' = $priceChangedKey }
$priceChangedBody = @{
    items = @(
        @{
            cartItemId = $cartItemId
            productVersion = $cartVersion
        }
    )
}
$priceChangedResult = Invoke-ApiCheck -Method POST -Path '/orders' -Headers $priceChangedHeaders -Body $priceChangedBody
```

通过标准：HTTP 409、`code = 1601`；订单不能创建，购物车项仍存在。

#### 10.5.10 库存不足不能创建订单

先把商品库存改成1，购物车数量仍为2：

```powershell
$latestProduct = Invoke-ApiCheck -Method GET -Path "/products/$productId" -Headers $merchantHeaders
$latestVersion = [long] $latestProduct.Body.data.version
$stockToOneBody = @{
    stock = 1
    version = $latestVersion
}
$stockToOne = Invoke-ApiCheck -Method PATCH -Path "/products/$productId" -Headers $merchantHeaders -Body $stockToOneBody

$cartNow = Invoke-ApiCheck -Method GET -Path '/cart-items' -Headers $userHeaders
$currentCartItem = $cartNow.Body.data.items | Where-Object { $_.id -eq $cartItemId } | Select-Object -First 1
$latestCartVersion = [long] $currentCartItem.product.version

$insufficientKey = [guid]::NewGuid().ToString()
$insufficientHeaders = $userHeaders + @{ 'X-Idempotency-Key' = $insufficientKey }
$insufficientBody = @{
    items = @(
        @{
            cartItemId = $cartItemId
            productVersion = $latestCartVersion
        }
    )
}
$insufficientResult = Invoke-ApiCheck -Method POST -Path '/orders' -Headers $insufficientHeaders -Body $insufficientBody
```

通过标准：HTTP 409、`code = 1402`；订单不能生成，购物车仍保留。随后必须恢复库存，否则后续成功下单无法继续：

```powershell
$productAtOne = Invoke-ApiCheck -Method GET -Path "/products/$productId" -Headers $merchantHeaders
$versionAtOne = [long] $productAtOne.Body.data.version
$restoreStockBody = @{
    stock = 20
    version = $versionAtOne
}
$restoreStock = Invoke-ApiCheck -Method PATCH -Path "/products/$productId" -Headers $merchantHeaders -Body $restoreStockBody
```

恢复请求必须为 HTTP 200、`code = 0`，Response 中 `stock = 20`。

#### 10.5.11 幂等重试、幂等冲突和重复取消

先把购物车数量改为1并读取最新版本：

```powershell
$quantityOne = Invoke-ApiCheck -Method PATCH -Path "/cart-items/$cartItemId" -Headers $userHeaders -Body @{ quantity = 1 }
$cartForOrder = Invoke-ApiCheck -Method GET -Path '/cart-items' -Headers $userHeaders
$orderCartItem = $cartForOrder.Body.data.items | Where-Object { $_.id -eq $cartItemId } | Select-Object -First 1
$orderProductVersion = [long] $orderCartItem.product.version

$idempotencyKey = [guid]::NewGuid().ToString()
$idempotencyHeaders = $userHeaders + @{ 'X-Idempotency-Key' = $idempotencyKey }
$orderBody = @{
    items = @(
        @{
            cartItemId = $cartItemId
            productVersion = $orderProductVersion
        }
    )
}
```

第一次创建订单：

```powershell
$firstOrder = Invoke-ApiCheck -Method POST -Path '/orders' -Headers $idempotencyHeaders -Body $orderBody
$newOrderId = [long] $firstOrder.Body.data.id
Write-Host "第一次订单ID=$newOrderId"
```

应为 HTTP 201、`code = 0`、`status = PENDING_PAYMENT`。用完全相同的幂等键和Body重试：

```powershell
$sameRetry = Invoke-ApiCheck -Method POST -Path '/orders' -Headers $idempotencyHeaders -Body $orderBody
Write-Host "重试订单ID=$($sameRetry.Body.data.id)"
```

第二次仍应 `code = 0`，订单ID必须等于 `$newOrderId`，不能生成第二张订单或再次扣库存。

再用同一个幂等键、不同Body：

```powershell
$differentBody = @{
    items = @(
        @{
            cartItemId = $cartItemId
            productVersion = $orderProductVersion + 1
        }
    )
}
$idempotencyConflict = Invoke-ApiCheck -Method POST -Path '/orders' -Headers $idempotencyHeaders -Body $differentBody
```

通过标准：HTTP 409、`code = 1603`。然后取消同一张新订单两次：

```powershell
$firstCancel = Invoke-ApiCheck -Method POST -Path "/orders/$newOrderId/cancel" -Headers $userHeaders
$secondCancel = Invoke-ApiCheck -Method POST -Path "/orders/$newOrderId/cancel" -Headers $userHeaders
$productAfterCancel = Invoke-ApiCheck -Method GET -Path "/products/$productId" -Headers $merchantHeaders
```

第一次取消应为 HTTP 200、`code = 0`、状态 `CANCELLED`；第二次应为 HTTP 409、`code = 1602`。库存恢复为20、下单数量为1时，第一次下单后库存为19，第一次取消后回到20，第二次取消后仍为20，不能变成21。

#### 10.5.12 把异常结果写入日志

每做完一项，只需向 `docs/test/test-log.md` 追加一行：

| 场景                     | 预期                                 | 实际       | 结论 |
| ------------------------ | ------------------------------------ | ---------- | ---- |
| 重复商家注册             | HTTP 409 / code 1201                 | 与预期一致 | 通过 |
| 重复用户注册             | HTTP 409 / code 1101                 | 与预期一致 | 通过 |
| 同名分类                 | HTTP 409 / code 1005                 | 与预期一致 | 通过 |
| 无 Token                 | HTTP 401 / code 1002                 | 与预期一致 | 通过 |
| 错误角色                 | HTTP 403 / code 1003                 | 与预期一致 | 通过 |
| 第二商家修改第一商家店铺 | HTTP 403 或 404                      | 与预期一致，数据未变 | 通过 |
| 旧商品版本修改           | HTTP 409 / code 1005                 | 与预期一致，数据未变 | 通过 |
| 下单版本变化             | HTTP 409 / code 1601                 | 与预期一致 | 通过 |
| 库存不足下单             | HTTP 409 / code 1402                 | 与预期一致，库存已恢复 | 通过 |
| 相同幂等键和Body重试     | 返回同一订单ID                       | 与预期一致 | 通过 |
| 相同幂等键不同Body       | HTTP 409 / code 1603                 | 与预期一致 | 通过 |
| 第二次取消订单           | HTTP 409 / code 1602，库存不重复恢复 | 与预期一致 | 通过 |

实际与预期完全一致就把结论写“通过”；不一致时写“失败”，立即执行10.6。测试完成后关闭这个 PowerShell 窗口，Token 变量随窗口销毁，不保存到磁盘。

### 10.6 任一步失败时立即这样记录

先暂停后续操作，不要猜测着改代码。保存：

1. 刚才点击了什么；
2. Request Method 和完整 URL；
3. Request Payload；
4. HTTP 状态码；
5. Response JSON；
6. 页面实际表现；
7. 同一时间的后端控制台异常。

判断原则：请求路径、字段或 Token 没发对，多数是前端问题；后端返回与 API/SRS 不一致或出现 HTTP 500，多数是后端问题；后端已经返回正确数据但页面不显示，是前端问题。截图必须遮住 Token 和密码。完成记录后再进入第 11 节，由 A 对齐契约、补 Red，并按责任分给 B 或 C 修复。

### 10.7 已完成记录：BUG-FE-001 Red、Green和复测

正常接口和异常接口都已经通过，现在不要继续制造测试数据。剩余唯一确认的问题是“退出只返回首页，没有删除登录状态”。它属于前端公共组件问题，不需要 B 修改数据库、ServiceImpl 或 DAO。

按 TDD 顺序处理：

1. [已完成] A 在 `frontend/src/tests/unit/AppHeader.spec.js` 增加退出测试；
2. [已完成] 只运行该测试，得到2项中1项失败；失败为 `access_token` 仍是 `token-to-clear`，已保存为有效 Red；
3. [已完成] C 在 `frontend/src/components/layout/AppHeader.vue` 增加最小退出逻辑，Green提交为 `312b6fd`；
4. [已完成] 目标测试2/2、前端全量95/95和生产构建通过；
5. [已完成] 浏览器真实登录后点击退出，确认 Local Storage 两项数据消失；
6. [已完成] 更新 `docs/test/bug-list.md` 和 `docs/test/test-log.md`；
7. 最后执行第12节，不在每个小步骤单独提交或 Push。

A 的 Red 必须验证三件事：

- 点击“退出”后 `access_token` 被删除；
- 点击“退出”后 `user_role` 被删除；
- 页面进入首页 `/`。

目标测试命令：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd run test:run -- AppHeader.spec.js
```

修改实现前，该新增测试必须失败；失败原因应是登录数据仍然存在或没有执行首页导航，不能是导入、语法或测试环境错误。A保存Red后，C只做以下最小实现：

```text
logout()
  ├─ localStorage.removeItem('access_token')
  ├─ localStorage.removeItem('user_role')
  └─ router.replace('/')
```

不要在这一步重构路由、状态管理或登录模块。Green后执行：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd run test:run -- AppHeader.spec.js
npm.cmd run test:run
npm.cmd run build
```

通过标准：

- `AppHeader.spec.js` 新增退出测试通过；
- 前端全量测试为95项且全部通过；
- 生产构建成功；
- 浏览器登录后，Application → Local Storage 中存在两项登录数据；
- 点击“退出”后自动回到首页，两项数据都消失；
- 手工打开 `/customer/cart` 或 `/merchant/store` 时不能继续使用旧登录身份。

上述复测和日志更新均已完成，`BUG-FE-001` 已关闭。现在直接执行第12节最终自动化验收。

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
$dbCredential = Get-Credential -UserName 'delivery_app' -Message '输入本机 delivery_app 数据库密码'
$env:DELIVERY_DB_PASSWORD = $dbCredential.GetNetworkCredential().Password
.\mvnw.cmd clean test
```

要求：`BUILD SUCCESS`，Failures、Errors、Skipped 都为 0，并生成 `backend/target/site/jacoco/index.html`。

确认 Flyway：

```powershell
mysql -u delivery_app -p delivery_test -e "SELECT installed_rank, version, script, success FROM flyway_schema_history ORDER BY installed_rank;"
```

必须看到 V1、V2 各一条且 `success=1`。

2026-09-08 最终复测已完成：Surefire 共生成16个测试类报告，合计171个测试；Failures、Errors、Skipped 均为0，Maven `BUILD SUCCESS`。B 已确认 `flyway_schema_history` 中 V1、V2 均存在且 `success=1`。

### 12.2 前端

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd ci
npm.cmd run test:run
npm.cmd run build
```

要求：全部 Vitest 测试通过且 Vite 构建成功。当前已经有前端测试体系，不再额外引入另一套大型框架。

2026-09-08 最终复测已完成：`npm.cmd ci` 成功安装 178 个包，25 个测试文件、95 个测试全部通过，生产构建成功并转换 1710 个模块。构建只提示主 chunk 大于 500 kB；`npm audit` 显示的依赖漏洞也是警告，不阻塞阶段 1，当前不要执行可能引入破坏性升级的 `npm audit fix --force`。

如果 `npm.cmd ci` 报 `EPERM ... esbuild.exe`、`EPERM ... node_modules\@babel`，并且后续提示找不到 `vitest` 或 `vite`，含义不是测试失败，而是仍在运行的 `npm run dev`/Vite 占用了 `node_modules`，导致依赖只删除了一部分。处理顺序：

1. 回到正在运行前端的 PowerShell 窗口，按 `Ctrl+C`，确认终止；
2. 关闭仍在使用该项目的前端终端；不要结束 Codex 自带的 Node 进程；
3. 回到 `frontend` 目录重新执行上面三条命令；
4. 必须先看到 `npm.cmd ci` 成功，才能判断后两条测试和构建结果。

本次实际占用者已确认是该项目的 `npm run dev` 和 Vite 进程，停止这两个进程后重新执行三条命令，全部通过。需要继续浏览器联调时，再在前端目录执行 `npm.cmd run dev`。

### 12.3 证据

至少保留：

- 后端完整测试汇总；
- 前端测试和构建汇总；
- Flyway V1/V2 查询结果；
- 30 接口联调清单；
- 商家建店/上架商品、用户下单/取消、商家查看订单的关键截图；
- 实际出现过的 Bug、Red、Green 和复测记录。

当前状态：测试日志、Bug记录、30接口正常/异常结果和Red/Green过程已经写入仓库；仓库中尚未发现联调截图。代码和文档可以先推送 `develop`，但在课程最终交付前至少补3张不含密码和完整Token的截图：商家商品/店铺结果、用户订单结果、商家订单结果。截图可以放到 `docs/test/evidence/`，随后在 `docs/test/test-log.md` 中写明文件名和对应场景。

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

| 工具         | 是否随 Git 提供 | 要求                                                          |
| ------------ | --------------- | ------------------------------------------------------------- |
| Git          | 否              | `git --version` 可用                                        |
| JDK 17       | 否              | `java -version` 和 Maven 都显示 17                          |
| Maven        | Wrapper 提供    | 不用全局安装；首次使用需要联网下载                            |
| MySQL Server | 否              | 本机 3306，创建`delivery_dev`、`delivery_test` 和应用账号 |
| Node.js/npm  | 否              | 安装 Node LTS，`node -v`、`npm -v` 可用                   |
| IDEA/VS Code | 否              | 可选                                                          |
| `rg`       | 否              | 可选，不影响构建                                              |

真实密码只通过当前终端的 `DELIVERY_DB_PASSWORD` 提供。Flyway 能自动建表和升级表，不能替团队成员安装 MySQL、创建数据库或同步密码。

---

## 15. 最终停止条件

- [X] 后端四层实现完成并合入远程 `develop`；
- [X] V1/V2 和后端完整测试通过；
- [X] P0-1 至 P0-5 的实现修复已进入远程 `develop`；
- [X] 购物车删除、刷新和下单成功已有有效回归测试；
- [X] R1 商品描述、R2 目标数量、R3 防重复点击已按 Red/Green 完成；
- [X] 30 个前端 API 的方法和路径已有自动契约覆盖；
- [X] 当前 `develop@312b6fd` 的25个测试文件、95个测试和生产构建通过；
- [X] 30 个接口正常业务路径已完成真实联调；
- [X] 必要的 401/403/404/409、乐观锁、幂等和重复取消已验证；
- [X] 测试日志和Bug记录真实可追踪；
- [ ] 至少3张不含敏感信息的关键联调截图已放入 `docs/test/evidence/` 并在日志中建立对应关系；
- [X] A、B、C 当前最终业务代码都已合入远程 `develop`；
- [X] B 已从当前 `develop` 完成后端171项测试、前端95项测试、构建和核心流程复现；
- [ ] A、C 各自在最新 `develop` 上完成一次拉取和启动确认。

全部勾选后停止增加功能，进入课程报告、个人总结、演示脚本和答辩准备。
