package com.answufeng.log

import org.junit.Assert.*
import org.junit.Test

class AwLogInterceptorTest {

    @Test
    fun `LogResult has ACCEPTED and REJECTED values`() {
        assertEquals(2, AwLogInterceptor.LogResult.entries.size)
        assertNotNull(AwLogInterceptor.LogResult.ACCEPTED)
        assertNotNull(AwLogInterceptor.LogResult.REJECTED)
    }

    @Test
    fun `interceptor can accept logs`() {
        val interceptor = object : AwLogInterceptor {
            override fun intercept(
                priority: Int, tag: String?, message: String, throwable: Throwable?
            ): AwLogInterceptor.LogResult {
                return AwLogInterceptor.LogResult.ACCEPTED
            }
        }
        assertEquals(
            AwLogInterceptor.LogResult.ACCEPTED,
            interceptor.intercept(android.util.Log.DEBUG, "Tag", "Message", null)
        )
    }

    @Test
    fun `interceptor can reject logs`() {
        val interceptor = object : AwLogInterceptor {
            override fun intercept(
                priority: Int, tag: String?, message: String, throwable: Throwable?
            ): AwLogInterceptor.LogResult {
                return AwLogInterceptor.LogResult.REJECTED
            }
        }
        assertEquals(
            AwLogInterceptor.LogResult.REJECTED,
            interceptor.intercept(android.util.Log.DEBUG, "Tag", "Message", null)
        )
    }

    @Test
    fun `interceptor receives correct parameters`() {
        val testException = RuntimeException("test")
        var receivedPriority = -1
        var receivedTag: String? = null
        var receivedMessage: String? = null
        var receivedThrowable: Throwable? = null

        val interceptor = object : AwLogInterceptor {
            override fun intercept(
                priority: Int, tag: String?, message: String, throwable: Throwable?
            ): AwLogInterceptor.LogResult {
                receivedPriority = priority
                receivedTag = tag
                receivedMessage = message
                receivedThrowable = throwable
                return AwLogInterceptor.LogResult.ACCEPTED
            }
        }

        interceptor.intercept(android.util.Log.ERROR, "TestTag", "TestMessage", testException)

        assertEquals(android.util.Log.ERROR, receivedPriority)
        assertEquals("TestTag", receivedTag)
        assertEquals("TestMessage", receivedMessage)
        assertEquals(testException, receivedThrowable)
    }
}
