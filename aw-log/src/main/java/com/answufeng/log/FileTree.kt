package com.answufeng.log

import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 文件日志树，将日志写入本地文件。
 *
 * 特性：
 * - 按日期自动分文件（每天一个日志文件）
 * - 单文件大小限制，超出后自动轮转
 * - 最大文件数量限制，超出后自动删除最旧文件
 * - 异步写入，不阻塞主线程
 * - 仅记录 INFO 级别及以上的日志
 *
 * ### 线程安全
 * 所有文件 I/O 通过单线程 [Executor] 串行化，可从任意线程调用 [log]。
 *
 * ### 数据丢失边界
 * 写入线程标记为 `daemon = true`，进程被强制终止时队列中尚未写入的日志会丢失。
 * 这是为了避免日志线程阻止 JVM 正常退出。对关键场景建议在退出前调用 flush。
 *
 * @param logDir 日志文件存储目录
 * @param maxFileSize 单个文件最大大小（字节），默认 5MB
 * @param maxFileCount 最大文件数量，默认 10
 */
internal class FileTree(
    private val logDir: String,
    private val maxFileSize: Long = 5L * 1024 * 1024,
    private val maxFileCount: Int = 10
) : Timber.Tree() {

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "BrickLog-FileWriter").apply { isDaemon = true }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /**
     * 仅记录 INFO 级别及以上。
     */
    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.INFO
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        executor.execute {
            try {
                writeLog(priority, tag, message, t)
            } catch (_: Exception) {
                // 文件写入失败时静默处理，避免循环日志
            }
        }
    }

    private fun writeLog(priority: Int, tag: String?, message: String, t: Throwable?) {
        val dir = File(logDir)
        if (!dir.exists()) dir.mkdirs()

        // 清理超出数量的旧文件
        cleanOldFiles(dir)

        val file = getLogFile(dir)

        // 文件大小检查，超出则轮转
        if (file.exists() && file.length() >= maxFileSize) {
            rotateFile(file)
        }

        FileWriter(file, true).use { writer ->
            val time = timeFormat.format(Date())
            val level = priorityToString(priority)
            val logTag = tag ?: "NoTag"

            writer.appendLine("$time $level/$logTag: $message")

            if (t != null) {
                val pw = PrintWriter(writer)
                t.printStackTrace(pw)
                pw.flush()
            }
        }
    }

    private fun getLogFile(dir: File): File {
        val date = dateFormat.format(Date())
        return File(dir, "log_$date.txt")
    }

    private fun rotateFile(file: File) {
        val rotatedName = "${file.nameWithoutExtension}_${System.currentTimeMillis()}.${file.extension}"
        val rotatedFile = File(file.parent, rotatedName)
        if (!file.renameTo(rotatedFile)) {
            Log.w("BrickLog", "Failed to rotate log file: ${file.name}")
        }
    }

    private fun cleanOldFiles(dir: File) {
        val logFiles = dir.listFiles { f -> f.isFile && f.name.startsWith("log_") }
            ?.sortedBy { it.lastModified() }
            ?: return

        if (logFiles.size > maxFileCount) {
            val toDelete = logFiles.size - maxFileCount
            logFiles.take(toDelete).forEach { f ->
                if (!f.delete()) {
                    Log.w("aw-log", "Failed to delete old log file: ${f.name}")
                }
            }
        }
    }

    private fun priorityToString(priority: Int): String = when (priority) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> "?"
    }
}
