package com.answufeng.log

import android.util.Log
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

object AwLogger {

    @Volatile
    private var initialized = false

    @Volatile
    private var minPriority = Log.VERBOSE

    private val interceptors = CopyOnWriteArrayList<AwLogInterceptor>()

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

    fun isInitialized(): Boolean = initialized

    fun reset() {
        Timber.uprootAll()
        interceptors.clear()
        initialized = false
        minPriority = Log.VERBOSE
    }

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

    fun v(message: String, vararg args: Any?) {
        if (shouldLog(Log.VERBOSE) && intercept(Log.VERBOSE, null, message)) {
            Timber.v(message, *args)
        }
    }

    inline fun v(crossinline message: () -> String) {
        if (shouldLog(Log.VERBOSE)) {
            val msg = message()
            if (intercept(Log.VERBOSE, null, msg)) Timber.v(msg)
        }
    }

    fun d(message: String, vararg args: Any?) {
        if (shouldLog(Log.DEBUG) && intercept(Log.DEBUG, null, message)) {
            Timber.d(message, *args)
        }
    }

    inline fun d(crossinline message: () -> String) {
        if (shouldLog(Log.DEBUG)) {
            val msg = message()
            if (intercept(Log.DEBUG, null, msg)) Timber.d(msg)
        }
    }

    fun i(message: String, vararg args: Any?) {
        if (shouldLog(Log.INFO) && intercept(Log.INFO, null, message)) {
            Timber.i(message, *args)
        }
    }

    inline fun i(crossinline message: () -> String) {
        if (shouldLog(Log.INFO)) {
            val msg = message()
            if (intercept(Log.INFO, null, msg)) Timber.i(msg)
        }
    }

    fun w(message: String, vararg args: Any?) {
        if (shouldLog(Log.WARN) && intercept(Log.WARN, null, message)) {
            Timber.w(message, *args)
        }
    }

    inline fun w(crossinline message: () -> String) {
        if (shouldLog(Log.WARN)) {
            val msg = message()
            if (intercept(Log.WARN, null, msg)) Timber.w(msg)
        }
    }

    fun w(t: Throwable?, message: String, vararg args: Any?) {
        if (shouldLog(Log.WARN)) Timber.w(t, message, *args)
    }

    fun e(message: String, vararg args: Any?) {
        if (shouldLog(Log.ERROR) && intercept(Log.ERROR, null, message)) {
            Timber.e(message, *args)
        }
    }

    inline fun e(crossinline message: () -> String) {
        if (shouldLog(Log.ERROR)) {
            val msg = message()
            if (intercept(Log.ERROR, null, msg)) Timber.e(msg)
        }
    }

    fun e(t: Throwable?, message: String, vararg args: Any?) {
        if (shouldLog(Log.ERROR)) Timber.e(t, message, *args)
    }

    fun e(t: Throwable?) {
        if (shouldLog(Log.ERROR)) Timber.e(t)
    }

    fun wtf(message: String, vararg args: Any?) {
        if (shouldLog(Log.ASSERT) && intercept(Log.ASSERT, null, message)) {
            Timber.wtf(message, *args)
        }
    }

    inline fun wtf(crossinline message: () -> String) {
        if (shouldLog(Log.ASSERT)) {
            val msg = message()
            if (intercept(Log.ASSERT, null, msg)) Timber.wtf(msg)
        }
    }

    fun wtf(t: Throwable?, message: String, vararg args: Any?) {
        if (shouldLog(Log.ASSERT)) Timber.wtf(t, message, *args)
    }

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

    fun tag(tag: String): Timber.Tree = Timber.tag(tag)
}
