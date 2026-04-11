# aw-log

基于 Timber 的日志工具库，提供结构化日志输出、文件日志、崩溃收集和日志文件管理。

## 引入

```kotlin
dependencies {
    implementation("com.github.answufeng:aw-log:1.0.0")
}
```

## 功能特性

- BrickDebugTree：Logcat 输出，自动 Tag 和调用者定位
- FileTree：文件日志，按日期分文件、大小限制、异步写入
- CrashTree：Error/Assert 级别捕获，支持自定义崩溃处理器回调
- LogFileManager：压缩、导出和清理日志文件
- JSON 格式化输出
- Lambda 延迟求值，关闭日志时零开销
- DSL 配置方式

## 使用示例

```kotlin
// 初始化
BrickLogger.init {
    debug = BuildConfig.DEBUG
    fileLog = true
    fileDir = "${cacheDir.absolutePath}/logs"
    maxFileSize = 5L * 1024 * 1024
    maxFileCount = 10
    crashLog = true
    crashHandler = { tag, throwable, message -> /* 上报到 Firebase */ }
}

// 使用
BrickLogger.d("请求成功")
BrickLogger.e(exception, "请求失败")
BrickLogger.json(jsonString, "API")
BrickLogger.d("Network") { "响应: ${response.body}" }

// 文件管理
LogFileManager.compressOldLogs(logDir)
LogFileManager.exportLogs(logDir, outputFile)
```

## 许可证

Apache License 2.0，详见 [LICENSE](LICENSE)。
