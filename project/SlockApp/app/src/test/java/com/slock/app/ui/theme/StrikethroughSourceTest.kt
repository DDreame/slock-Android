package com.slock.app.ui.theme

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class StrikethroughSourceTest {

    private val source: String = listOf(
        File("src/main/java/com/slock/app/ui/theme/MarkdownContent.kt"),
        File("app/src/main/java/com/slock/app/ui/theme/MarkdownContent.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `MarkdownInlineContext has strikethrough field`() {
        val contextClass = source.substringAfter("data class MarkdownInlineContext(")
            .substringBefore(")")
        assertTrue(
            "MarkdownInlineContext must have strikethrough field",
            contextClass.contains("strikethrough")
        )
    }

    @Test
    fun `spanStyle uses TextDecoration LineThrough for strikethrough`() {
        val spanStyleBody = source.substringAfter("fun spanStyle(")
            .substringBefore("}")
        assertTrue(
            "spanStyle must use TextDecoration.LineThrough",
            spanStyleBody.contains("LineThrough")
        )
    }

    @Test
    fun `tilde is in markdownTokenChars`() {
        val tokenChars = source.substringAfter("markdownTokenChars = setOf(")
            .substringBefore(")")
        assertTrue(
            "markdownTokenChars must include '~'",
            tokenChars.contains("'~'")
        )
    }

    @Test
    fun `appendMarkdownInline parses double tilde delimiter`() {
        val inlineBody = source.substringAfter("fun AnnotatedString.Builder.appendMarkdownInline(")
            .substringBefore("private fun findNextTokenIndex")
        assertTrue(
            "appendMarkdownInline must parse ~~ delimiter",
            inlineBody.contains("parseDelimited(content, index, \"~~\")")
        )
    }

    @Test
    fun `strikethrough context is passed when parsing double tilde`() {
        val inlineBody = source.substringAfter("fun AnnotatedString.Builder.appendMarkdownInline(")
            .substringBefore("private fun findNextTokenIndex")
        assertTrue(
            "strikethrough context must be set when parsing ~~",
            inlineBody.contains("context.copy(strikethrough = true)")
        )
    }
}
