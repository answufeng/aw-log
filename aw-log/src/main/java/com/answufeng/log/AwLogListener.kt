package com.answufeng.log

/**
 * 日志监听器，实时获取日志输出。
 *
 * 可用于 UI 展示、远程上报等场景。通过 [AwLogConfig.addListener] 注册。
 *
 * 注意：[onLog] 可能在任意线程调用，如需更新 UI 请自行切换到主线程。
 *
 * ```kotlin
 * AwLogger.init {
 *     addListener { priority, tag, message, throwable ->
 *         // 处理日志
 *     }
 * }
 * ```
 */
fun interface AwLogListener {

    /**
     * 日志输出回调。
     *
     * @param priority 日志级别，对应 [android.util.Log] 的常量
     * @param tag 日志标签，可能为 null
     * @param message 日志消息内容
     * @param throwable 关联的异常，可能为 null
     */
    fun onLog(priority: Int, tag: String?, message: String, throwable: Throwable?)
}
