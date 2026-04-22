package com.answufeng.log

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AwLogFileManagerTest {

    private lateinit var tempDir: File

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("aw_log_test_").toFile()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `getTotalSize returns 0 for non-existent directory`() {
        assertEquals(0L, AwLogFileManager.getTotalSize("/non/existent/path"))
    }

    @Test
    fun `getTotalSize returns 0 for empty directory`() {
        assertEquals(0L, AwLogFileManager.getTotalSize(tempDir.absolutePath))
    }

    @Test
    fun `getTotalSize sums log file sizes`() {
        File(tempDir, "log_2026-01-01.txt").writeText("hello")
        File(tempDir, "log_2026-01-02.txt").writeText("world!")
        assertEquals(11L, AwLogFileManager.getTotalSize(tempDir.absolutePath))
    }

    @Test
    fun `getTotalSize ignores non-log files`() {
        File(tempDir, "log_2026-01-01.txt").writeText("hello")
        File(tempDir, "other_file.txt").writeText("this should be ignored")
        assertEquals(5L, AwLogFileManager.getTotalSize(tempDir.absolutePath))
    }

    @Test
    fun `getLogFiles returns empty for non-existent directory`() {
        assertTrue(AwLogFileManager.getLogFiles("/non/existent/path").isEmpty())
    }

    @Test
    fun `getLogFiles returns log files sorted by modification time`() {
        val f1 = File(tempDir, "log_2026-01-01.txt").apply {
            writeText("a")
            setLastModified(1000)
        }
        val f2 = File(tempDir, "log_2026-01-02.txt").apply {
            writeText("b")
            setLastModified(2000)
        }
        val files = AwLogFileManager.getLogFiles(tempDir.absolutePath)
        assertEquals(2, files.size)
        assertEquals(f2.name, files[0].name)
        assertEquals(f1.name, files[1].name)
    }

    @Test
    fun `getLogFiles includes gz files`() {
        File(tempDir, "log_2026-01-01.txt.gz").writeText("compressed")
        val files = AwLogFileManager.getLogFiles(tempDir.absolutePath)
        assertEquals(1, files.size)
    }

    @Test
    fun `clearAll returns 0 for non-existent directory`() {
        assertEquals(0, AwLogFileManager.clearAll("/non/existent/path"))
    }

    @Test
    fun `clearAll removes all log files`() {
        File(tempDir, "log_2026-01-01.txt").writeText("a")
        File(tempDir, "log_2026-01-02.txt").writeText("b")
        File(tempDir, "log_2026-01-03.txt.gz").writeText("c")

        val count = AwLogFileManager.clearAll(tempDir.absolutePath)
        assertEquals(3, count)
        assertEquals(0, tempDir.listFiles()?.size ?: 0)
    }

    @Test
    fun `clearAll preserves non-log files`() {
        File(tempDir, "log_2026-01-01.txt").writeText("a")
        File(tempDir, "config.json").writeText("{}")

        AwLogFileManager.clearAll(tempDir.absolutePath)
        val remaining = tempDir.listFiles()!!
        assertEquals(1, remaining.size)
        assertEquals("config.json", remaining[0].name)
    }

    @Test
    fun `compressOldLogs returns 0 for non-existent directory`() {
        assertEquals(0, AwLogFileManager.compressOldLogs("/non/existent/path"))
    }

    @Test
    fun `compressOldLogs skips today files`() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        File(tempDir, "log_$today.txt").writeText("today's log")
        assertEquals(0, AwLogFileManager.compressOldLogs(tempDir.absolutePath))
        assertTrue(File(tempDir, "log_$today.txt").exists())
    }

    @Test
    fun `compressOldLogs compresses old files`() {
        File(tempDir, "log_2020-01-01.txt").writeText("old log content")
        val count = AwLogFileManager.compressOldLogs(tempDir.absolutePath)
        assertEquals(1, count)
        assertFalse(File(tempDir, "log_2020-01-01.txt").exists())
        assertTrue(File(tempDir, "log_2020-01-01.txt.gz").exists())
    }

    @Test
    fun `compressOldLogs does not compress already gzipped files`() {
        File(tempDir, "log_2020-01-01.txt.gz").writeText("already compressed")
        val count = AwLogFileManager.compressOldLogs(tempDir.absolutePath)
        assertEquals(0, count)
    }

    @Test
    fun `exportLogs returns null for non-existent directory`() {
        val output = File(tempDir, "export.zip")
        assertNull(AwLogFileManager.exportLogs("/non/existent/path", output))
    }

    @Test
    fun `exportLogs returns null for empty directory`() {
        val output = File(tempDir, "export.zip")
        assertNull(AwLogFileManager.exportLogs(tempDir.absolutePath, output))
    }

    @Test
    fun `exportLogs creates zip with log files`() {
        File(tempDir, "log_2026-01-01.txt").writeText("content A")
        File(tempDir, "log_2026-01-02.txt").writeText("content B")

        val exportDir = Files.createTempDirectory("aw_export_test_").toFile()
        try {
            val output = File(exportDir, "export.zip")
            val result = AwLogFileManager.exportLogs(tempDir.absolutePath, output)

            assertNotNull(result)
            assertTrue(result!!.exists())
            assertTrue(result.length() > 0)

            val zip = java.util.zip.ZipFile(result)
            val entries = zip.entries().toList()
            assertEquals(2, entries.size)
            zip.close()
        } finally {
            exportDir.deleteRecursively()
        }
    }

    @Test
    fun `exportLogs includes gz files`() {
        File(tempDir, "log_2026-01-01.txt").writeText("text log")
        File(tempDir, "log_2026-01-02.txt.gz").writeText("compressed log")

        val exportDir = Files.createTempDirectory("aw_export_gz_test_").toFile()
        try {
            val output = File(exportDir, "export.zip")
            val result = AwLogFileManager.exportLogs(tempDir.absolutePath, output)
            assertNotNull(result)

            val zip = java.util.zip.ZipFile(result)
            val entries = zip.entries().toList()
            assertEquals(2, entries.size)
            zip.close()
        } finally {
            exportDir.deleteRecursively()
        }
    }

    @Test
    fun `getAvailableSpace returns non-negative for existing directory`() {
        val space = AwLogFileManager.getAvailableSpace(tempDir.absolutePath)
        assertTrue(space >= 0)
    }

    @Test
    fun `getAvailableSpace returns 0 for non-existent directory`() {
        assertEquals(0L, AwLogFileManager.getAvailableSpace("/non/existent/path"))
    }

    @Test
    fun `compressOldLogsAsync does not crash`() {
        val latch = java.util.concurrent.CountDownLatch(1)
        AwLogFileManager.compressOldLogsAsync(tempDir.absolutePath) {
            latch.countDown()
        }
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
    }

    @Test
    fun `clearAllAsync does not crash`() {
        val latch = java.util.concurrent.CountDownLatch(1)
        AwLogFileManager.clearAllAsync(tempDir.absolutePath) {
            latch.countDown()
        }
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
    }

    @Test
    fun `search includes gzipped log files`() {
        val gz = java.io.File(tempDir, "log_2020-01-01.txt.gz")
        java.io.FileOutputStream(gz).use { fos ->
            java.util.zip.GZIPOutputStream(fos).use { gzos ->
                gzos.write("needle line\n".toByteArray())
            }
        }
        val results = AwLogFileManager.search(tempDir.absolutePath, "needle")
        assertEquals(1, results.size)
        assertTrue(results[0].contains("needle line"))
    }

    @Test
    fun `async works after shutdown`() {
        AwLogFileManager.shutdown()
        val latch = java.util.concurrent.CountDownLatch(1)
        AwLogFileManager.compressOldLogsAsync(tempDir.absolutePath) {
            latch.countDown()
        }
        assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS))
    }
}
