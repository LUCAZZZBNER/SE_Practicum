# `fix/api-alignment` 分支评审与后续执行文档

> 评审日期：2026-09-06
>
> 远程来源：`origin/fix/api-alignment`
>
> 本地评审分支：`codex/fix-api-alignment-review`
>
> 分支提交：`901884a feat: align frontend with backend api`
>
> 当前 `develop`：`90fd50c feat: add merchant order pages`
>
> 结论：该分支包含值得保留的前端重构和课程交付文档框架，但它从旧基线分叉，直接合并会产生 15 个冲突，而且仍有多项确定缺陷。禁止直接合入 `develop`；必须在最新 `develop` 上建立集成分支，先补 Red，再选择性吸收有效改动并修复问题。

---

## 1. 什么内容说了算

评审和后续修复按以下顺序判断：

1. 根目录 `26271学期-软件工程综合实践.md`：课程根本要求；
2. `docs/software-requirements-specification.md`：阶段 1 业务要求；
3. `docs/api/backend-api-design.md`：冻结的 30 个 HTTP 接口；
4. 后端 Controller、Service 契约和已经通过的后端测试；
5. 前端 API 契约测试、页面测试和真实联调结果；
6. 本文只负责记录评审结论和执行顺序，不改变上面文件的权威性。

`fix/api-alignment` 修改了 `docs/api/backend-api-design.md`、架构文档和功能分析文档。这些改动大部分是在补充当前后端已有行为，例如商品详情的 `includeOffSale` 和商品 PATCH 必须带 `version`，但仍属于修改权威文档。未经 A 和小组共同确认，不得因为前端分支改过文档就自动把它当成新契约。

---

## 2. 本地拉取结果和分支关系

已经完成：

```text
origin/fix/api-alignment              -> 901884a
codex/fix-api-alignment-review        -> 901884a，跟踪远程分支
develop / origin/develop              -> 90fd50c
两条分支的共同祖先                    -> 9dbab0d
```

为避免覆盖 `develop` 上尚未提交的执行文档，评审分支使用独立工作树：

```text
D:\Projects\SchoolWorks\SW_2609\SE_Practicum\.idea\api-alignment-review
```

主工作区仍在 `develop`，评审工作树在 `codex/fix-api-alignment-review`。两边互不覆盖。

重要含义：

- `develop` 在共同祖先之后新增了 `a611687`、`0b01546`、`90fd50c` 三批前端修正；
- `fix/api-alignment` 没有包含这些提交，而是在相同旧基线上独立重构；
- 不能把该分支理解为“develop 的新版”；它是与 develop 并行的另一套实现；
- 不允许用 `git reset --hard`、强制推送或整目录覆盖来处理差异。

查看分支时可执行：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git branch -vv --list 'codex/fix-api-alignment-review'
git log --oneline develop..codex/fix-api-alignment-review
git log --oneline codex/fix-api-alignment-review..develop
git diff --stat develop..codex/fix-api-alignment-review
```

---

## 3. 两个分支总体差异

### 3.1 后端

后端源码树没有差异：

```text
git diff develop..codex/fix-api-alignment-review -- backend
结果：无输出
```

因此本轮不得重新修改数据库、DAO、ServiceImpl 或 Controller。B 的后端实现不是这次分支整合对象。

### 3.2 前端

该分支主要增加或重构：

- `ApiError`，保留后端 HTTP 状态、业务码和错误数据；
- `auth/session.js`，统一保存和清理令牌、角色、商家所选店铺；
- 真实退出登录；
- `merchantShop` Pinia store，加载、选择、保存商家当前店铺；
- 创建店铺 API；
- 分类 API 与直接数组响应处理；
- 商家多店铺选择；
- 购物车勾选结算、同店铺检查和幂等键保存；
- 用户取消订单；
- 店铺列表或详情加入购物车；
- 商家订单列表和详情；
- 前端契约、会话、商家店铺 store 测试。

### 3.3 文档

该分支新增 `handin-docs`，包括：

- 项目说明；
- 系统架构和接口设计；
- 数据库设计；
- TDD 测试计划；
- 需求迭代基线；
- 交叉验收检查表；
- 测试日志、覆盖率、回归记录、分工日志和 AI 使用记录的目录占位。

课程根本要求确实需要项目说明、数据库设计、TDD 报告、需求迭代记录、团队分工与开发日志以及验收材料，所以这个目录方向有价值。但当前结果类目录只有 README，占位不等于已经完成实际记录。

---

## 4. 自动验证结果

在评审分支独立工作树执行：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\.idea\api-alignment-review\frontend'
npm.cmd ci
npm.cmd run test:run
npm.cmd run build
```

实际结果：

```text
Test Files：25 passed
Tests：74 passed
Vite build：成功
转换模块：1712
```

对比当前 `develop`：

```text
develop：25 个测试文件，79 个测试通过
fix/api-alignment：25 个测试文件，74 个测试通过
```

分支能够测试和构建，但不能因此判定可合并，原因如下：

- 测试期间反复出现 `el-checkbox`、`el-empty`、`el-option`、`el-select`、`el-button` 未注册警告；
- `AppHeader` 测试出现缺少 Router 注入警告；
- 这些警告未使 Vitest 失败，却会淹没真正的 Vue 警告；
- 分支相对 develop 删除了商家订单列表和详情的两个页面测试文件；
- `api-contract.spec.js` 只覆盖 22 个接口，仍缺用户和商家共 8 个身份接口。

构建的大包警告不阻塞当前课程项目，不要为了它额外做代码分包。

---

## 5. 值得选择性保留的改动

以下内容方向正确，但仍必须在最新 `develop` 上配合 Red 检查后整合：

| 改动 | 价值 | 整合注意事项 |
| --- | --- | --- |
| `frontend/src/api/errors.js` | 页面可以识别后端业务码和 `data.currentItems` | 保留 develop 的统一响应解包规则 |
| `frontend/src/auth/session.js` | 统一令牌、角色和商家店铺选择的存储清理 | 401 后还需主动进入登录流程 |
| `frontend/src/stores/merchantShop.js` | 支持商家多个店铺和当前店铺选择 | 登出、换商家和无店铺时必须重置 |
| 创建店铺 API | 补上 30 个接口中 develop 缺少的 `POST /shops` | 页面不能只用写死名称创建 |
| 分类直接数组处理 | 修复 develop 把 `Category[]` 当分页对象的问题 | 可以保留独立 `category.js`，不必搬进 `store.js` |
| 订单封装 | 把 items 和 idempotency key 分开传入，语义更清楚 | 同请求重试复用 key；请求内容变化必须换 key |
| 购物车选择结算 | 支持只结算所选项并阻止混合店铺 | 显示总额应与所选项一致；选择变化重置 key |
| 用户取消订单 | 补上阶段 1 必须页面操作 | 只在 `PENDING_PAYMENT` 显示并刷新结果 |
| 真实退出登录 | 清理会话后回到入口 | 增加 Router stub 和点击测试 |
| `handin-docs` 框架 | 对应课程最终交付类别 | 删除非交付指令文件，补真实证据，人工校对 |

---

## 6. 当前确定存在的问题

### 6.1 阻断真实联调的问题

#### 问题 1：Vite 开发代理被删除

该分支的 `frontend/vite.config.js` 只有端口和 host，而 `http.js` 默认使用 `/api/v1`。浏览器运行在 `localhost:5173` 时，请求会发给 Vite 自己，不能到达 `localhost:8080` 的 Spring Boot。

必须保留 develop 的配置：

```javascript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
},
```

#### 问题 2：店铺状态枚举回退为错误值

`MerchantStoreView.vue` 使用：

```text
TEMP_CLOSED
```

后端和 API 文档只接受：

```text
TEMPORARILY_CLOSED
```

当前代码选择临时闭店后会收到 HTTP 400。

#### 问题 3：商品详情读取后端不存在的字段

后端 `ProductView` 有：

```text
id, shopId, categoryId, name, description, price, stock, status, version, createdAt, updatedAt
```

没有 `shopName` 和 `categoryName`。该分支 `ProductDetailView.vue` 直接读取这两个字段，因此真实响应下两个名称为空。

最小修正：通过 `shopId` 查询店铺，通过 `shopId` 查询分类数组，再用 `categoryId` 找分类名称；或者不展示无法从当前接口取得的字段。不能假造后端返回字段。

#### 问题 4：商品和分类管理仍不完整

`MerchantProductsView.vue` 中：

- “新增分类”没有点击事件；
- 没有分类修改和删除操作；
- “编辑”商品没有点击事件；
- 新增商品固定创建名称“新商品”、价格 `0.01`、库存 `0`，不能让商家填写真实值；
- 再次点击可能因为同店重名产生 409。

阶段 1 明确要求商家管理分类和商品，不能只靠占位按钮通过验收。

#### 问题 5：401 只清理会话，不跳转

`http.js` 收到 401 时调用 `clearSession()`，这是进步；但没有进入登录入口。当前路由守卫只在发生路由跳转时运行，所以用户会停留在受保护页面。

需要在避免循环依赖的前提下统一跳转到 `/` 或独立登录入口，并保留原目标地址（若团队需要）。

#### 问题 6：幂等键在选择内容变化后仍可能复用

购物车第一次提交失败后，`idempotencyKey` 会保留，这是正确的网络重试行为。但如果用户随后改变勾选项，代码没有清除 key，下一次会用相同 key 提交不同 body，后端将返回 `IDEMPOTENCY_CONFLICT`。

规则必须是：

```text
同一批 items 重试 -> 复用原 key
勾选项、数量或购物车内容变化 -> 清除旧 key，下次生成新 key
成功 -> 清除 key
```

### 6.2 明确的页面和测试问题

#### 问题 7：店铺分类数量写死

`StoreDetailView.vue` 显示：

```text
分类数量：3
```

必须改为：

```text
categories.length
```

#### 问题 8：创建店铺仍是占位交互

无店铺时点击按钮会直接创建：

```json
{ "name": "新店铺", "description": "" }
```

应该先显示名称和简介输入表单，再提交用户填写值。固定名称只能证明 API 能调用，不能完成可用的店铺创建流程。

#### 问题 9：契约测试仍缺 8 个接口

`api-contract.spec.js` 覆盖店铺/商品 8 个、分类/购物车 8 个、订单 6 个，共 22 个。仍缺：

```text
POST  /users
POST  /users/login
GET   /users/me
PATCH /users/me
POST  /merchants
POST  /merchants/login
GET   /merchants/me
PATCH /merchants/me
```

此外，测试中 `createProduct({ shopId: 7 })` 使用不完整 body，只能验证 URL，不能保护真实请求字段。

#### 问题 10：测试覆盖发生回退

相对 develop，该分支删除或替换了部分测试，最终少 5 个用例。至少需要恢复：

- 商家订单列表页面的加载、展示、跳转和失败测试；
- 商家订单详情页面的加载、展示和失败测试；
- 用户订单详情加载失败测试；
- 当前 develop 中仍然有效的店铺/商品页面断言。

整合时只能合并两边有效测试，不能以新测试文件为理由删除原有保护。

#### 问题 11：组件测试警告未清理

为测试中用到的 Element Plus 组件增加最小 stub，并为 `AppHeader` 注入测试 Router。目标不是美化测试，而是让测试输出没有大量无关警告，便于发现真实异常。

### 6.3 文档问题

#### 问题 12：修改了冻结文档

`08dbfb2` 修改了：

```text
docs/api/backend-api-design.md
docs/architecture/backend-architecture-design.md
docs/feature-analysis.md
```

这些修改看起来与现有后端实现一致，但仍必须由 A 按 Controller、Service 和测试逐条确认。不要直接 cherry-pick `08dbfb2`，也不能让前端实现反过来决定后端接口。

#### 问题 13：交付材料仍有空壳

以下目录当前只有简短 README，没有实际证据：

```text
handin-docs/04-TDD测试报告/测试日志
handin-docs/04-TDD测试报告/覆盖率报告
handin-docs/04-TDD测试报告/回归测试记录
handin-docs/06-团队分工与开发日志
handin-docs/08-AI辅助使用记录
```

测试计划和验收检查表只是“计划/模板”，不能写成已经完成的测试结果。

#### 问题 14：`handin-docs/AGENTS.md` 不应作为课程交付物

它是自动化工具工作指令，不是课程要求的项目文档，还包含“不使用远程Git”等与团队当前流程冲突的内容。最终整合 `handin-docs` 时不要带入该文件，也不要为了它修改根目录 `.gitignore`。

课程手册明确要求记录 AI 辅助使用情况并由学生理解、校对和修改输出；不能用工具指令文件替代真实 AI 使用记录。

### 6.4 可整理但不阻断的问题

- `store.js` 同时存在 `updateStoreStatus()` 和 `updateStore()`，二者调用完全相同；保留一个清楚名称即可；
- 分类 API 被放进 `store.js`，而 develop 已有独立 `category.js`；为减少改动，建议继续使用独立文件；
- `router/guards.js` 的 import 放在文件末尾，语法有效但可移到顶部；
- 购物车总额目前按全部购物车项计算，选择部分结算时应显示所选项总额，避免用户误解；
- 不需要为解决 Vite 大包警告引入新的状态框架、组件库或构建插件。

---

## 7. 直接合并会冲突的 15 个文件

已使用 Git 合并树模拟，冲突文件为：

```text
frontend/src/api/order.js
frontend/src/api/store.js
frontend/src/layouts/MerchantLayout.vue
frontend/src/router/modules/merchant.js
frontend/src/tests/unit/MerchantProductsView.spec.js
frontend/src/tests/unit/OrderDetailView.spec.js
frontend/src/tests/unit/OrdersView.spec.js
frontend/src/views/CartView.vue
frontend/src/views/MerchantOrderDetailView.vue
frontend/src/views/MerchantOrdersView.vue
frontend/src/views/MerchantProductsView.vue
frontend/src/views/MerchantStoreView.vue
frontend/src/views/OrderDetailView.vue
frontend/src/views/ProductDetailView.vue
frontend/src/views/StoreDetailView.vue
```

因此禁止执行完 `git merge fix/api-alignment` 后对所有冲突统一选择 “Accept Current” 或 “Accept Incoming”：

- 全选 current 会丢掉重构的有效改进；
- 全选 incoming 会丢掉 develop 的代理、正确枚举、更多测试和最近页面修复；
- 必须按第 9 节逐功能整合。

---

## 8. 推荐的分支策略

不要改写或强推别人的 `fix/api-alignment`。把它保留为只读参考，在最新 develop 上创建新的集成分支：

```text
origin/fix/api-alignment             只读来源
codex/fix-api-alignment-review       本地只读评审分支
feature/api-alignment-integration    真正解决冲突和补测试的工作分支
develop                              最终验收通过后才合入
```

在创建集成分支前，先处理当前两个文档文件。确认内容后统一提交一次即可，不要把未提交文档带进代码集成：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git status --short
git add -- 'ABC阶段1业务TDD开发与联调执行文档.md' 'fix-api-alignment分支评审与后续执行文档.md'
git diff --cached --check
git commit -m 'docs: record api-alignment branch review'
git push origin develop
```

然后创建集成分支：

```powershell
git switch develop
git pull --ff-only origin develop
git switch -c feature/api-alignment-integration
git status --short --branch
```

不要在 `codex/fix-api-alignment-review` 上继续堆修复，也不要直接 rebase 或强推远程作者的分支。

---

## 9. 后续 TDD 执行顺序

### 9.1 A：先补 Red

A 在最新 `develop` 或集成分支最前端建立测试提交，不能先复制修复实现。至少补：

1. 用户和商家 8 个 API 包装契约；
2. Vite 开发环境请求能够转发到 8080 的配置检查；
3. 临时闭店只能提交 `TEMPORARILY_CLOSED`；
4. 商品详情不能依赖后端不存在的 `shopName/categoryName`；
5. 商家可以用表单创建店铺、分类和商品；
6. 分类可以修改和删除；
7. 商品编辑必须带 `version`；
8. 用户取消待支付订单；
9. 同一 items 重试复用 key，items 变化生成新 key；
10. HTTP 401 清理会话并进入登录入口；
11. 店铺分类数量使用真实数组长度；
12. 商家订单列表和详情测试不得丢失；
13. 测试输出不再出现未注册组件和 Router 注入警告。

运行 Red：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd ci
npm.cmd run test:run
```

有效 Red 的要求：原有 develop 79 个测试仍通过，新增测试因真实功能缺失而失败；不能用导入错误、语法错误或测试环境错误冒充 Red。

保存 Red：

```powershell
Set-Location '..'
git add -- frontend/src/tests
git diff --cached --check
git commit -m 'test(frontend): cover api alignment gaps [RED]'
```

### 9.2 C：第一批 Green——基础请求与会话

参考评审分支，但不要整文件覆盖：

```powershell
git diff develop..codex/fix-api-alignment-review -- frontend/src/api/http.js
git diff develop..codex/fix-api-alignment-review -- frontend/src/auth/session.js
git diff develop..codex/fix-api-alignment-review -- frontend/src/api/errors.js
git diff develop..codex/fix-api-alignment-review -- frontend/src/components/layout/AppHeader.vue
```

完成：

- 增加 `ApiError`；
- 统一 `saveSession/getSession/clearSession`；
- 用户和商家登录调用 `saveSession`；
- 退出登录清理会话并返回入口；
- 401 清理并跳转；
- 保留 develop 的 Vite `/api` 代理；
- 保留 `/api/v1` 基础路径。

执行测试，不提交无关页面。

### 9.3 C：第二批 Green——店铺上下文

参考：

```powershell
git diff develop..codex/fix-api-alignment-review -- frontend/src/stores/merchantShop.js
git diff develop..codex/fix-api-alignment-review -- frontend/src/api/store.js
git diff develop..codex/fix-api-alignment-review -- frontend/src/views/MerchantStoreView.vue
```

完成：

- 增加创建店铺 API；
- 增加商家店铺 Pinia store；
- 支持多个本人店铺的加载和选择；
- 无店铺时先填写名称、简介再创建；
- 店铺状态只使用 `OPEN/CLOSED/TEMPORARILY_CLOSED`；
- 不重复保留两个相同 PATCH 方法。

### 9.4 C：第三批 Green——分类和商品

保留 develop 的独立 `category.js`，不要因为评审分支把分类函数放入 `store.js` 就删除它。

完成：

- 分类列表直接读取数组；
- 商家分类新增、改名、排序、删除；
- 商品新增使用可填写的合法表单；
- 商品编辑携带当前 `version`；
- 商品详情通过现有接口组合店铺名和分类名，或者删除无来源字段；
- 店铺详情显示 `categories.length`；
- 店铺详情加入购物车按钮真实调用 `addCartItem`。

### 9.5 C：第四批 Green——购物车和订单

完成：

- 勾选要结算的可用购物车项；
- 显示所选项总额；
- 拒绝混合店铺；
- 相同 items 网络重试复用幂等键；
- 勾选、数量或购物车内容变化后清除旧 key；
- 成功下单后清除 key、刷新购物车并进入订单详情或订单列表；
- 待支付订单可取消并刷新；
- 商家订单按当前所选店铺查询；
- 保留 develop 已有的商家订单页面测试。

### 9.6 每批 Green 后验证

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd run test:run
npm.cmd run build
```

要求：

- 原有 79 个测试全部保留并通过；
- A 新增测试全部通过，因此最终数量必须大于 79；
- 没有 `skip`、空断言或删测试换绿色；
- 没有未注册组件和 Router 注入警告；
- Vite 构建成功。

完成所有 Green 后统一提交一次或按上述四个有意义批次提交，不要求每个按钮单独提交。

---

## 10. `handin-docs` 怎样处理

### 10.1 不要直接整批 cherry-pick

不要直接执行：

```text
git cherry-pick 1bd74b8^..08dbfb2
```

原因：这会带入 `handin-docs/AGENTS.md`、`.gitignore` 改动和未经审核的权威文档修改。

### 10.2 可以选择性参考的提交

```text
1bd74b8 项目说明框架，但排除 handin-docs/AGENTS.md
7baaeda 架构与接口交付文档
bc2e08f 数据库设计文档
a9515e8 TDD计划、需求基线、验收表
ff0e4fd 分工日志和AI记录目录框架
92af332 设计验收口径补充
```

每份文档都必须由对应成员对照真实代码和测试人工校对。课程手册明确要求学生理解、甄别和修改 AI 辅助产出，禁止直接原样当作小组设计决策。

### 10.3 必须补的真实材料

| 材料 | 负责人 | 什么时候填写 |
| --- | --- | --- |
| 后端完整测试日志 | B 执行，A复核 | 最终代码提交号确定后 |
| 前端测试和构建日志 | C 执行，A复核 | 最终 Green 后 |
| JaCoCo/Vitest覆盖率 | A整理 | 最终全量测试后 |
| Red/Green/回归记录 | A | 每个真实迭代完成后 |
| 团队分工与开发日志 | A/B/C各写自己的实际工作 | 不得由一人猜写全部 |
| AI辅助使用记录 | 使用者本人 | 写实际使用点和人工修改内容 |
| 交叉验收结果 | 第三周验收人员 | 真实执行后填写，不能预填通过 |

---

## 11. B 在本轮负责什么

B 不参与前端冲突的随意选边，也不为了前端重构修改后端契约。B 负责：

1. 确认后端源码在两个分支之间无差异；
2. 保持 V1、V2 迁移和数据库账号可用；
3. C 完成 Green 后运行后端完整测试；
4. 在同一台电脑启动 MySQL、后端和前端；
5. 真实联调证明是后端缺陷时，才创建 `fix/b-integration` 修复；
6. 提供真实后端测试、迁移和联调日志，不替 A/C 猜测结果。

后端启动：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$env:JAVA_HOME = 'D:\Dev\Java\JDK17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DELIVERY_DB_USERNAME = 'delivery_app'
$env:DELIVERY_DB_PASSWORD = Read-Host '输入本机 delivery_app 密码' -MaskInput
$env:SPRING_PROFILES_ACTIVE = 'dev'
.\mvnw.cmd spring-boot:run
```

前端启动：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd run dev
```

真实联调前必须确认浏览器请求 `/api/v1/...` 被代理到 `localhost:8080`。

---

## 12. 最终测试和合并

### 12.1 自动测试

后端：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\backend'
$env:SPRING_PROFILES_ACTIVE = 'test'
$env:DELIVERY_DB_USERNAME = 'delivery_app'
$env:DELIVERY_DB_PASSWORD = Read-Host '输入本机 delivery_app 密码' -MaskInput
.\mvnw.cmd clean test
```

前端：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd ci
npm.cmd run test:run
npm.cmd run build
```

### 12.2 真实业务链

至少实际完成：

```text
商家注册和登录
  → 创建店铺并设置 OPEN
  → 创建、修改、删除可删除分类
  → 创建、编辑、上架商品
用户注册和登录
  → 浏览店铺、分类、商品
  → 加入和修改购物车
  → 选择同店铺商品创建订单
  → 查询详情并取消待支付订单
商家重新登录
  → 按所选店铺查看订单列表和详情
```

同时验证 401、403、400、404、409、商品版本变化、库存不足、同键同请求、同键不同请求。

### 12.3 合并集成分支

全部通过后：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git switch feature/api-alignment-integration
git fetch origin --prune
git rebase origin/develop
```

rebase 后重新执行全部自动测试和真实关键业务链。然后：

```powershell
git push -u origin feature/api-alignment-integration
git switch develop
git pull --ff-only origin develop
git merge --no-ff feature/api-alignment-integration
git push origin develop
git status --short --branch
```

只有合并后的 develop 再次全绿并推送成功，才算整合完成。

---

## 13. 停止条件

出现以下任意情况，不得合并：

- 15 个冲突仍有任何一个未经过逐文件人工判断；
- Vite `/api` 代理缺失；
- 仍使用 `TEMP_CLOSED`；
- 页面仍读取后端不存在字段；
- 分类或商品管理仍是无效按钮或固定占位数据；
- 30 个接口契约没有全部覆盖；
- develop 原有测试被删除；
- 测试虽绿但仍大量输出 Vue 未注册组件警告；
- 401、幂等重试或版本冲突行为未验证；
- `handin-docs` 把计划、模板或空目录写成实际通过结果；
- `handin-docs/AGENTS.md` 仍在待合并内容中；
- 权威 API/SRS 被前端分支未经小组确认擅自改动；
- 真实前后端联调尚未执行。

达到以上反向条件后，才进入最终交付和第三周交叉验收。
