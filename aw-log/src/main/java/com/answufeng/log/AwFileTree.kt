package com.answufeng.log

import android.util.Log
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal class AwFileTree(
    private val logDir: String,
    private val maxFileSize: Long = 5L * 1024 * 1024,
    private val maxFileCount: Int = 10,
    private val minPriority: Int = Log.DEBUG,
    private val formatter: AwLogFormatter = AwLogFormatter.default(),
    private val interceptors: List<AwLogInterceptor> = emptyList()
) : Timber.Tree() {

    private val executor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(MAX_QUEUE_SIZE),
        { runnable -> Thread(runnable, "AwLog-FileWriter").apply { isDaemon = true } },
        ThreadPoolExecutor.DiscardPolicy()
    )

    private val dateFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd")
        .toFormatter()

    private var currentWriter: BufferedWriter? = null
    private var currentDate: String? = null
    private var currentFileName: String? = null

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= minPriority
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!runInterceptors(priority, tag, message, t)) return

        executor.execute {
            try {
                writeLog(priority, tag, message, t)
            } catch (_: Exception) {
            }
        }
    }

    fun flush() {
        val latch = CountDownLatch(1)
        executor.execute {
            try {
                closeCurrentWriter()
            } finally {
                latch.countDown()
            }
        }
        try {
            latch.await(5, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun runInterceptors(priority: Int, tag: String?, message: String, t: Throwable?): Boolean {
        if (interceptors.isEmpty()) return true
        return interceptors.all { interceptor ->
            interceptor.intercept(priority, tag, message, t) == AwLogInterceptor.LogResult.ACCEPTED
        }
    }

    @Synchronized
    private fun writeLog(priority: Int, tag: String?, message: String, t: Throwable?) {
        val dir = File(logDir)
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "Failed to create log directory: $logDir")
            return
        }

        val today = LocalDate.now().format(dateFormatter)
        val fileName = "log_$today.txt"

        rotateIfNeeded(dir, fileName)

        val file = File(dir, fileName)
        val writer = getWriter(dir, fileName, today)

        val formattedLine = formatter.format(priority, tag, message, t)
        writer.write(formattedLine)
        writer.write("\n")

        if (t != null) {
            val pw = PrintWriter(writer)
            t.printStackTrace(pw)
            pw.flush()
        }

        writer.flush()
    }

    @Synchronized
    private fun getWriter(dir: File, fileName: String, date: String): BufferedWriter {
        if (currentWriter != null && fileName == currentFileName) {
            return currentWriter!!
        }

        closeCurrentWriter()

        val file = File(dir, fileName)
        currentWriter = BufferedWriter(FileWriter(file, true), BUFFER_SIZE)
        currentDate = date
        currentFileName = fileName
        return currentWriter!!
    }

    @Synchronized
    private fun closeCurrentWriter() {
        currentWriter?.let {
            try {
                it.flush()
                it.close()
            } catch (_: Exception) {
            }
        }
        currentWriter = null
        currentDate = null
        currentFileName = null
    }

    private fun rotateIfNeeded(dir: File, fileName: String) {
        val file = File(dir, fileName)
        if (file.exists() && file.length() >= maxFileSize) {
            closeCurrentWriter()
            rotateFile(file)
        }
        cleanOldFiles(dir)
    }

    private fun rotateFile(file: File) {
        val timestamp = System.currentTimeMillis()
        val rotatedName = "${file.nameWithoutExtension}_$timestamp.${file.extension}"
        val rotatedFile = File(file.parent, rotatedName)
        if (!file.renameTo(rotatedFile)) {
            Log.w(TAG, "Failed to rotate log file: ${file.name}")
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
                    Log.w(TAG, "Failed to delete old log file: ${f.name}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "AwLog"
        private const val BUFFER_SIZE = 8192
        private const val MAX_QUEUE_SIZE = 1024
    }
}
