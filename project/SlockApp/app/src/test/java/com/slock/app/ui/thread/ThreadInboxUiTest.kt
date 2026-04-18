package com.slock.app.ui.thread

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThreadInboxUiTest {

    private val screenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/thread/ThreadListScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/thread/ThreadListScreen.kt")
    ).first { it.exists() }.readText()

    private val replyScreenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/thread/ThreadReplyScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/thread/ThreadReplyScreen.kt")
    ).first { it.exists() }.readText()

    private val replyViewModelSource: String = listOf(
        File("src/main/java/com/slock/app/ui/thread/ThreadReplyViewModel.kt"),
        File("app/src/main/java/com/slock/app/ui/thread/ThreadReplyViewModel.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `ThreadListScreen has onTabSelected callback`() {
        assertTrue(
            "ThreadListScreen must accept onTabSelected callback",
            screenSource.contains("onTabSelected: (ThreadInboxTab) -> Unit")
        )
    }

    @Test
    fun `ThreadListScreen has onMarkDone callback`() {
        assertTrue(
            "ThreadListScreen must accept onMarkDone callback",
            screenSource.contains("onMarkDone: (threadChannelId: String) -> Unit")
        )
    }

    @Test
    fun `ThreadListScreen has onUndoDone callback`() {
        assertTrue(
            "ThreadListScreen must accept onUndoDone callback",
            screenSource.contains("onUndoDone: (threadChannelId: String) -> Unit")
        )
    }

    @Test
    fun `ThreadListScreen has onUnfollow callback`() {
        assertTrue(
            "ThreadListScreen must accept onUnfollow callback",
            screenSource.contains("onUnfollow: (threadChannelId: String) -> Unit")
        )
    }

    @Test
    fun `ThreadInboxTabStrip composable renders three tabs`() {
        assertTrue(
            "ThreadInboxTabStrip must render Following All Done tabs",
            screenSource.contains("ThreadInboxTabStrip") &&
                screenSource.contains("\"Following\"") &&
                screenSource.contains("\"All\"") &&
                screenSource.contains("\"Done\"")
        )
    }

    @Test
    fun `ThreadInboxTabStrip has testTag for tab strip`() {
        assertTrue(
            "ThreadInboxTabStrip must have testTag threadInboxTabStrip",
            screenSource.contains("testTag(\"threadInboxTabStrip\")")
        )
    }

    @Test
    fun `ThreadCard shows markDoneButton on non-done tabs`() {
        assertTrue(
            "ThreadCard must have markDoneButton testTag",
            screenSource.contains("testTag(\"markDoneButton\")")
        )
    }

    @Test
    fun `ThreadCard shows undoDoneButton on done tab`() {
        assertTrue(
            "ThreadCard must have undoDoneButton testTag",
            screenSource.contains("testTag(\"undoDoneButton\")")
        )
    }

    @Test
    fun `ThreadCard shows unfollowButton on non-done tabs`() {
        assertTrue(
            "ThreadCard must have unfollowButton testTag",
            screenSource.contains("testTag(\"unfollowButton\")")
        )
    }

    @Test
    fun `ThreadCard shows reply count and unread indicator chips`() {
        assertTrue(
            "ThreadCard must render reply count and unread indicator chips",
            screenSource.contains("threadReplyCountChip") &&
                screenSource.contains("threadUnreadBadge") &&
                screenSource.contains("thread.replyCount") &&
                screenSource.contains("thread.unreadCount > 0")
        )
    }

    @Test
    fun `Empty state varies by tab`() {
        assertTrue(
            "Empty state must show tab-specific messages",
            screenSource.contains("No followed threads") &&
                screenSource.contains("No done threads")
        )
    }

    @Test
    fun `Done tab accent bar uses Lime color`() {
        assertTrue(
            "Done tab thread cards should use Lime accent",
            screenSource.contains("if (isDoneTab) Lime else Lavender")
        )
    }

    @Test
    fun `ThreadReplyScreen has onFollowThread callback`() {
        assertTrue(
            "ThreadReplyScreen must accept onFollowThread callback",
            replyScreenSource.contains("onFollowThread: () -> Unit")
        )
    }

    @Test
    fun `ThreadReplyScreen has onUnfollowThread callback`() {
        assertTrue(
            "ThreadReplyScreen must accept onUnfollowThread callback",
            replyScreenSource.contains("onUnfollowThread: () -> Unit")
        )
    }

    @Test
    fun `ThreadReplyScreen has onMarkDone callback`() {
        assertTrue(
            "ThreadReplyScreen must accept onMarkDone callback",
            replyScreenSource.contains("onMarkDone: () -> Unit")
        )
    }

    @Test
    fun `ParticipantBar has Done button`() {
        assertTrue(
            "ParticipantBar must show a Done button",
            replyScreenSource.contains("DONE") && replyScreenSource.contains("onMarkDone")
        )
    }

    @Test
    fun `ParticipantBar follow toggle calls API callbacks`() {
        assertTrue(
            "Follow toggle must call onFollowThread or onUnfollowThread",
            replyScreenSource.contains("onUnfollowThread()") &&
                replyScreenSource.contains("onFollowThread()")
        )
    }

    @Test
    fun `ThreadReplyUiState has isFollowing field`() {
        assertTrue(
            "ThreadReplyUiState must have isFollowing: Boolean",
            replyViewModelSource.contains("val isFollowing: Boolean")
        )
    }

    @Test
    fun `ThreadReplyViewModel loadThread checks followed threads for initial state`() {
        val loadMethod = replyViewModelSource
            .substringAfter("fun loadThread(")
            .substringBefore("fun sendReply(")
        assertTrue(
            "loadThread must check getFollowedThreads to determine isFollowing",
            loadMethod.contains("getFollowedThreads") && loadMethod.contains("isFollowing")
        )
    }

    @Test
    fun `ThreadReplyViewModel followThread updates isFollowing optimistically`() {
        val method = replyViewModelSource
            .substringAfter("fun followThread()")
            .substringBefore("fun unfollowThread()")
        assertTrue(
            "followThread must set isFollowing = true optimistically",
            method.contains("isFollowing = true")
        )
    }

    @Test
    fun `ThreadReplyViewModel unfollowThread updates isFollowing optimistically`() {
        val method = replyViewModelSource
            .substringAfter("fun unfollowThread()")
            .substringBefore("fun markThreadDone()")
        assertTrue(
            "unfollowThread must set isFollowing = false optimistically",
            method.contains("isFollowing = false")
        )
    }

    @Test
    fun `ThreadReplyScreen uses state isFollowing not local variable`() {
        assertTrue(
            "ThreadReplyScreen must use state.isFollowing for ParticipantBar",
            replyScreenSource.contains("state.isFollowing")
        )
    }

    @Test
    fun `ThreadReplyViewModel has followThread method`() {
        assertTrue(
            "ThreadReplyViewModel must have followThread calling repository",
            replyViewModelSource.contains("fun followThread()") &&
                replyViewModelSource.contains("threadRepository.followThread")
        )
    }

    @Test
    fun `ThreadReplyViewModel has unfollowThread method`() {
        assertTrue(
            "ThreadReplyViewModel must have unfollowThread calling repository",
            replyViewModelSource.contains("fun unfollowThread()") &&
                replyViewModelSource.contains("threadRepository.unfollowThread")
        )
    }

    @Test
    fun `ThreadReplyViewModel has markThreadDone method`() {
        assertTrue(
            "ThreadReplyViewModel must have markThreadDone calling repository",
            replyViewModelSource.contains("fun markThreadDone()") &&
                replyViewModelSource.contains("threadRepository.markThreadDone")
        )
    }
}
