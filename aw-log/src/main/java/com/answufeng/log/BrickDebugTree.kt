package com.answufeng.log

import timber.log.Timber

/**
 * Debug 日志树，输出到 Logcat。
 *
 * 特性：
 * - 自动获取调用者的类名作为 Tag（无需手动传入）
 * - 日志格式：`[方法名(文件名:行号)] 消息内容`
 * - 支持的 Tag 最大长度 23 字符（Android 限制）
 */
internal class BrickDebugTree : Timber.DebugTree() {

    /**
     * 自动生成 Tag，格式为调用者类名（去掉内部类后缀）。
     * 超过 23 字符自动截断。
     */
    override fun createStackElementTag(element: StackTraceElement): String {
        val className = super.createStackElementTag(element) ?: "Unknown"
        return if (className.length > MAX_TAG_LENGTH) {
            className.substring(0, MAX_TAG_LENGTH)
        } else {
            className
        }
    }

    /**
     * 格式化日志消息，添加方法名、文件名和行号。
     */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val stackTrace = Throwable().stackTrace
        val element = stackTrace.findCallElement()
        val formattedMessage = if (element != null) {
            "[${element.methodName}(${element.fileName}:${element.lineNumber})] $message"
        } else {
            message
        }
        super.log(priority, tag, formattedMessage, t)
    }

    private fun Array<StackTraceElement>.findCallElement(): StackTraceElement? {
        var foundTimber = false
        for (element in this) {
            val className = element.className
            if (className.startsWith("timber.log.Timber") ||
                className.startsWith("com.answufeng.log")
            ) {
                foundTimber = true
                continue
            }
            if (foundTimber) {
                return element
            }
        }
        return null
    }

    companion object {
        private const val MAX_TAG_LENGTH = 23
    }
}
