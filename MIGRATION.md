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
