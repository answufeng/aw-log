package com.answufeng.log

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志格式化器接口，用于自定义文件日志的输出格式。
 *
 * 实现此接口可控制日志行的时间格式、分隔符、显示字段等。
 * 内置实现包括 [default]（详细模式）、[compact]（简洁模式）。
 *
 * 线程安全要求：实现类应确保 [format] 方法线程安全。
 * 内置实现使用 ThreadLocal 包装 SimpleDateFormat 保证线程安全。
 *
 * 使用示例：
 * ```kotlin
 * AwLogger.init {
 *     fileFormatter = AwLogFormatter.create {
 *         timePattern = "HH:mm:ss.SSS"
 *         showThread = true
 *     }
 * }
 * ```
 */
interface AwLogFormatter {

    fun format(priority: Int, tag: String?, message: String, throwable: Throwable?): String

    companion object {
        @JvmStatic
        fun default(): AwLogFormatter = DefaultLogFormatter

        @JvmStatic
        fun create(block: AwFormatterDsl.() -> Unit): AwLogFormatter {
            return AwFormatterDsl().apply(block).build()
        }

        @JvmStatic
        fun compact(): AwLogFormatter = CompactLogFormatter

        @JvmStatic
        fun verbose(): AwLogFormatter = DefaultLogFormatter
    }
}

@DslMarker
annotation class AwLogDsl

@AwLogDsl
class AwFormatterDsl {
    var timePattern: String = "yyyy-MM-dd HH:mm:ss.SSS"

    var separator: String = " "

    var showLevel: Boolean = true

    var showTag: Boolean = true

    var showTime: Boolean = true

    var showThread: Boolean = false

    internal fun build(): AwLogFormatter = CustomLogFormatter(
        timePattern = timePattern,
        separator = separator,
        showLevel = showLevel,
        showTag = showTag,
        showTime = showTime,
        showThread = showThread
    )
}

private class CustomLogFormatter(
    timePattern: String,
    private val separator: String,
    private val showLevel: Boolean,
    private val showTag: Boolean,
    private val showTime: Boolean,
    private val showThread: Boolean
) : AwLogFormatter {

    private val dateFormat = ThreadLocal.withInitial { SimpleDateFormat(timePattern, Locale.US) }

    override fun format(priority: Int, tag: String?, message: String, throwable: Throwable?): String {
        val sb = StringBuilder(256)
        if (showTime) {
            sb.append(dateFormat.get()!!.format(Date()))
            sb.append(separator)
        }
        if (showThread) {
            sb.append("[${Thread.currentThread().name}]")
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

    private val dateFormat = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US) }

    override fun format(priority: Int, tag: String?, message: String, throwable: Throwable?): String {
        val time = dateFormat.get()!!.format(Date())
        val level = priorityToString(priority)
        val logTag = tag ?: "NoTag"
        return "$time $level/$logTag: $message"
    }
}

private object CompactLogFormatter : AwLogFormatter {

    private val dateFormat = ThreadLocal.withInitial { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    override fun format(priority: Int, tag: String?, message: String, throwable: Throwable?): String {
        val time = dateFormat.get()!!.format(Date())
        val level = priorityToString(priority)
        val logTag = tag ?: "NoTag"
        return "$time $level/$logTag: $message"
    }
}

/**
 * 将日志优先级转换为单字符字符串。
 *
 * 用于文件日志格式化时显示级别标识。
 *
 * @param priority 日志级别常量
 * @return 单字符级别标识（V/D/I/W/E/A），未知级别返回 "?"
 */
internal fun priorityToString(priority: Int): String = when (priority) {
    Log.VERBOSE -> "V"
    Log.DEBUG -> "D"
    Log.INFO -> "I"
    Log.WARN -> "W"
    Log.ERROR -> "E"
    Log.ASSERT -> "A"
    else -> "?"
}
