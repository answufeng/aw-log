package com.answufeng.log

import org.junit.Assert.*
import org.junit.Test

/**
 * LogConfig 的输入校验和默认值测试。
 */
class LogConfigTest {

    @Test
    fun `default values are correct`() {
        val config = LogConfig()
        assertTrue(config.debug)
        assertFalse(config.fileLog)
        assertEquals("", config.fileDir)
        assertEquals(5L * 1024 * 1024, config.maxFileSize)
        assertEquals(10, config.maxFileCount)
        assertFalse(config.crashLog)
        assertNull(config.crashHandler)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maxFileSize rejects zero`() {
        LogConfig().maxFileSize = 0
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maxFileSize rejects negative`() {
        LogConfig().maxFileSize = -1
    }

    @Test
    fun `maxFileSize accepts positive value`() {
        val config = LogConfig()
        config.maxFileSize = 1024
        assertEquals(1024, config.maxFileSize)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maxFileCount rejects zero`() {
        LogConfig().maxFileCount = 0
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maxFileCount rejects negative`() {
        LogConfig().maxFileCount = -1
    }

    @Test
    fun `maxFileCount accepts positive value`() {
        val config = LogConfig()
        config.maxFileCount = 5
        assertEquals(5, config.maxFileCount)
    }

    @Test
    fun `addTree collects trees`() {
        val config = LogConfig()
        assertEquals(0, config.extraTrees.size)
        config.addTree(timber.log.Timber.DebugTree())
        assertEquals(1, config.extraTrees.size)
    }
}
