# 架构设计

记录前后端分层、模块边界、认证方案和关键技术决策。

## Spring Modulith 单体模块

后端仍以一个 Spring Boot 进程和一个数据库部署，模块边界由 Spring Modulith 管理。启动类位于 `com.delivery.backend`，默认扫描其下的所有模块，并用 `@Modulithic` 注册单体模块。

| 模块 | 职责 | 对外入口 |
| --- | --- | --- |
| `com.delivery.backend.user` | 用户注册、登录和个人资料 | `UserModule` |
| `com.delivery.backend.merchant` | 商家账号和商家权限 | `MerchantModule` |
| `com.delivery.backend.restaurant` | 店铺信息和营业状态 | `RestaurantModule` |
| `com.delivery.backend.item` | 商品、价格、上下架和库存规则 | `ItemModule` |
| `com.delivery.backend.shopping` | 当前用户购物车 | `ShoppingModule` |
| `com.delivery.backend.order` | 下单、订单查询和状态迁移 | `OrderModule` |

每个模块的 `package-info.java` 使用 `@ApplicationModule` 声明模块元数据。模块间只应依赖其他模块公开的 facade、DTO 或领域事件；实体、持久化实现和控制器属于模块内部，后续新增 HTTP 或数据库适配器时放入对应模块包下，避免恢复为全局 `controller/service/mapper` 分层。
