# ABC 阶段 1 前后端联调、验收与交付执行文档

> 更新日期：2026-09-06
>
> 当前状态：后端阶段 1 实现已经完成，`feature/b-tdd` 已合并并推送到远程 `develop`。后端完整测试已经由 B 在本机 MySQL 环境中验证通过。
>
> 当前唯一下一阶段：A 冻结并检查契约，C 按契约修正前端并接入真实后端，B 只处理联调发现的后端缺陷；三人完成 30 个接口的真实联调、证据记录和最终验收。

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

截至 2026-09-06，以下内容已经完成：

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

## 5. 当前前端已经确认的契约问题

这些问题已经从当前 `frontend` 源码确认，不需要再次讨论是否存在：

| 文件/页面 | 当前问题 | 必须对齐 |
| --- | --- | --- |
| `src/api/cart.js` | 使用 `/cart`、`/cart/items` | 全部改为 `/cart-items` |
| `src/api/product.js` | 商品列表使用 `/products` | 列表使用 `/shops/{shopId}/products`；补分类接口 |
| `src/api/store.js` | 使用 `/stores/{id}/status` | 改为 `PATCH /shops/{shopId}`；补创建店铺和本人店铺列表 |
| `src/api/order.js` | 创建订单没有幂等请求头；缺取消和商家订单接口 | 加 `X-Idempotency-Key`；补齐 6 个订单接口 |
| `MerchantStoreView.vue` | 使用 `notice`、`TEMP_CLOSED` | 改为 `description`、`TEMPORARILY_CLOSED` |
| `MerchantProductsView.vue` | 查询缺 `shopId`；新增提交空对象；更新不带 `version` | 按 API 传完整字段和当前版本 |
| `CartView.vue` | 下单项没有 `productVersion` | 提交 `cartItemId` 和 `item.product.version` |
| `OrderDetailView.vue` | 仍是硬编码示例订单 | 改为按路由 ID 调用真实订单详情 |
| `OrdersView.vue` | 使用 `store`、`amount` 等旧展示字段 | 使用 `shopName`、`total`、`orderNumber` 等 API 字段 |
| `vite.config.js` | 开发服务器没有 `/api` 代理 | 增加到 `http://localhost:8080` 的开发代理 |
| 商家页面 | 没有商家订单列表和详情路由 | 增加最小页面或复用订单展示组件 |

前端单元测试使用 mock 是正常的；需要删除的是页面中的硬编码业务假数据，而不是测试中的 mock。

---

## 6. 第一步：三个人同步最终后端基线

三个人都在自己的项目根目录执行：

```powershell
git status --short
git fetch origin --prune
git switch develop
git pull --ff-only origin develop
git log -3 --oneline --decorate
git status --short
```

要求：

- `develop` 与 `origin/develop` 指向同一提交；
- `git status --short` 没有输出；
- 能看到合并 `feature/b-tdd` 的提交；
- 不再复制文件或压缩包手工同步。

C 创建本阶段工作分支：

```powershell
git switch -c feature/c-api-integration
```

A 只有在需要新增/修正测试时才创建 `test/a-api-contract`；B 只有在真实联调发现后端缺陷时才创建 `fix/b-integration`。没有代码改动的人不创建空分支和空提交。

### 6.1 先补最终后端 Green 记录

执行过后端完整测试的人，把真实结果追加到 `docs/test/test-log.md`：

- 日期和时间；
- 分支及提交号；
- `mvnw.cmd clean test` 命令；
- Tests、Failures、Errors、Skipped 的真实数量；
- `BUILD SUCCESS`；
- V1、V2 的 `success=1`；
- 不记录任何数据库密码。

不要凭记忆填写数量，应从 Maven 最后输出或 `backend/target/surefire-reports` 读取。

---

## 7. 第二步：A 先建立前端接口契约 Red

A 在 `test/a-api-contract` 上更新现有前端测试或新增一个小型 API 契约测试文件，至少固定以下内容：

1. Axios `baseURL` 为 `/api/v1`；
2. 用户和商家登录路径互相独立；
3. 四个购物车方法全部使用 `/cart-items`；
4. 商品列表必须带 `shopId` 组成 `/shops/{shopId}/products`；
5. 店铺 PATCH 使用 `/shops/{shopId}`；
6. 四个分类方法路径正确；
7. 创建订单把 `X-Idempotency-Key` 放进请求头；
8. 创建订单 body 的每项同时具有 `cartItemId`、`productVersion`；
9. 用户取消订单和商家订单查询方法存在且路径正确；
10. 页面使用 API 文档中的状态枚举及响应字段。

运行：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm ci
npm run test:run
```

预期新测试因当前旧路径或缺方法而失败，这才是有效 Red。A 记录失败用例和根因后提交：

```powershell
Set-Location '..'
git add -- frontend/src/tests docs/test/test-log.md
git diff --cached --check
git commit -m 'test(frontend): align api clients with frozen contract [RED]'
git push -u origin test/a-api-contract
```

A 的 Red 合入 `develop` 后，C 在个人分支同步：

```powershell
git fetch origin
git rebase origin/develop
```

如果 A 已经把这些断言写在现有测试里并能证明当前 Red，就不重复新建第二套相同测试。

---

## 8. 第三步：C 完成最小前端 Green

### 8.1 修正开发代理

保留 `src/api/http.js` 的：

```text
baseURL = /api/v1
```

在 `vite.config.js` 的 `server` 中增加 `/api` 代理到：

```text
http://localhost:8080
```

这样浏览器访问 `http://localhost:5173` 时，请求仍写 `/api/v1/...`，由 Vite 转发到后端，避免额外增加 CORS 配置。

### 8.2 修正 API 封装

按下面顺序修改，避免页面和封装同时失控：

1. `user.js`、`merchant.js`：确认独立登录和个人资料 8 个接口；
2. `store.js`：补 `createShop`、`listShops`、`getShop`、`updateShop`；
3. `product.js`：补四个分类方法，修正按店铺查询商品；
4. `cart.js`：四个方法统一为 `/cart-items`；
5. `order.js`：补齐用户订单 4 个、商家订单 2 个方法；
6. `index.js`：导出所有新增方法。

创建订单封装应接收独立的 `idempotencyKey`，请求形式为：

```text
POST /api/v1/orders
X-Idempotency-Key: <本次结算键>

{
  "items": [
    { "cartItemId": 31, "productVersion": 3 }
  ]
}
```

同一次结算的网络重试必须复用同一个 key；新的结算才生成新 key。不要把固定 key 写死在源码里。

### 8.3 修正页面字段和缺失页面

按依赖顺序处理：

1. 用户/商家注册、登录和资料页；
2. 商家店铺创建、本人店铺列表和店铺修改；
3. 分类新增、列表、修改、删除；
4. 商品新增、列表、详情和带 version 的修改；
5. 用户店铺/分类/商品浏览；
6. 购物车增删改查；
7. 用户创建、查询、查看和取消订单；
8. 商家订单列表和详情。

页面只实现阶段 1 必需操作。不要增加支付页、地图、配送员、优惠券、退款或 WebSocket。

### 8.4 每完成一组就验证

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm run test:run
npm run build
```

所有 A 新增的契约测试和原有页面测试必须 Green，构建目录能够正常生成。不要通过删除测试、`skip` 或放宽断言解决失败。

C 完成整个前端接入后只做一次 Green 提交：

```powershell
Set-Location '..'
git status --short
git diff --check
git add -- frontend
git diff --cached --check
git diff --cached --stat
git commit -m 'feat(frontend): integrate stage-one backend api [GREEN]'
```

---

## 9. 第四步：在同一台联调电脑上启动真实后端和前端

下面两个进程必须在同一台电脑上运行，因为 `localhost` 只代表当前电脑。B 和 C 如果使用不同电脑，C 的 `localhost:8080` 不能访问 B 的后端。

最少工作方案：C 先用前端自动测试完成 Green 并推送个人分支；然后在已经配置好 MySQL 的 B 电脑上拉取该分支，同时启动后端和前端进行真实联调。若 C 的电脑也完成了第 14 节环境准备，也可以直接把 C 的电脑作为联调机。

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
npm ci
npm run dev
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
npm ci
npm run test:run
npm run build
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

## 13. C 合并后怎样交付

C 在个人分支先同步最新后端：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git fetch origin --prune
git rebase origin/develop
```

重新执行前端测试和构建；若 rebase 包含后端修复，还要执行后端完整测试。全部通过后：

```powershell
git push -u origin feature/c-api-integration
git switch develop
git pull --ff-only origin develop
git merge --no-ff feature/c-api-integration
```

在合并后的 `develop` 上执行第 12 节最终验收。全部通过才推送：

```powershell
git push origin develop
git status --short
```

只有 `git push origin develop` 成功且 `git status --short` 为空，阶段 1 才算完成。个人分支要等合并成功后再删除。

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
- [ ] 前端所有旧路径和旧字段已按 API 文档修正；
- [ ] 页面硬编码业务假数据已移除；
- [ ] 前端测试和构建全部通过；
- [ ] 30 个接口真实联调全部通过；
- [ ] 必要的 401/403/404/409、乐观锁、幂等和重复取消已验证；
- [ ] 测试日志、Bug 记录和关键截图真实可追踪；
- [ ] A、B、C 的最终代码都已合入并推送远程 `develop`；
- [ ] 三个人从最新 `develop` 能按第 9 节启动并复现核心流程。

全部勾选后停止增加功能，进入课程报告、个人总结、演示脚本和答辩准备。
