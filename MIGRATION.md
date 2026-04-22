# 迁移指南

## 1.x → 2.x

| 变更 | 1.x | 2.x |
|------|-----|-----|
| Tag 设置 | `AwLogger.tag("MyTag").d("msg")` | `AwLogger.d("MyTag", "msg")` 或 `AwLogger.d("MyTag") { "msg" }` |
| 格式化日志 | `AwLogger.d(String.format("id=%d", id))` | `AwLogger.d("id=%d", id)` 或 `AwLogger.d("Tag", "id=%d", id)` |
| setMinPriority | `AwLogger.setMinPriority(Log.WARN)` | `val old = AwLogger.setMinPriority(Log.WARN)` (返回旧值) |
| isLoggable | 无 | `AwLogger.isLoggable(Log.DEBUG)` |
| XML 格式化 | 无 | `AwLogger.xml(xmlString, "Tag")` |

### 迁移步骤

1. **替换 Tag 设置方式**：将 `AwLogger.tag("Tag").d("msg")` 改为 `AwLogger.d("Tag", "msg")`
2. **使用内置格式化**：移除 `String.format` 包裹，直接传参数
3. **利用返回值**：`setMinPriority` 现在返回旧值，可用于临时调整级别后恢复
4. **新增功能**：可使用 `isLoggable` 避免不必要的日志构造开销

## 2.x 行为更新（库小版本）

| 变更 | 说明 |
|------|------|
| `fileLog = true` 且 `fileDir` 为空 | 现会在 `init` 时抛出 `IllegalArgumentException`（此前静默不挂载 `AwFileTree`） |
| 未捕获异常 | 由 `AwCrashCoordinator` 单例安装 `UncaughtExceptionHandler`，重复 `init` 不再嵌套多套 handler |
| `AwLogFileManager.shutdown()` / `AwLogger.reset()` 后 | 异步方法会自动重建后台线程池，可继续使用 |
| `AwLogFileManager.search` | 同时支持 `log_*.txt` 与 `log_*.txt.gz` |

新增 API：`AwLogger.isFileLoggable`、`AwLogConfig.rejectLogOnInterceptorFailure`、`AwLogConfig.crashEchoToLogcat`。

**默认 Logcat 行为**：`crashLog = true` 且 `debug = true` 时，`AwCrashTree` 默认**不再**对 ERROR 重复 `Log.e`（由 `AwDebugTree` 负责），避免双份输出；若仍需双份，请设置 `crashEchoToLogcat = true`。
