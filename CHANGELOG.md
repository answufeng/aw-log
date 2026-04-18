# Changelog

## 2.0.0

### 新增
- **AwLogListener** — 日志监听器接口，实时获取日志输出（UI 展示、远程上报）
- **AwLogger.setMinPriority()** — 运行时动态修改日志级别，无需重新初始化
- **AwLogger.getFileDir()** — 获取文件日志目录
- **AwLogger.e(t, tag) { message }** — Throwable + Tag + Lambda 组合方法
- **AwLogFormatter.compact()** — 简洁模式格式化器（HH:mm:ss.SSS）
- **AwLogFormatter.verbose()** — 详细模式格式化器（默认）
- **AwFormatterDsl.showThread** — 格式化器线程信息选项
- **AwLogFileManager.search()** — 按关键词搜索日志内容
- **AwLogFileManager.searchAsync()** — 异步搜索日志
- **AwLogFileManager.clearBefore()** — 按日期范围清理日志
- **AwLogFileManager.clearBeforeAsync()** — 异步按日期清理
- **AwFileTree.shutdown()** — 优雅关闭文件日志线程池

### 修复
- **[BUG-3]** `vararg args` 参数被完全忽略，现在通过 `String.format` 正确格式化
- **[BUG-1]** AwFileTree 重新初始化时线程池泄漏，`reset()` 和 `init()` 现在会关闭旧线程池
- **[BUG-2]** flushRunnable 竞态条件，改用 `ScheduledExecutorService.scheduleAtFixedRate`
- **[BUG-4]** 统一 AwFileTree 同步方式，全部使用 `synchronized` 块
- **[BUG-5]** README minSdk 从 21+ 修正为 24+（与 build.gradle.kts 一致）

### 改进
- **AwCrashTree** 增加真正的崩溃收集（设置 UncaughtExceptionHandler）
- **AwFileTree** 使用 `ScheduledExecutorService` 替代 `ThreadPoolExecutor` + `Thread.sleep`
- **AwFileTree** `cleanOldFiles` 改为周期性检查（每 100 次写入检查一次），避免高频日志性能开销
- **AwFileTree** 内存维护文件写入字节数计数器，避免每次 `file.length()` IO 操作
- **AwDesensitizeInterceptor** PHONE/ID_CARD/BANK_CARD/EMAIL 规则增加词边界匹配 `\b`，减少误匹配
- **AwDesensitizeInterceptor** BANK_CARD 规则增加常见卡 BIN 前缀校验
- **AwLogFileManager** 异步方法使用共享线程池，避免频繁创建线程
- **移除** `AwLogger.tag()` 方法（暴露 Timber 内部，破坏封装性）

### Demo 改进
- 增加 Info/Warn 日志级别按钮
- 增加实时日志显示区域（通过 AwLogListener）
- 增加格式化日志、异常+Lambda、并发日志、动态级别演示
- 增加日志搜索功能演示
- 增加 AlertDialog 选择日志级别
- 修复 AndroidManifest 主题不一致问题

## 1.0.0

- Initial release
- Package: `com.answufeng.log`
- AwLogger: DSL 初始化入口，支持 v/d/i/w/e/wtf/json/tag
- AwDebugTree: Logcat 输出，自动 Tag 与调用者定位
- AwFileTree: 文件日志，按日期分文件、大小限制轮转、异步写入、BufferedWriter 复用、背压保护
- AwCrashTree: Error/Assert 级别捕获，支持自定义回调，无回调时默认 Log.e 输出
- AwLogInterceptor: 日志拦截器接口，支持过滤、脱敏、增强
- AwLogFormatter: 自定义日志格式化接口，提供 DSL Builder 和默认实现
- AwLogFileManager: 压缩旧日志、导出 ZIP、查询大小、批量清理
- Lambda 延迟求值，关闭日志时零开销
- 全局最低日志级别配置 (minPriority)
- 文件日志最低级别配置 (fileMinPriority)
- flush() 确保日志写入磁盘
- reset() 重置日志系统
- minSdk 24+
