package com.slock.app.ui.thread

import com.slock.app.data.model.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThreadReplyPaginationStateTest {

    @Test
    fun `ThreadReplyUiState has isLoadingMore field defaulting to false`() {
        val state = ThreadReplyUiState()
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `ThreadReplyUiState has hasMoreReplies field defaulting to true`() {
        val state = ThreadReplyUiState()
        assertTrue(state.hasMoreReplies)
    }

    @Test
    fun `isLoadingMore can be set independently of isLoading`() {
        val state = ThreadReplyUiState(isLoading = false, isLoadingMore = true)
        assertFalse(state.isLoading)
        assertTrue(state.isLoadingMore)
    }

    @Test
    fun `hasMoreReplies false stops further loading`() {
        val state = ThreadReplyUiState(hasMoreReplies = false, isLoadingMore = false)
        assertFalse(state.hasMoreReplies)
    }

    @Test
    fun `pagination state preserved through copy with new replies`() {
        val initial = ThreadReplyUiState(
            replies = listOf(Message(id = "r1", seq = 10)),
            hasMoreReplies = true,
            isLoadingMore = true
        )
        val updated = initial.copy(
            replies = initial.replies + Message(id = "r2", seq = 5),
            isLoadingMore = false,
            hasMoreReplies = false
        )
        assertEquals(2, updated.replies.size)
        assertFalse(updated.isLoadingMore)
        assertFalse(updated.hasMoreReplies)
    }
}

class ThreadReplyPaginationStructuralTest {

    private val vmSource: String = listOf(
        File("src/main/java/com/slock/app/ui/thread/ThreadReplyViewModel.kt"),
        File("app/src/main/java/com/slock/app/ui/thread/ThreadReplyViewModel.kt")
    ).first { it.exists() }.readText()

    private val screenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/thread/ThreadReplyScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/thread/ThreadReplyScreen.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `loadMoreReplies guards against double fetch`() {
        val loadMoreBlock = vmSource
            .substringAfter("fun loadMoreReplies()")
            .substringBefore("fun sendReply(")
            .ifEmpty {
                vmSource.substringAfter("fun loadMoreReplies()")
                    .substringBefore("override fun onCleared")
            }
        assertTrue(
            "loadMoreReplies must check isLoadingMore to prevent double fetch",
            loadMoreBlock.contains("isLoadingMore")
        )
        assertTrue(
            "loadMoreReplies must check hasMoreReplies to stop at end",
            loadMoreBlock.contains("hasMoreReplies")
        )
    }

    @Test
    fun `loadMoreReplies uses before cursor from oldest reply seq`() {
        val loadMoreBlock = vmSource
            .substringAfter("fun loadMoreReplies()")
            .substringBefore("override fun onCleared")
        assertTrue(
            "loadMoreReplies must use seq as before cursor",
            loadMoreBlock.contains(".seq.toString()")
        )
    }

    @Test
    fun `loadMoreReplies calls threadRepository getThreadReplies`() {
        val loadMoreBlock = vmSource
            .substringAfter("fun loadMoreReplies()")
            .substringBefore("override fun onCleared")
        assertTrue(
            "loadMoreReplies must call threadRepository.getThreadReplies for pagination",
            loadMoreBlock.contains("threadRepository.getThreadReplies(")
        )
    }

    @Test
    fun `loadMoreReplies deduplicates replies`() {
        val loadMoreBlock = vmSource
            .substringAfter("fun loadMoreReplies()")
            .substringBefore("override fun onCleared")
        assertTrue(
            "loadMoreReplies must filter out existing reply IDs",
            loadMoreBlock.contains("existingIds") || loadMoreBlock.contains("!in")
        )
    }

    @Test
    fun `screen uses rememberLazyListState`() {
        assertTrue(
            "ThreadReplyScreen must use rememberLazyListState for scroll tracking",
            screenSource.contains("rememberLazyListState()")
        )
    }

    @Test
    fun `screen has scroll detection with derivedStateOf`() {
        assertTrue(
            "ThreadReplyScreen must use derivedStateOf for scroll detection",
            screenSource.contains("derivedStateOf")
        )
    }

    @Test
    fun `screen triggers onLoadMore via LaunchedEffect`() {
        val scrollBlock = screenSource
            .substringAfter("shouldLoadMore")
            .substringBefore("LazyColumn")
        assertTrue(
            "ThreadReplyScreen must call onLoadMore() when scroll threshold reached",
            scrollBlock.contains("onLoadMore()")
        )
    }

    @Test
    fun `screen shows loading indicator when isLoadingMore`() {
        assertTrue(
            "ThreadReplyScreen must show CircularProgressIndicator when loading more",
            screenSource.contains("state.isLoadingMore") && screenSource.contains("CircularProgressIndicator")
        )
    }
}
