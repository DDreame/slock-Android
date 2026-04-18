package com.slock.app.ui.message

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchBarFocusComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `search bar opens without FocusRequester crash`() {
        composeTestRule.setContent {
            MessageListScreen(
                channelName = "test-channel",
                state = MessageUiState(isSearchActive = true),
                onSendMessage = {},
                onLoadMore = {},
                onRetryLoad = {},
                onNavigateBack = {},
                onNavigateToThread = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("搜索消息...").assertIsDisplayed()
    }

    @Test
    fun `search bar does not crash when toggled on with empty messages`() {
        composeTestRule.setContent {
            MessageListScreen(
                channelName = "test-channel",
                state = MessageUiState(
                    isSearchActive = true,
                    searchQuery = "hello",
                    messages = emptyList()
                ),
                onSendMessage = {},
                onLoadMore = {},
                onRetryLoad = {},
                onNavigateBack = {},
                onNavigateToThread = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("0/0").assertIsDisplayed()
    }
}
