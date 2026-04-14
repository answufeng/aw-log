package com.answufeng.log

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志格式化器接口，控制日志行的输出格式。
 *
 * 默认格式为：`2026-01-01 12:00:00.000 D/Tag: message`
 *
 * 可通过 DSL 自定义：
 * ```kotlin
 * AwLogFormatter.create {
 *     timePattern = "HH:mm:ss.SSS"
 *     separator = " | "
 *     showLevel = true
 *     showTag = true
 *     showTime = true
 * }
 * ```
 */
interface AwLogFormatter {

    /**
     * 格式化日志行。
     *
     * @param priority 日志优先级
     * @param tag 日志标签
     * @param message 日志消息
     * @param throwable 关联的异常
     * @return 格式化后的字符串
     */
    fun format(priority: Int, tag: String?, message: String, throwable: Throwable?): String

    companion object {
        /** 获取默认格式化器。 */
        @JvmStatic
        fun default(): AwLogFormatter = DefaultLogFormatter

        /**
         * 使用 DSL 创建自定义格式化器。
         *
         * ```kotlin
         * AwLogFormatter.create {
         *     timePattern = "HH:mm:ss"
         *     showLevel = false
         * }
         * ```
         */
        @JvmStatic
        fun create(block: AwFormatterDsl.() -> Unit): AwLogFormatter {
            return AwFormatterDsl().apply(block).build()
        }
    }
}

/**
 * DSL 作用域标记注解，防止 DSL 嵌套时方法泄漏。
 */
@DslMarker
annotation class AwLogDsl

/**
 * 格式化器 DSL 构建器。
 */
@AwLogDsl
class AwFormatterDsl {
    /** 时间格式模式，默认 `yyyy-MM-dd HH:mm:ss.SSS`。 */
    var timePattern: String = "yyyy-MM-dd HH:mm:ss.SSS"

    /** 字段分隔符，默认空格。 */
    var separator: String = " "

    /** 是否显示日志级别，默认 true。 */
    var showLevel: Boolean = true

    /** 是否显示标签，默认 true。 */
    var showTag: Boolean = true

    /** 是否显示时间，默认 true。 */
    var showTime: Boolean = true

    internal fun build(): AwLogFormatter = CustomLogFormatter(
        timePattern = timePattern,
        separator = separator,
        showLevel = showLevel,
        showTag = showTag,
        showTime = showTime
    )
}

private class CustomLogFormatter(
    timePattern: String,
    private val separator: String,
    private val showLevel: Boolean,
    private val showTag: Boolean,
    private val showTime: Boolean
) : AwLogFormatter {

    private val dateFormat = SimpleDateFormat(timePattern, Locale.US)

    override fun format(priority: Int, tag: String?, message: String, throwable: Throwable?): String {
        val sb = StringBuilder(256)
        if (showTime) {
            sb.append(dateFormat.format(Date()))
            sb.append(separator)
        }
        if (showLevel) {
            sb.append(priorityToString(priority))
            sb.append("/")
        }
        if (showTag) {
            sb.append(tag ?: "NoTag")
            sb.append(": ")
        }
        sb.append(message)
        return sb.toString()
    }
}

private object DefaultLogFormatter : AwLogFormatter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    override fun format(priority: Int, tag: String?, message: String, throwable: Throwable?): String {
        val time = dateFormat.format(Date())
        val level = priorityToString(priority)
        val logTag = tag ?: "NoTag"
        return "$time $level/$logTag: $message"
    }
}

/** 将 [android.util.Log] 常量转换为可读字符串。 */
internal fun priorityToString(priority: Int): String = when (priority) {
    Log.VERBOSE -> "V"
    Log.DEBUG -> "D"
    Log.INFO -> "I"
    Log.WARN -> "W"
    Log.ERROR -> "E"
    Log.ASSERT -> "A"
    else -> "?"
}
