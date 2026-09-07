# ABC 阶段 1 前后端联调、验收与交付执行文档

> 更新日期：2026-09-07
>
> 当前状态：B 已再次执行 `git pull --ff-only origin develop`，从 `cb1d93a` 快进到 `83c3f93 fix: close frontend integration blockers`。本地与远程 `develop` 完全一致。最新前端 25 个测试文件、90 个测试全部通过，生产构建成功。`fix/api-alignment` 及其本地评审分支仍不参与本轮。
>
> 当前唯一下一阶段：补齐 SRS 仍明确要求的三个最小前端行为和缺失的购物车回归测试，然后在同一台电脑启动 MySQL、后端和前端，完成 30 个接口的真实联调、证据记录和最终验收。90 个 mock 单元测试全绿仍不能替代真实联调。

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

2026-09-07，A 的测试提交 `a808d79` 和 C 的修复提交 `83c3f93` 已先后进入 `develop`。B 已从 `cb1d93a` fast-forward 到 `83c3f93`。本轮共修改 10 个前端文件，处理上次列出的店铺权限、商品上架、商品详情店铺名、购物车刷新和 401 跳转问题。第 5 节已经重新按 `83c3f93` 审计；此前针对 `3aa82ac`、`90fd50c` 或 `fix/api-alignment` 的待办不能代替本次结论。

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

## 5. `develop@83c3f93` 最新审计结论

### 5.1 本次拉取和自动验证结果

本次快进包含两个提交：

| 提交 | 含义 |
| --- | --- |
| `a808d79 test: complete frontend contract red coverage` | 先修改商品详情、顾客店铺详情和用户契约测试 |
| `83c3f93 fix: close frontend integration blockers` | 修复页面，并补商品上架和 401 测试 |

B 在当前 `develop@83c3f93` 实际执行：

```text
npm.cmd run test:run：25 个测试文件通过，90 个测试通过
npm.cmd run build：成功，1710 个模块完成生产构建
```

构建只有大于 500 kB 的 chunk 警告，不影响阶段 1。受限执行环境第一次运行 Vitest/Vite 时因临时文件得到 EPERM，换成正常本机权限后测试和构建均成功；这不是项目代码错误。

### 5.2 上次五个问题的处理结果

| 原编号 | 最新结果 | 代码证据 |
| --- | --- | --- |
| P0-1 顾客错误请求下架商品 | 已修复 | `StoreDetailView.vue` 已删掉 `includeOffSale: true`；顾客只查询默认上架商品 |
| P0-2 新商品无法上架 | 已修复 | `MerchantProductsView.vue` 已根据状态提供上架/下架，并携带当前 `version` |
| P0-3 商品详情读取不存在的 `shopName` | 已修复 | 页面根据商品 `shopId` 调用 `getStoreDetail()` 取得店铺名，测试也不再伪造该字段 |
| P0-4 购物车操作后不刷新 | 实现已修复，测试未补齐 | 修改数量、删除、创建订单成功后都调用 `loadCart()`；创建订单还显示成功提示 |
| P0-5 401 只修改地址栏 | 已修复 | `http.js` 改为 `router.replace('/')`，并有测试验证 |

用户注册和资料修改的契约样例也已改回 `passwordConfirm`、`nickname`、`phone`，30 个接口的方法和路径继续全部覆盖。不要再次修复这五项实现。

### 5.3 当前仍需补的三个最小业务缺口

这些不是新增需求，均来自 SRS 已有文字：

| 编号 | 当前缺口 | SRS 依据 | 最小实现 |
| --- | --- | --- | --- |
| R1 | 商品描述无法录入、修改或查看 | FR-PRODUCT-002 将描述列为基本字段；FR-PRODUCT-003 允许修改描述；Product 对象也包含 `description` | 在现有商品表单增加一个描述输入；创建和编辑请求透传 `description`；商品详情显示 `description` |
| R2 | 购物车“修改数量”只能执行 `当前数量 + 1`，不能减少或直接选择目标数量 | FR-CART-003 要求用户可以调整数量，数量必须为正整数且不超过库存 | 每行增加一个最小数字输入，提交用户选定的 `quantity`；不做复杂购物车组件 |
| R3 | 创建订单请求进行中仍可连续点击按钮 | NFR 9.5 明确要求订单提交过程中避免重复点击 | 增加 `submitting` 状态；请求期间禁用按钮；`finally` 恢复。幂等键规则保持不变 |

R1 至 R3 完成后，不再新增页面或业务模块，直接进入真实联调。

### 5.4 当前测试证据仍有的缺口

1. `CartView.spec.js` 中名为“submits delete action”的测试没有触发 ConfirmAction 的确认事件，反而断言 `removeCartItem` 没有被调用。它只能证明“未确认时不删除”，不能证明删除接口可用。
2. 购物车修改、删除、下单成功后的 `loadCart()` 刷新没有断言。
3. 下单成功提示没有断言。
4. `a808d79` 确实先于 Green 保存了顾客商品列表和商品详情两个 Red；商品上架、401 的测试与实现一起出现在 `83c3f93`，购物车刷新则没有对应测试。不要篡改 Git 历史宣称五项全都严格先 Red 后 Green，应在测试日志中如实记录。

当前补购物车刷新测试属于“既有实现的回归测试”，会直接 Green，不是假装成 Red。R1 至 R3 尚未实现，应继续严格先写失败测试再修改页面。

### 5.5 不阻塞联调，但不能误判的界面问题

- 店铺列表的“搜索店铺或商品”输入框没有 `v-model`，也没有发送 `keyword`。阶段 1 不做复杂搜索；最少工作是删除无效输入框。若保留，只允许按后端已有店铺 `keyword` 查询，不能承诺搜索商品。
- 店铺列表禁止进入 CLOSED 店铺，而 SRS 允许查看指定店铺详情和当前状态。真实联调时需要确认验收者是否要求关闭店铺也能进入详情；后端仍会阻止购买。
- 店铺详情和商品详情的加入购物车按钮没有结合店铺状态禁用。后端会正确拒绝闭店购买；当前至少要确认错误提示清楚，不必复制后端权限规则到前端。
- Axios 拦截器和部分页面 `catch` 可能对同一个失败各弹一次消息。若真实联调出现重复提示，再做一次小修，不在联调前扩展范围。

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

- `HEAD` 和 `origin/develop` 都是 `83c3f93`；
- 第一行日志是 `83c3f93 fix: close frontend integration blockers`；
- 下一行能看到 A 的 `a808d79 test: complete frontend contract red coverage`；
- `git status --short` 没有输出；
- 不复制压缩包，不从 `fix/api-alignment` 取文件，不执行针对该分支的 merge、rebase 或 cherry-pick。

若 `git status --short` 显示个人未提交文件，先停止切分支，让文件所有者确认；不要用 `reset --hard` 或 `checkout --` 丢弃文件。

### 6.1 当前三个人是否还要互相等待

不需要空等，但代码顺序必须保持：

1. A 先按第 7 节为 R1 至 R3 写新的 Red，并补购物车已有实现的回归测试；
2. C 可以先阅读要求，但 R1 至 R3 必须等 A 保存 Red 后再修改；
3. B 现在即可检查 MySQL、JDK 和后端能否启动，不修改已全绿的业务代码；
4. C 完成最后三个 Green 后，三人再进行第 9、10 节真实联调；
5. 真实联调若证明后端有缺陷，才由 A 补后端 Red、B 修 ServiceImpl/DAO/XML。

这表示 B 现在等的不是“A 再写 Controller”。30 个 Controller 和 B 的 ServiceImpl/DAO 已完成。B 等的是最后三个前端必要行为进入 `develop`；等待期间可以保持 MySQL、JDK 和后端环境可用。

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

## 7. 第二步：A 为三个剩余行为补 Red

### 7.1 建立测试分支

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git switch develop
git pull --ff-only origin develop
git switch -c test/a-final-ui-red
```

本轮不用每写一个测试就提交或 Push。把同一批 Red 全部写好、确认失败原因正确后提交一次即可。

### 7.2 R1：商品描述 Red

修改 `frontend/src/tests/unit/MerchantProductsView.spec.js` 和 `ProductDetailView.spec.js`：

1. 新增商品时填写“商品描述”，断言 `createProduct()` Body 包含 `description`；
2. 编辑商品时先从商品数据载入 `description`，修改后断言 `updateProduct()` Body 包含新描述和当前 `version`；
3. 商品详情接口返回 `description` 后，页面必须显示它。

示例核心断言：

```javascript
expect(mocks.createProduct).toHaveBeenCalledWith({
  shopId: 7,
  categoryId: 21,
  name: '牛肉饭',
  description: '招牌套餐',
  price: 18.8,
  stock: 20,
})
```

当前表单和详情都没有描述字段，因此这些测试应当 Red。

### 7.3 R2：购物车目标数量 Red

修改 `frontend/src/tests/unit/CartView.spec.js`：

1. 给购物车项提供可编辑数量控件；
2. 把原数量 3 改成 2；
3. 点击“保存数量”后断言：

```javascript
expect(mocks.updateCartItem).toHaveBeenCalledWith(1, { quantity: 2 })
```

这个测试必须证明数量可以减少，不能继续只断言“API 曾经被调用”。当前实现固定发送 `item.quantity + 1`，所以应当 Red。

### 7.4 R3：下单防重复点击 Red

修改 `CartView.spec.js`：

1. 让 `createOrder()` 返回一个暂时不完成的 Promise；
2. 第一次点击“创建订单”；
3. 断言按钮在 Promise 完成前处于 disabled；
4. 再次点击不能产生第二次 `createOrder()`；
5. Promise 完成后按钮恢复。

不能把幂等键测试删除。幂等键保护后端结果，disabled 保护用户交互，两者用途不同。

### 7.5 补购物车既有实现的回归测试

下面这些代码已经存在，因此新增测试会直接 Green，应标为回归测试而不是 Red：

- 修改数量成功后 `getCart()` 总调用次数从 1 变为 2；
- 删除确认后调用 `removeCartItem(item.id)`，随后再次 `getCart()`；
- 创建订单成功后再次 `getCart()`，并调用 `ElMessage.success('订单创建成功')`。

ConfirmAction 测试替身必须能够发出 `confirm`：

```javascript
ConfirmAction: {
  emits: ['confirm'],
  template: '<div><slot /><button class="confirm-button" @click="$emit(\"confirm\")">确认</button></div>',
}
```

不要保留“测试名称说会删除，断言却要求未调用删除”的假测试。

### 7.6 运行并保存 Red

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd run test:run
```

有效 Red 的判断：旧的 90 个测试没有被删除或 `skip`；R1 至 R3 的新增失败明确指向当前缺失行为；购物车刷新回归测试允许直接通过；失败不能来自导入或语法错误。记录结果后一次性提交：

```powershell
Set-Location '..'
git status --short
git diff --check
git add -- frontend/src/tests
git diff --cached --check
git commit -m 'test(frontend): cover final required ui behavior [RED]'
git push -u origin test/a-final-ui-red
```

团队如果不要求保留独立 Red 分支，可以由 C 合入或基于该分支继续；但必须保留能证明“测试先失败”的提交历史。

---

## 8. 第三步：C 只完成三个最小 Green

### 8.1 从最新基线开始

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git fetch origin --prune
git switch develop
git pull --ff-only origin develop
git switch -c fix/c-final-ui-green
```

将 A 的 Red 合入该分支，具体用 merge 还是 cherry-pick 由团队现有流程决定，只取 A 的测试提交，不取 `fix/api-alignment`。

### 8.2 按依赖顺序修改

1. `MerchantProductsView.vue`：在现有 `productForm` 增加 `description`，重置、载入编辑值、创建和修改请求都处理它；模板增加一个文本输入即可。
2. `ProductDetailView.vue`：从后端已有 `data.description` 赋值并显示；不新增接口。
3. `CartView.vue`：用数字输入保存目标数量，最小值 1、最大值商品库存；提交所选值而非固定加 1。
4. `CartView.vue`：增加 `submitting`；创建订单开始时设为 true，在 `finally` 设回 false，按钮绑定 `:loading` 和 `:disabled`。
5. 保留 `83c3f93` 已完成的五项修复，不重构 API 层或后端。

### 8.3 每完成一组怎样验证

开发过程中可以只跑目标测试：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd run test:run -- MerchantProductsView.spec.js
npm.cmd run test:run -- ProductDetailView.spec.js
npm.cmd run test:run -- CartView.spec.js
```

三组都完成后必须跑全量：

```powershell
npm.cmd run test:run
npm.cmd run build
```

要求：原 90 个测试和 A 新增测试全部通过；测试总数只能保持或增加，不能减少；构建成功。不要在本轮增加支付、退款、骑手、配送、优惠券、地图、WebSocket、复杂搜索或 UI 重构。

### 8.4 Green 完成后提交一次

```powershell
Set-Location '..'
git status --short
git diff --check
git add -- frontend
git diff --cached --check
git diff --cached --stat
git commit -m 'fix(frontend): complete required product and cart behavior [GREEN]'
git push -u origin fix/c-final-ui-green
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
git push -u origin fix/c-final-ui-green
git switch develop
git pull --ff-only origin develop
git merge --no-ff fix/c-final-ui-green
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
- [x] P0-1 至 P0-5 的实现修复已进入远程 `develop`；
- [ ] 购物车删除、刷新和下单成功已有有效回归测试；
- [ ] R1 商品描述、R2 目标数量、R3 防重复点击已按 Red/Green 完成；
- [x] 30 个前端 API 的方法和路径已有自动契约覆盖；
- [x] 当前 `develop@83c3f93` 的 25 个测试文件、90 个测试和生产构建通过；
- [ ] 修复后的前端新增测试和生产构建全部通过；
- [ ] 30 个接口真实联调全部通过；
- [ ] 必要的 401/403/404/409、乐观锁、幂等和重复取消已验证；
- [ ] 测试日志、Bug 记录和关键截图真实可追踪；
- [ ] A、B、C 的最终代码都已合入并推送远程 `develop`；
- [ ] 三个人从最新 `develop` 能按第 9 节启动并复现核心流程。

全部勾选后停止增加功能，进入课程报告、个人总结、演示脚本和答辩准备。
