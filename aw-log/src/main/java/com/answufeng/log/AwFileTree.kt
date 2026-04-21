package com.answufeng.log

import android.util.Log
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 文件日志树
 *
 * 用于将日志写入文件，支持自动轮转、清理旧文件和低磁盘空间检测
 *
 * @param logDir 日志存储目录
 * @param maxFileSize 单个日志文件最大大小（字节），默认 5MB
 * @param maxFileCount 最大日志文件数量，默认 10
 * @param minPriority 最小日志优先级，默认 DEBUG
 * @param formatter 日志格式化器，默认使用 [AwLogFormatter.default()]
 * @param flushIntervalMs 日志刷新间隔（毫秒），默认 3000ms
 *
 * @see Timber.Tree
 * @see AwLogFormatter
 */
internal class AwFileTree(
    private val logDir: String,
    private val maxFileSize: Long = 5L * 1024 * 1024,
    private val maxFileCount: Int = 10,
    private val minPriority: Int = Log.DEBUG,
    private val formatter: AwLogFormatter = AwLogFormatter.default(),
    private val flushIntervalMs: Long = 3000L
) : Timber.Tree() {

    private val executor: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(
        1
    ) { runnable ->
        Thread(runnable, "AwLog-FileWriter").apply { isDaemon = true }
    }.apply {
        removeOnCancelPolicy = true
    }

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Volatile
    private var currentWriter: BufferedWriter? = null
    private var currentDate: String? = null
    private var currentFileName: String? = null

    @Volatile
    private var currentFileSize: Long = 0

    @Volatile
    private var writeCount: Long = 0

    @Volatile
    private var shutdown: Boolean = false

    @Volatile
    private var droppedCount: Long = 0

    init {
        executor.scheduleAtFixedRate({
            if (!shutdown) {
                try {
                    flushInternal()
                    cleanOldFilesIfNeeded()
                } catch (_: Exception) {
                }
            }
        }, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS)
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= minPriority
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (shutdown) return
        val queueSize = executor.queue.size
        if (queueSize >= MAX_QUEUE_SIZE) {
            droppedCount++
            if (droppedCount % 100L == 1L) {
                Log.w(TAG, "Log queue full ($queueSize), dropped $droppedCount messages total")
            }
            return
        }
        executor.execute {
            try {
                writeLog(priority, tag, message, t)
            } catch (_: Exception) {
            }
        }
    }

    fun flush() {
        if (shutdown) return
        val latch = CountDownLatch(1)
        executor.execute {
            try {
                flushInternal()
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

    fun shutdown() {
        if (shutdown) return
        shutdown = true
        executor.execute {
            flushInternal()
            closeCurrentWriter()
        }
        executor.shutdown()
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun flushInternal() {
        synchronized(this) {
            currentWriter?.let {
                try {
                    it.flush()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun writeLog(priority: Int, tag: String?, message: String, t: Throwable?) {
        val dir = File(logDir)
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "Failed to create log directory: $logDir")
            return
        }

        if (dir.usableSpace < MIN_DISK_SPACE_BYTES) {
            Log.w(TAG, "Low disk space, skipping log write")
            return
        }

        val today = dateFormatter.format(Date())
        val fileName = "log_$today.txt"

        rotateIfNeeded(dir, fileName)

        val writer = getWriter(dir, fileName, today)

        val formattedLine = formatter.format(priority, tag, message, t)
        writer.write(formattedLine)
        writer.newLine()

        currentFileSize += formattedLine.toByteArray(Charsets.UTF_8).size.toLong() + 1

        if (t != null) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            t.printStackTrace(pw)
            pw.flush()
            val stackTrace = sw.toString()
            writer.write(stackTrace)
            currentFileSize += stackTrace.length.toLong()
        }

        if (priority >= Log.ERROR) {
            writer.flush()
        }

        writeCount++
    }

    private fun getWriter(dir: File, fileName: String, date: String): BufferedWriter {
        synchronized(this) {
            if (currentWriter != null && fileName == currentFileName) {
                return currentWriter!!
            }

            closeCurrentWriterInternal()

            val file = File(dir, fileName)
            currentFileSize = if (file.exists()) file.length() else 0L
            currentWriter = BufferedWriter(FileWriter(file, true), BUFFER_SIZE)
            currentDate = date
            currentFileName = fileName
            return currentWriter!!
        }
    }

    private fun closeCurrentWriter() {
        synchronized(this) {
            closeCurrentWriterInternal()
        }
    }

    private fun closeCurrentWriterInternal() {
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
        currentFileSize = 0L
    }

    private fun rotateIfNeeded(dir: File, fileName: String) {
        if (currentFileSize >= maxFileSize && currentFileName == fileName) {
            closeCurrentWriter()
            val file = File(dir, fileName)
            rotateFile(file)
        } else {
            val file = File(dir, fileName)
            if (file.exists() && file.length() >= maxFileSize) {
                closeCurrentWriter()
                rotateFile(file)
            }
        }
    }

    private fun cleanOldFilesIfNeeded() {
        if (writeCount % CLEAN_CHECK_INTERVAL != 0L) return
        val dir = File(logDir)
        if (!dir.exists()) return
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
        private const val MIN_DISK_SPACE_BYTES = 10L * 1024 * 1024
        private const val CLEAN_CHECK_INTERVAL = 100L
    }
}
