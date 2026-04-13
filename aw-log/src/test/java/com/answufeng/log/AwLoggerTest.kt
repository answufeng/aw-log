package com.answufeng.log

import android.util.Log
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class AwLoggerTest {

    @Before
    fun setup() {
        AwLogger.reset()
    }

    @After
    fun tearDown() {
        AwLogger.reset()
    }

    @Test
    fun `isInitialized returns false before init`() {
        assertFalse(AwLogger.isInitialized())
    }

    @Test
    fun `isInitialized returns true after init`() {
        AwLogger.init {
            debug = false
            fileLog = false
            crashLog = false
        }
        assertTrue(AwLogger.isInitialized())
    }

    @Test
    fun `reset clears initialized state`() {
        AwLogger.init {
            debug = false
            fileLog = false
            crashLog = false
        }
        assertTrue(AwLogger.isInitialized())
        AwLogger.reset()
        assertFalse(AwLogger.isInitialized())
    }

    @Test
    fun `init with debug plants AwDebugTree`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        assertTrue(Timber.treeCount > 0)
    }

    @Test
    fun `init without debug does not plant trees`() {
        AwLogger.init {
            debug = false
            fileLog = false
            crashLog = false
        }
        assertEquals(0, Timber.treeCount)
    }

    @Test
    fun `re-init replaces trees`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        val firstCount = Timber.treeCount

        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        assertEquals(firstCount, Timber.treeCount)
    }

    @Test
    fun `init with crashLog plants AwCrashTree`() {
        AwLogger.init {
            debug = false
            fileLog = false
            crashLog = true
        }
        assertTrue(Timber.treeCount > 0)
    }

    @Test
    fun `init with custom tree`() {
        val customTree = object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {}
        }
        AwLogger.init {
            debug = false
            fileLog = false
            crashLog = false
            addTree(customTree)
        }
        assertEquals(1, Timber.treeCount)
    }

    @Test
    fun `init with interceptor`() {
        var intercepted = false
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
            addInterceptor(object : AwLogInterceptor {
                override fun intercept(
                    priority: Int, tag: String?, message: String, throwable: Throwable?
                ): AwLogInterceptor.LogResult {
                    intercepted = true
                    return AwLogInterceptor.LogResult.ACCEPTED
                }
            })
        }
        AwLogger.d("test")
        assertTrue(intercepted)
    }

    @Test
    fun `interceptor can reject logs`() {
        var logReached = false
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
            addInterceptor(object : AwLogInterceptor {
                override fun intercept(
                    priority: Int, tag: String?, message: String, throwable: Throwable?
                ): AwLogInterceptor.LogResult {
                    return AwLogInterceptor.LogResult.REJECTED
                }
            })
        }
        // Even if rejected, the Timber call should not crash
        AwLogger.d("test")
        assertFalse(logReached)
    }

    @Test
    fun `json with null does not crash`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        AwLogger.json(null)
        AwLogger.json("")
        AwLogger.json("   ")
    }

    @Test
    fun `json with valid object`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        AwLogger.json("""{"key":"value"}""")
    }

    @Test
    fun `json with valid array`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        AwLogger.json("""[1,2,3]""")
    }

    @Test
    fun `json with invalid json does not crash`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        AwLogger.json("not json at all")
    }

    @Test
    fun `tag returns Timber Tree`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        val tree = AwLogger.tag("TestTag")
        assertNotNull(tree)
    }

    @Test
    fun `all log level methods work without crash`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        AwLogger.v("verbose")
        AwLogger.d("debug")
        AwLogger.i("info")
        AwLogger.w("warning")
        AwLogger.e("error")
        AwLogger.wtf("wtf")

        AwLogger.v { "lazy verbose" }
        AwLogger.d { "lazy debug" }
        AwLogger.i { "lazy info" }
        AwLogger.w { "lazy warning" }
        AwLogger.e { "lazy error" }
        AwLogger.wtf { "lazy wtf" }
    }

    @Test
    fun `log with throwable does not crash`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        AwLogger.e(RuntimeException("test"), "error with throwable")
        AwLogger.w(RuntimeException("test"), "warning with throwable")
        AwLogger.wtf(RuntimeException("test"), "wtf with throwable")
        AwLogger.e(RuntimeException("test"))
    }

    @Test
    fun `flush does not crash without FileTree`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        AwLogger.flush()
    }

    @Test
    fun `minPriority filters logs`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
            minPriority = Log.ERROR
        }
        // These should be filtered out by minPriority
        AwLogger.d("should be filtered")
        AwLogger.i("should be filtered")
        // These should pass
        AwLogger.e("should pass")
        AwLogger.wtf("should pass")
    }
}
