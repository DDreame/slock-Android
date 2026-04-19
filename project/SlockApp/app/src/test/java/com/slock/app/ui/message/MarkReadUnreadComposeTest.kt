package com.slock.app.ui.message

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.slock.app.data.model.Message
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MarkReadUnreadComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testMessage = Message(
        id = "msg-1",
        channelId = "ch-1",
        content = "Hello world",
        senderId = "user-1",
        senderName = "Test User",
        senderType = "human",
        createdAt = "2026-04-18T10:00:00Z"
    )

    private val stateWithMessage = MessageUiState(
        messages = listOf(testMessage)
    )

    @Test
    fun `long press message shows Mark as Read in action sheet`() {
        composeTestRule.setContent {
            MessageListScreen(
                channelName = "general",
                state = stateWithMessage,
                onSendMessage = {},
                onLoadMore = {},
                onRetryLoad = {},
                onNavigateBack = {},
                onNavigateToThread = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hello world").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Mark as Read").assertExists()
    }

    @Test
    fun `long press message shows Mark as Unread in action sheet`() {
        composeTestRule.setContent {
            MessageListScreen(
                channelName = "general",
                state = stateWithMessage,
                onSendMessage = {},
                onLoadMore = {},
                onRetryLoad = {},
                onNavigateBack = {},
                onNavigateToThread = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hello world").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Mark as Unread").assertExists()
    }

    @Test
    fun `clicking Mark as Read triggers onMarkAsRead callback`() {
        var readCalled = false
        composeTestRule.setContent {
            MessageListScreen(
                channelName = "general",
                state = stateWithMessage,
                onSendMessage = {},
                onLoadMore = {},
                onRetryLoad = {},
                onNavigateBack = {},
                onNavigateToThread = { _, _ -> },
                onMarkAsRead = { readCalled = true }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hello world").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Mark as Read").performClick()
        composeTestRule.waitForIdle()
        assertTrue("onMarkAsRead must be called when Mark as Read is clicked", readCalled)
    }

    @Test
    fun `clicking Mark as Unread triggers onMarkAsUnread callback`() {
        var unreadCalled = false
        composeTestRule.setContent {
            MessageListScreen(
                channelName = "general",
                state = stateWithMessage,
                onSendMessage = {},
                onLoadMore = {},
                onRetryLoad = {},
                onNavigateBack = {},
                onNavigateToThread = { _, _ -> },
                onMarkAsUnread = { unreadCalled = true }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hello world").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Mark as Unread").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue("onMarkAsUnread must be called when Mark as Unread is clicked", unreadCalled)
    }

    @Test
    fun `feedback message shows Toast and triggers consume callback`() {
        var feedbackConsumed = false
        composeTestRule.setContent {
            MessageListScreen(
                channelName = "general",
                state = stateWithMessage,
                onSendMessage = {},
                onLoadMore = {},
                onRetryLoad = {},
                onNavigateBack = {},
                onNavigateToThread = { _, _ -> },
                markReadFeedbackMessage = "Mark as unread failed: 500",
                onMarkReadFeedbackShown = { feedbackConsumed = true }
            )
        }
        composeTestRule.waitForIdle()
        assertTrue(
            "Toast should display feedback message",
            ShadowToast.showedToast("Mark as unread failed: 500")
        )
        assertTrue("onMarkReadFeedbackShown should be called after showing Toast", feedbackConsumed)
    }

    @Test
    fun `existing action sheet items still visible alongside new ones`() {
        composeTestRule.setContent {
            MessageListScreen(
                channelName = "general",
                state = stateWithMessage,
                onSendMessage = {},
                onLoadMore = {},
                onRetryLoad = {},
                onNavigateBack = {},
                onNavigateToThread = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hello world").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Quote Reply").assertExists()
        composeTestRule.onNodeWithText("Copy Markdown").assertExists()
        composeTestRule.onNodeWithText("Mark as Read").assertExists()
        composeTestRule.onNodeWithText("Mark as Unread").assertExists()
    }
}
