package com.answufeng.log

import android.util.Log
import timber.log.Timber

/**
 * 日志系统 DSL 配置类。
 *
 * 通过 [AwLogger.init] 的 DSL 块进行配置：
 * ```kotlin
 * AwLogger.init {
 *     debug = BuildConfig.DEBUG
 *     fileLog = true
 *     fileDir = "${cacheDir.absolutePath}/logs"
 *     maxFileSize = 5L * 1024 * 1024
 *     maxFileCount = 10
 *     flushIntervalMs = 3000L
 *     crashLog = true
 *     crashHandler = { tag, throwable, message ->
 *         // 上报崩溃
 *     }
 *     // crashEchoToLogcat = true  // 可选：与 debug 同时为 true 时仍让 AwCrashTree 打 Logcat
 *     addInterceptor(AwDesensitizeInterceptor.create {
 *         phone()
 *         email()
 *     })
 * }
 * ```
 */
@AwLogDsl
class AwLogConfig {

    /** 是否启用 Logcat 调试输出（AwDebugTree），默认 true。 */
    var debug: Boolean = true

    /** 是否启用文件日志（AwFileTree），默认 false。 */
    var fileLog: Boolean = false

    /** 文件日志目录绝对路径。 */
    var fileDir: String = ""

    /** 单个日志文件最大大小（字节），默认 5MB。 */
    var maxFileSize: Long = 5L * 1024 * 1024
        set(value) {
            require(value > 0) { "maxFileSize must be > 0, got $value" }
            field = value
        }

    /** 最大日志文件数量，超出后自动删除最旧的文件，默认 10。 */
    var maxFileCount: Int = 10
        set(value) {
            require(value > 0) { "maxFileCount must be > 0, got $value" }
            field = value
        }

    /** 文件日志最低级别，默认 [Log.DEBUG]。 */
    var fileMinPriority: Int = Log.DEBUG
        set(value) {
            require(value >= Log.VERBOSE && value <= Log.ASSERT) {
                "fileMinPriority must be between ${Log.VERBOSE} and ${Log.ASSERT}, got $value"
            }
            field = value
        }

    /** 文件日志定时刷新间隔（毫秒），默认 3000ms。ERROR 级别日志始终即时刷新。 */
    var flushIntervalMs: Long = 3000L
        set(value) {
            require(value > 0) { "flushIntervalMs must be > 0, got $value" }
            field = value
        }

    /** 是否启用崩溃日志收集（AwCrashTree），默认 false。 */
    var crashLog: Boolean = false

    /** 崩溃处理回调，接收 (tag, throwable, message) 三个参数。 */
    var crashHandler: ((String?, Throwable?, String?) -> Unit)? = null

    /**
     * [AwCrashTree] 是否将经 Timber 分发的 ERROR/WTF 再次写入系统 Logcat。
     *
     * 为 `null`（默认）时等价于 `!debug`：Debug 构建已有 [AwDebugTree]，避免同一条 ERROR 打两遍；
     * Release（`debug = false`）默认为 `true`，便于在无 DebugTree 时仍能看见 ERROR 行。
     *
     * 未捕获异常仍由 [AwCrashCoordinator] 处理（`Log.e` 或 [crashHandler]），不受此开关影响。
     */
    var crashEchoToLogcat: Boolean? = null

    /**
     * 拦截器在 [AwLogInterceptor.intercept] 中抛出异常时的策略。
     *
     * 为 false（默认）时：记录一条 Logcat 警告并仍输出**原始**消息（与历史行为一致，但可能绕过脱敏）。
     * 为 true 时：丢弃该条日志（[AwLogInterceptor.LogResult.Rejected]），更适合强合规场景。
     */
    var rejectLogOnInterceptorFailure: Boolean = false

    /** 全局最低日志级别，低于此级别的日志不会输出，默认 [Log.VERBOSE]。 */
    var minPriority: Int = Log.VERBOSE
        set(value) {
            require(value >= Log.VERBOSE && value <= Log.ASSERT) {
                "minPriority must be between ${Log.VERBOSE} and ${Log.ASSERT}, got $value"
            }
            field = value
        }

    /** 文件日志格式化器，默认使用 [AwLogFormatter.default]。 */
    var fileFormatter: AwLogFormatter = AwLogFormatter.default()

    internal val extraTrees = mutableListOf<Timber.Tree>()
    internal val interceptors = mutableListOf<AwLogInterceptor>()
    internal val listeners = mutableListOf<AwLogListener>()

    fun addTree(tree: Timber.Tree) {
        extraTrees.add(tree)
    }

    fun addInterceptor(interceptor: AwLogInterceptor) {
        interceptors.add(interceptor)
    }

    fun addListener(listener: AwLogListener) {
        listeners.add(listener)
    }
}
