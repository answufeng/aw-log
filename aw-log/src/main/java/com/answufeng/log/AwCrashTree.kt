package com.answufeng.log

import android.util.Log
import timber.log.Timber

/**
 * 崩溃日志树
 *
 * 用于捕获未处理的异常并记录崩溃日志
 *
 * @param crashHandler 崩溃处理回调，可用于自定义崩溃处理逻辑
 *
 * @see Timber.Tree
 */
internal class AwCrashTree(
    private val crashHandler: ((String?, Throwable?, String?) -> Unit)? = null
) : Timber.Tree() {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    private var hasInstalledHandler = false

    /**
     * 安装全局崩溃处理器。
     *
     * 会保存原有的 [Thread.UncaughtExceptionHandler]，并在捕获崩溃后调用原有处理器。
     * 幂等操作，重复调用只会安装一次。
     *
     * 崩溃发生时：
     * 1. 调用 [crashHandler] 回调（若提供）或输出到 Logcat
     * 2. 将崩溃传递回原有 Handler
     */
    fun installCrashHandler() {
        if (hasInstalledHandler) return
        hasInstalledHandler = true
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val tag = "AwCrash"
                val message = "Uncaught exception in thread: ${thread.name}"
                crashHandler?.invoke(tag, throwable, message)
                    ?: Log.e(tag, message, throwable)
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.ERROR
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t != null) {
            Log.e(tag ?: "AwCrash", message, t)
        } else {
            Log.e(tag ?: "AwCrash", message)
        }
    }
}
