package com.answufeng.log

import android.util.Log
import timber.log.Timber

/**
 * 崩溃日志树，捕获 ERROR 和 ASSERT 级别的日志/异常。
 *
 * 可配合第三方崩溃收集平台（如 Firebase Crashlytics、Bugly）使用：
 *
 * ```kotlin
 * BrickLogger.init {
 *     crashLog = true
 *     crashHandler = { tag, throwable, message ->
 *         // 上报到 Firebase Crashlytics
 *         FirebaseCrashlytics.getInstance().apply {
 *             setCustomKey("tag", tag ?: "unknown")
 *             message?.let { log(it) }
 *             throwable?.let { recordException(it) }
 *         }
 *     }
 * }
 * ```
 *
 * @param crashHandler 可选的崩溃处理回调
 */
internal class CrashTree(
    private val crashHandler: ((String?, Throwable?, String?) -> Unit)? = null
) : Timber.Tree() {

    /**
     * 仅捕获 ERROR 和 ASSERT 级别。
     */
    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.ERROR
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        crashHandler?.invoke(tag, t, message)
    }
}
