# SE_Practicum

轻量级外卖服务平台（仿饿了么）课程实践项目。

## 目录结构

```text
backend/                 Spring Boot + Spring Modulith 单体后端
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
frontend/                Vue 3 前端（待初始化）
  src/api/               Axios 接口封装
  src/components/        公共组件
  src/router/            路由
  src/stores/            状态管理
  src/utils/             前端工具
  src/views/             页面
  src/tests/             前端单元和集成测试
e2e/                     端到端验收场景
docs/                    项目文档
  api/                   接口文档
  architecture/         架构设计
  database/              数据库设计
  test/                  测试用例、日志和报告
  dev-log/               开发日志和过程记录
```

需求基线见 [软件需求规格说明书](docs/software-requirements-specification.md)。

后端已完成 Spring Modulith 单体模块骨架，前端仍待初始化。各业务模块的边界和职责见[架构设计](docs/architecture/README.md)。
