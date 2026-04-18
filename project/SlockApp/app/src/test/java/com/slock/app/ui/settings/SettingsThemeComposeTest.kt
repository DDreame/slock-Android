package com.slock.app.ui.settings

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
class SettingsThemeComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun renderSettings(state: SettingsUiState = SettingsUiState()) {
        composeTestRule.setContent {
            SettingsScreen(
                state = state,
                onNavigateBack = {},
                onNotificationPreferenceChange = {},
                onRefreshAccount = {},
                onSendFeedback = {},
                onLogout = {}
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `theme section shows Neo Brutalism as built-in theme`() {
        renderSettings()
        composeTestRule.onNodeWithText("Neo Brutalism").assertIsDisplayed()
        composeTestRule.onNodeWithText("Built-in app theme").assertIsDisplayed()
    }

    @Test
    fun `header does not mention theme as controllable`() {
        renderSettings()
        composeTestRule.onNodeWithText("Control notifications, account details, and app info.")
            .assertIsDisplayed()
    }

    @Test
    fun `notifications section still renders`() {
        renderSettings()
        composeTestRule.onNodeWithText("NOTIFICATIONS").assertIsDisplayed()
    }

    @Test
    fun `account section still renders`() {
        renderSettings()
        composeTestRule.onNodeWithText("ACCOUNT").assertIsDisplayed()
    }
}
