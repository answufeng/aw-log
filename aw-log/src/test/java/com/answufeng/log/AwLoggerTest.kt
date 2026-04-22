package com.answufeng.log

import android.util.Log
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.nio.file.Files

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
                override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
                    intercepted = true
                    return chain.proceed()
                }
            })
        }
        AwLogger.d("test")
        assertTrue(intercepted)
    }

    @Test
    fun `interceptor can reject logs`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
            addInterceptor(object : AwLogInterceptor {
                override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
                    return AwLogInterceptor.LogResult.Rejected("test reject")
                }
            })
        }
        AwLogger.d("test")
    }

    @Test
    fun `interceptor can modify message`() {
        var receivedMessage: String? = null
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
            addInterceptor(object : AwLogInterceptor {
                override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
                    return chain.proceed(message = chain.message.uppercase())
                }
            })
            addInterceptor(object : AwLogInterceptor {
                override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
                    receivedMessage = chain.message
                    return chain.proceed()
                }
            })
        }
        AwLogger.d("test")
        assertEquals("TEST", receivedMessage)
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
    fun `json with custom priority`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        AwLogger.json("""{"key":"value"}""", priority = Log.INFO)
    }

    @Test
    fun `formatMessage with args works`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        AwLogger.i("用户登录: userId=%d, name=%s", 123, "张三")
        AwLogger.d("请求耗时: %dms", 256)
    }

    @Test
    fun `throwable with tag lambda works`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        val ex = RuntimeException("网络超时")
        AwLogger.e(ex, "API") { "请求失败: ${ex.message}" }
    }

    @Test
    fun `setMinPriority works`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
            minPriority = Log.VERBOSE
        }
        AwLogger.setMinPriority(Log.ERROR)
        AwLogger.d("should be filtered")
        AwLogger.e("should pass")
    }

    @Test
    fun `getFileDir returns empty when fileLog disabled`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        assertEquals("", AwLogger.getFileDir())
    }

    @Test
    fun `log listener receives logs`() {
        var receivedPriority = -1
        var receivedMessage = ""
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
            addListener(object : AwLogListener {
                override fun onLog(priority: Int, tag: String?, message: String, throwable: Throwable?) {
                    receivedPriority = priority
                    receivedMessage = message
                }
            })
        }
        AwLogger.d("test listener")
        assertEquals(Log.DEBUG, receivedPriority)
        assertEquals("test listener", receivedMessage)
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
    fun `tagged lambda methods work without crash`() {
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
        }
        AwLogger.v("TagV") { "lazy verbose" }
        AwLogger.d("TagD") { "lazy debug" }
        AwLogger.i("TagI") { "lazy info" }
        AwLogger.w("TagW") { "lazy warning" }
        AwLogger.e("TagE") { "lazy error" }
        AwLogger.wtf("TagWtf") { "lazy wtf" }
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
        AwLogger.d("should be filtered")
        AwLogger.i("should be filtered")
        AwLogger.e("should pass")
        AwLogger.wtf("should pass")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `init rejects fileLog without fileDir`() {
        AwLogger.init {
            debug = false
            fileLog = true
            fileDir = ""
            crashLog = false
        }
    }

    @Test
    fun `isFileLoggable respects fileMinPriority`() {
        val dir = Files.createTempDirectory("aw_log_isfile").toFile()
        try {
            AwLogger.init {
                debug = true
                fileLog = true
                fileDir = dir.absolutePath
                fileMinPriority = Log.WARN
                crashLog = false
            }
            assertFalse(AwLogger.isFileLoggable(Log.DEBUG))
            assertTrue(AwLogger.isFileLoggable(Log.WARN))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `rejectLogOnInterceptorFailure drops log when interceptor throws`() {
        var listenerMessage: String? = null
        AwLogger.init {
            debug = true
            fileLog = false
            crashLog = false
            rejectLogOnInterceptorFailure = true
            addInterceptor(object : AwLogInterceptor {
                override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
                    error("boom")
                }
            })
            addListener { _, _, message, _ ->
                listenerMessage = message
            }
        }
        AwLogger.d("secret")
        assertNull(listenerMessage)
    }
}
