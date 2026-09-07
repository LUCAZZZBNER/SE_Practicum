# ABC 阶段 1 前后端联调、验收与交付执行文档

> 更新日期：2026-09-07
>
> 当前状态：B 已回到 `develop` 并执行 `git pull --ff-only origin develop`，本地和远程共同基线为 `3aa82ac feat: complete frontend integration fixes`。前端 25 个测试文件、89 个测试全部通过，生产构建成功。`fix/api-alignment` 及其本地评审分支不参与本轮，不合并、不摘取提交，也不作为结论来源。
>
> 当前唯一下一阶段：先修复最新 `develop` 中仍会阻断真实联调的最小前端缺陷，再在同一台电脑启动 MySQL、后端和前端，完成 30 个接口的真实联调、证据记录和最终验收。89 个 mock 单元测试全绿不能替代真实联调。

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

2026-09-07，C 又将前端修正推入 `develop`。B 已从 `241deb0` fast-forward 到 `3aa82ac`。该提交修改了 17 个前端文件，主要补充建店、分类、商品、购物车、订单、401 处理及其测试。第 5 节已经重新按 `3aa82ac` 的源码和当前后端可执行契约审计；此前针对 `90fd50c` 或 `fix/api-alignment` 的问题清单不能代替本次结论。

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

## 5. `develop@3aa82ac` 最新审计结论

### 5.1 已完成且已经自动验证的内容

本轮不要重做以下内容：

- 30 个 HTTP 接口的前端 API 函数都已存在，方法和路径已由 `apiContracts.spec.js` 覆盖；
- 用户和商家继续使用独立注册、登录、资料接口；
- 创建店铺、店铺资料修改及三个正确店铺状态已经接入；
- 分类列表按后端直接数组读取，分类新增、改名、删除已经接入；
- 商品新增、编辑、下架已经接入，商品修改携带 `version`；
- 购物车使用 `/cart-items`，创建订单携带 `cartItemId`、`productVersion` 和 `X-Idempotency-Key`；
- 同一次结算第一次失败后重试会复用幂等键；
- 用户订单取消、商家订单列表和详情已经接入；
- 401 已经清理 `access_token` 和 `user_role`；
- Vite `/api` 代理仍指向 `http://localhost:8080`。

2026-09-07，B 在 `develop@3aa82ac` 上实际执行：

```text
npm.cmd run test:run：25 个测试文件通过，89 个测试通过
npm.cmd run build：成功，1710 个模块完成生产构建
```

构建只有大于 500 kB 的 chunk 警告，不是失败。不要为了这个警告新增拆包、性能优化或更换构建工具。

### 5.2 当前确认存在的最小必要问题

下表来自最新前端源码与后端 Controller、Service 请求/返回类型、SRS 和 API 文档的逐项对照。测试全绿但这些问题仍然存在，是因为相关页面测试 mock 了后端，没有真正经过权限和响应字段校验。其中 P0-1、P0-2 会直接阻断“商家上架商品—顾客浏览下单”主流程；P0-3 至 P0-5 会使必需页面信息、操作结果或登录失效处理不正确。

| 编号 | 问题 | 为什么一定有问题 | 最小修正 |
| --- | --- | --- | --- |
| P0-1 | 顾客店铺详情请求了 `includeOffSale: true` | `StoreDetailView.vue` 调用商品列表时固定传 `true`；后端 `ItemServiceImpl` 明确规定：只有拥有该店铺的 Merchant 才能查看下架商品，普通用户会得到 403。因此顾客主流程会在打开店铺时失败 | 顾客页面删掉 `includeOffSale: true`，只请求默认的上架商品；商家页面继续保留它 |
| P0-2 | 新商品无法上架 | 后端新商品默认 `OFF_SALE`；`MerchantProductsView.vue` 目前只有“下架”，没有把商品改为 `ON_SALE` 的入口。这样顾客永远看不到新建商品，无法完成下单主流程 | 根据当前状态显示“上架”或“下架”，都调用 `PATCH /products/{id}`，Body 只需 `{status, version}` |
| P0-3 | 商品详情假设后端返回 `shopName` | 后端 `ProductView` 只有 `shopId`，没有 `shopName`；页面和测试却从商品响应读取 `shopName`，真实页面“所属店铺”会为空 | 取得 `shopId` 后调用已有 `GET /shops/{shopId}` 获得名称，或者删除非必需的店铺名展示；若保留该栏，优先复用现有 `getStoreDetail()` |
| P0-4 | 购物车修改、删除和下单成功后页面不刷新 | 当前三个请求成功后没有重新读取购物车。后端数据已经变了，页面仍显示旧数量、已删除项或已下单项，用户会误以为失败并重复操作 | 三种成功操作后调用 `loadCart()`；创建订单成功后至少显示成功消息，并可进入订单列表 |
| P0-5 | 401 只改地址栏，不完成 Vue 页面跳转 | `window.history.replaceState({}, '', '/')` 只替换 URL，不触发 Vue Router 导航或整页加载。令牌虽被删掉，受保护页面组件可能仍留在屏幕上 | 使用可测试的路由跳转，或最小使用 `window.location.assign('/')`/`replace('/')` 触发真正导航 |

### 5.3 测试本身仍有的契约问题

这两项不会立刻让 89 个测试失败，但必须在真实联调前修正，否则测试会给出错误信心：

1. `apiContracts.spec.js` 的用户注册样例写成了 `name`、`address`，还缺少必填的 `passwordConfirm`。正确字段是 `account`、`password`、`passwordConfirm`、`nickname`、可选 `phone`。
2. 同一文件的用户资料修改样例写成了 `name`、`address`。正确字段只有可选的 `nickname`、`phone`。
3. `ProductDetailView.spec.js` 的 mock 人为添加了后端不存在的 `shopName`，因此掩盖了 P0-3。
4. `StoreDetailView.spec.js` 明确期待顾客发送 `includeOffSale: true`，因此保护了 P0-1 这个错误行为。

30 个接口的“方法和路径”现在已经覆盖，不再重复补所谓“剩余 11 个接口”。当前应补的是上述真实页面行为 Red。

### 5.4 可以延后但联调时必须观察的问题

- 店铺列表有“搜索店铺或商品”输入框，但没有绑定值，也没有发送 `keyword`。阶段 1 不要求复杂搜索；最小方案是接入现有店铺 `keyword` 查询，或者删除这个无效输入框。
- 购物车“修改数量”现在只能每次加 1。它确实调用了修改接口，但操作后必须刷新；若验收要求直接填写数量，再换成最小数字输入框，不做额外购物车 UI。
- 多个页面同时由 Axios 和页面 `catch` 显示错误，真实失败时可能弹两次提示。它不阻断主流程，放到主要问题修完以后处理。

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

- `HEAD` 和 `origin/develop` 都是 `3aa82ac`；
- 第一行日志是 `3aa82ac feat: complete frontend integration fixes`；
- `git status --short` 没有输出；
- 不复制压缩包，不从 `fix/api-alignment` 取文件，不执行针对该分支的 merge、rebase 或 cherry-pick。

若 `git status --short` 显示个人未提交文件，先停止切分支，让文件所有者确认；不要用 `reset --hard` 或 `checkout --` 丢弃文件。

### 6.1 当前三个人是否还要互相等待

不需要空等，但代码顺序必须保持：

1. A 先按第 7 节补能暴露 P0-1 至 P0-5 的测试；
2. C 同时可以读第 8 节和准备实现，但应在 A 的 Red 可用后再完成最终 Green；
3. B 现在即可检查 MySQL、JDK 和后端能否启动，不修改已全绿的业务代码；
4. C 完成 Green 后，三人再进行第 9、10 节真实联调；
5. 真实联调若证明后端有缺陷，才由 A 补后端 Red、B 修 ServiceImpl/DAO/XML。

这表示 B 现在等的不是“A 再写 Controller”。30 个 Controller 和 B 的 ServiceImpl/DAO 已完成。B 只是在等待 C 把最后的前端阻断问题修好，随后用自己的 MySQL 和后端承担真实联调。

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

## 7. 第二步：A 为当前五个问题补 Red

### 7.1 建立测试分支

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git switch develop
git pull --ff-only origin develop
git switch -c test/a-final-integration-red
```

本轮不用每写一个测试就提交或 Push。把同一批 Red 全部写好、确认失败原因正确后提交一次即可。

### 7.2 P0-1：顾客只能看上架商品

修改 `frontend/src/tests/unit/StoreDetailView.spec.js`：

1. 保持 `listCategories` 返回直接数组；
2. 让 `listProducts` 返回分页对象；
3. 页面挂载完成后，正确期望应为顾客请求中不包含 `includeOffSale: true`。

推荐断言：

```javascript
expect(mocks.listProducts).toHaveBeenCalledWith(7, {
  page: 1,
  pageSize: 100,
})
```

当前实现会因为多发了 `includeOffSale: true` 而 Red。这个 Red 直接对应后端 403 权限规则。

### 7.3 P0-2：商品必须能从下架变成上架

修改 `frontend/src/tests/unit/MerchantProductsView.spec.js`：

1. mock 一个 `status: 'OFF_SALE'`、`version: 3` 的商品；
2. 页面应显示“上架”；
3. 点击确认后断言：

```javascript
expect(mocks.updateProduct).toHaveBeenCalledWith(1, {
  status: 'ON_SALE',
  version: 3,
})
```

再保留现有 ON_SALE 商品下架测试，证明两个方向都能操作。当前页面没有上架按钮，新测试应 Red。

### 7.4 P0-3：商品详情不能读取不存在的字段

修改 `frontend/src/tests/unit/ProductDetailView.spec.js`：

1. 商品 mock 删除 `shopName`，只保留后端真实返回的 `shopId`；
2. mock `getStoreDetail(7)` 返回 `{id: 7, name: '示例快餐店'}`；
3. 断言页面调用 `getStoreDetail(7)` 并显示店铺名。

不要继续向 Product mock 偷加 `shopName`。当前实现没有调用店铺详情接口，新测试应 Red。

### 7.5 P0-4：购物车操作成功后刷新

修改 `frontend/src/tests/unit/CartView.spec.js`，分别验证：

- 修改数量成功后再次调用 `getCart()`；
- 删除成功后再次调用 `getCart()`；
- 创建订单成功后再次调用 `getCart()`，并显示成功消息或进入订单列表；
- 创建订单第一次网络失败、第二次重试仍复用同一个幂等键；该现有测试保留。

测试要真正触发 ConfirmAction 的 `confirm`，不能只确认页面上有“删除”文字。当前删除测试只检查 `removeCartItem` 没被调用，不能证明删除功能。

### 7.6 P0-5：401 必须真的离开受保护页面

修改 `frontend/src/tests/unit/http.spec.js`。除了令牌和角色被删除，还要验证实际导航函数被调用。不要只检查 `window.location.pathname`，因为 `history.replaceState` 恰好能让这个断言通过，却不会通知 Vue Router。

可以先把跳转封装成一个很小的函数并 mock；或在测试环境可控的前提下监视 `window.location.replace/assign`。断言的业务含义是：收到 401 后，不再停留在 `/customer/...` 或 `/merchant/...` 页面。

### 7.7 修正不是 Red、但写错的契约样例

在 `apiContracts.spec.js` 把用户样例改成：

```javascript
const registerBody = {
  account: 'user01',
  password: 'pass123456',
  passwordConfirm: 'pass123456',
  nickname: '普通用户',
  phone: '13800000000',
}
const profileBody = {
  nickname: '新用户',
  phone: '13900000000',
}
```

它可能直接 Green，因为 API 包装函数只透传 Body；这属于修正测试数据，不要伪称 Red。

### 7.8 运行并保存 Red

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd run test:run
```

有效 Red 的判断：旧的 89 个测试没有被删除或 `skip`；新增失败明确指向 P0-1 至 P0-5；不是导入错误、语法错误或环境错误。记录失败测试名称和原因后，一次性提交：

```powershell
Set-Location '..'
git status --short
git diff --check
git add -- frontend/src/tests
git diff --cached --check
git commit -m 'test(frontend): expose final integration blockers [RED]'
git push -u origin test/a-final-integration-red
```

团队如果不要求保留独立 Red 分支，可以由 C 合入或基于该分支继续；但必须保留能证明“测试先失败”的提交历史。

---

## 8. 第三步：C 只完成五个最小 Green

### 8.1 从最新基线开始

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git fetch origin --prune
git switch develop
git pull --ff-only origin develop
git switch -c fix/c-final-integration-green
```

将 A 的 Red 合入该分支，具体用 merge 还是 cherry-pick 由团队现有流程决定，只取 A 的测试提交，不取 `fix/api-alignment`。

### 8.2 按依赖顺序修改

1. `StoreDetailView.vue`：顾客商品列表请求删掉 `includeOffSale: true`。
2. `MerchantProductsView.vue`：OFF_SALE 显示“上架”，提交 `{status: 'ON_SALE', version}`；ON_SALE 继续显示“下架”。
3. `ProductDetailView.vue`：根据 `shopId` 调用现有 `getStoreDetail()` 获取店铺名；不修改后端 `ProductView`，不擅自修改 API 文档。
4. `CartView.vue`：修改、删除、下单成功后执行 `loadCart()`；下单成功给出明确反馈，必要时进入 `/customer/orders`。
5. `http.js`：401 清状态后执行真正导航；避免只调用 `history.replaceState`。
6. 同步修改错误 mock 和断言，不能通过放宽断言让 Red 消失。

### 8.3 每完成一组怎样验证

开发过程中可以只跑目标测试：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd run test:run -- StoreDetailView.spec.js
npm.cmd run test:run -- MerchantProductsView.spec.js
npm.cmd run test:run -- ProductDetailView.spec.js
npm.cmd run test:run -- CartView.spec.js
npm.cmd run test:run -- http.spec.js
```

五组都完成后必须跑全量：

```powershell
npm.cmd run test:run
npm.cmd run build
```

要求：原 89 个测试和 A 新增测试全部通过；测试总数只能保持或增加，不能减少；构建成功。不要在本轮增加支付、退款、骑手、配送、优惠券、地图、WebSocket、复杂搜索或 UI 重构。

### 8.4 Green 完成后提交一次

```powershell
Set-Location '..'
git status --short
git diff --check
git add -- frontend
git diff --cached --check
git diff --cached --stat
git commit -m 'fix(frontend): clear final integration blockers [GREEN]'
git push -u origin fix/c-final-integration-green
```

在真实联调完成前不要删除该分支。合入 `develop` 后，B 再拉取一次并从第 9 节开始操作。

---

## 9. 第四步：在同一台联调电脑上启动真实后端和前端

下面两个进程必须在同一台电脑上运行，因为 `localhost` 只代表当前电脑。B 和 C 如果使用不同电脑，C 的 `localhost:8080` 不能访问 B 的后端。

最少工作方案：A 的 Red 和 C 的 Green 合入远程 `develop` 后，B 的电脑再次执行 `git pull --ff-only origin develop`，然后同时启动后端和前端进行真实联调。不要直接在未合并的评审分支上做最终验收。

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

## 13. 剩余 Green 完成后怎样交付

C 在个人分支先同步最新 `develop`：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git fetch origin --prune
git rebase origin/develop
```

重新执行前端测试和构建；若 rebase 包含后端修复，还要执行后端完整测试。全部通过后：

```powershell
git push -u origin fix/c-final-integration-green
git switch develop
git pull --ff-only origin develop
git merge --no-ff fix/c-final-integration-green
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
- [ ] P0-1 至 P0-5 已用 Red/Green 修复并进入远程 `develop`；
- [x] 30 个前端 API 的方法和路径已有自动契约覆盖；
- [x] 当前 `develop@3aa82ac` 的 25 个测试文件、89 个测试和生产构建通过；
- [ ] 修复后的前端新增测试和生产构建全部通过；
- [ ] 30 个接口真实联调全部通过；
- [ ] 必要的 401/403/404/409、乐观锁、幂等和重复取消已验证；
- [ ] 测试日志、Bug 记录和关键截图真实可追踪；
- [ ] A、B、C 的最终代码都已合入并推送远程 `develop`；
- [ ] 三个人从最新 `develop` 能按第 9 节启动并复现核心流程。

全部勾选后停止增加功能，进入课程报告、个人总结、演示脚本和答辩准备。
