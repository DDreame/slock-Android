package com.slock.app.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FootnoteSourceTest {

    private val source: String = listOf(
        File("src/main/java/com/slock/app/ui/theme/MarkdownContent.kt"),
        File("app/src/main/java/com/slock/app/ui/theme/MarkdownContent.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `MarkdownBlock sealed interface declares Footnotes block`() {
        val sealedBlock = source.substringAfter("sealed interface MarkdownBlock")
            .substringBefore("private data class")
        assertTrue(
            "MarkdownBlock must declare Footnotes",
            sealedBlock.contains("data class Footnotes")
        )
    }

    @Test
    fun `footnote definition regex is defined`() {
        assertTrue(
            "footnoteDefinitionRegex must be defined",
            source.contains("footnoteDefinitionRegex")
        )
    }

    @Test
    fun `parseTextBlocks calls parseFootnotes before paragraph fallback`() {
        val parseTextBlocksBody = source.substringAfter("fun parseTextBlocks(")
            .substringBefore("private fun parseTaskList(")
        assertTrue(
            "parseTextBlocks must call parseFootnotes",
            parseTextBlocksBody.contains("parseFootnotes(lines, index, highlightQuery)")
        )
    }

    @Test
    fun `MarkdownBlockView renders Footnotes branch`() {
        val blockViewBody = source.substringAfter("fun MarkdownBlockView(")
            .substringBefore("@Composable\nprivate fun MarkdownAnnotatedText(")
        assertTrue(
            "MarkdownBlockView must handle Footnotes",
            blockViewBody.contains("is MarkdownBlock.Footnotes")
        )
        assertTrue(
            "Footnotes renderer must label the block clearly",
            blockViewBody.contains("FOOTNOTES")
        )
    }

    @Test
    fun `inline parser has dedicated footnote reference parser`() {
        val inlineBody = source.substringAfter("fun AnnotatedString.Builder.appendMarkdownInline(")
            .substringBefore("private fun findNextTokenIndex(")
        assertTrue(
            "appendMarkdownInline must check parseFootnoteReferenceAt",
            inlineBody.contains("parseFootnoteReferenceAt(content, index)")
        )
    }
}
