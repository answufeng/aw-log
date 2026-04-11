package com.answufeng.log

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

/**
 * 日志文件管理工具，提供压缩、清理和导出功能。
 *
 * ### 压缩旧日志
 * ```kotlin
 * val compressedCount = LogFileManager.compressOldLogs(logDir)
 * ```
 *
 * ### 获取日志文件总大小
 * ```kotlin
 * val totalSize = LogFileManager.getTotalSize(logDir)
 * ```
 *
 * ### 导出所有日志为单个压缩文件
 * ```kotlin
 * val zipFile = LogFileManager.exportLogs(logDir, outputFile)
 * ```
 *
 * ### 清理所有日志
 * ```kotlin
 * LogFileManager.clearAll(logDir)
 * ```
 */
object LogFileManager {

    /**
     * 压缩指定目录下的旧日志文件（非当天的 .txt 文件）。
     *
     * 压缩后删除原始 .txt 文件，生成 .txt.gz 文件。
     *
     * @param logDir 日志目录路径
     * @return 压缩的文件数量
     */
    fun compressOldLogs(logDir: String): Int {
        val dir = File(logDir)
        if (!dir.exists()) return 0

        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        val txtFiles = dir.listFiles { file ->
            file.isFile && file.name.startsWith("log_") && file.extension == "txt"
                    && !file.name.contains(today)
        } ?: return 0

        var count = 0
        for (file in txtFiles) {
            try {
                val gzFile = File(file.parent, "${file.name}.gz")
                FileInputStream(file).use { input ->
                    GZIPOutputStream(FileOutputStream(gzFile)).use { output ->
                        input.copyTo(output, bufferSize = 8192)
                    }
                }
                file.delete()
                count++
            } catch (_: Exception) {
                // 压缩失败静默跳过
            }
        }
        return count
    }

    /**
     * 获取日志目录下所有文件的总大小。
     *
     * @param logDir 日志目录路径
     * @return 总大小（字节）
     */
    fun getTotalSize(logDir: String): Long {
        val dir = File(logDir)
        if (!dir.exists()) return 0
        return dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("log_") }
            ?.sumOf { it.length() }
            ?: 0
    }

    /**
     * 获取日志文件列表（按修改时间倒序）。
     *
     * @param logDir 日志目录路径
     * @return 日志文件列表
     */
    fun getLogFiles(logDir: String): List<File> {
        val dir = File(logDir)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("log_") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * 导出所有日志文件到单个 zip 文件。
     *
     * @param logDir 日志目录路径
     * @param outputFile 输出 zip 文件路径
     * @return 导出的 zip 文件，失败返回 null
     */
    fun exportLogs(logDir: String, outputFile: File): File? {
        val dir = File(logDir)
        if (!dir.exists()) return null

        val logFiles = dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("log_") }
            ?: return null

        if (logFiles.isEmpty()) return null

        return try {
            java.util.zip.ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                for (file in logFiles) {
                    FileInputStream(file).use { input ->
                        val entry = java.util.zip.ZipEntry(file.name)
                        zipOut.putNextEntry(entry)
                        input.copyTo(zipOut, bufferSize = 8192)
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
     * 清理指定目录下的所有日志文件。
     *
     * @param logDir 日志目录路径
     * @return 清理的文件数量
     */
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
}
