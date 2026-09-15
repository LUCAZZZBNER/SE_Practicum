# 测试执行日志

记录测试日期、测试版本、执行命令、通过/失败数量及失败原因。

## 2026-09-05 B 业务 TDD 起始 Red 基线

| 项目 | 结果 |
| --- | --- |
| 时间 | 2026-09-05 17:18（Asia/Shanghai） |
| 分支 | `feature/b-tdd` |
| 测试版本 | `d9a346d` |
| Java | Microsoft OpenJDK `17.0.20.1` |
| Maven | Wrapper 已下载的 Apache Maven `3.9.16` |
| Profile | `test` |
| 完整测试 | 169 |
| 通过 | 143 |
| Failures | 0 |
| Errors | 26 |
| Skipped | 0 |
| 构建结论 | `BUILD FAILURE`，属于实现前预期 Red |

计划命令：

```powershell
.\mvnw.cmd -version
.\mvnw.cmd clean test
```

Codex 非交互执行环境调用仓库 `mvnw.cmd` 时，在 Maven 启动前出现 `Cannot index into a null array` / `Cannot start maven from wrapper`。该脚本在开发者普通 PowerShell 中此前可以正常显示 Maven 3.9.16 和 Java 17；为完成本次代码基线验证，实际使用 Wrapper 已下载的同一份 Maven 3.9.16 执行：

```powershell
& 'C:\Users\2006d\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd' clean test
```

结果分布：

| 测试类 | Tests | Errors |
| --- | ---: | ---: |
| `ItemServiceContractTests` | 5 | 5 |
| `MerchantServiceContractTests` | 4 | 4 |
| `OrderServiceContractTests` | 5 | 5 |
| `RestaurantServiceContractTests` | 4 | 4 |
| `ShoppingServiceContractTests` | 4 | 4 |
| `UserServiceContractTests` | 4 | 4 |

第一个根因：

```text
No qualifying bean of type
'com.delivery.backend.item.service.ItemService'
```

未发现数据库拒绝连接、未知数据库、Flyway checksum、编译错误、断言失败或跳过测试。结论：143 个公共层/Controller 测试保持绿色；26 个 Error 全部来自尚无 ServiceImpl Bean，符合 B 开始实现前的约定 Red 基线。

## 2026-09-08 阶段 1 真实联调：正常流程

| 项目 | 结果 |
| --- | --- |
| 测试电脑 | B 的电脑 |
| 分支 | `develop` |
| 测试版本 | `2203eea`（业务代码基线 `1168626`） |
| 联调范围 | 执行文档 10.3.1 至 10.4.8 |
| 环境 | MySQL、Spring Boot、Vite、浏览器 Network |
| 正常流程 | 商家注册与登录、资料、店铺、分类、商品、用户注册与登录、购物车、下单、取消、商家查单全部通过 |
| 30 个接口 | 正常路径全部通过 |
| 当前结论 | 正常业务主链路和 10.5 异常场景全部通过；剩余 BUG-FE-001 和最终自动化验收 |

本轮最初曾因 MySQL 和后端未运行导致商家注册显示 HTTP 500；恢复 MySQL、后端 8080 后重新测试通过。该次失败属于联调环境未启动完整，不判定为业务缺陷。

已确认一个前端缺陷：点击“退出”只返回首页，没有删除本地的 `access_token` 和 `user_role`。详见 `BUG-FE-001`。

### 10.5 异常场景联调结果

| 场景 | 预期结果 | 实际结果 | 结论 |
| --- | --- | --- | --- |
| 重复商家注册 | HTTP 409 / code 1201 | 与预期一致 | 通过 |
| 重复用户注册 | HTTP 409 / code 1101 | 与预期一致 | 通过 |
| 同一店铺创建同名分类 | HTTP 409 / code 1005 | 与预期一致 | 通过 |
| 无 Token 访问受保护接口 | HTTP 401 / code 1002 | 与预期一致 | 通过 |
| User/Merchant 错误角色访问 | HTTP 403 / code 1003 | 与预期一致 | 通过 |
| 第二商家修改第一商家店铺 | HTTP 403 或 404，数据不变 | 与预期一致，未越权修改 | 通过 |
| 使用旧 version 修改商品 | HTTP 409 / code 1005，数据不变 | 与预期一致 | 通过 |
| 下单时商品版本变化 | HTTP 409 / code 1601 | 与预期一致 | 通过 |
| 库存不足创建订单 | HTTP 409 / code 1402，不生成订单 | 与预期一致，测试后库存已恢复 | 通过 |
| 相同幂等键和相同 Body 重试 | 返回同一订单 ID，不重复扣库存 | 与预期一致 | 通过 |
| 相同幂等键和不同 Body | HTTP 409 / code 1603 | 与预期一致 | 通过 |
| 第二次取消同一订单 | HTTP 409 / code 1602，不重复恢复库存 | 与预期一致 | 通过 |

结论：10.5 要求的认证、角色、资源归属、冲突、乐观锁、库存、幂等和订单状态异常均通过。异常场景返回规定的 4xx 和业务码，未发现新的后端缺陷。

### BUG-FE-001 退出清理登录状态 Red

| 项目 | 结果 |
| --- | --- |
| 测试文件 | `frontend/src/tests/unit/AppHeader.spec.js` |
| 测试命令 | `npm.cmd run test:run -- AppHeader.spec.js` |
| 测试数量 | 2 |
| 通过 | 1 |
| 失败 | 1 |
| 预期 | 点击退出后 `access_token` 和 `user_role` 都不存在 |
| 实际 | `access_token` 仍为 `token-to-clear` |
| 结论 | 有效 Red，证明退出没有清除登录状态 |

受限执行环境第一次启动 Vitest 时因临时文件写入出现 EPERM；以正常本机权限重跑后得到上述业务断言失败。Red 不是语法、导入或测试环境错误。

### BUG-FE-001 Green 自动验证

| 项目 | 结果 |
| --- | --- |
| 正式 Red 提交 | `77e3226 test(frontend): cover logout behavior [RED]` |
| Green 提交 | `312b6fd fix(frontend): clear auth state on logout [GREEN]` |
| 目标测试 | `AppHeader.spec.js`：2/2 通过 |
| 前端全量测试 | 25 个测试文件、95 个测试全部通过 |
| 生产构建 | 成功，1710 个模块完成构建 |
| 构建警告 | 仅主 chunk 大于500 kB，不阻塞阶段1 |
| 浏览器人工复测 | 通过 |

Green 已实现删除 `access_token`、删除 `user_role` 并使用路由返回首页。2026-09-08 在B的电脑完成真实浏览器复测：登录后两项数据存在；点击“退出”后返回首页且两项数据消失；退出功能通过。

### 阶段 1 最终前端全量复测

| 项目 | 结果 |
| --- | --- |
| 测试日期 | 2026-09-08 |
| 依赖安装 | `npm.cmd ci` 成功，安装178个包 |
| 前端全量测试 | 25个测试文件、95个测试全部通过 |
| 生产构建 | 成功，1710个模块完成构建 |
| 阻塞性错误 | 无 |
| 非阻塞警告 | 依赖审计警告；主 chunk 大于500 kB |

第一次执行 `npm.cmd ci` 时，仍在运行的本项目前端开发服务器占用 `node_modules/@esbuild/win32-x64/esbuild.exe`，出现 `EPERM unlink`；随后依赖目录处于不完整状态，导致 `vitest` 和 `vite` 暂时找不到。这属于本机进程占用，不是代码或测试失败。确认并停止本项目的 `npm run dev` 和 Vite 进程后，重新执行 `npm.cmd ci`、`npm.cmd run test:run` 和 `npm.cmd run build`，三项全部通过。未停止 Codex 自带的 Node 进程，也未执行 `npm audit fix --force`。

### 阶段 1 最终后端全量复测与迁移确认

| 项目 | 结果 |
| --- | --- |
| 测试日期 | 2026-09-08 |
| 分支 | `develop` |
| 测试命令 | `./mvnw.cmd clean test` |
| Surefire测试类 | 16个 |
| 测试总数 | 171 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Maven结果 | `BUILD SUCCESS` |
| Flyway历史 | V1、V2均存在且`success=1` |

结论：当前 `develop` 的后端完整自动化测试和数据库迁移确认均通过。结合前端95项测试、生产构建、30个接口正常路径和10.5异常路径结果，阶段1代码及测试已达到提交 `develop` 的条件。

### 尚待补充的交付证据

当前仓库没有发现联调截图文件。课程最终交付前至少补3张已隐藏密码和完整Token的截图，并在此处登记：

| 截图 | 对应场景 | 状态 |
| --- | --- | --- |
| 商家店铺或商品结果 | 商家建店、创建分类和上架商品 | 待补 |
| 用户订单结果 | 加购、下单或取消订单 | 待补 |
| 商家订单结果 | 商家订单列表或订单详情 | 待补 |
