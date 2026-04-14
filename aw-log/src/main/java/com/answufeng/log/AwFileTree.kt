package com.answufeng.log

import android.util.Log
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 文件日志 Tree，支持按日期分文件、大小限制轮转、异步写入和智能刷新。
 *
 * 核心特性：
 * - **异步写入**：单线程 [ThreadPoolExecutor] 保证写入顺序，不阻塞调用线程
 * - **智能刷新**：定时刷新（默认 3 秒）+ ERROR 级别即时 flush，平衡性能与可靠性
 * - **磁盘空间检查**：可用空间低于 10MB 时跳过写入
 * - **按日期分文件**：每天一个日志文件，格式 `log_yyyy-MM-dd.txt`
 * - **大小轮转**：超过 [maxFileSize] 时自动重命名并创建新文件
 * - **数量清理**：超过 [maxFileCount] 时自动删除最旧的文件
 * - **队列溢出策略**：[ThreadPoolExecutor.DiscardOldestPolicy]，保留最新日志
 */
internal class AwFileTree(
    private val logDir: String,
    private val maxFileSize: Long = 5L * 1024 * 1024,
    private val maxFileCount: Int = 10,
    private val minPriority: Int = Log.DEBUG,
    private val formatter: AwLogFormatter = AwLogFormatter.default(),
    private val flushIntervalMs: Long = 3000L
) : Timber.Tree() {

    private val executor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(MAX_QUEUE_SIZE),
        { runnable -> Thread(runnable, "AwLog-FileWriter").apply { isDaemon = true } },
        ThreadPoolExecutor.DiscardOldestPolicy()
    )

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Volatile
    private var currentWriter: BufferedWriter? = null
    private var currentDate: String? = null
    private var currentFileName: String? = null

    private val flushRunnable = object : Runnable {
        override fun run() {
            flushInternal()
            if (!executor.isShutdown) {
                executor.execute(this)
            }
        }
    }

    init {
        executor.execute {
            Thread.sleep(flushIntervalMs)
            flushRunnable.run()
        }
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= minPriority
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        executor.execute {
            try {
                writeLog(priority, tag, message, t)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 刷新文件日志缓冲区，确保日志写入磁盘。
     *
     * 最多等待 5 秒，超时后返回。建议在 Activity.onDestroy 或 Application.onTerminate 中调用。
     */
    fun flush() {
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

    @Synchronized
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

        val file = File(dir, fileName)
        val writer = getWriter(dir, fileName, today)

        val formattedLine = formatter.format(priority, tag, message, t)
        writer.write(formattedLine)
        writer.newLine()

        if (t != null) {
            val pw = PrintWriter(writer)
            t.printStackTrace(pw)
            pw.flush()
        }

        if (priority >= Log.ERROR) {
            writer.flush()
        }
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
        private const val MIN_DISK_SPACE_BYTES = 10L * 1024 * 1024
    }
}
