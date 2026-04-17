package com.slock.app.ui.theme

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownContentTest {

    @Test
    fun `inline markdown renders bold italic and link annotations`() {
        val result = buildMentionAnnotatedString(
            "This is **bold** and *italic* and [OpenAI](https://openai.com)"
        )

        assertEquals("This is bold and italic and OpenAI", result.text)
        assertTrue(result.spanStyles.any { span ->
            span.item.fontWeight == FontWeight.Bold &&
                result.text.substring(span.start, span.end).contains("bold")
        })
        assertTrue(result.spanStyles.any { span ->
            span.item.fontStyle == FontStyle.Italic &&
                result.text.substring(span.start, span.end).contains("italic")
        })

        val openAiIndex = result.text.indexOf("OpenAI")
        val annotations = result.getStringAnnotations("markdown_link", openAiIndex, openAiIndex)
        assertEquals(1, annotations.size)
        assertEquals("https://openai.com", annotations.single().item)
    }

    @Test
    fun `parse markdown blocks covers heading lists and horizontal rule`() {
        val blocks = parseMarkdownBlocks(
            "# Title\n\n- first item\n- second item\n\n1. step one\n2. step two\n\n---\n\nParagraph"
        )

        assertEquals(5, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Heading)
        assertTrue(blocks[1] is MarkdownBlock.ListBlock)
        assertTrue(blocks[2] is MarkdownBlock.ListBlock)
        assertTrue(blocks[3] is MarkdownBlock.HorizontalRule)
        assertTrue(blocks[4] is MarkdownBlock.Paragraph)

        val heading = blocks[0] as MarkdownBlock.Heading
        assertEquals(1, heading.level)
        assertEquals("Title", heading.text.text)

        val unorderedList = blocks[1] as MarkdownBlock.ListBlock
        assertFalse(unorderedList.ordered)
        assertEquals(listOf("first item", "second item"), unorderedList.items.map { it.text })

        val orderedList = blocks[2] as MarkdownBlock.ListBlock
        assertTrue(orderedList.ordered)
        assertEquals(1, orderedList.startNumber)
        assertEquals(listOf("step one", "step two"), orderedList.items.map { it.text })

        val paragraph = blocks[4] as MarkdownBlock.Paragraph
        assertEquals("Paragraph", paragraph.text.text)
    }

    @Test
    fun `parse markdown blocks keeps rule and paragraph after lists`() {
        val blocks = parseMarkdownBlocks(
            "- first item\n- second item\n\n---\n\nParagraph"
        )

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.ListBlock)
        assertTrue(blocks[1] is MarkdownBlock.HorizontalRule)
        assertTrue(blocks[2] is MarkdownBlock.Paragraph)
        assertEquals("Paragraph", (blocks[2] as MarkdownBlock.Paragraph).text.text)
    }

    @Test
    fun `parse markdown blocks keeps quote code block and table`() {
        val blocks = parseMarkdownBlocks(
            "> quoted line\n\n```kotlin\nval answer = 42\n```\n\n| Name | Value |\n| --- | --- |\n| foo | bar |"
        )

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Quote)
        assertTrue(blocks[1] is MarkdownBlock.CodeBlock)
        assertTrue(blocks[2] is MarkdownBlock.Table)

        val quote = blocks[0] as MarkdownBlock.Quote
        assertEquals("quoted line", quote.text.text)

        val code = blocks[1] as MarkdownBlock.CodeBlock
        assertEquals("val answer = 42", code.code)

        val table = blocks[2] as MarkdownBlock.Table
        assertEquals(listOf("Name", "Value"), table.headers.map { it.text })
        assertEquals(listOf(listOf("foo", "bar")), table.rows.map { row -> row.map { it.text } })
    }

    @Test
    fun `extract markdown code blocks returns copyable code payloads`() {
        val blocks = extractMarkdownCodeBlocks(
            "Before\n```kotlin\nval answer = 42\n```\nAfter\n```bash\necho ok\n```"
        )

        assertEquals(listOf("val answer = 42", "echo ok"), blocks)
    }

    @Test
    fun `highlight still works inside markdown inline content`() {
        val result = buildMentionAnnotatedString(
            "**Search** [search docs](https://openai.com) and @search-bot",
            highlightQuery = "search"
        )

        assertEquals("Search search docs and @search-bot", result.text)
        val highlightSpans = result.spanStyles.filter {
            it.item.background.alpha > 0f && it.item.fontWeight == FontWeight.Bold
        }
        assertTrue(highlightSpans.size >= 3)
    }
}
