# SE_Practicum

轻量级外卖服务平台（仿饿了么）课程实践项目。

## 目录结构

```text
backend/                 Spring Boot compatibility/domain backend (migration seam)
  src/main/java/         Java 源代码
    com/delivery/backend/user/       用户模块
    com/delivery/backend/merchant/   商家模块
    com/delivery/backend/shopping/   购物车模块
    com/delivery/backend/order/      订单模块
    com/delivery/backend/restaurant/ 店铺模块
    com/delivery/backend/item/       商品模块
  src/main/resources/    后端配置和资源
  src/test/java/         后端单元、接口和集成测试
  src/test/resources/    测试配置及数据库脚本
frontend/                Vue 3 前端
  src/api/               Axios 接口封装
  src/components/        公共组件
  src/router/            路由
  src/stores/            状态管理
  src/utils/             前端工具
  src/views/             页面
  src/tests/             前端单元和集成测试
e2e/                     端到端验收场景
gateway/                 Spring Boot compatibility API gateway
services/                Independently deployable identity, catalog, cart, order, and media services
handin-docs/             Migration decisions and independently derived validation records
docs/                    项目文档
  api/                   接口文档
  architecture/         架构设计
  database/              数据库设计
  test/                  测试用例、日志和报告
  dev-log/               开发日志和过程记录
```

需求基线见 [软件需求规格说明书](docs/软件需求规格说明书.md)，HTTP 契约见 [后端 API 设计](docs/api/backend-api-design.md)，最终数据库结构见 [数据库设计说明书](docs/database/数据库设计说明书.md)，测试证据见 [测试执行日志](docs/test/test-log.md)。

后端正在从简单四层 MVC 单体迁移为由 gateway 和独立领域服务组成的架构；gateway 保留现有 `/api/v1` 契约。

## Docker 启动

项目提供了包含 MySQL、Kafka、Spring Boot gateway、兼容后端和 Nginx/Vue 前端的 Docker Compose 配置。

首次启动前复制 `.env.example` 为 `.env`，修改其中的本地开发密码，并确保 `JWT_SECRET` 至少包含 32 个 UTF-8 字节：

```bash
cp .env.example .env
```

构建并启动完整栈：

```bash
docker compose up --build
```

启动后访问：

- 前端：http://localhost:5173
- 后端 API：http://localhost:8080/api/v1

停止服务但保留数据库数据：

```bash
docker compose down
```

如需清空本地数据库并重新执行 Flyway 迁移，明确删除 Compose 数据卷：

```bash
docker compose down -v
```

## 运行完整测试套件

从项目根目录执行：

```bash
./scripts/run-tests.sh
```

脚本会启动 Docker 服务，确保 `delivery_test` 存在，然后依次运行后端 Maven 测试、前端 Vitest 测试和端到端业务风险检查。测试结束后 Docker 服务保持运行。

只运行后端测试时，可以执行：

```bash
./backend/run-tests.sh
```

独立验证微服务身份与媒体边界（不启动单体后端）时，可以执行：

```bash
./scripts/run-microservices-tests.sh
```
