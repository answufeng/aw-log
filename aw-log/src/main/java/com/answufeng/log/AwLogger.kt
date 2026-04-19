package com.answufeng.log

import android.util.Log
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 日志库入口，基于 Timber 封装的结构化日志工具。
 *
 * 支持 Logcat 调试输出、文件日志持久化、崩溃收集、日志拦截与脱敏、
 * 自定义格式化、JSON/XML 格式化输出等功能。
 *
 * 使用前须调用 [init] 进行初始化，推荐在 `Application.onCreate()` 中调用。
 */
object AwLogger {

    @Volatile
    private var initialized = false

    @Volatile
    private var minPriority = Log.VERBOSE

    @Volatile
    private var fileDir: String = ""

    private val interceptors = CopyOnWriteArrayList<AwLogInterceptor>()

    private val listeners = CopyOnWriteArrayList<AwLogListener>()

    /**
     * 初始化日志系统。通过 DSL 块配置各项参数。
     *
     * 重复调用会先关闭旧的 Tree 再重新初始化。线程安全。
     *
     * @param block 配置 DSL 块，参见 [AwLogConfig]
     */
    fun init(block: AwLogConfig.() -> Unit = {}) {
        synchronized(this) {
            val config = AwLogConfig().apply(block)

            if (initialized) {
                Timber.forest()
                    .filterIsInstance<AwFileTree>()
                    .forEach { it.shutdown() }
                Log.w("AwLogger", "AwLogger already initialized, re-initializing with new config")
            }

            Timber.uprootAll()

            minPriority = config.minPriority
            fileDir = if (config.fileLog) config.fileDir else ""
            interceptors.clear()
            interceptors.addAll(config.interceptors)
            listeners.clear()
            listeners.addAll(config.listeners)

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
                val crashTree = AwCrashTree(config.crashHandler)
                crashTree.installCrashHandler()
                Timber.plant(crashTree)
            }

            config.extraTrees.forEach { Timber.plant(it) }

            initialized = true
        }
    }

    /** 日志系统是否已初始化。 */
    fun isInitialized(): Boolean = initialized

    /**
     * 重置日志系统，移除所有 Tree、拦截器和监听器。
     *
     * 会先关闭文件日志线程池，再清除所有状态。线程安全。
     */
    fun reset() {
        synchronized(this) {
            Timber.forest()
                .filterIsInstance<AwFileTree>()
                .forEach { it.shutdown() }
            AwLogFileManager.shutdown()
            Timber.uprootAll()
            interceptors.clear()
            listeners.clear()
            initialized = false
            minPriority = Log.VERBOSE
            fileDir = ""
        }
    }

    /** 将文件日志缓冲区刷新到磁盘。通常在 Activity.onDestroy() 中调用。 */
    fun flush() {
        Timber.forest()
            .filterIsInstance<AwFileTree>()
            .forEach { it.flush() }
    }

    /**
     * 动态设置全局最低日志级别，低于此级别的日志不会输出。
     *
     * @param priority 日志级别，取值为 [Log.VERBOSE] 到 [Log.ASSERT]
     * @return 之前的最低日志级别，方便恢复
     */
    fun setMinPriority(priority: Int): Int {
        require(priority >= Log.VERBOSE && priority <= Log.ASSERT) {
            "minPriority must be between ${Log.VERBOSE} and ${Log.ASSERT}, got $priority"
        }
        val old = minPriority
        minPriority = priority
        return old
    }

    /**
     * 判断指定级别的日志是否会被输出。
     *
     * 可用于在构造复杂日志对象前判断是否需要记录，避免不必要的开销。
     *
     * @param priority 日志级别
     * @return true 表示该级别的日志会被输出
     */
    fun isLoggable(priority: Int): Boolean {
        return Timber.treeCount > 0 && minPriority <= priority
    }

    /** 获取文件日志目录路径，未启用文件日志时返回空字符串。 */
    fun getFileDir(): String = fileDir

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
            notifyListeners(priority, effectiveTag, finalMessage, t)
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

    private fun notifyListeners(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (listeners.isEmpty()) return
        for (listener in listeners) {
            try {
                listener.onLog(priority, tag, message, t)
            } catch (_: Exception) {
            }
        }
    }

    private fun formatMessage(message: String, args: Array<out Any?>): String {
        return if (args.isEmpty()) message
        else try {
            String.format(java.util.Locale.US, message, *args)
        } catch (e: Exception) {
            "$message [format error: ${e.message}]"
        }
    }

    fun v(message: String, vararg args: Any?) {
        if (shouldLog(Log.VERBOSE)) logInternal(Log.VERBOSE, message = formatMessage(message, args))
    }

    fun v(tag: String, message: String, vararg args: Any?) {
        if (shouldLog(Log.VERBOSE)) logInternal(Log.VERBOSE, tag, formatMessage(message, args))
    }

    inline fun v(crossinline message: () -> String) {
        if (shouldLog(Log.VERBOSE)) logInternal(Log.VERBOSE, message = message())
    }

    inline fun v(tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.VERBOSE)) logInternal(Log.VERBOSE, tag, message())
    }

    fun d(message: String, vararg args: Any?) {
        if (shouldLog(Log.DEBUG)) logInternal(Log.DEBUG, message = formatMessage(message, args))
    }

    fun d(tag: String, message: String, vararg args: Any?) {
        if (shouldLog(Log.DEBUG)) logInternal(Log.DEBUG, tag, formatMessage(message, args))
    }

    inline fun d(crossinline message: () -> String) {
        if (shouldLog(Log.DEBUG)) logInternal(Log.DEBUG, message = message())
    }

    inline fun d(tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.DEBUG)) logInternal(Log.DEBUG, tag, message())
    }

    fun i(message: String, vararg args: Any?) {
        if (shouldLog(Log.INFO)) logInternal(Log.INFO, message = formatMessage(message, args))
    }

    fun i(tag: String, message: String, vararg args: Any?) {
        if (shouldLog(Log.INFO)) logInternal(Log.INFO, tag, formatMessage(message, args))
    }

    inline fun i(crossinline message: () -> String) {
        if (shouldLog(Log.INFO)) logInternal(Log.INFO, message = message())
    }

    inline fun i(tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.INFO)) logInternal(Log.INFO, tag, message())
    }

    fun w(message: String, vararg args: Any?) {
        if (shouldLog(Log.WARN)) logInternal(Log.WARN, message = formatMessage(message, args))
    }

    fun w(tag: String, message: String, vararg args: Any?) {
        if (shouldLog(Log.WARN)) logInternal(Log.WARN, tag, formatMessage(message, args))
    }

    inline fun w(crossinline message: () -> String) {
        if (shouldLog(Log.WARN)) logInternal(Log.WARN, message = message())
    }

    inline fun w(tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.WARN)) logInternal(Log.WARN, tag, message())
    }

    fun w(t: Throwable?, message: String, vararg args: Any?) {
        if (shouldLog(Log.WARN)) logInternal(Log.WARN, message = formatMessage(message, args), t = t)
    }

    fun e(message: String, vararg args: Any?) {
        if (shouldLog(Log.ERROR)) logInternal(Log.ERROR, message = formatMessage(message, args))
    }

    fun e(tag: String, message: String, vararg args: Any?) {
        if (shouldLog(Log.ERROR)) logInternal(Log.ERROR, tag, formatMessage(message, args))
    }

    inline fun e(crossinline message: () -> String) {
        if (shouldLog(Log.ERROR)) logInternal(Log.ERROR, message = message())
    }

    inline fun e(tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.ERROR)) logInternal(Log.ERROR, tag, message())
    }

    fun e(t: Throwable?, message: String, vararg args: Any?) {
        if (shouldLog(Log.ERROR)) logInternal(Log.ERROR, message = formatMessage(message, args), t = t)
    }

    fun e(t: Throwable?) {
        if (shouldLog(Log.ERROR)) logInternal(Log.ERROR, message = t?.stackTraceToString() ?: "Unknown error", t = t)
    }

    inline fun e(t: Throwable?, tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.ERROR)) logInternal(Log.ERROR, tag, message(), t)
    }

    fun wtf(message: String, vararg args: Any?) {
        if (shouldLog(Log.ASSERT)) logInternal(Log.ASSERT, message = formatMessage(message, args))
    }

    fun wtf(tag: String, message: String, vararg args: Any?) {
        if (shouldLog(Log.ASSERT)) logInternal(Log.ASSERT, tag, formatMessage(message, args))
    }

    inline fun wtf(crossinline message: () -> String) {
        if (shouldLog(Log.ASSERT)) logInternal(Log.ASSERT, message = message())
    }

    inline fun wtf(tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.ASSERT)) logInternal(Log.ASSERT, tag, message())
    }

    fun wtf(t: Throwable?, message: String, vararg args: Any?) {
        if (shouldLog(Log.ASSERT)) logInternal(Log.ASSERT, message = formatMessage(message, args), t = t)
    }

    /**
     * 格式化输出 JSON 日志，自动识别 JSONObject/JSONArray 并美化输出。
     *
     * @param json JSON 字符串，为 null 或空白时输出提示信息
     * @param tag 日志标签，为 null 时不设置
     * @param priority 日志级别，默认 [Log.DEBUG]
     */
    fun json(json: String?, tag: String? = null, priority: Int = Log.DEBUG) {
        if (json.isNullOrBlank()) {
            logInternal(priority, tag, "Empty/Null JSON")
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
            sb.appendLine("┌────────────────────────────────────")
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
     * 格式化输出 XML 日志，自动缩进美化。
     *
     * @param xml XML 字符串，为 null 或空白时输出提示信息
     * @param tag 日志标签，为 null 时不设置
     * @param priority 日志级别，默认 [Log.DEBUG]
     */
    fun xml(xml: String?, tag: String? = null, priority: Int = Log.DEBUG) {
        if (xml.isNullOrBlank()) {
            logInternal(priority, tag, "Empty/Null XML")
            return
        }
        try {
            val trimmed = xml.trim()
            val formatted = formatXml(trimmed)
            val lines = formatted.split("\n")
            val sb = StringBuilder()
            sb.appendLine("┌────────────────────────────────────")
            lines.forEach { line ->
                sb.appendLine("│ $line")
            }
            sb.appendLine("└────────────────────────────────────")
            logInternal(priority, tag, sb.toString())
        } catch (e: Exception) {
            logInternal(Log.ERROR, tag, "XML format error", e)
        }
    }

    private fun formatXml(xml: String): String {
        val sb = StringBuilder()
        var indent = 0
        val regex = Regex("(<[^>]+>)|([^<]+)")
        val tokens = regex.findAll(xml).map { it.value }.toList()
        for (token in tokens) {
            val trimmed = token.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("</")) {
                indent = maxOf(0, indent - 1)
                repeat(indent) { sb.append("  ") }
                sb.appendLine(trimmed)
            } else if (trimmed.startsWith("<") && trimmed.endsWith("/>")) {
                repeat(indent) { sb.append("  ") }
                sb.appendLine(trimmed)
            } else if (trimmed.startsWith("<") && trimmed.contains("</")) {
                repeat(indent) { sb.append("  ") }
                sb.appendLine(trimmed)
            } else if (trimmed.startsWith("<")) {
                repeat(indent) { sb.append("  ") }
                sb.appendLine(trimmed)
                indent++
            } else {
                repeat(indent) { sb.append("  ") }
                sb.appendLine(trimmed)
            }
        }
        return sb.toString().trimEnd()
    }
}
