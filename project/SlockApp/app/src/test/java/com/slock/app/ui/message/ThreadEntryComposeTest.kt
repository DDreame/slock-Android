package com.slock.app.ui.message

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.slock.app.data.model.Message
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThreadEntryComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `clicking message with thread navigates to thread`() {
        var navigatedThreadId: String? = null
        val threadMessage = Message(
            id = "msg-1",
            channelId = "ch-1",
            content = "Hello thread",
            senderName = "Alice",
            senderType = "human",
            createdAt = "2026-04-18T00:00:00Z",
            threadChannelId = "thread-ch-1",
            replyCount = 3
        )
        composeTestRule.setContent {
            MessageListScreen(
                channelName = "test-channel",
                state = MessageUiState(
                    messages = listOf(threadMessage)
                ),
                onSendMessage = {},
                onLoadMore = {},
                onRetryLoad = {},
                onNavigateBack = {},
                onNavigateToThread = { threadId, _ -> navigatedThreadId = threadId }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hello thread", substring = true).performClick()
        composeTestRule.waitForIdle()
        assertEquals("thread-ch-1", navigatedThreadId)
    }

    @Test
    fun `clicking message without thread does not navigate`() {
        var navigated = false
        val normalMessage = Message(
            id = "msg-2",
            channelId = "ch-1",
            content = "Normal message",
            senderName = "Bob",
            senderType = "human",
            createdAt = "2026-04-18T00:00:00Z",
            threadChannelId = null,
            replyCount = 0
        )
        composeTestRule.setContent {
            MessageListScreen(
                channelName = "test-channel",
                state = MessageUiState(
                    messages = listOf(normalMessage)
                ),
                onSendMessage = {},
                onLoadMore = {},
                onRetryLoad = {},
                onNavigateBack = {},
                onNavigateToThread = { _, _ -> navigated = true }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Normal message", substring = true).performClick()
        composeTestRule.waitForIdle()
        assertFalse("Clicking message without thread should not navigate", navigated)
    }
}
