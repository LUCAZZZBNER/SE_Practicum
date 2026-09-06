# ABC 阶段 1 前后端联调、验收与交付执行文档

> 更新日期：2026-09-06
>
> 当前状态：2026-09-06 本轮同步检查时，本地和远程 `develop` 的共同基线为 `90fd50c`。后端阶段 1 实现已经完成并全绿；C 的三批前端契约修正 `a611687`、`0b01546`、`90fd50c` 已进入 `develop`。B 已验证最新前端 25 个测试文件、79 个测试全部通过，生产构建成功。
>
> 当前唯一下一阶段：A 为最新审计仍缺少的必要功能补 Red，C 完成剩余最小 Green，B 保持数据库和后端稳定；随后三人在同一台电脑完成 30 个接口的真实联调、证据记录和最终验收。

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

2026-09-06 第一次同步检查时，共同基线为 `69efde1`，当时最后一条前端提交是 `a633dd6`，由此确认 B 的后端合并没有覆盖或回退 C 的前端。随后 C 将 `a611687`、`0b01546`、`90fd50c` 三批修正推入 `develop`；B 再次执行 `git pull --ff-only origin develop`，以 fast-forward 拉取到 `90fd50c`。第 5 节已经按这批最新源码重新审计，旧结论不能代替当前结论。

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

## 5. 最新前端审计结果：大部分契约已修正，仍有必要缺口

### 5.1 本轮已经修复并验证的内容

`develop` 从 `9dbab0d` 前进到 `90fd50c`，新增或修改 27 个前端文件。下列旧问题已经解决，不要重复返工：

- 购物车四个 API 已统一为 `/cart-items`；
- 商品列表已使用 `/shops/{shopId}/products`；
- 店铺修改已使用 `PATCH /shops/{shopId}`；
- 已新增四个分类 API；
- 已补齐用户订单四个、商家订单两个 API；
- 创建订单已携带 `cartItemId`、`productVersion` 和 `X-Idempotency-Key`；
- 店铺字段已改为 `description`，状态已改为 `TEMPORARILY_CLOSED`；
- 商品下架已携带当前 `version`；
- 用户订单详情已改为按路由 ID 查询，订单列表已使用 `orderNumber`、`shopName`、`total`；
- 已新增商家订单列表、详情页面和路由；
- Vite 已把 `/api` 代理到 `http://localhost:8080`；
- 用户和商家资料页已经调用各自独立的 API。

2026-09-06 在 B 的电脑执行最新前端自动验证：

```text
npm.cmd run test:run：25 个测试文件通过，79 个测试通过
npm.cmd run build：成功，1710 个模块完成生产构建
```

### 5.2 当前仍然确认存在的最小必要问题

这些结论来自 `90fd50c` 前端源码与后端 Controller、SRS、API 文档的逐项对照，不是猜测：

| 优先级 | 当前问题 | 证据 | 最小修正 |
| --- | --- | --- | --- |
| 必须 | 缺少创建店铺 | `store.js` 只有列表、详情、修改；无 `POST /shops`。无店铺时 `MerchantStoreView.vue` 只显示“未找到店铺” | 增加 `createShop(data)`；无店铺时显示名称、简介表单并调用它 |
| 必须 | 分类列表响应读取错误 | 后端返回 `Category[]`；`ProductDetailView.vue`、`StoreDetailView.vue` 却读取 `categories?.items` | 直接把返回值作为数组使用 |
| 必须 | 分类管理没有页面操作 | `MerchantProductsView.vue` 的“新增分类”按钮没有事件，也没有修改、删除入口 | 在现有商品管理页增加最小分类新增、改名、删除操作 |
| 必须 | 商品新增和编辑不可用 | “新增商品”立即发送 `categoryId: null`、空名称、`price: 0`，必然得到 400；“编辑”按钮没有事件 | 增加最小表单，提交合法 `shopId/categoryId/name/price/stock`；编辑时带 `version` |
| 必须 | 用户无法从页面取消订单 | `cancelOrder()` API 已存在，但 `OrderDetailView.vue` 没有取消按钮 | 仅在允许状态显示取消按钮，调用后刷新订单 |
| 必须 | 同一次结算不能正确复用幂等键 | `CartView.vue` 每次点击都执行 `crypto.randomUUID()` | 一次结算先生成并保存 key；超时重试复用；成功或购物车变化后再清除 |
| 必须 | 401 不会清理失效登录 | `http.js` 只显示错误；API 文档要求 401 时清除令牌并回到登录流程 | 清除 `access_token`、`user_role`，进入登录入口，并增加测试 |
| 必须 | 当前契约测试没有覆盖全部 30 个接口 | `apiContracts.spec.js` 只有 5 个测试块，实际断言 19 个接口调用；未覆盖用户/商家 8 个、创建店铺 1 个、商品新增/修改 2 个 | A 补齐剩余 11 个接口调用断言及上述页面行为 Red |

另外，店铺详情页的“加入购物车”按钮目前没有事件。最小方案二选一：接入现有 `addCartItem()`，或者删除该无效按钮并只保留“详情”，由商品详情页完成加入购物车。不要保留一个点击后没有任何效果的按钮。

前端单元测试使用 mock 是正常的；问题在于现有 mock 断言接受了无效请求，例如测试明确期待新增商品提交空字段，因此测试虽绿，真实后端仍会返回 400。不能为保持旧测试绿色而保留错误业务行为。

---

## 6. 第一步：同步 `90fd50c` 后补齐剩余 Red

B 的电脑已经拉取并验证到 `90fd50c`。A、C 在各自电脑执行：

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
- 能看到 `a611687`、`0b01546`、`90fd50c` 三批前端修正；
- 不再复制文件或压缩包手工同步。

B 当前不再等待新的前端 Push。三人按下面顺序完成剩余工作：

1. A 依据第 5.2 节补齐剩余 Red，不重写已经通过的 19 个接口断言；
2. C 从最新 `develop` 创建修正分支，只完成第 5.2 节的最小必要 Green；
3. B 保持数据库与后端稳定，准备第 9 节真实联调环境；
4. A 的补充 Red 进入共同基线后，C 同步并逐项变绿；
5. 三人不再重复检查已经修正的旧路径。

C 创建本阶段工作分支：

```powershell
git switch -c feature/c-integration-fixes
```

A 只有在补充当前缺少的测试时才创建 `test/a-contract-completion`；B 只有在真实联调发现后端缺陷时才创建 `fix/b-integration`。没有代码改动的人不创建空分支和空提交。

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

### 6.2 B 的前端运行准备已经完成

2026-09-06，B 已使用下列环境完成最新前端基线验证：

```text
Node.js v24.19.0
npm 11.17.0
npm ci：成功，安装并审计 179 个依赖包
npm.cmd run test:run：初次基线为 22 个测试文件、67 个测试通过
拉取 `90fd50c` 后：25 个测试文件、79 个测试通过
npm.cmd run build：两次均成功；最新一次转换 1710 个模块
```

依赖安装过程中显示的 deprecated、allow-scripts、5 vulnerabilities 和大于 500 kB 的 chunk 均为警告，没有导致测试或构建失败。当前阶段不要执行 `npm audit fix --force`，以免强制升级依赖并改动已经锁定的版本。

以后在 B 的电脑重新验证前端时执行：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
node -v
npm.cmd -v
npm.cmd ci
npm.cmd run test:run
npm.cmd run build
```

若其他成员或新电脑提示无法识别 `node` 和 `npm`，先安装 Node.js LTS：

```powershell
winget install --id OpenJS.NodeJS.LTS -e
```

安装完成后必须关闭旧 PowerShell 并重新打开，再执行上面的验证命令。如果 PowerShell 的脚本执行策略拦截 `npm.ps1`，直接使用文中所写的 `npm.cmd`，不需要修改系统执行策略。

这一步只证明“C 当前交付版的前端自身测试和构建通过”，不能宣布联调完成。当前 79 个测试大量 mock 了 API 模块，没有真正调用 Spring Boot；其中 `apiContracts.spec.js` 的 5 个测试块合计只断言了 19 个接口调用，不是覆盖了全部 30 个接口。下一步仍必须由 A 完成第 7 节补充 Red，再由 C 完成第 8 节剩余 Green。

---

## 7. 第二步：A 只补当前缺少的 Red

### 7.1 接口契约测试究竟测什么

接口契约测试检查的是“前端准备发送的请求是否和后端规定完全一致”，不是检查页面样式，也不直接访问 MySQL。每个接口至少核对：

1. HTTP 方法：`GET`、`POST`、`PATCH`、`DELETE`；
2. 路径：例如用户登录必须是 `/users/login`；
3. Path 参数：例如订单 ID 必须进入 `/orders/1001`；
4. Query 参数：分页、筛选和排序必须放在 Axios 的 `params` 中；
5. Body：字段名、嵌套结构和值是否符合 API 文档；
6. Header：Bearer Token 和创建订单的 `X-Idempotency-Key`；
7. 返回结构的使用方式：分页对象读取 `.items`，直接数组不能读取 `.items`。

以前端用户登录为例，测试调用真实的前端 API 函数：

```javascript
loginCustomer({
  account: 'user01',
  password: '123456',
})

expect(mocks.post).toHaveBeenCalledWith('/users/login', {
  account: 'user01',
  password: '123456',
})
```

如果实现误写为 `/user/login`，测试立即失败；写成 `/users/login` 才通过。

### 7.2 为什么契约测试仍然使用 mock

这里仅 mock 最底层的 `api/http.js`，不 mock 正在检查的 `loginCustomer()`、`createShop()` 等 API 函数：

```text
测试
  → 真实调用前端 API 函数
  → API 函数调用假的 http.get/post/patch/delete
  → 测试检查假的 http 方法实际收到了什么
```

这与页面测试把整个 API 模块替换掉不同。页面测试如果直接伪造 `createProduct()` 成功，可能看不到内部路径；契约测试观察底层 HTTP 调用，所以能发现错误的方法、路径、参数、Body 和 Header。

### 7.3 Red 是什么，怎样才算有效

Red 不是故意写坏测试，而是先把 SRS/API 文档中的正确要求写成断言，让当前缺失或错误实现暴露为测试失败。例如当前没有 `createShop()`：

```javascript
import { createShop } from '../../api/store'

it('uses POST /shops to create a shop', () => {
  const body = { name: '测试店铺', description: '测试简介' }

  createShop(body)

  expect(mocks.post).toHaveBeenCalledWith('/shops', body)
})
```

当前代码会因为没有导出 `createShop` 而失败，这是有效 Red。C 随后只需增加正确封装，测试变绿：

```javascript
export function createShop(data) {
  return http.post('/shops', data)
}
```

有效 Red 必须同时满足：

1. 预期来自 SRS、`backend-api-design.md` 或后端 Controller，而不是凭记忆；
2. 失败原因是当前业务缺失或契约错误；
3. 原有 79 个测试仍然通过，新增测试失败；
4. 记录测试名称、期望、实际结果和根因；
5. 不能用错误导入、测试文件找不到、环境没装好等非业务错误冒充 Red；
6. 不能先修改实现再补一条从未失败过的测试。

### 7.4 先补剩余 11 个接口调用断言

不要删除或重写已经通过的 `apiContracts.spec.js`。A 在 `test/a-contract-completion` 上扩充它，先把当前未覆盖的 11 个接口调用固定下来：

1. `POST /users`、`POST /users/login`、`GET /users/me`、`PATCH /users/me`；
2. `POST /merchants`、`POST /merchants/login`、`GET /merchants/me`、`PATCH /merchants/me`；
3. `POST /shops`，请求体包含合法 `name` 和可选 `description`；
4. `POST /products`，请求体包含合法 `shopId`、`categoryId`、`name`、`price`、`stock`；
5. `PATCH /products/{productId}`，修改时携带当前 `version`。

用户四个接口的最小断言示例：

```javascript
registerCustomer(registerBody)
expect(mocks.post).toHaveBeenCalledWith('/users', registerBody)

loginCustomer(loginBody)
expect(mocks.post).toHaveBeenCalledWith('/users/login', loginBody)

getProfile()
expect(mocks.get).toHaveBeenCalledWith('/users/me')

updateProfile(profileBody)
expect(mocks.patch).toHaveBeenCalledWith('/users/me', profileBody)
```

商家的四个接口使用同样方法，但路径必须是 `/merchants`、`/merchants/login`、`/merchants/me`，不能和用户账号体系混用。这八个函数当前看起来已经正确，所以新增断言可能直接通过；直接通过的测试属于补覆盖，不是假 Red。当前可以确定失败的接口级 Red 是缺少 `createShop()`。

商品新增和修改至少固定：

```javascript
const createBody = {
  shopId: 7,
  categoryId: 21,
  name: '牛肉饭',
  price: 18.8,
  stock: 20,
}
createProduct(createBody)
expect(mocks.post).toHaveBeenCalledWith('/products', createBody)

const patchBody = { price: 20, stock: 18, version: 3 }
updateProduct(11, patchBody)
expect(mocks.patch).toHaveBeenCalledWith('/products/11', patchBody)
```

### 7.5 再补页面行为 Red

然后为第 5.2 节的页面缺口补最小行为测试：

1. 分类列表的 mock 直接返回数组，页面必须正确展示，不能再用 `.items`；
2. 商家没有店铺时能够填写表单并创建，而不是只报错；
3. 分类新增、修改、删除按钮会分别调用正确 API；
4. 商品新增表单不会提交空名称、空分类或 0 元价格，编辑会携带 `version`；
5. 允许取消的用户订单显示取消按钮并调用 `cancelOrder(orderId)`；
6. 同一次结算重试两次时，两次请求使用同一个 `X-Idempotency-Key`；
7. HTTP 401 会删除 `access_token` 和 `user_role`；
8. 店铺详情不再保留无点击行为的“加入购物车”按钮。

分类响应测试必须按后端真实的直接数组编写：

```javascript
listCategories.mockResolvedValue([
  { id: 21, name: '主食', sortOrder: 0 },
  { id: 22, name: '饮品', sortOrder: 1 },
])
```

页面渲染后断言能看到“主食”和“饮品”。如果测试继续伪造 `{ items: [...] }`，只会保护错误实现，不算有效契约测试。

新增商品测试必须先在表单填写合法数据，再断言：

```javascript
expect(createProduct).toHaveBeenCalledWith({
  shopId: 7,
  categoryId: 21,
  name: '牛肉饭',
  price: 18.8,
  stock: 20,
})
```

不能继续断言 `categoryId: null`、空名称和 `price: 0`，因为后端会返回 400。

取消订单测试让详情接口返回 `PENDING_PAYMENT` 订单，断言页面出现取消按钮；点击后必须调用：

```javascript
expect(cancelOrder).toHaveBeenCalledWith(10001)
```

幂等测试模拟同一次结算第一次网络超时、第二次重试，读取两次 `createOrder` 的 Header，要求两次 `X-Idempotency-Key` 完全相同。测试开始前可 mock `crypto.randomUUID()` 返回固定值，但断言重点是同一次结算只生成一次，而不是每次点击都重新生成。

401 测试先向 `localStorage` 写入令牌和角色，再调用响应失败拦截器，最后断言两项都被删除。该测试还应确认用户被带回统一登录入口，不能留在需要登录的页面不断收到 401。

### 7.6 运行、记录并提交 Red

运行：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd ci
npm.cmd run test:run
```

预期新增测试因为缺 `createShop`、页面操作或错误响应读取而失败，这才是有效 Red。原有 79 个测试必须继续通过。A 记录失败用例和根因后提交：

```powershell
Set-Location '..'
git add -- frontend/src/tests docs/test/test-log.md
git diff --cached --check
git commit -m 'test(frontend): cover remaining stage-one contracts [RED]'
git push -u origin test/a-contract-completion
```

A 的 Red 合入 `develop` 后，C 在个人分支同步：

```powershell
git fetch origin
git rebase origin/develop
```

如果现有测试已经覆盖某项且断言符合 SRS/API 文档，就不重复新建第二套相同测试。不能继续保留“期待提交空商品数据”这种与后端校验相反的测试。

---

## 8. 第三步：C 完成剩余最小前端 Green

### 8.1 已经完成，不要重复修改

`baseURL=/api/v1`、Vite `/api` 代理、购物车路径、分类四个 API、商品列表路径、店铺 PATCH、订单六个 API、订单基本字段、用户/商家资料和商家订单页面已经完成。除非新的 Red 或真实联调能够证明有错，否则不再改这些部分。

### 8.2 只补剩余 API 和规则

1. 在 `store.js` 增加 `createShop(data)`，调用 `POST /shops`；
2. 分类列表直接处理后端返回的数组；
3. 订单页保存“本次结算”的幂等键，同一请求重试复用，成功或购物车内容变化后清除；
4. `http.js` 收到 401 时清除失效登录信息并回到登录入口；
5. 如果继续保留 `api/index.js` 作为统一出口，把 `category.js` 和 `merchant.js` 一并导出；当前页面使用直接导入时，这一点不单独阻塞联调。

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

### 8.3 只补剩余页面操作

按依赖顺序处理：

1. 商家无店铺时显示创建表单；已有店铺时继续使用现有修改表单；
2. 在现有商品管理页提供最小分类新增、改名、删除操作；
3. 用最小表单完成合法商品新增和带 `version` 的编辑；
4. 用户订单详情页增加取消操作；
5. 店铺详情页把无效“加入购物车”按钮接入 API，或者删除它并只从商品详情加入购物车；
6. 操作成功后刷新当前列表或详情，让页面与后端状态一致。

页面只实现阶段 1 必需操作。不要增加支付页、地图、配送员、优惠券、退款或 WebSocket。

### 8.4 每完成一组就验证

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum\frontend'
npm.cmd run test:run
npm.cmd run build
```

所有 A 新增的契约测试和原有 79 个测试必须 Green，构建目录能够正常生成。新总数应大于 79；不要通过删除测试、`skip`、保留错误 mock 或放宽断言解决失败。

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

C 在个人分支先同步最新后端：

```powershell
Set-Location 'D:\Projects\SchoolWorks\SW_2609\SE_Practicum'
git fetch origin --prune
git rebase origin/develop
```

重新执行前端测试和构建；若 rebase 包含后端修复，还要执行后端完整测试。全部通过后：

```powershell
git push -u origin feature/c-integration-fixes
git switch develop
git pull --ff-only origin develop
git merge --no-ff feature/c-integration-fixes
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
