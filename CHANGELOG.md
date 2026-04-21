# Changelog

## 1.0.0 (2026-04-21)

### Features
- **AwDebugTree** — Logcat 输出，自动 Tag 包含方法名与调用位置（一次栈遍历，零冗余）
- **AwFileTree** — 文件日志，按日期分文件、大小限制轮转、异步写入、智能刷新（定时 + ERROR 即时）、磁盘空间检查、队列溢出保护
- **AwCrashTree** — 崩溃收集（UncaughtExceptionHandler + ERROR 级别捕获），支持自定义回调
- **AwLogInterceptor** — 责任链模式拦截器，支持过滤、脱敏、消息增强、Tag 修改
- **AwDesensitizeInterceptor** — 内置脱敏拦截器，预置手机号/身份证/银行卡/邮箱/key=value 规则
- **AwLogFormatter** — 自定义日志格式化，可定制时间格式、分隔符、显示字段、线程信息
- **AwLogFileManager** — 压缩旧日志、导出为 ZIP、查询大小、批量清理、按日期清理、关键词搜索
- **AwLogListener** — 日志监听器，实时获取日志输出
- **JSON / XML 格式化** — 自动识别 JSONObject/JSONArray 并美化输出，支持 XML 美化
- **Lambda 延迟求值** — 关闭日志时零开销，支持 Lambda + Tag 组合
- **DSL 配置** — 简洁优雅的初始化方式
- **动态级别调整** — 运行时修改日志级别，返回旧值方便恢复
- **isLoggable** — 判断指定级别是否会被输出

### Performance
- 拦截器单次执行，Tree 不重复拦截
- 智能文件刷新：定时刷新 + ERROR 级别即时 flush
- Lambda 零开销
- 线程安全格式化：ThreadLocal 包装 SimpleDateFormat
- 单次栈遍历消除双重开销
- 异常隔离不中断链路
- 内存文件大小追踪避免 IO
- 队列溢出保护（最大 1024 条）
- 单线程池减少开销
