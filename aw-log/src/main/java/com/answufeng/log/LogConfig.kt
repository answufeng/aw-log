package com.answufeng.log

import timber.log.Timber

/**
 * 日志配置类，通过 DSL 方式配置。
 *
 * ```kotlin
 * BrickLogger.init {
 *     debug = true
 *     fileLog = true
 *     fileDir = "/path/to/logs"
 *     maxFileSize = 5L * 1024 * 1024
 *     maxFileCount = 10
 *     crashLog = true
 *     crashHandler = { tag, throwable, message ->
 *         // 上报到 Firebase / Bugly 等
 *     }
 *     addTree(MyCustomTree())
 * }
 * ```
 */
class LogConfig {

    /** 是否启用 Debug 日志树（Logcat 输出），建议设为 `BuildConfig.DEBUG` */
    var debug: Boolean = true

    /** 是否启用文件日志 */
    var fileLog: Boolean = false

    /** 文件日志存储目录（绝对路径） */
    var fileDir: String = ""

    /** 单个日志文件最大大小（字节），默认 5MB，必须 > 0 */
    var maxFileSize: Long = 5L * 1024 * 1024
        set(value) {
            require(value > 0) { "maxFileSize must be > 0, got $value" }
            field = value
        }

    /** 最大日志文件数量（超出后自动删除最久的），默认 10，必须 > 0 */
    var maxFileCount: Int = 10
        set(value) {
            require(value > 0) { "maxFileCount must be > 0, got $value" }
            field = value
        }

    /** 是否启用崩溃日志收集 */
    var crashLog: Boolean = false

    /**
     * 崩溃处理回调，可用于上报到第三方平台。
     *
     * 参数：(tag: String?, throwable: Throwable?, message: String?)
     */
    var crashHandler: ((String?, Throwable?, String?) -> Unit)? = null

    internal val extraTrees = mutableListOf<Timber.Tree>()

    /**
     * 添加自定义 Timber Tree。
     *
     * @param tree 自定义的 Timber.Tree 实现
     */
    fun addTree(tree: Timber.Tree) {
        extraTrees.add(tree)
    }
}
