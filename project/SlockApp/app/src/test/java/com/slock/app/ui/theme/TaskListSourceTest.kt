package com.slock.app.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TaskListSourceTest {

    private val source: String = listOf(
        File("src/main/java/com/slock/app/ui/theme/MarkdownContent.kt"),
        File("app/src/main/java/com/slock/app/ui/theme/MarkdownContent.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `taskListRegex is defined for task list matching`() {
        assertTrue(
            "taskListRegex must be defined",
            source.contains("taskListRegex")
        )
    }

    @Test
    fun `MarkdownBlock sealed interface declares TaskListBlock`() {
        val sealedBlock = source.substringAfter("sealed interface MarkdownBlock")
            .substringBefore("private data class")
        assertTrue(
            "MarkdownBlock must declare TaskListBlock",
            sealedBlock.contains("TaskListBlock")
        )
    }

    @Test
    fun `TaskListBlock items carry checked boolean and text`() {
        val taskListBlock = source.substringAfter("data class TaskListBlock(")
            .substringBefore(") : MarkdownBlock")
        assertTrue(
            "TaskListBlock items must be List<Pair<Boolean, AnnotatedString>>",
            taskListBlock.contains("Pair<Boolean, AnnotatedString>")
        )
    }

    @Test
    fun `parseTaskList function exists and is called before parseList`() {
        val parseTextBlocksBody = source.substringAfter("fun parseTextBlocks(")
            .substringBefore("private fun parseTaskList(")
        assertTrue(
            "parseTaskList must be called in parseTextBlocks",
            parseTextBlocksBody.contains("parseTaskList(lines, index, highlightQuery)")
        )
    }

    @Test
    fun `MarkdownBlockView renders TaskListBlock branch`() {
        val blockViewBody = source.substringAfter("fun MarkdownBlockView(")
            .substringBefore("@Composable\nprivate fun MarkdownAnnotatedText(")
        assertTrue(
            "MarkdownBlockView must handle TaskListBlock",
            blockViewBody.contains("is MarkdownBlock.TaskListBlock")
        )
    }

    @Test
    fun `TaskListBlock renderer uses checkbox symbols`() {
        val blockViewBody = source.substringAfter("is MarkdownBlock.TaskListBlock")
            .substringBefore("MarkdownBlock.HorizontalRule")
        assertTrue(
            "TaskListBlock renderer must use checkbox symbols",
            blockViewBody.contains("☑") && blockViewBody.contains("☐")
        )
    }

    @Test
    fun `isListLine recognizes task list lines`() {
        val isListLineBody = source.substringAfter("fun isListLine(")
            .substringBefore("}")
        assertTrue(
            "isListLine must check taskListRegex",
            isListLineBody.contains("taskListRegex")
        )
    }
}
