# aw-log

[![](https://jitpack.io/v/answufeng/aw-log.svg)](https://jitpack.io/#answufeng/aw-log)

基于 [Timber](https://github.com/JakeWharton/timber) 的 Android 结构化日志工具库。

提供 Logcat 调试输出、文件日志持久化、崩溃收集、日志拦截与脱敏、自定义格式化、日志文件管理（压缩/导出/清理/搜索）等能力。

## 特性

- **AwDebugTree** — Logcat 输出，自动 Tag 包含方法名与调用位置（一次栈遍历，零冗余）
- **AwFileTree** — 文件日志，按日期分文件、大小限制轮转、异步写入、智能刷新（定时 + ERROR 即时）、磁盘空间检查、队列溢出保护
- **AwCrashTree** — 崩溃收集（UncaughtExceptionHandler + ERROR 级别捕获），支持自定义回调（可对接 Firebase Crashlytics、Bugly 等）
- **AwLogInterceptor** — 责任链模式拦截器，支持过滤、脱敏、消息增强、Tag 修改，异常自动隔离
- **AwDesensitizeInterceptor** — 内置脱敏拦截器，预置手机号/身份证/银行卡/邮箱/key=value 规则（带词边界匹配）
- **AwLogFormatter** — 自定义日志格式化，可定制时间格式、分隔符、显示字段、线程信息
- **AwLogFileManager** — 压缩旧日志、导出为 ZIP、查询大小、批量清理、按日期清理、关键词搜索，支持异步操作
- **AwLogListener** — 日志监听器，实时获取日志输出（UI 展示、远程上报）
- **JSON / XML 格式化** — 自动识别 JSONObject/JSONArray 并美化输出，支持 XML 美化，支持指定日志级别
- **Lambda 延迟求值** — 关闭日志时零开销，支持 Lambda + Tag 组合
- **DSL 配置** — 简洁优雅的初始化方式
- **动态级别调整** — 运行时修改日志级别，`setMinPriority` 返回旧值方便恢复
- **isLoggable** — 判断指定级别是否会被输出，避免不必要的日志构造开销
- **minSdk 24+** — 覆盖 99%+ 的 Android 设备

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

            // 内置脱敏拦截器
            addInterceptor(AwDesensitizeInterceptor.create {
                phone()
                idCard()
                email()
                keyValue()
            })

            // 日志监听器
            addListener { priority, tag, message, throwable ->
                // 实时获取日志（UI 展示、远程上报等）
            }
        }
    }
}
```

### 2. 使用

```kotlin
// 基本日志（支持 String.format 格式化）
AwLogger.d("请求成功")
AwLogger.e(exception, "请求失败")
AwLogger.i("用户登录: userId=%d", userId)

// 带 Tag 的日志（支持 String.format 格式化）
AwLogger.d("Network", "连接超时: %s", url)
AwLogger.e("API", "请求失败: 状态码 %d", 503)
AwLogger.i("UI", "Activity 创建: %s", localClassName)

// Lambda 延迟求值（日志关闭时不会执行 Lambda）
AwLogger.d { "响应: ${response.body}" }

// Lambda + Tag 组合（零开销）
AwLogger.d("Network") { "连接超时: ${url}" }
AwLogger.e("API") { "请求失败: ${error.message}" }

// Throwable + Tag + Lambda 组合
AwLogger.e(exception, "API") { "请求失败: ${error.message}" }

// JSON 格式化（默认 DEBUG 级别）
AwLogger.json(jsonString, "API")

// JSON 格式化（指定级别）
AwLogger.json(jsonString, "API", priority = Log.ERROR)

// XML 格式化
AwLogger.xml(xmlString, "API")

// WTF 级别
AwLogger.wtf("不应该到达的分支")

// 动态修改日志级别（返回旧值，方便恢复）
val oldLevel = AwLogger.setMinPriority(Log.WARN)
// ... 只记录 WARN 及以上
AwLogger.setMinPriority(oldLevel)  // 恢复

// 判断是否会被记录（避免不必要的日志构造开销）
if (AwLogger.isLoggable(Log.DEBUG)) {
    val expensiveData = computeExpensiveDebugInfo()
    AwLogger.d(expensiveData)
}

// 获取文件日志目录
val logDir = AwLogger.getFileDir()
```

### 3. 文件管理

```kotlin
val logDir = AwLogger.getFileDir()

// 压缩旧日志（非当天的 .txt 文件压缩为 .gz）
val count = AwLogFileManager.compressOldLogs(logDir)

// 异步压缩（不阻塞主线程）
AwLogFileManager.compressOldLogsAsync(logDir) { count ->
    runOnUiThread { showToast("压缩了 $count 个文件") }
}

// 导出所有日志为 ZIP
val zipFile = AwLogFileManager.exportLogs(logDir, File(exportDir, "logs.zip"))

// 异步导出
AwLogFileManager.exportLogsAsync(logDir, File(exportDir, "logs.zip")) { file ->
    // file 为导出结果
}

// 查询日志大小
val size = AwLogFileManager.getTotalSize(logDir)

// 获取日志文件列表
val files = AwLogFileManager.getLogFiles(logDir)

// 查询可用磁盘空间
val available = AwLogFileManager.getAvailableSpace(logDir)

// 清理所有日志
val deleted = AwLogFileManager.clearAll(logDir)

// 按日期清理（清理指定日期之前的日志）
val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -7) }
AwLogFileManager.clearBefore(logDir, calendar.time)

// 异步清理
AwLogFileManager.clearAllAsync(logDir) { count ->
    // 清理完成
}

// 搜索日志（按关键词）
val results = AwLogFileManager.search(logDir, "NullPointerException")

// 异步搜索
AwLogFileManager.searchAsync(logDir, "error") { results ->
    // results 为匹配的日志行列表
}
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
| `flushIntervalMs` | Long | `3000` | 文件日志定时刷新间隔（毫秒） |
| `crashLog` | Boolean | `false` | 是否启用崩溃日志收集 |
| `crashHandler` | Function | `null` | 崩溃处理回调 `(tag, throwable, message) -> Unit` |
| `minPriority` | Int | `Log.VERBOSE` | 全局最低日志级别 |
| `fileFormatter` | AwLogFormatter | 默认格式化器 | 文件日志格式化器 |

## 日志拦截器

拦截器采用 **责任链模式**（类似 OkHttp Interceptor），可以在日志被写入之前进行过滤、脱敏或增强：

```kotlin
AwLogger.init {
    debug = true
    fileLog = true
    fileDir = "${cacheDir.absolutePath}/logs"

    // 脱敏拦截器：隐藏敏感信息
    addInterceptor(object : AwLogInterceptor {
        override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
            val message = chain.message.replace(Regex("""password=\S+"""), "password=****")
            return chain.proceed(message = message)
        }
    })

    // 级别过滤拦截器：Release 只记录 WARN 及以上
    addInterceptor(object : AwLogInterceptor {
        override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
            return if (chain.priority >= Log.WARN) {
                chain.proceed()
            } else {
                AwLogInterceptor.LogResult.Rejected("low priority")
            }
        }
    })

    // Tag 修改拦截器：为所有日志添加前缀
    addInterceptor(object : AwLogInterceptor {
        override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
            return chain.proceed(tag = "MyApp-${chain.tag}")
        }
    })
}
```

### 内置脱敏拦截器

```kotlin
addInterceptor(AwDesensitizeInterceptor.create {
    phone()       // 中国手机号：138****5678（带词边界匹配）
    idCard()      // 身份证号：110***********1234（带词边界匹配）
    bankCard()    // 银行卡号：622***********1234（常见卡 BIN 开头，带词边界匹配）
    email()       // 邮箱：******（带词边界匹配）
    keyValue()    // password=xxx, token=xxx 等：******

    // 自定义规则
    custom("creditCard", Regex("""\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}"""))

    // 自定义脱敏策略
    custom("apiKey", Regex("""api[_-]?key\s*[=:]\s*\S+"""), AwDesensitizeInterceptor.Strategy.FULL)
})
```

脱敏策略：

| 策略 | 效果 | 示例 |
|------|------|------|
| `PARTIAL` | 保留前3后4 | `13812345678` → `138****5678` |
| `FULL` | 全部掩码 | `user@mail.com` → `******` |
| `HASH` | 取 hashCode | `secret` → `6c25a14` |

## 日志监听器

```kotlin
AwLogger.init {
    debug = true

    // 添加日志监听器，实时获取日志输出
    addListener(object : AwLogListener {
        override fun onLog(priority: Int, tag: String?, message: String, throwable: Throwable?) {
            // UI 展示、远程上报等
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
        showThread = true  // 显示线程信息
    }
}

// 使用预设格式化器
fileFormatter = AwLogFormatter.compact()  // 简洁模式：HH:mm:ss.SSS D/Tag: message
fileFormatter = AwLogFormatter.verbose()  // 详细模式：yyyy-MM-dd HH:mm:ss.SSS D/Tag: message

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
    ├── AwDebugTree            → Logcat 输出（单次栈遍历）
    ├── AwFileTree             → 文件日志（异步写入、智能刷新、磁盘检查、轮转清理）
    ├── AwCrashTree            → 崩溃收集（UncaughtExceptionHandler + ERROR 级别回调）
    └── Custom Trees           → 用户自定义 Tree

AwLogInterceptor (Chain)       → 责任链拦截器（过滤/脱敏/增强/Tag修改）
  └── AwDesensitizeInterceptor → 内置脱敏拦截器（手机号/身份证/银行卡/邮箱/key=value）
AwLogFormatter                 → 日志格式化（compact/verbose/自定义）
AwLogFileManager               → 文件管理（压缩/导出/清理/搜索/异步操作/磁盘查询）
AwLogListener                  → 日志监听器（实时回调）
```

## 性能优化

- **拦截器单次执行**：拦截仅在 AwLogger 层执行一次，Tree 不再重复拦截
- **智能文件刷新**：ScheduledThreadPoolExecutor 定时刷新（默认 3 秒）+ ERROR 级别即时 flush
- **Lambda 零开销**：日志关闭时 Lambda 完全不执行
- **线程安全格式化**：SimpleDateFormat 使用 ThreadLocal 包装，消除多线程竞争
- **单次栈遍历**：AwDebugTree 将方法位置信息编码到 Tag 中，消除双重栈遍历
- **异常隔离**：拦截器链中任一拦截器异常不会中断链路
- **内存文件大小追踪**：避免每次写入时调用 file.length() IO 操作（含 throwable 堆栈字节数）
- **队列溢出保护**：AwFileTree 日志队列最大 1024 条，满时丢弃最旧日志并输出警告，防止 OOM
- **单线程池**：AwFileTree 合并写入和调度为单个 ScheduledThreadPoolExecutor，减少线程开销
- **周期性文件清理**：每 100 次写入检查一次文件数量，避免高频日志场景下的性能开销
- **共享线程池**：AwLogFileManager 异步操作使用共享线程池，避免频繁创建线程

## 兼容性

| 依赖 | 最低版本 |
|------|----------|
| Android minSdk | 24+ |
| Kotlin | 2.0+ |
| Timber | 5.0.1 |
| Android Gradle Plugin | 8.0+ |

### ProGuard / R8

不需要额外配置。库已通过 `consumer-rules.pro` 自动配置 ProGuard 规则，引入依赖即生效。

## 迁移指南（1.x → 2.x）

| 变更 | 1.x | 2.x |
|------|-----|-----|
| Tag 设置 | `AwLogger.tag("MyTag").d("msg")` | `AwLogger.d("MyTag", "msg")` 或 `AwLogger.d("MyTag") { "msg" }` |
| 格式化日志 | `AwLogger.d(String.format("id=%d", id))` | `AwLogger.d("id=%d", id)` 或 `AwLogger.d("Tag", "id=%d", id)` |
| setMinPriority | `AwLogger.setMinPriority(Log.WARN)` | `val old = AwLogger.setMinPriority(Log.WARN)` (返回旧值) |
| isLoggable | 无 | `AwLogger.isLoggable(Log.DEBUG)` |
| XML 格式化 | 无 | `AwLogger.xml(xmlString, "Tag")` |

## FAQ

**Q: 日志文件存储位置应该选哪里？**
A: 推荐使用 `context.cacheDir.absolutePath + "/logs"`，这是应用私有目录，不需要存储权限，卸载时自动清理。

**Q: 多进程可以使用文件日志吗？**
A: 当前版本不支持多进程文件日志写入。多进程场景建议只在主进程启用文件日志，或使用 ContentProvider 协调。

**Q: Release 构建如何配置？**
A: 建议关闭 debug 输出，仅启用文件日志和崩溃收集：
```kotlin
AwLogger.init {
    debug = false
    fileLog = true
    fileDir = "${cacheDir.absolutePath}/logs"
    fileMinPriority = Log.WARN
    crashLog = true
}
```

**Q: ProGuard/R8 需要额外配置吗？**
A: 不需要。库已通过 `consumer-rules.pro` 自动配置 ProGuard 规则，引入依赖即生效。

**Q: 日志暴增时会不会 OOM？**
A: 不会。AwFileTree 内部队列最大 1024 条，满时会丢弃最旧日志并输出警告到 Logcat。

**Q: 多线程写日志安全吗？**
A: 安全。SimpleDateFormat 使用 ThreadLocal 包装，AwLogger.init() 使用 synchronized 保护，文件写入在单线程执行器中串行执行。

## 许可证

Apache License 2.0，详见 [LICENSE](LICENSE)。

# Last updated: 2026年 4月 21日
