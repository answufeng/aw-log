package com.answufeng.log

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object AwLogFileManager {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd")
        .toFormatter()

    fun compressOldLogs(logDir: String): Int {
        val dir = File(logDir)
        if (!dir.exists()) return 0

        val today = LocalDate.now().format(dateFormatter)
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

    fun getTotalSize(logDir: String): Long {
        val dir = File(logDir)
        if (!dir.exists()) return 0
        return dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("log_") }
            ?.sumOf { it.length() }
            ?: 0
    }

    fun getLogFiles(logDir: String): List<File> {
        val dir = File(logDir)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("log_") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

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

    private const val BUFFER_SIZE = 8192
}
