package com.answufeng.log

import android.util.Log
import org.junit.Assert.*
import org.junit.Test

class AwLogConfigTest {

    @Test
    fun `default values are correct`() {
        val config = AwLogConfig()
        assertTrue(config.debug)
        assertFalse(config.fileLog)
        assertEquals("", config.fileDir)
        assertEquals(5L * 1024 * 1024, config.maxFileSize)
        assertEquals(10, config.maxFileCount)
        assertFalse(config.crashLog)
        assertNull(config.crashHandler)
        assertEquals(Log.VERBOSE, config.minPriority)
        assertEquals(Log.DEBUG, config.fileMinPriority)
        assertEquals(3000L, config.flushIntervalMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maxFileSize rejects zero`() {
        AwLogConfig().maxFileSize = 0
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maxFileSize rejects negative`() {
        AwLogConfig().maxFileSize = -1
    }

    @Test
    fun `maxFileSize accepts positive value`() {
        val config = AwLogConfig()
        config.maxFileSize = 1024
        assertEquals(1024, config.maxFileSize)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maxFileCount rejects zero`() {
        AwLogConfig().maxFileCount = 0
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maxFileCount rejects negative`() {
        AwLogConfig().maxFileCount = -1
    }

    @Test
    fun `maxFileCount accepts positive value`() {
        val config = AwLogConfig()
        config.maxFileCount = 5
        assertEquals(5, config.maxFileCount)
    }

    @Test
    fun `addTree collects trees`() {
        val config = AwLogConfig()
        assertEquals(0, config.extraTrees.size)
        config.addTree(timber.log.Timber.DebugTree())
        assertEquals(1, config.extraTrees.size)
    }

    @Test
    fun `minPriority accepts valid values`() {
        val config = AwLogConfig()
        config.minPriority = Log.DEBUG
        assertEquals(Log.DEBUG, config.minPriority)
        config.minPriority = Log.ERROR
        assertEquals(Log.ERROR, config.minPriority)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `minPriority rejects invalid low value`() {
        AwLogConfig().minPriority = -1
    }

    @Test(expected = IllegalArgumentException::class)
    fun `minPriority rejects invalid high value`() {
        AwLogConfig().minPriority = 100
    }

    @Test
    fun `fileMinPriority accepts valid values`() {
        val config = AwLogConfig()
        config.fileMinPriority = Log.INFO
        assertEquals(Log.INFO, config.fileMinPriority)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fileMinPriority rejects invalid value`() {
        AwLogConfig().fileMinPriority = -1
    }

    @Test
    fun `flushIntervalMs accepts positive value`() {
        val config = AwLogConfig()
        config.flushIntervalMs = 5000
        assertEquals(5000, config.flushIntervalMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `flushIntervalMs rejects zero`() {
        AwLogConfig().flushIntervalMs = 0
    }

    @Test(expected = IllegalArgumentException::class)
    fun `flushIntervalMs rejects negative`() {
        AwLogConfig().flushIntervalMs = -1
    }
}
