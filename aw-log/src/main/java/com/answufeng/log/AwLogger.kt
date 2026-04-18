package com.answufeng.log

import android.util.Log
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

object AwLogger {

    @Volatile
    private var initialized = false

    @Volatile
    private var minPriority = Log.VERBOSE

    @Volatile
    private var fileDir: String = ""

    private val interceptors = CopyOnWriteArrayList<AwLogInterceptor>()

    private val listeners = CopyOnWriteArrayList<AwLogListener>()

    fun init(block: AwLogConfig.() -> Unit = {}) {
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
            Timber.plant(AwCrashTree(config.crashHandler))
        }

        config.extraTrees.forEach { Timber.plant(it) }

        initialized = true
    }

    fun isInitialized(): Boolean = initialized

    fun reset() {
        Timber.forest()
            .filterIsInstance<AwFileTree>()
            .forEach { it.shutdown() }
        Timber.uprootAll()
        interceptors.clear()
        listeners.clear()
        initialized = false
        minPriority = Log.VERBOSE
        fileDir = ""
    }

    fun flush() {
        Timber.forest()
            .filterIsInstance<AwFileTree>()
            .forEach { it.flush() }
    }

    fun setMinPriority(priority: Int) {
        require(priority >= Log.VERBOSE && priority <= Log.ASSERT) {
            "minPriority must be between ${Log.VERBOSE} and ${Log.ASSERT}, got $priority"
        }
        minPriority = priority
    }

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
        return if (args.isEmpty()) message else String.format(message, *args)
    }

    fun v(message: String, vararg args: Any?) {
        if (shouldLog(Log.VERBOSE)) logInternal(Log.VERBOSE, message = formatMessage(message, args))
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

    inline fun d(crossinline message: () -> String) {
        if (shouldLog(Log.DEBUG)) logInternal(Log.DEBUG, message = message())
    }

    inline fun d(tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.DEBUG)) logInternal(Log.DEBUG, tag, message())
    }

    fun i(message: String, vararg args: Any?) {
        if (shouldLog(Log.INFO)) logInternal(Log.INFO, message = formatMessage(message, args))
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

    inline fun wtf(crossinline message: () -> String) {
        if (shouldLog(Log.ASSERT)) logInternal(Log.ASSERT, message = message())
    }

    inline fun wtf(tag: String, crossinline message: () -> String) {
        if (shouldLog(Log.ASSERT)) logInternal(Log.ASSERT, tag, message())
    }

    fun wtf(t: Throwable?, message: String, vararg args: Any?) {
        if (shouldLog(Log.ASSERT)) logInternal(Log.ASSERT, message = formatMessage(message, args), t = t)
    }

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
}
