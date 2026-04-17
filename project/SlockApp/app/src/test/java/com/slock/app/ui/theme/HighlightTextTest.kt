package com.slock.app.ui.theme

import org.junit.Assert.*
import org.junit.Test

class HighlightTextTest {

    @Test
    fun `no highlight for blank query`() {
        val result = buildMentionAnnotatedString("Hello world", "")
        assertEquals("Hello world", result.text)
        assertTrue(result.spanStyles.isEmpty() || result.spanStyles.all { it.item.background == androidx.compose.ui.graphics.Color.Unspecified })
    }

    @Test
    fun `plain text gets highlighted`() {
        val result = buildMentionAnnotatedString("Hello world", "world")
        assertEquals("Hello world", result.text)
        val yellowSpans = result.spanStyles.filter {
            it.item.background != androidx.compose.ui.graphics.Color.Unspecified &&
            it.item.background.alpha > 0
        }
        assertTrue(yellowSpans.isNotEmpty())
    }

    @Test
    fun `case insensitive highlight`() {
        val result = buildMentionAnnotatedString("HELLO hello HeLLo", "hello")
        assertEquals("HELLO hello HeLLo", result.text)
        val boldSpans = result.spanStyles.filter {
            it.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold
        }
        assertTrue("Should have highlight spans for each match", boldSpans.size >= 3)
    }

    @Test
    fun `mention text is preserved`() {
        val result = buildMentionAnnotatedString("Hi @user check this", "")
        assertTrue(result.text.contains("@user"))
    }

    @Test
    fun `inline code text is preserved`() {
        val result = buildMentionAnnotatedString("Run `npm install` now", "")
        assertTrue(result.text.contains("npm install"))
    }

    @Test
    fun `highlight inside mention text`() {
        val result = buildMentionAnnotatedString("Hello @username bye", "user")
        assertEquals("Hello @username bye", result.text)
        val highlightSpans = result.spanStyles.filter {
            it.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold &&
            it.item.background.alpha > 0
        }
        assertTrue("Should highlight 'user' inside @username", highlightSpans.isNotEmpty())
    }

    @Test
    fun `highlight inside inline code`() {
        val result = buildMentionAnnotatedString("Use `search function` here", "search")
        assertTrue(result.text.contains("search function"))
        val highlightSpans = result.spanStyles.filter {
            it.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold
        }
        assertTrue("Should highlight 'search' inside code block", highlightSpans.isNotEmpty())
    }

    @Test
    fun `no match returns plain text`() {
        val result = buildMentionAnnotatedString("Hello world", "xyz")
        assertEquals("Hello world", result.text)
    }

    @Test
    fun `chinese character highlight`() {
        val result = buildMentionAnnotatedString("搜索消息功能实现", "消息")
        assertEquals("搜索消息功能实现", result.text)
        val highlightSpans = result.spanStyles.filter {
            it.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold &&
            it.item.background.alpha > 0
        }
        assertTrue("Should highlight Chinese characters", highlightSpans.isNotEmpty())
    }

    @Test
    fun `multiple occurrences all highlighted`() {
        val result = buildMentionAnnotatedString("test one test two test", "test")
        assertEquals("test one test two test", result.text)
        val highlightSpans = result.spanStyles.filter {
            it.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold &&
            it.item.background.alpha > 0
        }
        assertTrue("Should have at least 3 highlight spans", highlightSpans.size >= 3)
    }
}
