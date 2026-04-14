package com.answufeng.log

import android.util.Log
import timber.log.Timber

/**
 * 崩溃日志收集 Tree，仅捕获 ERROR 及以上级别的日志。
 *
 * 当提供 [crashHandler] 回调时，会将错误信息传递给回调（可对接 Firebase Crashlytics、Bugly 等）。
 * 未提供回调时，默认使用 [Log.e] 输出到 Logcat。
 */
internal class AwCrashTree(
    private val crashHandler: ((String?, Throwable?, String?) -> Unit)? = null
) : Timber.Tree() {

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
