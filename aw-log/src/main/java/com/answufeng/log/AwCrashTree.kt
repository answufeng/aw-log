package com.answufeng.log

import android.util.Log
import timber.log.Timber

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
