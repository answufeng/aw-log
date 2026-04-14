package com.answufeng.log

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 日志文件管理工具，提供压缩、导出、清理、查询等操作。
 *
 * 所有方法同时提供同步和异步版本，异步版本在独立线程执行并通过回调返回结果。
 *
 * ```kotlin
 * val logDir = "${cacheDir.absolutePath}/logs"
 *
 * // 同步操作
 * val size = AwLogFileManager.getTotalSize(logDir)
 * val files = AwLogFileManager.getLogFiles(logDir)
 * val count = AwLogFileManager.compressOldLogs(logDir)
 *
 * // 异步操作（不阻塞主线程）
 * AwLogFileManager.compressOldLogsAsync(logDir) { count ->
 *     runOnUiThread { showToast("压缩了 $count 个文件") }
 * }
 * ```
 */
object AwLogFileManager {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * 压缩旧日志文件（非当天的 .txt 文件压缩为 .gz）。
     *
     * 当天的日志文件不会被压缩（可能仍在写入中）。
     *
     * @param logDir 日志目录路径
     * @return 成功压缩的文件数量
     */
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
            } catch (_: Exception) {
            }
        }
        return count
    }

    /**
     * 获取日志目录下所有日志文件的总大小（字节）。
     *
     * @param logDir 日志目录路径
     * @return 总大小，目录不存在时返回 0
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
     * 获取日志文件列表，按修改时间降序排列（最新的在前）。
     *
     * @param logDir 日志目录路径
     * @return 日志文件列表
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
     * 获取日志目录所在分区的可用磁盘空间（字节）。
     *
     * @param logDir 日志目录路径
     * @return 可用空间，目录不存在时返回 0
     */
    @JvmStatic
    fun getAvailableSpace(logDir: String): Long {
        val dir = File(logDir)
        if (!dir.exists()) return 0
        return dir.usableSpace
    }

    /**
     * 将所有日志文件导出为 ZIP 文件。
     *
     * @param logDir 日志目录路径
     * @param outputFile 输出 ZIP 文件
     * @return 导出成功返回文件对象，失败返回 null
     */
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
                    BufferedInputStream(FileInputStream(file), BUFFER_SIZE).use { input ->
                        val entry = ZipEntry(file.name)
                        zipOut.putNextEntry(entry)
                        input.copyTo(zipOut, BUFFER_SIZE)
                        zipOut.closeEntry()
                    }
                }
            }
            outputFile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 清理所有日志文件。
     *
     * @param logDir 日志目录路径
     * @return 成功删除的文件数量
     */
    @JvmStatic
    fun clearAll(logDir: String): Int {
        val dir = File(logDir)
        if (!dir.exists()) return 0

        var count = 0
        dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("log_") }
            ?.forEach {
                if (it.delete()) count++
            }
        return count
    }

    /**
     * 异步压缩旧日志文件。
     *
     * @param logDir 日志目录路径
     * @param callback 完成回调，参数为成功压缩的文件数量
     */
    @JvmStatic
    fun compressOldLogsAsync(logDir: String, callback: ((Int) -> Unit)? = null) {
        Thread {
            val count = compressOldLogs(logDir)
            callback?.invoke(count)
        }.start()
    }

    /**
     * 异步导出日志为 ZIP 文件。
     *
     * @param logDir 日志目录路径
     * @param outputFile 输出 ZIP 文件
     * @param callback 完成回调，参数为导出结果文件（失败时为 null）
     */
    @JvmStatic
    fun exportLogsAsync(logDir: String, outputFile: File, callback: ((File?) -> Unit)? = null) {
        Thread {
            val result = exportLogs(logDir, outputFile)
            callback?.invoke(result)
        }.start()
    }

    /**
     * 异步清理所有日志文件。
     *
     * @param logDir 日志目录路径
     * @param callback 完成回调，参数为成功删除的文件数量
     */
    @JvmStatic
    fun clearAllAsync(logDir: String, callback: ((Int) -> Unit)? = null) {
        Thread {
            val count = clearAll(logDir)
            callback?.invoke(count)
        }.start()
    }

    private const val BUFFER_SIZE = 8192
}
