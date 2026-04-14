package com.answufeng.log

import timber.log.Timber

/**
 * Logcat 调试输出 Tree，自动在 Tag 中包含调用者位置信息。
 *
 * Tag 格式为 `ClassName#methodName(fileName:lineNumber)`，
 * 受 Android Logcat 23 字符限制，过长时会被截断。
 *
 * 相比 Timber 默认的 DebugTree，本类仅进行一次栈遍历（通过 [createStackElementTag]），
 * 避免了二次创建 Throwable 遍历栈帧的性能开销。
 */
internal class AwDebugTree : Timber.DebugTree() {

    override fun createStackElementTag(element: StackTraceElement): String? {
        val className = element.className.substringAfterLast('.')
        val methodInfo = "${element.methodName}(${element.fileName}:${element.lineNumber})"
        val combinedTag = "$className#$methodInfo"
        return if (combinedTag.length > MAX_TAG_LENGTH) {
            combinedTag.substring(0, MAX_TAG_LENGTH)
        } else {
            combinedTag
        }
    }

    companion object {
        private const val MAX_TAG_LENGTH = 23
    }
}
