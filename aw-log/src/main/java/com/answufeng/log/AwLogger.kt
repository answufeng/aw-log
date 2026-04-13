package com.answufeng.log

import android.util.Log
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Android 结构化日志工具，基于 Timber 封装。
 *
 * 提供 Logcat 调试输出、文件日志持久化、崩溃收集、日志拦截与脱敏、自定义格式化等能力。
 *
 * 使用前需在 [Application.onCreate] 中调用 [init] 进行初始化：
 * ```kotlin
 * AwLogger.init {
 *     debug = BuildConfig.DEBUG
 *     fileLog = true
 *     fileDir = "${cacheDir.absolutePath}/logs"
 * }
 * ```
 */
object AwLogger {

    @Volatile
    private var initialized = false

    @Volatile
    private var minPriority = Log.VERBOSE

    private val interceptors = CopyOnWriteArrayList<AwLogInterceptor>()

    /**
     * 初始化日志系统。
     *
     * 可重复调用，每次调用会清除旧配置并重新初始化。
     *
     * @param block DSL 配置块，详见 [AwLogConfig]
     */
    fun init(block: AwLogConfig.() -> Unit = {}) {
        val config = AwLogConfig().apply(block)

        val wasInitialized = initialized
        if (wasInitialized) {
            Log.w("AwLogger", "AwLogger already initialized, re-initializing with new config")
        }

        Timber.uprootAll()

        minPriority = config.minPriority
        interceptors.clear()
        interceptors.addAll(config.interceptors)

        if (config.debug) {
            Timber.plant(AwDebugTree())
        }

        if (config.fileLog && config.fileDir.isNotBlank()) {
            Timber.plant(
                AwFileTree(
                    logDir = config.fileDir,
                    maxFileSize = config.maxFileSize,
                    maxFileCount = config.maxFileCount,
                    minPriority = config.fileMinPriority,
                    formatter = config.fileFormatter,
                    interceptors = config.interceptors.toList()
                )
            )
        }

        if (config.crashLog) {
            Timber.plant(AwCrashTree(config.crashHandler))
        }

        config.extraTrees.forEach { Timber.plant(it) }

        initialized = true
    }

    /**
     * 是否已初始化。
     */
    fun isInitialized(): Boolean = initialized

    /**
     * 重置日志系统，清除所有 Tree 和拦截器。
     *
     * 通常用于测试或动态切换配置。
     */
    fun reset() {
        Timber.uprootAll()
        interceptors.clear()
        initialized = false
        minPriority = Log.VERBOSE
    }

    /**
     * 刷新文件日志缓冲区，确保日志写入磁盘。
     *
     * 建议在 [Activity.onDestroy] 或 [Application.onTerminate] 中调用。
     */
    fun flush() {
        Timber.forest()
            .filterIsInstance<AwFileTree>()
            .forEach { it.flush() }
    }

    @PublishedApi
    internal fun shouldLog(priority: Int): Boolean {
        return Timber.treeCount > 0 && minPriority <= priority
    }

    @PublishedApi
    internal fun intercept(priority: Int, tag: String?, message: String): Boolean {
        if (interceptors.isEmpty()) return true
        return interceptors.all { interceptor ->
            interceptor.intercept(priority, tag, message, null) == AwLogInterceptor.LogResult.ACCEPTED
        }
    }

    /**
     * 记录 VERBOSE 级别日志。
     *
     * @param message 日志消息，支持格式化参数（如 `"userId=%s"` ）
     * @param args 格式化参数
     */
    fun v(message: String, vararg args: Any?) {
        if (shouldLog(Log.VERBOSE) && intercept(Log.VERBOSE, null, message)) {
            Timber.v(message, *args)
        }
    }

    /**
     * 记录 VERBOSE 级别日志（Lambda 延迟求值）。
     *
     * 日志关闭时不会执行 Lambda，零开销。
     */
    inline fun v(crossinline message: () -> String) {
        if (shouldLog(Log.VERBOSE)) {
            val msg = message()
            if (intercept(Log.VERBOSE, null, msg)) Timber.v(msg)
        }
    }

    /**
     * 记录 DEBUG 级别日志。
     *
     * @param message 日志消息，支持格式化参数
     * @param args 格式化参数
     */
    fun d(message: String, vararg args: Any?) {
        if (shouldLog(Log.DEBUG) && intercept(Log.DEBUG, null, message)) {
            Timber.d(message, *args)
        }
    }

    /**
     * 记录 DEBUG 级别日志（Lambda 延迟求值）。
     */
    inline fun d(crossinline message: () -> String) {
        if (shouldLog(Log.DEBUG)) {
            val msg = message()
            if (intercept(Log.DEBUG, null, msg)) Timber.d(msg)
        }
    }

    /**
     * 记录 INFO 级别日志。
     *
     * @param message 日志消息，支持格式化参数
     * @param args 格式化参数
     */
    fun i(message: String, vararg args: Any?) {
        if (shouldLog(Log.INFO) && intercept(Log.INFO, null, message)) {
            Timber.i(message, *args)
        }
    }

    /**
     * 记录 INFO 级别日志（Lambda 延迟求值）。
     */
    inline fun i(crossinline message: () -> String) {
        if (shouldLog(Log.INFO)) {
            val msg = message()
            if (intercept(Log.INFO, null, msg)) Timber.i(msg)
        }
    }

    /**
     * 记录 WARN 级别日志。
     *
     * @param message 日志消息，支持格式化参数
     * @param args 格式化参数
     */
    fun w(message: String, vararg args: Any?) {
        if (shouldLog(Log.WARN) && intercept(Log.WARN, null, message)) {
            Timber.w(message, *args)
        }
    }

    /**
     * 记录 WARN 级别日志（Lambda 延迟求值）。
     */
    inline fun w(crossinline message: () -> String) {
        if (shouldLog(Log.WARN)) {
            val msg = message()
            if (intercept(Log.WARN, null, msg)) Timber.w(msg)
        }
    }

    /**
     * 记录 WARN 级别日志，附带异常。
     *
     * @param t 异常对象
     * @param message 日志消息，支持格式化参数
     * @param args 格式化参数
     */
    fun w(t: Throwable?, message: String, vararg args: Any?) {
        if (shouldLog(Log.WARN) && intercept(Log.WARN, null, message)) Timber.w(t, message, *args)
    }

    /**
     * 记录 ERROR 级别日志。
     *
     * @param message 日志消息，支持格式化参数
     * @param args 格式化参数
     */
    fun e(message: String, vararg args: Any?) {
        if (shouldLog(Log.ERROR) && intercept(Log.ERROR, null, message)) {
            Timber.e(message, *args)
        }
    }

    /**
     * 记录 ERROR 级别日志（Lambda 延迟求值）。
     */
    inline fun e(crossinline message: () -> String) {
        if (shouldLog(Log.ERROR)) {
            val msg = message()
            if (intercept(Log.ERROR, null, msg)) Timber.e(msg)
        }
    }

    /**
     * 记录 ERROR 级别日志，附带异常。
     *
     * @param t 异常对象
     * @param message 日志消息，支持格式化参数
     * @param args 格式化参数
     */
    fun e(t: Throwable?, message: String, vararg args: Any?) {
        if (shouldLog(Log.ERROR) && intercept(Log.ERROR, null, message)) Timber.e(t, message, *args)
    }

    /**
     * 记录 ERROR 级别日志，仅附异常。
     *
     * @param t 异常对象
     */
    fun e(t: Throwable?) {
        if (shouldLog(Log.ERROR)) Timber.e(t)
    }

    /**
     * 记录 ASSERT（WTF）级别日志。
     *
     * @param message 日志消息，支持格式化参数
     * @param args 格式化参数
     */
    fun wtf(message: String, vararg args: Any?) {
        if (shouldLog(Log.ASSERT) && intercept(Log.ASSERT, null, message)) {
            Timber.wtf(message, *args)
        }
    }

    /**
     * 记录 ASSERT（WTF）级别日志（Lambda 延迟求值）。
     */
    inline fun wtf(crossinline message: () -> String) {
        if (shouldLog(Log.ASSERT)) {
            val msg = message()
            if (intercept(Log.ASSERT, null, msg)) Timber.wtf(msg)
        }
    }

    /**
     * 记录 ASSERT（WTF）级别日志，附带异常。
     *
     * @param t 异常对象
     * @param message 日志消息，支持格式化参数
     * @param args 格式化参数
     */
    fun wtf(t: Throwable?, message: String, vararg args: Any?) {
        if (shouldLog(Log.ASSERT)) Timber.wtf(t, message, *args)
    }

    /**
     * 格式化并输出 JSON 字符串。
     *
     * 自动识别 JSONObject / JSONArray 并美化输出，非 JSON 内容原样输出。
     *
     * @param json JSON 字符串，为 null 或空白时输出提示
     * @param tag 可选标签，显示在 JSON 输出头部
     */
    fun json(json: String?, tag: String? = null) {
        if (json.isNullOrBlank()) {
            d("${tag?.let { "[$it] " } ?: ""}Empty/Null JSON")
            return
        }
        try {
            val trimmed = json.trim()
            val formatted = when {
                trimmed.startsWith("{") -> {
                    org.json.JSONObject(trimmed).toString(2)
                }
                trimmed.startsWith("[") -> {
                    org.json.JSONArray(trimmed).toString(2)
                }
                else -> json
            }
            val lines = formatted.split("\n")
            val sb = StringBuilder()
            sb.appendLine("${tag?.let { "[$it] " } ?: ""}┌────────────────────────────────────")
            lines.forEach { line ->
                sb.appendLine("│ $line")
            }
            sb.appendLine("└────────────────────────────────────")
            d(sb.toString())
        } catch (e: org.json.JSONException) {
            e(e, "JSON parse error")
        } catch (e: Exception) {
            e(e, "JSON format error")
        }
    }

    /**
     * 设置后续日志的 Tag，返回 Timber.Tree 供链式调用。
     *
     * ```kotlin
     * AwLogger.tag("Network").d("连接超时")
     * ```
     *
     * @param tag 日志标签
     * @return 带标签的 Timber.Tree
     */
    fun tag(tag: String): Timber.Tree = Timber.tag(tag)
}
