# aw-log

[![JitPack](https://jitpack.io/v/answufeng/aw-log.svg)](https://jitpack.io/#answufeng/aw-log)

基于 [Timber](https://github.com/JakeWharton/timber) 的 Android 日志库：**Logcat**、**文件落盘**、**崩溃回调**、**拦截链 / 脱敏**、**格式化**、**日志文件压缩 / 导出 / 搜索**。

如果你只想最快接入并打出第一条日志，直接看下面的「5 分钟上手（最小接入）」即可；其它内容都可以后置按需查阅。

---

## 5 分钟上手（最小接入）

### 1) 添加依赖（JitPack）

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.answufeng:aw-log:1.0.2")
}
```

`implementation` 中的 **版本号与 Git / JitPack 的 tag 一致**（上例为 `1.0.2`）。

### 2) 初始化（Application）

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AwLogger.init {
            debug = BuildConfig.DEBUG
        }
    }
}
```

### 3) 打第一条日志

```kotlin
AwLogger.d("Hello aw-log")
```

---

## 目录（按常见需求跳转）

| 我想… | 跳转 |
|--------|------|
| 最短时间跑通依赖与日志 | [5 分钟上手（最小接入）](#5-分钟上手最小接入) · [快速开始](#快速开始) |
| 看能力清单 | [特性](#特性) |
| 上线前检查 | [生产检查清单](#生产检查清单) · [与业务集成](#与业务系统集成) |
| 查配置项 | [AwLogConfig 配置表](#awlogconfig-配置表) |
| 拦截 / 脱敏 / 监听 / 格式化 | [进阶用法](#进阶用法) |
| 性能、线程、混淆 | [性能与线程](#性能与线程) · [兼容性](#兼容性) |
| 常见问题 | [FAQ](#faq) |

---

## 环境要求

| 项 | 最低版本 |
|----|----------|
| Android minSdk | 24 |
| Kotlin | 2.0+ |
| AGP | 8.0+ |
| Timber | 5.0.1 |
| Demo compileSdk / targetSdk（验证用） | 35 |

---

## 特性

| 模块 | 作用 |
|------|------|
| **AwDebugTree** | Logcat；Tag 含类名/方法/行号（单次栈遍历） |
| **AwFileTree** | 按日文件、大小轮转、异步队列、定时 + ERROR flush、磁盘与队列保护 |
| **AwCrashTree + AwCrashCoordinator** | 未捕获异常统一出口；可选 ERROR 再写 Logcat |
| **AwLogInterceptor** | 责任链：过滤、改文案、改 Tag |
| **AwDesensitizeInterceptor** | 内置手机/身份证/银行卡/邮箱/kv 等规则 |
| **AwLogFormatter** | 文件行格式；compact / verbose / DSL 自定义 |
| **AwLogFileManager** | 压缩、ZIP 导出、清理、按时间删、关键词搜索（含 .gz） |
| **AwLogListener** | 旁路监听（注意线程，勿在回调里同步网络） |
| 其它 | JSON/XML 美化（有大小上限）、Lambda 懒求值、`isLoggable` / `isFileLoggable`、DSL 初始化 |

---

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
    implementation("com.github.answufeng:aw-log:1.0.2")
}
```

**ProGuard / R8**：无需手写规则，依赖已带 `consumer-rules.pro`。

---

## 快速开始

在 `Application.onCreate()` 中调用 **`AwLogger.init { }`**（可重复调用，会重建配置）。

### 最小示例

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AwLogger.init {
            debug = BuildConfig.DEBUG
        }
    }
}
```

### 文件日志 + 自动目录

`fileLog = true` 且省略 `fileDir` 时，可用 **`AwLogger.init(context) { }`**，默认使用 `applicationContext.cacheDir/logs`：

```kotlin
AwLogger.init(this) {
    debug = BuildConfig.DEBUG
    fileLog = true
}
```

### 常用 API 摘录

```kotlin
AwLogger.d("ok")
AwLogger.d("Net", "timeout: %s", url)
AwLogger.d { "body=${response}" }
AwLogger.e(t, "API", "code=%d", code)
AwLogger.json(json, "API")
AwLogger.xml(xml, "API")

if (AwLogger.isLoggable(Log.DEBUG)) { /* 昂贵字符串只在这里拼 */ }

AwLogger.flush()   // 如 Activity.onDestroy
AwLogger.reset()   // 测试或切换配置
```

<details>
<summary><b>完整初始化示例（脱敏、监听、崩溃回调）</b></summary>

```kotlin
AwLogger.init {
    debug = BuildConfig.DEBUG
    fileLog = true
    fileDir = "${cacheDir.absolutePath}/logs"
    maxFileSize = 5L * 1024 * 1024
    maxFileCount = 10
    crashLog = true
    crashHandler = { tag, throwable, message ->
        // 对接 Crashlytics / Bugly 等
    }
    addInterceptor(AwDesensitizeInterceptor.create {
        phone(); idCard(); email(); keyValue()
    })
    addListener { p, tag, msg, t -> /* 旁路：入队再上传，勿同步网络 */ }
}
```

</details>

<details>
<summary><b>文件管理 API（压缩 / ZIP / 清理 / 搜索）</b></summary>

```kotlin
val dir = AwLogger.getFileDir()
AwLogFileManager.compressOldLogs(dir)
AwLogFileManager.exportLogs(dir, File(exportDir, "logs.zip"))  // 父目录会自动 mkdirs
AwLogFileManager.clearAll(dir)
AwLogFileManager.clearBefore(dir, Date(/* 早于该时间的文件 */))
AwLogFileManager.search(dir, "keyword")
// 异步：compressOldLogsAsync / exportLogsAsync / clearAllAsync / searchAsync …
```

阻塞 API 请在后台线程调用；异步回调在后台线程，更新 UI 请切主线程。

</details>

---

## AwLogConfig 配置表

| 配置项 | 类型 | 默认 | 说明 |
|--------|------|------|------|
| `debug` | Boolean | `true` | Logcat（AwDebugTree） |
| `fileLog` | Boolean | `false` | 文件（AwFileTree） |
| `fileDir` | String | `""` | 绝对路径；`fileLog` 为 true 时须非空（`init(Context)` 可自动填） |
| `maxFileSize` | Long | 5MB | 单文件上限 |
| `maxFileCount` | Int | `10` | 超出删最旧 |
| `fileMinPriority` | Int | `DEBUG` | 写入文件的最低级别 |
| `flushIntervalMs` | Long | `3000` | 定时 flush；ERROR 仍会即时 flush |
| `crashLog` | Boolean | `false` | 崩溃树 + Coordinator |
| `crashHandler` | 函数 | `null` | `(tag, throwable, message) -> Unit` |
| `crashEchoToLogcat` | Boolean? | `null` | `null`≈`!debug`，避免与 DebugTree 双写 ERROR |
| `minPriority` | Int | `VERBOSE` | 全局最低级别 |
| `fileFormatter` | AwLogFormatter | 默认 | 文件行格式 |
| `rejectLogOnInterceptorFailure` | Boolean | `false` | 拦截器抛错时是否**丢弃**该条（`true` 更偏合规） |

---

## 进阶用法

- **拦截**：实现 `AwLogInterceptor`，`chain.proceed(...)` 或 `LogResult.Rejected`。
- **脱敏**：`AwDesensitizeInterceptor.create { phone(); custom("x", Regex("...")) }`。
- **监听**：`addListener { ... }`；回调在**打日志线程**，勿阻塞。
- **格式化**：`AwLogFormatter.create { ... }` / `compact()` / `verbose()`。
- **自定义 Tree**：`addTree(object : Timber.Tree() { ... })`。

<details>
<summary><b>拦截器与脱敏（代码示例）</b></summary>

```kotlin
AwLogger.init {
    debug = true
    fileLog = true
    fileDir = "${cacheDir.absolutePath}/logs"

    addInterceptor(object : AwLogInterceptor {
        override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
            val msg = chain.message.replace(Regex("""password=\S+"""), "password=****")
            return chain.proceed(message = msg)
        }
    })

    addInterceptor(AwDesensitizeInterceptor.create {
        phone()
        idCard()
        email()
        keyValue()
        custom("apiKey", Regex("""api[_-]?key\s*[=:]\s*\S+"""), AwDesensitizeInterceptor.Strategy.FULL)
    })
}
```

脱敏策略：`PARTIAL`（前 3 后 4）、`FULL`、`HASH`。

</details>

**模块关系（鸟瞰）**：

```
AwLogger
├── AwDebugTree      → Logcat
├── AwFileTree       → 文件
├── AwCrashTree      → 崩溃相关 + 可选 Logcat 回显
└── 自定义 Tree
AwLogInterceptor 链 → AwDesensitizeInterceptor 等
AwLogFormatter / AwLogFileManager / AwLogListener
```

---

## 生产检查清单

- [ ] Release：收敛 `minPriority` / `fileMinPriority`，控制磁盘与隐私。
- [ ] 脱敏规则覆盖业务字段。
- [ ] 崩溃：`UncaughtExceptionHandler` 与第三方 SDK **只保留一套主导逻辑**。
- [ ] 日志目录：优先应用专属路径（如 `cacheDir/logs`）。
- [ ] 发版前宿主 **release + R8** 冒烟（demo 已 minify）。

---

## 与业务系统集成

本库**不内置** HTTP/OSS 上报，建议：

| 需求 | 做法 |
|------|------|
| 实时旁路 | `AwLogListener` → 队列/Channel → 后台批量上传（**禁止**回调里同步网络、避免监听里再打日志死循环） |
| 离线包 | `AwLogFileManager.exportLogs` / 异步回调；ZIP 可能含 PII，先脱敏或走加密通道 |
| 崩溃 | `crashHandler` 或与三方 SDK 协调单一 handler |

---

## 性能与线程

- 拦截只在 `AwLogger` 走一轮；Lambda 在级别关闭时不执行。
- 文件写入单线程；队列满丢弃最旧并告警；JSON/XML 美化有 **~512KB** 单条上限（超限则截断说明 + 前缀原文）。
- 文件体积累计用 UTF-8 码点计字节，避免每行 `toByteArray`。
- **监听器**在调用链线程执行；**AwLogFileManager** 同步方法须在后台线程。

| 组件 | 线程安全 |
|------|----------|
| AwLogger / Debug / File / Crash / 拦截链 | 安全（设计目标） |
| 内置 AwLogFormatter | ThreadLocal `SimpleDateFormat` |
| AwLogListener | 回调线程由调用方注意 |

---

## 兼容性

| 项 | 版本 |
|----|------|
| minSdk | 24+ |
| Kotlin | 2.0+ |
| Timber | 5.0.1 |
| AGP | 8.0+ |

---

## FAQ

| 问题 | 简要说明 |
|------|----------|
| 日志放哪？ | 推荐 `cacheDir` 下应用私有目录，无需存储权限。 |
| 多进程写文件？ | 不支持；仅主进程开文件日志或自协调。 |
| `fileLog` 且 `fileDir` 空？ | `init { }` 会抛 `IllegalArgumentException`；可用 `init(context){}` 自动目录。 |
| 队列会 OOM 吗？ | 队列有上限，满则丢最旧 + Logcat 警告。 |
| AGP 8.5+ 报 “requires core library desugaring”？ | 从 `1.0.2` 起库本身不再要求使用端开启 coreLibraryDesugaring；若你依赖了更旧版本，请在宿主 `:app` 启用 coreLibraryDesugaring。 |
| ERROR 打两次 Logcat？ | 默认 `crashEchoToLogcat=null` 时与 DebugTree 不重复；见配置表。 |
| 导出 ZIP 父目录不存在？ | `exportLogs` / `exportLogsAsync` 会 `mkdirs`，失败返回 `null`。 |
| `clearBefore` 按什么删？ | 按文件 **lastModified**，非严格按文件名日期。 |
| `.gz` 能搜吗？ | `search` 会读 `log_*.txt` 与 `log_*.txt.gz`。 |

**Release 示例**：

```kotlin
AwLogger.init {
    debug = false
    fileLog = true
    fileDir = "${cacheDir.absolutePath}/logs"
    fileMinPriority = Log.WARN
    crashLog = true
}
```

---

## 迁移

版本说明见 [MIGRATION.md](MIGRATION.md)。

---

## 演示

模块 **`demo`**：开关组合、文件、脱敏、崩溃等；手测清单见 [demo/DEMO_MATRIX.md](demo/DEMO_MATRIX.md)。**勿在生产照搬全开。**

**CI**：[`.github/workflows/ci.yml`](.github/workflows/ci.yml)。本地可参考：

`./gradlew :aw-log:assembleRelease :aw-log:ktlintCheck :aw-log:lintRelease :demo:assembleRelease`

---

## 许可证

Apache License 2.0，见 [LICENSE](LICENSE)。

---

*文档更新：2026-04-27*
