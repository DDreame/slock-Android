package com.slock.app.ui.thread

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.slock.app.data.model.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThreadInboxComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testThread = ThreadItem(
        parentMessage = Message(id = "m1", channelId = "c1", content = "Test message", senderName = "Alice"),
        channelName = "general",
        threadChannelId = "tc1",
        replyCount = 3,
        lastActivity = "2026-04-17T12:00:00Z"
    )

    private fun stateWithThreads(
        tab: ThreadInboxTab = ThreadInboxTab.FOLLOWING
    ) = ThreadListUiState(
        threads = listOf(testThread),
        followingThreads = if (tab != ThreadInboxTab.DONE) listOf(testThread) else emptyList(),
        doneThreads = if (tab == ThreadInboxTab.DONE) listOf(testThread) else emptyList(),
        selectedTab = tab
    )

    @Test
    fun `Tab strip renders Following All Done tabs`() {
        composeTestRule.setContent {
            ThreadInboxTabStrip(
                selectedTab = ThreadInboxTab.FOLLOWING,
                onTabSelected = {}
            )
        }
        composeTestRule.onNodeWithText("Following").assertExists()
        composeTestRule.onNodeWithText("All").assertExists()
        composeTestRule.onNodeWithText("Done").assertExists()
    }

    @Test
    fun `Tab selection calls onTabSelected with correct tab`() {
        var selectedTab: ThreadInboxTab? = null
        composeTestRule.setContent {
            ThreadInboxTabStrip(
                selectedTab = ThreadInboxTab.FOLLOWING,
                onTabSelected = { selectedTab = it }
            )
        }
        composeTestRule.onNodeWithTag("tab_Done").performClick()
        composeTestRule.waitForIdle()
        assertEquals(ThreadInboxTab.DONE, selectedTab)
    }

    @Test
    fun `Following tab shows DONE and UNFOLLOW buttons`() {
        composeTestRule.setContent {
            ThreadListScreen(
                state = stateWithThreads(ThreadInboxTab.FOLLOWING),
                onThreadClick = { _, _, _ -> },
                onNavigateBack = {},
                showHeader = false
            )
        }
        composeTestRule.onNodeWithTag("markDoneButton").assertExists()
        composeTestRule.onNodeWithTag("unfollowButton").assertExists()
    }

    @Test
    fun `Done tab shows UNDO button instead of DONE`() {
        composeTestRule.setContent {
            ThreadListScreen(
                state = stateWithThreads(ThreadInboxTab.DONE),
                onThreadClick = { _, _, _ -> },
                onNavigateBack = {},
                showHeader = false
            )
        }
        composeTestRule.onNodeWithTag("undoDoneButton").assertExists()
    }

    @Test
    fun `Mark Done button calls onMarkDone with threadChannelId`() {
        var markedDoneId: String? = null
        composeTestRule.setContent {
            ThreadListScreen(
                state = stateWithThreads(ThreadInboxTab.FOLLOWING),
                onThreadClick = { _, _, _ -> },
                onNavigateBack = {},
                onMarkDone = { markedDoneId = it },
                showHeader = false
            )
        }
        composeTestRule.onNodeWithTag("markDoneButton").performClick()
        composeTestRule.waitForIdle()
        assertEquals("tc1", markedDoneId)
    }

    @Test
    fun `Undo Done button calls onUndoDone with threadChannelId`() {
        var undoneId: String? = null
        composeTestRule.setContent {
            ThreadListScreen(
                state = stateWithThreads(ThreadInboxTab.DONE),
                onThreadClick = { _, _, _ -> },
                onNavigateBack = {},
                onUndoDone = { undoneId = it },
                showHeader = false
            )
        }
        composeTestRule.onNodeWithTag("undoDoneButton").performClick()
        composeTestRule.waitForIdle()
        assertEquals("tc1", undoneId)
    }

    @Test
    fun `Unfollow button calls onUnfollow with threadChannelId`() {
        var unfollowedId: String? = null
        composeTestRule.setContent {
            ThreadListScreen(
                state = stateWithThreads(ThreadInboxTab.FOLLOWING),
                onThreadClick = { _, _, _ -> },
                onNavigateBack = {},
                onUnfollow = { unfollowedId = it },
                showHeader = false
            )
        }
        composeTestRule.onNodeWithTag("unfollowButton").performClick()
        composeTestRule.waitForIdle()
        assertEquals("tc1", unfollowedId)
    }

    @Test
    fun `Empty Following tab shows correct message`() {
        composeTestRule.setContent {
            ThreadListScreen(
                state = ThreadListUiState(selectedTab = ThreadInboxTab.FOLLOWING),
                onThreadClick = { _, _, _ -> },
                onNavigateBack = {},
                showHeader = false
            )
        }
        composeTestRule.onNodeWithText("No followed threads").assertExists()
    }

    @Test
    fun `Empty Done tab shows correct message`() {
        composeTestRule.setContent {
            ThreadListScreen(
                state = ThreadListUiState(selectedTab = ThreadInboxTab.DONE),
                onThreadClick = { _, _, _ -> },
                onNavigateBack = {},
                showHeader = false
            )
        }
        composeTestRule.onNodeWithText("No done threads").assertExists()
    }
}
