package com.answufeng.log

import android.util.Log
import androidx.annotation.WorkerThread
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 日志文件管理工具。
 *
 * 提供日志文件的压缩、导出、清理、搜索等功能。
 * 异步操作使用共享的单线程 ExecutorService（守护线程），避免频繁创建线程。
 *
 * 线程模型：所有同步方法（[compressOldLogs]、[exportLogs] 等）为阻塞操作，
 * 必须在后台线程调用。异步方法（[compressOldLogsAsync] 等）在内部线程池执行，
 * 回调也在后台线程执行，如需更新 UI 请自行切换主线程。
 */
object AwLogFileManager {

    private const val TAG = "AwLogFileManager"

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "AwLog-FileManager").apply { isDaemon = true }
        }

    /**
     * 关闭文件管理器的后台线程池。
     *
     * 通常不需要手动调用，守护线程会在 JVM 退出时自动销毁。
     * 仅在需要优雅关闭的场景（如测试）中使用。
     */
    @JvmStatic
    fun shutdown() {
        executor.shutdown()
    }

    /**
     * 压缩旧日志文件（非今天的 .txt 文件会被压缩为 .gz 并删除原文件）。
     *
     * 此方法为阻塞操作，必须在后台线程调用。
     *
     * @param logDir 日志文件所在目录路径
     * @return 成功压缩的文件数量
     */
    @WorkerThread
    @JvmStatic
    fun compressOldLogs(logDir: String): Int {
        val dir = File(logDir)
        if (!dir.exists()) return 0

        val today = dateFormatter.format(Date())
        val todayPrefix = "log_$today"

        val txtFiles = dir.listFiles { file ->
            file.isFile &&
                file.name.startsWith("log_") &&
                file.name.endsWith(".txt") &&
                !file.name.startsWith(todayPrefix)
        } ?: return 0

        var count = 0
        for (file in txtFiles) {
            try {
                val gzFile = File(file.parent, "${file.name}.gz")
                BufferedInputStream(FileInputStream(file), BUFFER_SIZE).use { input ->
                    GZIPOutputStream(BufferedOutputStream(FileOutputStream(gzFile), BUFFER_SIZE)).use { output ->
                        input.copyTo(output, BUFFER_SIZE)
                    }
                }
                file.delete()
                count++
            } catch (e: Exception) {
                Log.w(TAG, "Failed to compress log file: ${file.name}", e)
            }
        }
        return count
    }

    /**
     * 获取日志文件的总大小。
     *
     * @param logDir 日志文件所在目录路径
     * @return 所有日志文件的总字节数，目录不存在时返回 0
     */
    @JvmStatic
    fun getTotalSize(logDir: String): Long {
        val dir = File(logDir)
        if (!dir.exists()) return 0
        return dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("log_") }
            ?.sumOf { it.length() }
            ?: 0
    }

    /**
     * 获取日志文件列表，按修改时间降序排列。
     *
     * @param logDir 日志文件所在目录路径
     * @return 日志文件列表，目录不存在时返回空列表
     */
    @JvmStatic
    fun getLogFiles(logDir: String): List<File> {
        val dir = File(logDir)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("log_") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * 获取日志目录的可用空间。
     *
     * @param logDir 日志文件所在目录路径
     * @return 可用空间的字节数，目录不存在时返回 0
     */
    @JvmStatic
    fun getAvailableSpace(logDir: String): Long {
        val dir = File(logDir)
        if (!dir.exists()) return 0
        return dir.usableSpace
    }

    /**
     * 将所有日志文件导出为 ZIP 压缩包。
     *
     * 此方法为阻塞操作，必须在后台线程调用。
     *
     * @param logDir 日志文件所在目录路径
     * @param outputFile 输出的 ZIP 文件
     * @return 导出成功返回输出文件，失败或无日志文件时返回 null
     */
    @WorkerThread
    @JvmStatic
    fun exportLogs(logDir: String, outputFile: File): File? {
        val dir = File(logDir)
        if (!dir.exists()) return null

        val logFiles = dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("log_") }
            ?: return null

        if (logFiles.isEmpty()) return null

        return try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile), BUFFER_SIZE)).use { zipOut ->
                for (file in logFiles) {
                    try {
                        BufferedInputStream(FileInputStream(file), BUFFER_SIZE).use { input ->
                            val entry = ZipEntry(file.name)
                            zipOut.putNextEntry(entry)
                            input.copyTo(zipOut, BUFFER_SIZE)
                            zipOut.closeEntry()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to add file to zip: ${file.name}", e)
                    }
                }
            }
            outputFile
        } catch (e: Exception) {
            Log.w(TAG, "Failed to export logs", e)
            null
        }
    }

    /**
     * 删除所有日志文件。
     *
     * 此方法为阻塞操作，必须在后台线程调用。
     *
     * @param logDir 日志文件所在目录路径
     * @return 成功删除的文件数量
     */
    @WorkerThread
    @JvmStatic
    fun clearAll(logDir: String): Int {
        val dir = File(logDir)
        if (!dir.exists()) return 0

        var count = 0
        dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("log_") }
            ?.forEach {
                if (it.delete()) count++
                else Log.w(TAG, "Failed to delete log file: ${it.name}")
            }
        return count
    }

    /**
     * 删除指定日期之前的日志文件。
     *
     * 此方法为阻塞操作，必须在后台线程调用。
     *
     * @param logDir 日志文件所在目录路径
     * @param beforeDate 截止日期，此日期之前的文件将被删除
     * @return 成功删除的文件数量
     */
    @WorkerThread
    @JvmStatic
    fun clearBefore(logDir: String, beforeDate: Date): Int {
        val dir = File(logDir)
        if (!dir.exists()) return 0

        val cutoff = beforeDate.time
        var count = 0
        dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("log_") && it.lastModified() < cutoff }
            ?.forEach {
                if (it.delete()) count++
                else Log.w(TAG, "Failed to delete old log file: ${it.name}")
            }
        return count
    }

    /**
     * 在日志文件中搜索包含关键词的行。
     *
     * 此方法为阻塞操作，必须在后台线程调用。
     *
     * @param logDir 日志文件所在目录路径
     * @param keyword 搜索关键词，忽略大小写
     * @param maxResults 最大返回结果数，默认 100
     * @return 匹配的行列表，格式为 "[文件名] 日志行内容"
     */
    @WorkerThread
    @JvmStatic
    fun search(logDir: String, keyword: String, maxResults: Int = 100): List<String> {
        val dir = File(logDir)
        if (!dir.exists() || keyword.isBlank()) return emptyList()

        val results = mutableListOf<String>()
        val logFiles = dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("log_") && it.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?: return emptyList()

        for (file in logFiles) {
            try {
                file.forEachLine { line ->
                    if (line.contains(keyword, ignoreCase = true)) {
                        results.add("[${file.name}] $line")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to search in file: ${file.name}", e)
            }
            if (results.size >= maxResults) break
        }
        return results
    }

    /**
     * 异步压缩旧日志文件。
     *
     * @param logDir 日志文件所在目录路径
     * @param callback 压缩完成后的回调，参数为成功压缩的文件数量，在后台线程执行
     */
    @JvmStatic
    fun compressOldLogsAsync(logDir: String, callback: ((Int) -> Unit)? = null) {
        executor.execute {
            val count = compressOldLogs(logDir)
            callback?.invoke(count)
        }
    }

    /**
     * 异步导出日志为 ZIP 文件。
     *
     * @param logDir 日志文件所在目录路径
     * @param outputFile 输出的 ZIP 文件
     * @param callback 导出完成后的回调，参数为输出文件或 null，在后台线程执行
     */
    @JvmStatic
    fun exportLogsAsync(logDir: String, outputFile: File, callback: ((File?) -> Unit)? = null) {
        executor.execute {
            val result = exportLogs(logDir, outputFile)
            callback?.invoke(result)
        }
    }

    /**
     * 异步删除所有日志文件。
     *
     * @param logDir 日志文件所在目录路径
     * @param callback 删除完成后的回调，参数为成功删除的文件数量，在后台线程执行
     */
    @JvmStatic
    fun clearAllAsync(logDir: String, callback: ((Int) -> Unit)? = null) {
        executor.execute {
            val count = clearAll(logDir)
            callback?.invoke(count)
        }
    }

    /**
     * 异步删除指定日期之前的日志文件。
     *
     * @param logDir 日志文件所在目录路径
     * @param beforeDate 截止日期
     * @param callback 删除完成后的回调，参数为成功删除的文件数量，在后台线程执行
     */
    @JvmStatic
    fun clearBeforeAsync(logDir: String, beforeDate: Date, callback: ((Int) -> Unit)? = null) {
        executor.execute {
            val count = clearBefore(logDir, beforeDate)
            callback?.invoke(count)
        }
    }

    /**
     * 异步搜索日志内容。
     *
     * @param logDir 日志文件所在目录路径
     * @param keyword 搜索关键词
     * @param maxResults 最大返回结果数
     * @param callback 搜索完成后的回调，参数为匹配的行列表，在后台线程执行
     */
    @JvmStatic
    fun searchAsync(logDir: String, keyword: String, maxResults: Int = 100, callback: ((List<String>) -> Unit)? = null) {
        executor.execute {
            val results = search(logDir, keyword, maxResults)
            callback?.invoke(results)
        }
    }

    private const val BUFFER_SIZE = 8192
}
