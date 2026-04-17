package com.slock.app.ui.theme

import org.junit.Assert.*
import org.junit.Test

class HighlightTextTest {

    private fun highlightRanges(text: String, query: String): List<IntRange> {
        if (query.isBlank()) return emptyList()
        val ranges = mutableListOf<IntRange>()
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var start = 0
        while (start < text.length) {
            val idx = lowerText.indexOf(lowerQuery, start)
            if (idx < 0) break
            ranges.add(idx until idx + query.length)
            start = idx + query.length
        }
        return ranges
    }

    @Test
    fun `no highlights for blank query`() {
        val ranges = highlightRanges("Hello world", "")
        assertTrue(ranges.isEmpty())
    }

    @Test
    fun `single match highlighted`() {
        val ranges = highlightRanges("Hello world", "world")
        assertEquals(1, ranges.size)
        assertEquals(6 until 11, ranges[0])
    }

    @Test
    fun `multiple matches highlighted`() {
        val ranges = highlightRanges("hello hello hello", "hello")
        assertEquals(3, ranges.size)
        assertEquals(0 until 5, ranges[0])
        assertEquals(6 until 11, ranges[1])
        assertEquals(12 until 17, ranges[2])
    }

    @Test
    fun `case insensitive highlighting`() {
        val ranges = highlightRanges("Hello HELLO hElLo", "hello")
        assertEquals(3, ranges.size)
    }

    @Test
    fun `no match returns empty`() {
        val ranges = highlightRanges("Hello world", "xyz")
        assertTrue(ranges.isEmpty())
    }

    @Test
    fun `match at start of text`() {
        val ranges = highlightRanges("test message", "test")
        assertEquals(1, ranges.size)
        assertEquals(0 until 4, ranges[0])
    }

    @Test
    fun `match at end of text`() {
        val ranges = highlightRanges("a message test", "test")
        assertEquals(1, ranges.size)
        assertEquals(10 until 14, ranges[0])
    }

    @Test
    fun `partial word match`() {
        val ranges = highlightRanges("testing tester tested", "test")
        assertEquals(3, ranges.size)
    }

    @Test
    fun `chinese character search`() {
        val ranges = highlightRanges("搜索消息功能实现", "消息")
        assertEquals(1, ranges.size)
        assertEquals(2 until 4, ranges[0])
    }
}
