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
