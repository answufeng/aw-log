package com.answufeng.log

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

object AwLogFileManager {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "AwLog-FileManager").apply { isDaemon = true }
        }

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

    @JvmStatic
    fun getTotalSize(logDir: String): Long {
        val dir = File(logDir)
        if (!dir.exists()) return 0
        return dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("log_") }
            ?.sumOf { it.length() }
            ?: 0
    }

    @JvmStatic
    fun getLogFiles(logDir: String): List<File> {
        val dir = File(logDir)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("log_") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    @JvmStatic
    fun getAvailableSpace(logDir: String): Long {
        val dir = File(logDir)
        if (!dir.exists()) return 0
        return dir.usableSpace
    }

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
            }
        return count
    }

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
            } catch (_: Exception) {
            }
            if (results.size >= maxResults) break
        }
        return results
    }

    @JvmStatic
    fun compressOldLogsAsync(logDir: String, callback: ((Int) -> Unit)? = null) {
        executor.execute {
            val count = compressOldLogs(logDir)
            callback?.invoke(count)
        }
    }

    @JvmStatic
    fun exportLogsAsync(logDir: String, outputFile: File, callback: ((File?) -> Unit)? = null) {
        executor.execute {
            val result = exportLogs(logDir, outputFile)
            callback?.invoke(result)
        }
    }

    @JvmStatic
    fun clearAllAsync(logDir: String, callback: ((Int) -> Unit)? = null) {
        executor.execute {
            val count = clearAll(logDir)
            callback?.invoke(count)
        }
    }

    @JvmStatic
    fun clearBeforeAsync(logDir: String, beforeDate: Date, callback: ((Int) -> Unit)? = null) {
        executor.execute {
            val count = clearBefore(logDir, beforeDate)
            callback?.invoke(count)
        }
    }

    @JvmStatic
    fun searchAsync(logDir: String, keyword: String, maxResults: Int = 100, callback: ((List<String>) -> Unit)? = null) {
        executor.execute {
            val results = search(logDir, keyword, maxResults)
            callback?.invoke(results)
        }
    }

    private const val BUFFER_SIZE = 8192
}
