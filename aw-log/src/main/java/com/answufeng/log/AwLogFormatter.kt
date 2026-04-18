package com.answufeng.log

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

internal fun priorityToString(priority: Int): String = when (priority) {
    Log.VERBOSE -> "V"
    Log.DEBUG -> "D"
    Log.INFO -> "I"
    Log.WARN -> "W"
    Log.ERROR -> "E"
    Log.ASSERT -> "A"
    else -> "?"
}
