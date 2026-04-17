package com.slock.app.ui.message

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.slock.app.ui.theme.MarkdownBlock
import com.slock.app.ui.theme.parseMarkdownBlocks
import org.junit.Assert.*
import org.junit.Test

class SystemMessageRenderingTest {

    @Test
    fun `system message with bold renders through markdown path`() {
        val blocks = parseMarkdownBlocks("**User joined** the channel")
        assertTrue("Markdown parser must produce blocks for system message content", blocks.isNotEmpty())
        val paragraph = blocks.first() as MarkdownBlock.Paragraph
        val boldSpans = paragraph.text.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue("Bold text in system message must be parsed as bold span", boldSpans.isNotEmpty())
    }

    @Test
    fun `system message with italic renders through markdown path`() {
        val blocks = parseMarkdownBlocks("Channel *renamed* to #general")
        val paragraph = blocks.first() as MarkdownBlock.Paragraph
        val italicSpans = paragraph.text.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertTrue("Italic text in system message must be parsed as italic span", italicSpans.isNotEmpty())
    }

    @Test
    fun `system message with link renders through markdown path`() {
        val blocks = parseMarkdownBlocks("See [documentation](https://example.com) for details")
        val paragraph = blocks.first() as MarkdownBlock.Paragraph
        assertTrue(
            "Link text in system message must be parsed with annotation",
            paragraph.text.text.contains("documentation")
        )
    }

    @Test
    fun `plain system message renders as paragraph block`() {
        val blocks = parseMarkdownBlocks("User left the channel")
        assertEquals("Plain system message must produce exactly one paragraph block", 1, blocks.size)
        assertTrue("Block must be a Paragraph", blocks.first() is MarkdownBlock.Paragraph)
        assertEquals("User left the channel", (blocks.first() as MarkdownBlock.Paragraph).text.text)
    }

    @Test
    fun `system message with heading renders as heading block`() {
        val blocks = parseMarkdownBlocks("# Channel Created")
        assertTrue("Heading in system message must be parsed as Heading block", blocks.any { it is MarkdownBlock.Heading })
    }

    @Test
    fun `empty system message produces no blocks`() {
        val blocks = parseMarkdownBlocks("")
        assertTrue("Empty content must produce no blocks", blocks.isEmpty())
    }

    @Test
    fun `message body text goes through same parseMarkdownBlocks path as system messages`() {
        val regularContent = "Hello **world** with `code` and *italic*"
        val blocks = parseMarkdownBlocks(regularContent)
        assertTrue("Regular message content must produce blocks", blocks.isNotEmpty())
        val paragraph = blocks.first() as MarkdownBlock.Paragraph
        assertTrue(
            "Regular message bold must be parsed",
            paragraph.text.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        )
    }
}
