package com.answufeng.log

import android.util.Log
import timber.log.Timber

/**
 * 崩溃日志树
 *
 * 接收 [Log.ERROR] / [Log.ASSERT] 级别日志；可选是否再次 [Log.e] 到 Logcat（见 [echoToLogcat]）。
 * 未捕获异常由 [AwCrashCoordinator] 统一分发，不在此类安装全局 Handler。
 *
 * @param echoToLogcat 为 false 时 [log] 不输出（通常与 [AwDebugTree] 并存时使用，由 DebugTree 负责 Logcat）。
 *
 * @see Timber.Tree
 * @see AwCrashCoordinator
 */
internal class AwCrashTree(
    private val echoToLogcat: Boolean = true
) : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.ERROR
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!echoToLogcat) return
        if (t != null) {
            Log.e(tag ?: "AwCrash", message, t)
        } else {
            Log.e(tag ?: "AwCrash", message)
        }
    }
}
