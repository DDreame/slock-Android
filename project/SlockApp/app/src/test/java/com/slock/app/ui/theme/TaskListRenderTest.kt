package com.slock.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskListRenderTest {

    @Test
    fun `unchecked task list item parses as TaskListBlock with false`() {
        val blocks = parseMarkdownBlocks("- [ ] buy milk")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TaskListBlock)

        val taskList = blocks[0] as MarkdownBlock.TaskListBlock
        assertEquals(1, taskList.items.size)
        assertFalse(taskList.items[0].first)
        assertEquals("buy milk", taskList.items[0].second.text)
    }

    @Test
    fun `checked task list item parses as TaskListBlock with true`() {
        val blocks = parseMarkdownBlocks("- [x] buy milk")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TaskListBlock)

        val taskList = blocks[0] as MarkdownBlock.TaskListBlock
        assertEquals(1, taskList.items.size)
        assertTrue(taskList.items[0].first)
        assertEquals("buy milk", taskList.items[0].second.text)
    }

    @Test
    fun `uppercase X is also recognized as checked`() {
        val blocks = parseMarkdownBlocks("- [X] done item")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TaskListBlock)

        val taskList = blocks[0] as MarkdownBlock.TaskListBlock
        assertTrue(taskList.items[0].first)
    }

    @Test
    fun `mixed checked and unchecked items in one block`() {
        val blocks = parseMarkdownBlocks("- [ ] todo\n- [x] done\n- [ ] another todo")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TaskListBlock)

        val taskList = blocks[0] as MarkdownBlock.TaskListBlock
        assertEquals(3, taskList.items.size)
        assertFalse(taskList.items[0].first)
        assertEquals("todo", taskList.items[0].second.text)
        assertTrue(taskList.items[1].first)
        assertEquals("done", taskList.items[1].second.text)
        assertFalse(taskList.items[2].first)
        assertEquals("another todo", taskList.items[2].second.text)
    }

    @Test
    fun `regular unordered list is not misidentified as task list`() {
        val blocks = parseMarkdownBlocks("- regular item\n- another item")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.ListBlock)
        assertFalse(blocks[0] is MarkdownBlock.TaskListBlock)
    }

    @Test
    fun `task list with inline markdown preserves formatting`() {
        val blocks = parseMarkdownBlocks("- [ ] **bold task**")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TaskListBlock)

        val taskList = blocks[0] as MarkdownBlock.TaskListBlock
        assertEquals("bold task", taskList.items[0].second.text)
    }

    @Test
    fun `task list followed by paragraph produces separate blocks`() {
        val blocks = parseMarkdownBlocks("- [ ] task one\n- [x] task two\n\nSome paragraph")
        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TaskListBlock)
        assertTrue(blocks[1] is MarkdownBlock.Paragraph)
    }

    @Test
    fun `star marker task list also works`() {
        val blocks = parseMarkdownBlocks("* [ ] star task\n* [x] star done")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.TaskListBlock)

        val taskList = blocks[0] as MarkdownBlock.TaskListBlock
        assertEquals(2, taskList.items.size)
        assertFalse(taskList.items[0].first)
        assertTrue(taskList.items[1].first)
    }
}
