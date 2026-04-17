package com.slock.app.ui.message

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TypographyRegressionTest {

    private fun readSource(vararg candidates: String): String {
        return candidates.map { File(it) }.first { it.exists() }.readText()
    }

    private val messageListSource = readSource(
        "src/main/java/com/slock/app/ui/message/MessageListScreen.kt",
        "app/src/main/java/com/slock/app/ui/message/MessageListScreen.kt"
    )

    private val threadReplySource = readSource(
        "src/main/java/com/slock/app/ui/thread/ThreadReplyScreen.kt",
        "app/src/main/java/com/slock/app/ui/thread/ThreadReplyScreen.kt"
    )

    private val markdownSource = readSource(
        "src/main/java/com/slock/app/ui/theme/MarkdownContent.kt",
        "app/src/main/java/com/slock/app/ui/theme/MarkdownContent.kt"
    )

    @Test
    fun `AGENT badges use labelSmall not hardcoded 9sp`() {
        val agentBadgePattern = Regex("""text\s*=\s*"AGENT"[^}]*fontSize\s*=\s*9\.sp""", RegexOption.DOT_MATCHES_ALL)
        assertFalse(
            "MessageListScreen AGENT badges must not use hardcoded 9.sp",
            agentBadgePattern.containsMatchIn(messageListSource)
        )
        assertFalse(
            "ThreadReplyScreen AGENT badges must not use hardcoded 9.sp",
            agentBadgePattern.containsMatchIn(threadReplySource)
        )
    }

    @Test
    fun `timestamps use bodySmall not hardcoded 10sp with low alpha`() {
        val hardcodedTimestamp = Regex("""fontSize\s*=\s*10\.sp[^}]*alpha\s*=\s*0\.4f""", RegexOption.DOT_MATCHES_ALL)
        assertFalse(
            "MessageListScreen timestamps must not use hardcoded 10.sp + 0.4f alpha",
            hardcodedTimestamp.containsMatchIn(messageListSource)
        )
        assertFalse(
            "ThreadReplyScreen timestamps must not use hardcoded 10.sp + 0.4f alpha",
            hardcodedTimestamp.containsMatchIn(threadReplySource)
        )
    }

    @Test
    fun `timestamps use TextSecondary color`() {
        val timestampBlocks = extractTimestampBlocks(messageListSource) + extractTimestampBlocks(threadReplySource)
        assertTrue(
            "At least one timestamp block should exist",
            timestampBlocks.isNotEmpty()
        )
        timestampBlocks.forEach { block ->
            assertTrue(
                "Timestamp must use TextSecondary (directly or via MessageTextStyles), found: ${block.take(100)}",
                block.contains("TextSecondary") || block.contains("MessageTextStyles")
            )
        }
    }

    @Test
    fun `message body uses bodyLarge not bodyMedium in markdown paragraphs`() {
        val paragraphBlock = markdownSource
            .substringAfter("MarkdownParagraph", "")
        if (paragraphBlock.isNotEmpty()) {
            assertFalse(
                "Markdown paragraphs should not use bodyMedium for body text",
                paragraphBlock.substringBefore("@Composable").contains("bodyMedium.copy(lineHeight")
            )
        }
    }

    @Test
    fun `SystemMessageDivider uses NeoMessageContent not plain Text`() {
        val sysBlock = messageListSource
            .substringAfter("fun SystemMessageDivider(")
            .substringBefore("@Composable")
        assertTrue(
            "SystemMessageDivider must route through NeoMessageContent for rich text rendering",
            sysBlock.contains("NeoMessageContent(")
        )
        assertFalse(
            "SystemMessageDivider must not use plain Text for content",
            sysBlock.contains("Text(\n") || sysBlock.matches(Regex(""".*\bText\(\s*\n\s*text\s*=\s*content.*""", RegexOption.DOT_MATCHES_ALL))
        )
    }

    @Test
    fun `SystemMessageDivider passes TextSecondary as textColor to NeoMessageContent`() {
        val sysBlock = messageListSource
            .substringAfter("fun SystemMessageDivider(")
            .substringBefore("@Composable")
        assertTrue(
            "SystemMessageDivider must pass TextSecondary as textColor",
            sysBlock.contains("textColor = TextSecondary")
        )
    }

    @Test
    fun `no hardcoded 13sp in markdown content`() {
        assertFalse(
            "MarkdownContent must not use hardcoded 13.sp",
            markdownSource.contains("13.sp")
        )
    }

    @Test
    fun `timestamps use MessageTextStyles not inline style construction`() {
        val timestampBlocks = extractTimestampBlocks(messageListSource) + extractTimestampBlocks(threadReplySource)
        assertTrue("At least one timestamp block should exist", timestampBlocks.isNotEmpty())
        timestampBlocks.forEach { block ->
            assertTrue(
                "Timestamp must use MessageTextStyles.timestampStyle, found: ${block.take(120)}",
                block.contains("MessageTextStyles")
            )
        }
    }

    @Test
    fun `markdown paragraph and list use MessageTextStyles bodyStyle`() {
        assertTrue(
            "MarkdownContent paragraphs must use MessageTextStyles.bodyStyle",
            markdownSource.contains("MessageTextStyles.bodyStyle")
        )
    }

    private fun extractTimestampBlocks(source: String): List<String> {
        val pattern = Regex("""split\("T"\)[^}]*\}""", RegexOption.DOT_MATCHES_ALL)
        return pattern.findAll(source).map { it.value }.toList()
    }
}
