package com.answufeng.log

import android.util.Log
import org.junit.Assert.*
import org.junit.Test

class AwLogFormatterTest {

    @Test
    fun `default formatter produces non-empty output`() {
        val formatter = AwLogFormatter.default()
        val result = formatter.format(Log.DEBUG, "TestTag", "TestMessage", null)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `default formatter includes tag`() {
        val formatter = AwLogFormatter.default()
        val result = formatter.format(Log.DEBUG, "TestTag", "TestMessage", null)
        assertTrue(result.contains("TestTag"))
    }

    @Test
    fun `default formatter includes message`() {
        val formatter = AwLogFormatter.default()
        val result = formatter.format(Log.DEBUG, "TestTag", "TestMessage", null)
        assertTrue(result.contains("TestMessage"))
    }

    @Test
    fun `default formatter includes level`() {
        val formatter = AwLogFormatter.default()
        val result = formatter.format(Log.DEBUG, "TestTag", "TestMessage", null)
        assertTrue(result.contains("D/"))
    }

    @Test
    fun `default formatter handles null tag`() {
        val formatter = AwLogFormatter.default()
        val result = formatter.format(Log.DEBUG, null, "TestMessage", null)
        assertTrue(result.contains("NoTag"))
    }

    @Test
    fun `default formatter handles all priority levels`() {
        val formatter = AwLogFormatter.default()
        val levels = mapOf(
            Log.VERBOSE to "V",
            Log.DEBUG to "D",
            Log.INFO to "I",
            Log.WARN to "W",
            Log.ERROR to "E",
            Log.ASSERT to "A"
        )
        levels.forEach { (priority, expected) ->
            val result = formatter.format(priority, "Tag", "Msg", null)
            assertTrue("Expected $expected in result for priority $priority", result.contains(expected))
        }
    }

    @Test
    fun `custom formatter with DSL`() {
        val formatter = AwLogFormatter.create {
            timePattern = "HH:mm:ss"
            separator = " | "
            showLevel = false
            showTag = false
            showTime = true
        }
        val result = formatter.format(Log.DEBUG, "Tag", "Message", null)
        assertTrue(result.contains("Message"))
        assertFalse(result.contains("Tag"))
        assertFalse(result.contains("D/"))
    }

    @Test
    fun `custom formatter without time`() {
        val formatter = AwLogFormatter.create {
            showTime = false
        }
        val result = formatter.format(Log.DEBUG, "Tag", "Message", null)
        assertFalse(result.matches(Regex("^\\d{4}-\\d{2}-\\d{2}.*")))
    }

    @Test
    fun `fully custom formatter`() {
        val formatter = object : AwLogFormatter {
            override fun format(priority: Int, tag: String?, message: String, throwable: Throwable?): String {
                return "[$tag] $message"
            }
        }
        val result = formatter.format(Log.DEBUG, "MyTag", "MyMessage", null)
        assertEquals("[MyTag] MyMessage", result)
    }
}
