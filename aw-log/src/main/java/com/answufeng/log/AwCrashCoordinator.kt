package com.answufeng.log

import android.util.Log

/**
 * 进程级未捕获异常分发：只安装一次 [Thread.setDefaultUncaughtExceptionHandler]，
 * 通过 [syncFromConfig] 开关与更新回调，避免重复 init 时 handler 嵌套。
 *
 * 注意：与第三方崩溃 SDK（如 Crashlytics、Bugly 等）共存时，aw-log 会转发
 * 至先前安装的 handler，但反复开关可能导致第三方 handler 丢失。建议
 * 项目中只保留一套 UncaughtExceptionHandler 主导逻辑，或将第三方 SDK
 * 的初始化放在 aw-log 之后。
 */
internal object AwCrashCoordinator {

    private val installLock = Any()

    @Volatile
    private var installed: Boolean = false

    private var delegate: Thread.UncaughtExceptionHandler? = null

    @Volatile
    private var userHandler: ((String?, Throwable?, String?) -> Unit)? = null

    fun syncFromConfig(crashLog: Boolean, crashHandler: ((String?, Throwable?, String?) -> Unit)?) {
        synchronized(installLock) {
            if (crashLog) {
                userHandler = crashHandler
                if (!installed) {
                    delegate = Thread.getDefaultUncaughtExceptionHandler()
                    Thread.setDefaultUncaughtExceptionHandler(::dispatchUncaught)
                    installed = true
                }
            } else {
                userHandler = null
                if (installed) {
                    Thread.setDefaultUncaughtExceptionHandler(delegate)
                    delegate = null
                    installed = false
                }
            }
        }
    }

    private fun dispatchUncaught(thread: Thread, throwable: Throwable) {
        try {
            val tag = "AwCrash"
            val message = "Uncaught exception in thread: ${thread.name}"
            userHandler?.invoke(tag, throwable, message)
                ?: Log.e(tag, message, throwable)
        } catch (_: Exception) {
        }
        try {
            delegate?.uncaughtException(thread, throwable)
        } catch (_: Exception) {
        }
    }
}
