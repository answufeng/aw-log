package com.answufeng.log

import android.util.Log
import timber.log.Timber

internal class AwDebugTree : Timber.DebugTree() {

    override fun createStackElementTag(element: StackTraceElement): String {
        val className = super.createStackElementTag(element) ?: "Unknown"
        return if (className.length > MAX_TAG_LENGTH) {
            className.substring(0, MAX_TAG_LENGTH)
        } else {
            className
        }
    }

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
