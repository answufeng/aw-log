package com.answufeng.log

import android.util.Log
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Android 结构化日志工具，基于 Timber 封装。
 *
 * 提供 Logcat 调试输出、文件日志持久化、崩溃收集、日志拦截与脱敏、自定义格式化等能力。
 *
 * 使用前需在 [Application.onCreate][android.app.Application.onCreate] 中调用 [init] 进行初始化：
 * ```kotlin
 * AwLogger.init {
 *     debug = BuildConfig.DEBUG
 *     fileLog = true
 *     fileDir = "${cacheDir.absolutePath}/logs"
 *     addInterceptor(AwDesensitizeInterceptor.create {
 *         phone()
 *         email()
 *         keyValue()
 *     })
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
     * 拦截器仅在 [AwLogger] 层执行一次，Tree 不再重复拦截。
     *
     * @param block DSL 配置块，详见 [AwLogConfig]
     */
    fun init(block: AwLogConfig.() -> Unit = {}) {
        val config = AwLogConfig().apply(block)

        if (initialized) {
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
                    flushIntervalMs = config.flushIntervalMs
                )
            )
        }

        if (config.crashLog) {
            Timber.plant(AwCrashTree(config.crashHandler))
        }

        config.extraTrees.forEach { Timber.plant(it) }

        initialized = true
    }

    /** 是否已初始化。 */
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
     * 建议在 [Activity.onDestroy][android.app.Activity.onDestroy] 或
     * [Application.onTerminate][android.app.Application.onTerminate] 中调用。
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
    internal fun intercept(priority: Int, tag: String?, message: String, t: Throwable?): AwLogInterceptor.LogResult {
        if (interceptors.isEmpty()) return AwLogInterceptor.LogResult.Accepted(message, tag)
        val chain = RealInterceptorChain(interceptors, 0, priority, tag, message, t)
        return chain.proceed(message, tag)
    }

    private class RealInterceptorChain(
        private val interceptors: List<AwLogInterceptor>,
        private val index: Int,
        override val priority: Int,
        override val tag: String?,
        override val message: String,
        override val throwable: Throwable?
    ) : AwLogInterceptor.Chain {

        override fun proceed(message: String, tag: String?): AwLogInterceptor.LogResult {
            if (index >= interceptors.size) {
                return AwLogInterceptor.LogResult.Accepted(message, tag)
            }
            val next = RealInterceptorChain(interceptors, index + 1, priority, tag, message, throwable)
            return try {
                interceptors[index].intercept(next)
            } catch (e: Exception) {
                AwLogInterceptor.LogResult.Accepted(message, tag)
            }
        }
    }

    @PublishedApi
    internal fun logInternal(priority: Int, tag: String? = null, message: String, t: Throwable? = null) {
        val result = intercept(priority, tag, message, t)
        if (result is AwLogInterceptor.LogResult.Accepted) {
            val effectiveTag = result.tag ?: tag
            val finalMessage = result.message
            if (effectiveTag != null) {
                Timber.tag(effectiveTag)
            }
            when (priority) {
                Log.VERBOSE -> if (t != null) Timber.v(t, finalMessage) else Timber.v(finalMessage)
                Log.DEBUG -> if (t != null) Timber.d(t, finalMessage) else Timber.d(finalMessage)
                Log.INFO -> if (t != null) Timber.i(t, finalMessage) else Timber.i(finalMessage)
                Log.WARN -> if (t != null) Timber.w(t, finalMessage) else Timber.w(finalMessage)
                Log.ERROR -> if (t != null) Timber.e(t, finalMessage) else Timber.e(finalMessage)
                Log.ASSERT -> if (t != null) Timber.wtf(t, finalMessage) else Timber.wtf(finalMessage)
            }
        }
    }

    /** 记录 VERBOSE 级别日志。 */
    fun v(message: String, vararg args: Any?) {
        if (shouldLog(Log.VERBOSE)) logInternal(Log.VERBOSE, message = message)
    }

    /** 记录 VERBOSE 级别日志（Lambda 延迟求值，日志关闭时零开销）。 */
    inline fun v(crossinline message: () -> String) {
        if (shouldLog(Log.VERBOSE)) logInternal(Log.VERBOSE, message = message())
    }

    /**
     * 记录 VERBOSE 级别日志（Lambda 延迟求值 + 指定 Tag）。
     *
     * ```kotlin
     * AwLogger.v("Network") { "连接建立: ${url}" }
     * ```
     */
    inline fun v(tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.VERBOSE)) logInternal(Log.VERBOSE, tag, message())
    }

    /** 记录 DEBUG 级别日志。 */
    fun d(message: String, vararg args: Any?) {
        if (shouldLog(Log.DEBUG)) logInternal(Log.DEBUG, message = message)
    }

    /** 记录 DEBUG 级别日志（Lambda 延迟求值，日志关闭时零开销）。 */
    inline fun d(crossinline message: () -> String) {
        if (shouldLog(Log.DEBUG)) logInternal(Log.DEBUG, message = message())
    }

    /**
     * 记录 DEBUG 级别日志（Lambda 延迟求值 + 指定 Tag）。
     *
     * ```kotlin
     * AwLogger.d("Network") { "响应: ${response.body}" }
     * ```
     */
    inline fun d(tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.DEBUG)) logInternal(Log.DEBUG, tag, message())
    }

    /** 记录 INFO 级别日志。 */
    fun i(message: String, vararg args: Any?) {
        if (shouldLog(Log.INFO)) logInternal(Log.INFO, message = message)
    }

    /** 记录 INFO 级别日志（Lambda 延迟求值，日志关闭时零开销）。 */
    inline fun i(crossinline message: () -> String) {
        if (shouldLog(Log.INFO)) logInternal(Log.INFO, message = message())
    }

    /** 记录 INFO 级别日志（Lambda 延迟求值 + 指定 Tag）。 */
    inline fun i(tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.INFO)) logInternal(Log.INFO, tag, message())
    }

    /** 记录 WARN 级别日志。 */
    fun w(message: String, vararg args: Any?) {
        if (shouldLog(Log.WARN)) logInternal(Log.WARN, message = message)
    }

    /** 记录 WARN 级别日志（Lambda 延迟求值，日志关闭时零开销）。 */
    inline fun w(crossinline message: () -> String) {
        if (shouldLog(Log.WARN)) logInternal(Log.WARN, message = message())
    }

    /** 记录 WARN 级别日志（Lambda 延迟求值 + 指定 Tag）。 */
    inline fun w(tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.WARN)) logInternal(Log.WARN, tag, message())
    }

    /** 记录 WARN 级别日志，附带异常。 */
    fun w(t: Throwable?, message: String, vararg args: Any?) {
        if (shouldLog(Log.WARN)) logInternal(Log.WARN, message = message, t = t)
    }

    /** 记录 ERROR 级别日志。 */
    fun e(message: String, vararg args: Any?) {
        if (shouldLog(Log.ERROR)) logInternal(Log.ERROR, message = message)
    }

    /** 记录 ERROR 级别日志（Lambda 延迟求值，日志关闭时零开销）。 */
    inline fun e(crossinline message: () -> String) {
        if (shouldLog(Log.ERROR)) logInternal(Log.ERROR, message = message())
    }

    /**
     * 记录 ERROR 级别日志（Lambda 延迟求值 + 指定 Tag）。
     *
     * ```kotlin
     * AwLogger.e("API") { "请求失败: ${error.message}" }
     * ```
     */
    inline fun e(tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.ERROR)) logInternal(Log.ERROR, tag, message())
    }

    /** 记录 ERROR 级别日志，附带异常。 */
    fun e(t: Throwable?, message: String, vararg args: Any?) {
        if (shouldLog(Log.ERROR)) logInternal(Log.ERROR, message = message, t = t)
    }

    /** 记录 ERROR 级别日志，仅附异常。 */
    fun e(t: Throwable?) {
        if (shouldLog(Log.ERROR)) logInternal(Log.ERROR, message = t?.stackTraceToString() ?: "Unknown error", t = t)
    }

    /** 记录 ASSERT（WTF）级别日志。 */
    fun wtf(message: String, vararg args: Any?) {
        if (shouldLog(Log.ASSERT)) logInternal(Log.ASSERT, message = message)
    }

    /** 记录 ASSERT（WTF）级别日志（Lambda 延迟求值，日志关闭时零开销）。 */
    inline fun wtf(crossinline message: () -> String) {
        if (shouldLog(Log.ASSERT)) logInternal(Log.ASSERT, message = message())
    }

    /** 记录 ASSERT（WTF）级别日志（Lambda 延迟求值 + 指定 Tag）。 */
    inline fun wtf(tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.ASSERT)) logInternal(Log.ASSERT, tag, message())
    }

    /** 记录 ASSERT（WTF）级别日志，附带异常。 */
    fun wtf(t: Throwable?, message: String, vararg args: Any?) {
        if (shouldLog(Log.ASSERT)) logInternal(Log.ASSERT, message = message, t = t)
    }

    /**
     * 格式化并输出 JSON 字符串。
     *
     * 自动识别 JSONObject / JSONArray 并美化输出，非 JSON 内容原样输出。
     *
     * @param json JSON 字符串，为 null 或空白时输出提示
     * @param tag 可选标签，显示在 JSON 输出头部
     * @param priority 日志级别，默认 [Log.DEBUG]，可指定为 [Log.INFO] 或 [Log.ERROR] 等
     */
    fun json(json: String?, tag: String? = null, priority: Int = Log.DEBUG) {
        if (json.isNullOrBlank()) {
            logInternal(priority, tag, "${tag?.let { "[$it] " } ?: ""}Empty/Null JSON")
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
            logInternal(priority, tag, sb.toString())
        } catch (e: org.json.JSONException) {
            logInternal(Log.ERROR, tag, "JSON parse error", e)
        } catch (e: Exception) {
            logInternal(Log.ERROR, tag, "JSON format error", e)
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
