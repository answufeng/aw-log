package com.answufeng.log

import android.util.Log
import timber.log.Timber

internal class AwCrashTree(
    private val crashHandler: ((String?, Throwable?, String?) -> Unit)? = null
) : Timber.Tree() {

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    private var hasInstalledHandler = false

    fun installCrashHandler() {
        if (hasInstalledHandler) return
        hasInstalledHandler = true
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
        crashHandler?.invoke(tag, t, message)
            ?: run {
                if (t != null) {
                    Log.e(tag ?: "AwCrash", message, t)
                } else {
                    Log.e(tag ?: "AwCrash", message)
                }
            }
    }
}
