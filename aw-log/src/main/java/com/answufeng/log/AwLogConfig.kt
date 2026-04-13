package com.answufeng.log

import android.util.Log
import timber.log.Timber

@AwLogDsl
class AwLogConfig {

    var debug: Boolean = true

    var fileLog: Boolean = false

    var fileDir: String = ""

    var maxFileSize: Long = 5L * 1024 * 1024
        set(value) {
            require(value > 0) { "maxFileSize must be > 0, got $value" }
            field = value
        }

    var maxFileCount: Int = 10
        set(value) {
            require(value > 0) { "maxFileCount must be > 0, got $value" }
            field = value
        }

    var fileMinPriority: Int = Log.DEBUG
        set(value) {
            require(value >= Log.VERBOSE && value <= Log.ASSERT) {
                "fileMinPriority must be between ${Log.VERBOSE} and ${Log.ASSERT}, got $value"
            }
            field = value
        }

    var crashLog: Boolean = false

    var crashHandler: ((String?, Throwable?, String?) -> Unit)? = null

    var minPriority: Int = Log.VERBOSE
        set(value) {
            require(value >= Log.VERBOSE && value <= Log.ASSERT) {
                "minPriority must be between ${Log.VERBOSE} and ${Log.ASSERT}, got $value"
            }
            field = value
        }

    var fileFormatter: AwLogFormatter = AwLogFormatter.default()

    internal val extraTrees = mutableListOf<Timber.Tree>()
    internal val interceptors = mutableListOf<AwLogInterceptor>()

    fun addTree(tree: Timber.Tree) {
        extraTrees.add(tree)
    }

    fun addInterceptor(interceptor: AwLogInterceptor) {
        interceptors.add(interceptor)
    }
}
