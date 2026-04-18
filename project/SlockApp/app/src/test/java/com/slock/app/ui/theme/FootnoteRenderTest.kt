package com.slock.app.ui.theme

import androidx.compose.ui.text.style.BaselineShift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FootnoteRenderTest {

    @Test
    fun `footnote reference renders as semantic marker instead of raw caret syntax`() {
        val result = buildMentionAnnotatedString("Use the note[^1] here.")

        assertEquals("Use the note[1] here.", result.text)
        assertFalse(result.text.contains("[^1]"))
        assertTrue(
            "footnote reference must carry superscript-like styling",
            result.spanStyles.any { span ->
                result.text.substring(span.start, span.end) == "[1]" &&
                    span.item.baselineShift == BaselineShift.Superscript
            }
        )
    }

    @Test
    fun `parse markdown blocks groups footnote definitions into Footnotes block`() {
        val blocks = parseMarkdownBlocks(
            "Paragraph with note[^1].\n\n[^1]: Footnote text\n[^long]: Another footnote"
        )

        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Paragraph)
        assertTrue(blocks[1] is MarkdownBlock.Footnotes)

        val paragraph = blocks[0] as MarkdownBlock.Paragraph
        assertEquals("Paragraph with note[1].", paragraph.text.text)

        val footnotes = blocks[1] as MarkdownBlock.Footnotes
        assertEquals(2, footnotes.items.size)
        assertEquals("1", footnotes.items[0].first)
        assertEquals("Footnote text", footnotes.items[0].second.text)
        assertEquals("long", footnotes.items[1].first)
        assertEquals("Another footnote", footnotes.items[1].second.text)
    }

    @Test
    fun `non footnote bracket text is not misidentified as footnote reference`() {
        val result = buildMentionAnnotatedString("Keep [^ not-a-footnote] and [link] literal.")

        assertEquals("Keep [^ not-a-footnote] and [link] literal.", result.text)
        assertTrue(
            "non-footnote bracket text must not get superscript styling",
            result.spanStyles.none { span -> span.item.baselineShift == BaselineShift.Superscript }
        )
    }

    @Test
    fun `footnote parser does not steal regular markdown links`() {
        val result = buildMentionAnnotatedString("Read [OpenAI](https://openai.com) and [^1] footnote")

        val openAiIndex = result.text.indexOf("OpenAI")
        val linkAnnotations = result.getStringAnnotations("markdown_link", openAiIndex, openAiIndex)
        assertEquals(1, linkAnnotations.size)
        assertEquals("https://openai.com", linkAnnotations.single().item)
        assertTrue(result.text.contains("[1] footnote"))
    }
}
