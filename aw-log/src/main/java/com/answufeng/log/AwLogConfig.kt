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

    /** 添加自定义 Timber.Tree。 */
    fun addTree(tree: Timber.Tree) {
        extraTrees.add(tree)
    }

    /** 添加日志拦截器，拦截器按添加顺序组成责任链。 */
    fun addInterceptor(interceptor: AwLogInterceptor) {
        interceptors.add(interceptor)
    }
}
