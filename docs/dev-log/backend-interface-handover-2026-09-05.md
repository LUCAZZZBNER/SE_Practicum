# 后端接口层交接（2026-09-05）

## 1. 当前状态

本人负责的 Controller 到 Service 接口、Service 行为契约测试、后端架构以及认证和全局异常等公共代码已经完成。六个基础业务模块保持为 `user`、`merchant`、`restaurant`、`item`、`shopping`、`order`，采用手册要求的 Controller、Service、DAO/Mapper、Entity 分层和简单 Spring MVC 组织，不使用 Spring Modulith。

当前代码有意停在 TDD 交接红态：Controller、安全和异常处理的 143 个测试全部通过，六个 Controller 的指令、分支、行和方法覆盖率均为 100%；26 个 Service 行为契约测试已定义预期业务行为，但由于 ServiceImpl、DAO 和数据库尚未实现，完整测试套件尚不能通过。该红态只用于把可执行契约交给下一位开发者，不是手册所允许的最终提交状态。

## 2. 我已完成的部分

- 根据 `docs/api/backend-api-design.md` 完成六个模块全部 `/api/v1` Controller，包含 Bean Validation、分页/排序白名单、PATCH 非空更新、正确 HTTP 状态和统一 `ApiResponse<T>`。
- 完成六个模块的公开 Service 接口和类型化请求、查询、结果、分页、快照及删除结果契约；没有使用无类型 `Object` 返回值。
- 补齐跨模块只读/编排入口，包括用户和商家有效性、店铺归属/可下单、商品预留/恢复库存、购物车结算读取/清理。
- 完成 JWT 公共设施：HS256 签发和验证、`USER`/`MERCHANT` 角色、`CurrentPrincipal`、公开/可选认证标记、角色拦截器和 MVC 注册。密钥及有效期可由环境变量配置。
- 完成统一业务异常和 `1001`–`1603`、`9000` 错误映射；校验错误可返回字段信息，未知异常只返回脱敏后的内部错误。
- 完成 116 个 Controller 测试、8 个 JWT/拦截器测试和 19 个异常映射测试，当前共 143/143 通过。
- 完成 26 个 Service 行为契约测试：`user` 4 个、`merchant` 4 个、`restaurant` 4 个、`item` 5 个、`shopping` 4 个、`order` 5 个。
- 确认六个 Service 的全部公开方法均由契约测试调用，并补充店铺详情和商家订单列表断言；加入 JaCoCo 以生成覆盖率报告。
- 清除旧 Spring Modulith Java 骨架；当前生产源码中没有 ServiceImpl、DAO/Mapper、Entity、SQL 或迁移脚本。

## 3. 已冻结的接口契约

| 模块 | Service 入口重点 |
| --- | --- |
| `user` | 注册、登录、本人资料查询/更新、`requireActive` |
| `merchant` | 独立注册/登录、本人资料查询/更新、`requireActive` |
| `restaurant` | 店铺创建、公开/本人列表、详情、更新、`requireOwned`、`requireOrderable` |
| `item` | 分类 CRUD、商品创建/查询/更新、`reserveForOrder`、`restoreStock` |
| `shopping` | 加入/合并、本人购物车、改数量、删除、`loadForCheckout`、`removeAfterCheckout` |
| `order` | 创建、用户列表/详情/取消、商家列表/详情 |

Service 测试以接口类型注入和调用实际 Spring Bean，断言业务返回、错误码及操作后的可观察状态。它们不是“接口测试”和“实现测试”两套测试，也不检查某个类是否存在；新增实现后应直接让现有断言变绿。若发现契约确实与三份源文档冲突，先同步讨论和修订文档，不要为了迁就实现随意弱化测试。

## 4. 交给你的实现范围

你负责从 ServiceImpl 到数据库的全部下游工作：

- 为六个 Service 接口提供 Spring `@Service` 实现，完成业务规则、跨模块编排、密码摘要、JWT 会话签发、事务边界、归属判断和状态迁移。
- 设计并实现每个模块自己的 DAO/Mapper 接口与实现、Entity/Record、MyBatis 映射和 SQL；不得跨模块直接调用对方 DAO、Entity 或表访问实现。
- 完成 MySQL 表结构、约束、索引和迁移/初始化脚本，配置可测试的数据源。重点保证账号及购物车唯一约束、逻辑删除、乐观锁/原子库存、订单幂等和快照数据。
- 编写 DAO/Mapper 接口的行为测试，以及 ServiceImpl 以下层次所需的数据库集成、并发和事务回滚测试。Service 级行为测试已由我完成，不要另写只检查接口或实现是否存在的测试。
- 在同一数据源事务中完成订单创建和取消；失败必须回滚订单、明细、库存、购物车和幂等记录。
- 保持 Controller 和 Service 公开签名不变；如实现需要内部 DTO，可在模块内部增加，但不得把 Entity 暴露给 Controller 或其他模块。

先为六个接口建立可注入的 ServiceImpl 并接好测试数据源，使 Spring 测试上下文能够启动；随后按依赖顺序完成行为：`user`、`merchant` → `restaurant` → `item` → `shopping` → `order`。上下文可启动后，每完成一个模块就运行对应 Service 契约测试；全部实现后运行完整套件并补充 DAO/数据库测试证据。

## 5. 测试命令和交接证据

在 `backend/` 目录执行：

```bash
./mvnw -Dtest='*ControllerTests,DefaultJwtTokenServiceTests,AuthenticationInterceptorTests,GlobalExceptionHandlerTests' test
./mvnw clean test
```

2026-09-05 的结果：第一条命令 143/143 通过，六个业务 Controller 的指令、分支、行、方法覆盖率均为 100%；第二条命令发现 169 个测试，其中 26 个 Service 契约测试因尚无可注入的 Service 实现而处于预期红态。详细记录见 [测试执行日志](../test/test-log.md)和[测试用例](../test/test-cases.md)。

现有 Git 历史尚未包含这批未提交的接口代码和测试，因此不能据此证明 Controller/公共层最初是否严格按“测试提交早于实现提交”完成；不要补造历史。下游实现应保留当前 Service 红态日志，并通过后续独立实现提交展示红到绿的过程。

最终完成条件不是保持红态，而是现有 169 个测试以及你新增的全部 DAO/数据库测试 100% 通过，0 失败、0 错误、0 跳过；核心 API 覆盖率 100%，关键业务方法覆盖率至少 90%。旧的 [2026-09-02 交接文档](project-handover-2026-09-02.md) 仅为历史记录，其中 Spring Modulith、旧分工和待定技术选型均已作废。
