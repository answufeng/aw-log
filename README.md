# aw-log

[![](https://jitpack.io/v/answufeng/aw-log.svg)](https://jitpack.io/#answufeng/aw-log)

基于 [Timber](https://github.com/JakeWharton/timber) 的 Android 结构化日志工具库。

提供 Logcat 调试输出、文件日志持久化、崩溃收集、日志拦截与脱敏、自定义格式化、日志文件管理（压缩/导出/清理）等能力。

## 特性

- **AwDebugTree** — Logcat 输出，自动 Tag 与调用者定位（方法名、文件名、行号）
- **AwFileTree** — 文件日志，按日期分文件、大小限制轮转、异步写入、可配置最低级别
- **AwCrashTree** — Error/Assert 级别捕获，支持自定义回调（可对接 Firebase Crashlytics、Bugly 等）
- **AwLogInterceptor** — 日志拦截器，支持过滤、脱敏、增强
- **AwLogFormatter** — 自定义日志格式化，可定制时间格式、分隔符、显示字段
- **AwLogFileManager** — 压缩旧日志、导出为 ZIP、查询大小、批量清理
- **JSON 格式化** — 自动识别 JSONObject/JSONArray 并美化输出
- **Lambda 延迟求值** — 关闭日志时零开销
- **DSL 配置** — 简洁优雅的初始化方式
- **minSdk 21+** — 覆盖 99%+ 的 Android 设备

## 引入

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.answufeng:aw-log:1.0.0")
}
```

## 快速开始

### 1. 初始化

在 `Application.onCreate()` 中初始化：

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        AwLogger.init {
            debug = BuildConfig.DEBUG
            fileLog = true
            fileDir = "${cacheDir.absolutePath}/logs"
            maxFileSize = 5L * 1024 * 1024  // 5MB
            maxFileCount = 10
            crashLog = true
            crashHandler = { tag, throwable, message ->
                // 上报到 Firebase Crashlytics / Bugly 等
            }
        }
    }
}
```

### 2. 使用

```kotlin
// 基本日志
AwLogger.d("请求成功")
AwLogger.e(exception, "请求失败")
AwLogger.i("用户登录: userId=%s", userId)

// Lambda 延迟求值（日志关闭时不会执行 Lambda）
AwLogger.d { "响应: ${response.body}" }

// 带 Tag 的日志
AwLogger.tag("Network").d("连接超时")

// JSON 格式化
AwLogger.json(jsonString, "API")

// WTF 级别
AwLogger.wtf("不应该到达的分支")
```

### 3. 文件管理

```kotlin
val logDir = "${cacheDir.absolutePath}/logs"

// 压缩旧日志（非当天的 .txt 文件压缩为 .gz）
val count = AwLogFileManager.compressOldLogs(logDir)

// 导出所有日志为 ZIP
val zipFile = AwLogFileManager.exportLogs(logDir, File(exportDir, "logs.zip"))

// 查询日志大小
val size = AwLogFileManager.getTotalSize(logDir)

// 获取日志文件列表
val files = AwLogFileManager.getLogFiles(logDir)

// 清理所有日志
val deleted = AwLogFileManager.clearAll(logDir)
```

### 4. 生命周期管理

```kotlin
override fun onDestroy() {
    super.onDestroy()
    AwLogger.flush()   // 确保日志写入磁盘
}

// 重置（清除所有 Tree，用于测试或动态切换）
AwLogger.reset()
```

## 配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `debug` | Boolean | `true` | 是否启用 Logcat 输出（AwDebugTree） |
| `fileLog` | Boolean | `false` | 是否启用文件日志（AwFileTree） |
| `fileDir` | String | `""` | 文件日志目录（绝对路径） |
| `maxFileSize` | Long | `5242880` (5MB) | 单个日志文件最大大小（字节） |
| `maxFileCount` | Int | `10` | 最大日志文件数量 |
| `fileMinPriority` | Int | `Log.DEBUG` | 文件日志最低级别 |
| `crashLog` | Boolean | `false` | 是否启用崩溃日志收集 |
| `crashHandler` | Function | `null` | 崩溃处理回调 `(tag, throwable, message) -> Unit` |
| `minPriority` | Int | `Log.VERBOSE` | 全局最低日志级别 |
| `fileFormatter` | AwLogFormatter | 默认格式化器 | 文件日志格式化器 |

## 日志拦截器

拦截器可以在日志被写入之前进行过滤、脱敏或增强：

```kotlin
AwLogger.init {
    debug = true
    fileLog = true
    fileDir = "${cacheDir.absolutePath}/logs"

    // 脱敏拦截器：隐藏敏感信息
    addInterceptor(object : AwLogInterceptor {
        override fun intercept(
            priority: Int, tag: String?, message: String, throwable: Throwable?
        ): AwLogInterceptor.LogResult {
            // 拦截包含密码的日志
            if (message.contains("password=")) return AwLogInterceptor.LogResult.REJECTED
            return AwLogInterceptor.LogResult.ACCEPTED
        }
    })

    // 级别过滤拦截器：Release 只记录 WARN 及以上
    addInterceptor(object : AwLogInterceptor {
        override fun intercept(
            priority: Int, tag: String?, message: String, throwable: Throwable?
        ): AwLogInterceptor.LogResult {
            return if (priority >= Log.WARN) {
                AwLogInterceptor.LogResult.ACCEPTED
            } else {
                AwLogInterceptor.LogResult.REJECTED
            }
        }
    })
}
```

## 自定义格式化器

```kotlin
// 使用 DSL 创建自定义格式化器
AwLogger.init {
    fileLog = true
    fileDir = "${cacheDir.absolutePath}/logs"
    fileFormatter = AwLogFormatter.create {
        timePattern = "HH:mm:ss.SSS"
        separator = " | "
        showLevel = true
        showTag = true
        showTime = true
    }
}

// 完全自定义格式化器
fileFormatter = object : AwLogFormatter {
    override fun format(priority: Int, tag: String?, message: String, throwable: Throwable?): String {
        return "[${Thread.currentThread().name}] $message"
    }
}
```

## 自定义 Tree

```kotlin
AwLogger.init {
    debug = true
    addTree(object : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // 发送到远程日志服务器
        }
    })
}
```

## 架构

```
AwLogger (入口)
    ├── AwDebugTree   → Logcat 输出
    ├── AwFileTree    → 文件日志（异步写入、轮转、清理）
    ├── AwCrashTree   → 崩溃收集（回调 + 默认 Log.e 输出）
    └── Custom Trees  → 用户自定义 Tree

AwLogInterceptor → 日志拦截/过滤/脱敏
AwLogFormatter   → 日志格式化
AwLogFileManager → 文件管理（压缩/导出/清理）
```

## 兼容性

- minSdk 24+
- Kotlin 2.0+
- Timber 5.0.1

## 许可证

Apache License 2.0，详见 [LICENSE](LICENSE)。
