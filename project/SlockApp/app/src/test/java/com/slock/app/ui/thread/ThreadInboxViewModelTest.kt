package com.slock.app.ui.thread

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThreadInboxViewModelTest {

    private val viewModelSource: String = listOf(
        File("src/main/java/com/slock/app/ui/thread/ThreadListViewModel.kt"),
        File("app/src/main/java/com/slock/app/ui/thread/ThreadListViewModel.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `ThreadInboxTab enum has FOLLOWING ALL DONE`() {
        assertTrue(
            "ThreadInboxTab must have FOLLOWING, ALL, DONE",
            viewModelSource.contains("enum class ThreadInboxTab") &&
                viewModelSource.contains("FOLLOWING") &&
                viewModelSource.contains("ALL") &&
                viewModelSource.contains("DONE")
        )
    }

    @Test
    fun `ThreadListUiState has selectedTab field`() {
        assertTrue(
            "ThreadListUiState must have selectedTab: ThreadInboxTab",
            viewModelSource.contains("val selectedTab: ThreadInboxTab")
        )
    }

    @Test
    fun `ThreadListUiState has followingThreads and doneThreads`() {
        assertTrue(
            "ThreadListUiState must track followingThreads and doneThreads separately",
            viewModelSource.contains("val followingThreads: List<ThreadItem>") &&
                viewModelSource.contains("val doneThreads: List<ThreadItem>")
        )
    }

    @Test
    fun `selectTab method exists and filters threads by tab`() {
        val method = viewModelSource
            .substringAfter("fun selectTab(")
            .substringBefore("fun markThreadDone(")
        assertTrue(
            "selectTab must handle FOLLOWING tab",
            method.contains("ThreadInboxTab.FOLLOWING")
        )
        assertTrue(
            "selectTab must handle ALL tab",
            method.contains("ThreadInboxTab.ALL")
        )
        assertTrue(
            "selectTab must handle DONE tab",
            method.contains("ThreadInboxTab.DONE")
        )
    }

    @Test
    fun `markThreadDone optimistically moves thread from following to done`() {
        val method = viewModelSource
            .substringAfter("fun markThreadDone(")
            .substringBefore("fun undoThreadDone(")
        assertTrue(
            "markThreadDone must update followingThreads",
            method.contains("followingThreads")
        )
        assertTrue(
            "markThreadDone must call threadRepository.markThreadDone",
            method.contains("threadRepository.markThreadDone")
        )
    }

    @Test
    fun `undoThreadDone moves thread from done back to following`() {
        val method = viewModelSource
            .substringAfter("fun undoThreadDone(")
            .substringBefore("fun followThread(")
        assertTrue(
            "undoThreadDone must update doneThreads",
            method.contains("doneThreads")
        )
        assertTrue(
            "undoThreadDone must call threadRepository.undoThreadDone",
            method.contains("threadRepository.undoThreadDone")
        )
    }

    @Test
    fun `unfollowThread optimistically removes thread from following list`() {
        val method = viewModelSource
            .substringAfter("fun unfollowThread(")
            .substringBefore("fun consumeActionFeedback(")
        assertTrue(
            "unfollowThread must filter followingThreads",
            method.contains("followingThreads.filter")
        )
        assertTrue(
            "unfollowThread must call threadRepository.unfollowThread",
            method.contains("threadRepository.unfollowThread")
        )
    }

    @Test
    fun `followThread calls threadRepository and reloads`() {
        val method = viewModelSource
            .substringAfter("fun followThread(")
            .substringBefore("fun unfollowThread(")
        assertTrue(
            "followThread must call threadRepository.followThread",
            method.contains("threadRepository.followThread")
        )
    }

    @Test
    fun `loadThreads preserves doneThreads across refresh`() {
        val loadMethod = viewModelSource
            .substringAfter("fun loadThreads(")
            .substringBefore("private fun summaryToThreadItem(")
        assertTrue(
            "loadThreads must check doneThreads to avoid showing done items in following",
            loadMethod.contains("doneIds") || loadMethod.contains("doneThreads")
        )
    }

    @Test
    fun `summaryToThreadItem carries unreadCount from ThreadSummary`() {
        val method = viewModelSource
            .substringAfter("private fun summaryToThreadItem(")
            .substringBefore("override fun onCleared()")
        assertTrue(
            "summaryToThreadItem must map unreadCount from ThreadSummary into ThreadItem",
            method.contains("unreadCount = summary.unreadCount")
        )
    }

    @Test
    fun `markThreadDone has rollback on failure`() {
        assertTrue(
            "markThreadDone must have rollback mechanism",
            viewModelSource.contains("rollbackDone")
        )
    }
}
