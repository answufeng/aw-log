package com.answufeng.log

import android.util.Log
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
    fun `AwCrashTree with echoToLogcat false still accepts ERROR in forest`() {
        val tree = AwCrashTree(echoToLogcat = false)
        assertTrue(tree.isLoggable(null, Log.ERROR))
        tree.log(Log.ERROR, "Tag", "msg", null)
    }

    @Test
    fun `AwCrashTree isLoggable only for ERROR and above`() {
        val tree = AwCrashTree()
        assertFalse(tree.isLoggable(null, Log.VERBOSE))
        assertFalse(tree.isLoggable(null, Log.DEBUG))
        assertFalse(tree.isLoggable(null, Log.INFO))
        assertFalse(tree.isLoggable(null, Log.WARN))
        assertTrue(tree.isLoggable(null, Log.ERROR))
        assertTrue(tree.isLoggable(null, Log.ASSERT))
    }

    @Test
    fun `crashHandler invoked for uncaught exception on background thread`() {
        val latch = CountDownLatch(1)
        var invoked = false
        AwLogger.init {
            debug = false
            fileLog = false
            crashLog = true
            crashHandler = { _, _, _ ->
                invoked = true
                latch.countDown()
            }
        }

        val t = Thread {
            throw RuntimeException("uncaught test")
        }
        t.start()
        t.join(5000)

        assertTrue("crashHandler should run before thread dies", latch.await(3, TimeUnit.SECONDS))
        assertTrue(invoked)
    }

    @Test
    fun `AwCrashTree without handler does not crash on init`() {
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
    fun `crash handler is replaced on second init`() {
        val firstLatch = CountDownLatch(1)
        val secondLatch = CountDownLatch(1)
        AwLogger.init {
            debug = false
            fileLog = false
            crashLog = true
            crashHandler = { _, _, _ -> firstLatch.countDown() }
        }
        AwLogger.init {
            debug = false
            fileLog = false
            crashLog = true
            crashHandler = { _, _, _ -> secondLatch.countDown() }
        }

        val t = Thread { throw RuntimeException("replace handler test") }
        t.start()
        t.join(5000)

        assertFalse("first handler should not run", firstLatch.await(300, TimeUnit.MILLISECONDS))
        assertTrue(secondLatch.await(3, TimeUnit.SECONDS))
    }
}
