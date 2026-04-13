# Changelog

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
- minSdk 21+
