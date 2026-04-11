package com.answufeng.log

import timber.log.Timber

/**
 * brick-log 模块初始化入口，基于 Timber 提供结构化日志能力。
 *
 * 支持功能：
 * - Debug / Release 自动切换日志树
 * - 文件日志（可选，按日期轮转）
 * - 崩溃日志收集（CrashTree）
 * - JSON 格式化输出
 * - 自定义 Tree 扩展
 *
 * ### 初始化
 * ```kotlin
 * BrickLogger.init {
 *     debug = BuildConfig.DEBUG
 *     fileLog = true           // 启用文件日志
 *     fileDir = cacheDir.absolutePath + "/logs"
 *     maxFileSize = 5L * 1024 * 1024  // 5MB
 *     maxFileCount = 10
 *     crashLog = true          // 启用崩溃日志
 * }
 * ```
 *
 * ### 使用
 * ```kotlin
 * BrickLogger.d("请求成功")
 * BrickLogger.e(exception, "请求失败")
 * BrickLogger.json(jsonString)
 * BrickLogger.d("Network") { "响应: ${response.body}" }  // Lambda 延迟拼接
 * ```
 */
object BrickLogger {

    @Volatile
    private var initialized = false

    /**
     * 初始化日志系统。
     *
     * @param block 配置 DSL
     */
    fun init(block: LogConfig.() -> Unit = {}) {
        val config = LogConfig().apply(block)

        // 清除之前安装的所有 Tree
        Timber.uprootAll()

        if (config.debug) {
            Timber.plant(BrickDebugTree())
        }

        if (config.fileLog && config.fileDir.isNotBlank()) {
            Timber.plant(
                FileTree(
                    logDir = config.fileDir,
                    maxFileSize = config.maxFileSize,
                    maxFileCount = config.maxFileCount
                )
            )
        }

        if (config.crashLog) {
            Timber.plant(CrashTree(config.crashHandler))
        }

        // 安装用户自定义 Tree
        config.extraTrees.forEach { Timber.plant(it) }

        initialized = true
    }

    // ==================== 快捷日志方法 ====================

    /** Verbose 级别日志 */
    fun v(message: String, vararg args: Any?) = Timber.v(message, *args)

    /** Verbose 级别日志（Lambda 延迟拼接） */
    inline fun v(crossinline message: () -> String) {
        if (Timber.treeCount > 0) Timber.v(message())
    }

    /** Debug 级别日志 */
    fun d(message: String, vararg args: Any?) = Timber.d(message, *args)

    /** Debug 级别日志（Lambda 延迟拼接） */
    inline fun d(crossinline message: () -> String) {
        if (Timber.treeCount > 0) Timber.d(message())
    }

    /** Info 级别日志 */
    fun i(message: String, vararg args: Any?) = Timber.i(message, *args)

    /** Info 级别日志（Lambda 延迟拼接） */
    inline fun i(crossinline message: () -> String) {
        if (Timber.treeCount > 0) Timber.i(message())
    }

    /** Warning 级别日志 */
    fun w(message: String, vararg args: Any?) = Timber.w(message, *args)

    /** Warning 级别日志（Lambda 延迟拼接） */
    inline fun w(crossinline message: () -> String) {
        if (Timber.treeCount > 0) Timber.w(message())
    }

    /** Warning 级别日志（带异常） */
    fun w(t: Throwable?, message: String, vararg args: Any?) = Timber.w(t, message, *args)

    /** Error 级别日志 */
    fun e(message: String, vararg args: Any?) = Timber.e(message, *args)

    /** Error 级别日志（Lambda 延迟拼接） */
    inline fun e(crossinline message: () -> String) {
        if (Timber.treeCount > 0) Timber.e(message())
    }

    /** Error 级别日志（带异常） */
    fun e(t: Throwable?, message: String, vararg args: Any?) = Timber.e(t, message, *args)

    /** Error 级别日志（仅异常） */
    fun e(t: Throwable?) = Timber.e(t)

    /** WTF 级别日志 */
    fun wtf(message: String, vararg args: Any?) = Timber.wtf(message, *args)

    /** WTF 级别日志（带异常） */
    fun wtf(t: Throwable?, message: String, vararg args: Any?) = Timber.wtf(t, message, *args)

    /**
     * 格式化输出 JSON 字符串。
     *
     * @param json JSON 字符串
     * @param tag 可选标签
     */
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
        } catch (e: Exception) {
            e(e, "JSON parse error: $json")
        }
    }

    /**
     * 带 Tag 的日志方法
     *
     * @param tag 自定义标签
     */
    fun tag(tag: String): Timber.Tree = Timber.tag(tag)
}
