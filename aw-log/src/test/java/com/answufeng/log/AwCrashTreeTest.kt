package com.answufeng.log

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class AwCrashTreeTest {

    @Before
    fun setup() {
        Timber.uprootAll()
        AwLogger.reset()
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
        AwLogger.reset()
    }

    @Test
    fun `AwCrashTree with handler invokes handler on error`() {
        var handlerCalled = false

        AwLogger.init {
            debug = false
            fileLog = false
            crashLog = true
            crashHandler = { tag, throwable, message ->
                handlerCalled = true
            }
        }

        AwLogger.e(RuntimeException("test"), "error message")

        assertTrue(handlerCalled)
    }

    @Test
    fun `AwCrashTree without handler does not crash`() {
        AwLogger.init {
            debug = false
            fileLog = false
            crashLog = true
        }
        AwLogger.e("error without handler")
        AwLogger.e(RuntimeException("test"), "error with throwable")
    }

    @Test
    fun `AwCrashTree can be planted via AwLogger`() {
        AwLogger.init {
            debug = false
            fileLog = false
            crashLog = true
        }
        assertTrue(Timber.treeCount > 0)
    }

    @Test
    fun `AwCrashTree only captures error and above`() {
        var handlerCallCount = 0

        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = true
            crashHandler = { _, _, _ ->
                handlerCallCount++
            }
        }

        AwLogger.d("debug message")
        AwLogger.i("info message")
        AwLogger.w("warning message")
        AwLogger.e("error message")
        AwLogger.wtf("wtf message")

        assertEquals(2, handlerCallCount)
    }
}
