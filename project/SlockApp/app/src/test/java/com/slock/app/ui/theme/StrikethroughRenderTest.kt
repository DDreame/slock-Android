package com.slock.app.ui.theme

import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.*
import org.junit.Test

class StrikethroughRenderTest {

    @Test
    fun `strikethrough text renders with LineThrough decoration`() {
        val result = buildMentionAnnotatedString("hello ~~deleted~~ world")
        val lineThrough = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.LineThrough
        }
        assertTrue(
            "~~deleted~~ must produce a LineThrough span",
            lineThrough.isNotEmpty()
        )
        val span = lineThrough.first()
        assertEquals("deleted", result.text.substring(span.start, span.end))
    }

    @Test
    fun `strikethrough combined with bold renders both styles`() {
        val result = buildMentionAnnotatedString("**~~bold and deleted~~**")
        val lineThrough = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.LineThrough
        }
        assertTrue("must have LineThrough span", lineThrough.isNotEmpty())
    }

    @Test
    fun `text without strikethrough has no LineThrough decoration`() {
        val result = buildMentionAnnotatedString("normal text with ~single tilde~")
        val lineThrough = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.LineThrough
        }
        assertTrue("single tilde must not produce LineThrough", lineThrough.isEmpty())
    }

    @Test
    fun `multiple strikethrough segments render independently`() {
        val result = buildMentionAnnotatedString("~~first~~ and ~~second~~")
        val lineThrough = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.LineThrough
        }
        assertEquals("must have 2 LineThrough spans", 2, lineThrough.size)
    }

    @Test
    fun `empty strikethrough is not parsed`() {
        val result = buildMentionAnnotatedString("~~~~ empty")
        val lineThrough = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.LineThrough
        }
        assertTrue("empty ~~ delimiters must not produce LineThrough", lineThrough.isEmpty())
    }
}
