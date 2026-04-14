package com.answufeng.log

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AwFileTreeTest {

    private lateinit var tempDir: File

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("aw_file_tree_test_").toFile()
        AwLogger.reset()
    }

    @After
    fun tearDown() {
        AwLogger.reset()
        tempDir.deleteRecursively()
    }

    @Test
    fun `AwFileTree can be initialized via AwLogger`() {
        AwLogger.init {
            debug = false
            fileLog = true
            fileDir = tempDir.absolutePath
            crashLog = false
        }
        assertTrue(AwLogger.isInitialized())
    }

    @Test
    fun `AwFileTree creates log directory`() {
        val logDir = File(tempDir, "logs")
        AwLogger.init {
            debug = false
            fileLog = true
            fileDir = logDir.absolutePath
            crashLog = false
        }
        AwLogger.i("test message")
        AwLogger.flush()
        assertTrue(logDir.exists())
    }

    @Test
    fun `AwFileTree writes log to file`() {
        AwLogger.init {
            debug = false
            fileLog = true
            fileDir = tempDir.absolutePath
            fileMinPriority = android.util.Log.VERBOSE
            crashLog = false
        }
        AwLogger.i("test message for file")
        AwLogger.flush()

        val logFiles = tempDir.listFiles { f -> f.name.startsWith("log_") }
        assertNotNull(logFiles)
        assertTrue(logFiles!!.isNotEmpty())

        val content = logFiles[0].readText()
        assertTrue(content.contains("test message for file"))
    }

    @Test
    fun `AwFileTree respects minPriority`() {
        AwLogger.init {
            debug = false
            fileLog = true
            fileDir = tempDir.absolutePath
            fileMinPriority = android.util.Log.ERROR
            crashLog = false
        }
        AwLogger.d("debug should be filtered")
        AwLogger.i("info should be filtered")
        AwLogger.e("error should pass")
        AwLogger.flush()

        val logFiles = tempDir.listFiles { f -> f.name.startsWith("log_") }
        if (logFiles != null && logFiles.isNotEmpty()) {
            val content = logFiles[0].readText()
            assertFalse(content.contains("debug should be filtered"))
            assertFalse(content.contains("info should be filtered"))
            assertTrue(content.contains("error should pass"))
        }
    }

    @Test
    fun `AwFileTree flush does not crash`() {
        AwLogger.init {
            debug = false
            fileLog = true
            fileDir = tempDir.absolutePath
            crashLog = false
        }
        AwLogger.flush()
    }

    @Test
    fun `AwFileTree handles throwable`() {
        AwLogger.init {
            debug = false
            fileLog = true
            fileDir = tempDir.absolutePath
            fileMinPriority = android.util.Log.VERBOSE
            crashLog = false
        }
        AwLogger.e(RuntimeException("test exception"), "error with exception")
        AwLogger.flush()

        val logFiles = tempDir.listFiles { f -> f.name.startsWith("log_") }
        assertNotNull(logFiles)
        assertTrue(logFiles!!.isNotEmpty())

        val content = logFiles[0].readText()
        assertTrue(content.contains("error with exception"))
        assertTrue(content.contains("RuntimeException"))
    }

    @Test
    fun `AwFileTree with custom formatter`() {
        AwLogger.init {
            debug = false
            fileLog = true
            fileDir = tempDir.absolutePath
            fileMinPriority = android.util.Log.VERBOSE
            crashLog = false
            fileFormatter = AwLogFormatter.create {
                showTime = false
                showLevel = true
                showTag = true
            }
        }
        AwLogger.i("custom format test")
        AwLogger.flush()

        val logFiles = tempDir.listFiles { f -> f.name.startsWith("log_") }
        assertNotNull(logFiles)
        assertTrue(logFiles!!.isNotEmpty())

        val content = logFiles[0].readText()
        assertTrue(content.contains("custom format test"))
        assertFalse(content.matches(Regex("^\\d{4}-\\d{2}-\\d{2}.*")))
    }

    @Test
    fun `AwFileTree with custom flushIntervalMs`() {
        AwLogger.init {
            debug = false
            fileLog = true
            fileDir = tempDir.absolutePath
            fileMinPriority = android.util.Log.VERBOSE
            crashLog = false
            flushIntervalMs = 1000L
        }
        AwLogger.i("test with custom flush interval")
        AwLogger.flush()

        val logFiles = tempDir.listFiles { f -> f.name.startsWith("log_") }
        assertNotNull(logFiles)
        assertTrue(logFiles!!.isNotEmpty())

        val content = logFiles[0].readText()
        assertTrue(content.contains("test with custom flush interval"))
    }
}
