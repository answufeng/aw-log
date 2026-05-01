package com.answufeng.log

import android.content.Context
import android.util.Log
import java.io.File
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 日志库入口，基于 Timber 封装的结构化日志工具。
 *
 * 支持 Logcat 调试输出、文件日志持久化、崩溃收集、日志拦截与脱敏、
 * 自定义格式化、JSON/XML 格式化输出等功能。
 *
 * **性能**：超大 JSON/XML 会在内存中完整格式化；生产环境建议先截断再调用 [json]/[xml]，或仅调试期开启。
 *
 * 使用前须调用 [init] 进行初始化，推荐在 `Application.onCreate()` 中调用。
 */
object AwLogger {

    private const val MAX_JSON_OR_XML_CHARS = 512_000

    @Volatile
    private var initialized = false

    @Volatile
    private var minPriority = Log.VERBOSE

    @Volatile
    private var fileDir: String = ""

    @Volatile
    private var fileLogActive: Boolean = false

    @Volatile
    private var fileMinPriorityStored: Int = Log.DEBUG

    @Volatile
    private var rejectLogOnInterceptorFailure: Boolean = false

    private val interceptors = CopyOnWriteArrayList<AwLogInterceptor>()

    private val listeners = CopyOnWriteArrayList<AwLogListener>()

    /**
     * 初始化日志系统。通过 DSL 块配置各项参数。
     *
     * 重复调用会先关闭旧的 Tree 再重新初始化。线程安全。
     *
     * @param block 配置 DSL 块，参见 [AwLogConfig]
     */
    /**
     * 使用 [Context] 預設檔案目錄（`context.cacheDir/logs`）初始化；若 DSL 中已指定 [AwLogConfig.fileDir] 則不覆寫。
     */
    fun init(context: Context, block: AwLogConfig.() -> Unit = {}) {
        init {
            if (fileDir.isBlank()) {
                fileDir = File(context.applicationContext.cacheDir, "logs").absolutePath
            }
            block()
        }
    }

    fun init(block: AwLogConfig.() -> Unit = {}) {
        synchronized(this) {
            val config = AwLogConfig().apply(block)

            if (config.fileLog) {
                require(config.fileDir.isNotBlank()) {
                    "fileDir must not be blank when fileLog is true"
                }
            }

            if (initialized) {
                Timber.forest()
                    .filterIsInstance<AwFileTree>()
                    .forEach { it.shutdown() }
                Log.w("AwLogger", "AwLogger already initialized, re-initializing with new config")
            }

            Timber.uprootAll()

            minPriority = config.minPriority
            fileDir = if (config.fileLog) config.fileDir else ""
            fileLogActive = config.fileLog && config.fileDir.isNotBlank()
            fileMinPriorityStored = config.fileMinPriority
            rejectLogOnInterceptorFailure = config.rejectLogOnInterceptorFailure
            interceptors.clear()
            interceptors.addAll(config.interceptors)
            listeners.clear()
            listeners.addAll(config.listeners)

            AwCrashCoordinator.syncFromConfig(config.crashLog, config.crashHandler)

            if (config.debug) {
                Timber.plant(AwDebugTree())
            }

            if (fileLogActive) {
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
                val crashEcho = config.crashEchoToLogcat ?: !config.debug
                Timber.plant(AwCrashTree(echoToLogcat = crashEcho))
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
            AwCrashCoordinator.syncFromConfig(false, null)
            Timber.uprootAll()
            interceptors.clear()
            listeners.clear()
            initialized = false
            minPriority = Log.VERBOSE
            fileDir = ""
            fileLogActive = false
            fileMinPriorityStored = Log.DEBUG
            rejectLogOnInterceptorFailure = false
        }
    }

    /** 将文件日志缓冲区刷新到磁盘。通常在 Activity.onDestroy() 中调用。 */
    fun flush() {
        synchronized(this) {
            Timber.forest()
                .filterIsInstance<AwFileTree>()
                .forEach { it.flush() }
        }
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

    /** 供 [AwFileTree] 与动态 [setMinPriority] 对齐：直接走 Timber 时文件树也尊重全局门槛。 */
    @PublishedApi
    internal fun globalMinPriority(): Int = minPriority

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

    /**
     * 判断指定级别的日志是否会被写入文件（[AwFileTree]）。
     *
     * 需同时满足：已启用文件日志、[isLoggable] 为 true，且级别不低于 [AwLogConfig.fileMinPriority]。
     */
    fun isFileLoggable(priority: Int): Boolean {
        return fileLogActive && isLoggable(priority) && priority >= fileMinPriorityStored
    }

    /** 获取文件日志目录路径，未启用文件日志时返回空字符串。 */
    fun getFileDir(): String = fileDir

    /**
     * 内部方法：判断指定级别的日志是否会被输出。
     *
     * 与 [isLoggable] 相同，但供 inline 函数内部调用以避免反射开销。
     * 线程安全（读取 @Volatile 变量）。
     *
     * @param priority 日志级别
     * @return true 表示该级别的日志会被输出
     */
    @PublishedApi
    internal fun shouldLog(priority: Int): Boolean {
        return Timber.treeCount > 0 && minPriority <= priority
    }

    /**
     * 内部方法：执行拦截器链处理日志。
     *
     * 按添加顺序依次执行拦截器，任一拦截器抛出异常时会被自动捕获，
     * 并返回 Accepted 结果以不中断日志输出。线程安全。
     *
     * @param priority 日志级别
     * @param tag 日志标签
     * @param message 日志消息
     * @param t 关联的异常
     * @return 拦截结果
     */
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
                Log.w("AwLogger", "Interceptor failed at index $index", e)
                if (rejectLogOnInterceptorFailure) {
                    AwLogInterceptor.LogResult.Rejected("interceptor failure: ${e.javaClass.simpleName}")
                } else {
                    AwLogInterceptor.LogResult.Accepted(message, tag)
                }
            }
        }
    }

    /**
     * 内部方法：实际执行日志输出的核心方法。
     *
     * 先经过拦截器链处理，然后通知监听器，最后分发到 Timber 各 Tree。
     * 线程安全，由调用方确保在正确的线程上下文中调用。
     *
     * @param priority 日志级别
     * @param tag 日志标签
     * @param message 日志消息
     * @param t 关联的异常
     */
    @PublishedApi
    internal fun logInternal(priority: Int, tag: String? = null, message: String, t: Throwable? = null) {
        val result = intercept(priority, tag, message, t)
        if (result is AwLogInterceptor.LogResult.Accepted) {
            val effectiveTag = result.tag ?: tag
            val finalMessage = result.message
            notifyListeners(priority, effectiveTag, finalMessage, t)
            dispatchToTimber(priority, effectiveTag, finalMessage, t)
        }
    }

    /**
     * 使用 Timber 5 推荐的链式 tag，避免依赖「下一次 Forest 调用」的一次性 tag 语义。
     */
    private fun dispatchToTimber(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (tag != null) {
            when (priority) {
                Log.VERBOSE -> if (t != null) Timber.tag(tag).v(t, message) else Timber.tag(tag).v(message)
                Log.DEBUG -> if (t != null) Timber.tag(tag).d(t, message) else Timber.tag(tag).d(message)
                Log.INFO -> if (t != null) Timber.tag(tag).i(t, message) else Timber.tag(tag).i(message)
                Log.WARN -> if (t != null) Timber.tag(tag).w(t, message) else Timber.tag(tag).w(message)
                Log.ERROR -> if (t != null) Timber.tag(tag).e(t, message) else Timber.tag(tag).e(message)
                Log.ASSERT -> if (t != null) Timber.tag(tag).wtf(t, message) else Timber.tag(tag).wtf(message)
            }
        } else {
            when (priority) {
                Log.VERBOSE -> if (t != null) Timber.v(t, message) else Timber.v(message)
                Log.DEBUG -> if (t != null) Timber.d(t, message) else Timber.d(message)
                Log.INFO -> if (t != null) Timber.i(t, message) else Timber.i(message)
                Log.WARN -> if (t != null) Timber.w(t, message) else Timber.w(message)
                Log.ERROR -> if (t != null) Timber.e(t, message) else Timber.e(message)
                Log.ASSERT -> if (t != null) Timber.wtf(t, message) else Timber.wtf(message)
            }
        }
    }

    private fun notifyListeners(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (listeners.isEmpty()) return
        for (listener in listeners) {
            try {
                listener.onLog(priority, tag, message, t)
            } catch (e: Exception) {
                Log.w("AwLogger", "AwLogListener.onLog failed", e)
            }
        }
    }

    /**
     * 内部方法：格式化日志消息，支持 String.format 风格。
     *
     * 使用 [java.util.Locale.US] 确保格式化行为在不同语言环境下一致。
     * 格式化失败时返回原始消息并附加错误信息。线程安全。
     *
     * @param message 原始消息模板
     * @param args 格式化参数
     * @return 格式化后的消息
     */
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
        if (!shouldLog(priority)) return
        if (json.isNullOrBlank()) {
            logInternal(priority, tag, "Empty/Null JSON")
            return
        }
        try {
            val trimmed = json.trim()
            if (trimmed.length > MAX_JSON_OR_XML_CHARS) {
                logInternal(
                    priority,
                    tag,
                    "JSON too large (${trimmed.length} chars), logging truncated preview only"
                )
            }
            val toParse = if (trimmed.length > MAX_JSON_OR_XML_CHARS) {
                trimmed.take(MAX_JSON_OR_XML_CHARS)
            } else {
                trimmed
            }
            val formatted = when {
                trimmed.startsWith("{") -> {
                    org.json.JSONObject(toParse).toString(2)
                }
                trimmed.startsWith("[") -> {
                    org.json.JSONArray(toParse).toString(2)
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
     * 格式化输出 JSON 日志，支持 String.format 风格占位符。
     *
     * @param json JSON 字符串模板，为 null 或空白时输出提示信息
     * @param tag 日志标签
     * @param priority 日志级别，默认 [Log.DEBUG]
     * @param args 格式化参数
     */
    fun json(json: String?, tag: String, priority: Int = Log.DEBUG, vararg args: Any?) {
        val formattedJson = if (args.isEmpty()) json
        else json?.let { formatMessage(it, args) }
        json(
            json = formattedJson,
            tag = tag,
            priority = priority
        )
    }

    /**
     * 格式化输出 XML 日志，自动缩进美化。
     *
     * @param xml XML 字符串，为 null 或空白时输出提示信息
     * @param tag 日志标签，为 null 时不设置
     * @param priority 日志级别，默认 [Log.DEBUG]
     */
    fun xml(xml: String?, tag: String? = null, priority: Int = Log.DEBUG) {
        if (!shouldLog(priority)) return
        if (xml.isNullOrBlank()) {
            logInternal(priority, tag, "Empty/Null XML")
            return
        }
        try {
            val trimmed = xml.trim()
            if (trimmed.length > MAX_JSON_OR_XML_CHARS) {
                logInternal(
                    priority,
                    tag,
                    "XML too large (${trimmed.length} chars), logging truncated preview only"
                )
            }
            val toParse = if (trimmed.length > MAX_JSON_OR_XML_CHARS) {
                trimmed.take(MAX_JSON_OR_XML_CHARS)
            } else {
                trimmed
            }
            val formatted = formatXml(toParse)
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

    /**
     * 格式化输出 XML 日志，支持 String.format 风格占位符。
     *
     * @param xml XML 字符串模板，为 null 或空白时输出提示信息
     * @param tag 日志标签
     * @param priority 日志级别，默认 [Log.DEBUG]
     * @param args 格式化参数
     */
    fun xml(xml: String?, tag: String, priority: Int = Log.DEBUG, vararg args: Any?) {
        val formattedXml = if (args.isEmpty()) xml
        else xml?.let { formatMessage(it, args) }
        xml(
            xml = formattedXml,
            tag = tag,
            priority = priority
        )
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
