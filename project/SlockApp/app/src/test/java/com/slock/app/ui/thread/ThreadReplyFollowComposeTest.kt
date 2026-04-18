package com.slock.app.ui.thread

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThreadReplyFollowComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `Unfollowed thread shows FOLLOW button and clicking calls onFollowThread`() {
        var followCalled = false
        composeTestRule.setContent {
            ThreadReplyScreen(
                state = ThreadReplyUiState(isFollowing = false),
                onSendReply = {},
                onLoadMore = {},
                onNavigateBack = {},
                onFollowThread = { followCalled = true },
                onUnfollowThread = {},
                onMarkDone = {}
            )
        }
        composeTestRule.onNodeWithText("\uD83D\uDD14 FOLLOW").assertExists()
        composeTestRule.onNodeWithText("\uD83D\uDD14 FOLLOW").performClick()
        composeTestRule.waitForIdle()
        assertTrue("onFollowThread must be called when clicking FOLLOW", followCalled)
    }

    @Test
    fun `Followed thread shows FOLLOWING button and clicking calls onUnfollowThread`() {
        var unfollowCalled = false
        composeTestRule.setContent {
            ThreadReplyScreen(
                state = ThreadReplyUiState(isFollowing = true),
                onSendReply = {},
                onLoadMore = {},
                onNavigateBack = {},
                onFollowThread = {},
                onUnfollowThread = { unfollowCalled = true },
                onMarkDone = {}
            )
        }
        composeTestRule.onNodeWithText("\uD83D\uDD14 FOLLOWING").assertExists()
        composeTestRule.onNodeWithText("\uD83D\uDD14 FOLLOWING").performClick()
        composeTestRule.waitForIdle()
        assertTrue("onUnfollowThread must be called when clicking FOLLOWING", unfollowCalled)
    }

    @Test
    fun `Done button in ParticipantBar calls onMarkDone`() {
        var doneCalled = false
        composeTestRule.setContent {
            ThreadReplyScreen(
                state = ThreadReplyUiState(isFollowing = true),
                onSendReply = {},
                onLoadMore = {},
                onNavigateBack = {},
                onFollowThread = {},
                onUnfollowThread = {},
                onMarkDone = { doneCalled = true }
            )
        }
        composeTestRule.onNodeWithText("\u2713 DONE").assertExists()
        composeTestRule.onNodeWithText("\u2713 DONE").performClick()
        composeTestRule.waitForIdle()
        assertTrue("onMarkDone must be called when clicking DONE", doneCalled)
    }
}
