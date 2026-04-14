package com.answufeng.log

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class AwDebugTreeTest {

    @Before
    fun setup() {
        Timber.uprootAll()
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    @Test
    fun `AwDebugTree can be planted via AwLogger`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        assertTrue(Timber.treeCount > 0)
        AwLogger.reset()
    }

    @Test
    fun `AwDebugTree logs do not crash`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        AwLogger.d("test debug message")
        AwLogger.e(RuntimeException("test"), "test error message")
        AwLogger.v("test verbose")
        AwLogger.i("test info")
        AwLogger.w("test warning")
        AwLogger.wtf("test wtf")
        AwLogger.reset()
    }

    @Test
    fun `AwDebugTree handles lambda logs`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        AwLogger.d { "lazy debug ${System.currentTimeMillis()}" }
        AwLogger.v { "lazy verbose" }
        AwLogger.i { "lazy info" }
        AwLogger.w { "lazy warning" }
        AwLogger.e { "lazy error" }
        AwLogger.wtf { "lazy wtf" }
        AwLogger.reset()
    }

    @Test
    fun `AwDebugTree handles tagged logs`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        AwLogger.tag("TestTag").d("tagged message")
        AwLogger.reset()
    }

    @Test
    fun `AwDebugTree handles tagged lambda logs`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        AwLogger.d("TestTag") { "tagged lazy debug" }
        AwLogger.e("TestTag") { "tagged lazy error" }
        AwLogger.reset()
    }
}
