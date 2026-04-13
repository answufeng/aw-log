package com.answufeng.log

import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

interface AwLogFormatter {

    fun format(priority: Int, tag: String?, message: String, throwable: Throwable?): String

    companion object {
        fun default(): AwLogFormatter = DefaultLogFormatter

        fun create(block: AwFormatterDsl.() -> Unit): AwLogFormatter {
            return AwFormatterDsl().apply(block).build()
        }
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

    internal fun build(): AwLogFormatter = CustomLogFormatter(
        timePattern = timePattern,
        separator = separator,
        showLevel = showLevel,
        showTag = showTag,
        showTime = showTime
    )
}

private class CustomLogFormatter(
    private val timePattern: String,
    private val separator: String,
    private val showLevel: Boolean,
    private val showTag: Boolean,
    private val showTime: Boolean
) : AwLogFormatter {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendPattern(timePattern)
        .toFormatter()

    override fun format(priority: Int, tag: String?, message: String, throwable: Throwable?): String {
        val sb = StringBuilder()
        if (showTime) {
            sb.append(LocalDateTime.now().format(timeFormatter))
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

    private val timeFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .toFormatter()

    override fun format(priority: Int, tag: String?, message: String, throwable: Throwable?): String {
        val time = LocalDateTime.now().format(timeFormatter)
        val level = priorityToString(priority)
        val logTag = tag ?: "NoTag"
        return "$time $level/$logTag: $message"
    }
}

internal fun priorityToString(priority: Int): String = when (priority) {
    Log.VERBOSE -> "V"
    Log.DEBUG -> "D"
    Log.INFO -> "I"
    Log.WARN -> "W"
    Log.ERROR -> "E"
    Log.ASSERT -> "A"
    else -> "?"
}
