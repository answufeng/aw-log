package com.answufeng.log

import org.junit.Assert.*
import org.junit.Test

class AwLogInterceptorTest {

    @Test
    fun `LogResult Accepted holds message and tag`() {
        val result = AwLogInterceptor.LogResult.Accepted("test message", "testTag")
        assertEquals("test message", result.message)
        assertEquals("testTag", result.tag)
    }

    @Test
    fun `LogResult Rejected holds reason`() {
        val result = AwLogInterceptor.LogResult.Rejected("sensitive data")
        assertEquals("sensitive data", result.reason)
    }

    @Test
    fun `interceptor can accept logs via chain proceed`() {
        val interceptor = object : AwLogInterceptor {
            override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
                return chain.proceed()
            }
        }
        val chain = TestChain(
            priority = android.util.Log.DEBUG,
            tag = "Tag",
            message = "Message",
            throwable = null
        )
        val result = interceptor.intercept(chain)
        assertTrue(result is AwLogInterceptor.LogResult.Accepted)
        assertEquals("Message", (result as AwLogInterceptor.LogResult.Accepted).message)
    }

    @Test
    fun `interceptor can reject logs`() {
        val interceptor = object : AwLogInterceptor {
            override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
                return AwLogInterceptor.LogResult.Rejected("blocked")
            }
        }
        val chain = TestChain(
            priority = android.util.Log.DEBUG,
            tag = "Tag",
            message = "Message",
            throwable = null
        )
        val result = interceptor.intercept(chain)
        assertTrue(result is AwLogInterceptor.LogResult.Rejected)
    }

    @Test
    fun `interceptor can modify message`() {
        val interceptor = object : AwLogInterceptor {
            override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
                return chain.proceed(message = chain.message.uppercase())
            }
        }
        val chain = TestChain(
            priority = android.util.Log.DEBUG,
            tag = "Tag",
            message = "Message",
            throwable = null
        )
        val result = interceptor.intercept(chain)
        assertTrue(result is AwLogInterceptor.LogResult.Accepted)
        assertEquals("MESSAGE", (result as AwLogInterceptor.LogResult.Accepted).message)
    }

    @Test
    fun `interceptor can modify tag`() {
        val interceptor = object : AwLogInterceptor {
            override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
                return chain.proceed(tag = "NewTag")
            }
        }
        val chain = TestChain(
            priority = android.util.Log.DEBUG,
            tag = "OldTag",
            message = "Message",
            throwable = null
        )
        val result = interceptor.intercept(chain)
        assertTrue(result is AwLogInterceptor.LogResult.Accepted)
        assertEquals("NewTag", (result as AwLogInterceptor.LogResult.Accepted).tag)
    }

    @Test
    fun `interceptor receives correct parameters`() {
        val testException = RuntimeException("test")
        var receivedPriority = -1
        var receivedTag: String? = null
        var receivedMessage: String? = null
        var receivedThrowable: Throwable? = null

        val interceptor = object : AwLogInterceptor {
            override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
                receivedPriority = chain.priority
                receivedTag = chain.tag
                receivedMessage = chain.message
                receivedThrowable = chain.throwable
                return chain.proceed()
            }
        }

        interceptor.intercept(TestChain(
            priority = android.util.Log.ERROR,
            tag = "TestTag",
            message = "TestMessage",
            throwable = testException
        ))

        assertEquals(android.util.Log.ERROR, receivedPriority)
        assertEquals("TestTag", receivedTag)
        assertEquals("TestMessage", receivedMessage)
        assertEquals(testException, receivedThrowable)
    }

    private class TestChain(
        override val priority: Int,
        override val tag: String?,
        override val message: String,
        override val throwable: Throwable?
    ) : AwLogInterceptor.Chain {
        override fun proceed(message: String, tag: String?): AwLogInterceptor.LogResult {
            return AwLogInterceptor.LogResult.Accepted(message, tag)
        }
    }
}
