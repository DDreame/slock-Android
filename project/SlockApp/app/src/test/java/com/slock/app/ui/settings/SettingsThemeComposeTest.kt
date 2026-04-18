package com.slock.app.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
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

    private fun renderSettings(
        state: SettingsUiState = SettingsUiState(),
        onOpenBillingPlans: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            SettingsScreen(
                state = state,
                onNavigateBack = {},
                onNotificationPreferenceChange = {},
                onRefreshAccount = {},
                onOpenBillingPlans = onOpenBillingPlans,
                onSendFeedback = {},
                onLogout = {}
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `theme section shows Neo Brutalism as built-in theme`() {
        renderSettings()
        composeTestRule.onNodeWithText("Neo Brutalism").assertExists()
        composeTestRule.onNodeWithText("Built-in app theme").assertExists()
    }

    @Test
    fun `header does not mention theme as controllable`() {
        renderSettings()
        composeTestRule.onNodeWithText("Control notifications, account details, and app info.")
            .assertExists()
    }

    @Test
    fun `notifications section still renders`() {
        renderSettings()
        composeTestRule.onNodeWithText("NOTIFICATIONS").assertExists()
    }

    @Test
    fun `account section still renders`() {
        renderSettings()
        composeTestRule.onNodeWithText("ACCOUNT").assertExists()
    }

    @Test
    fun `workspace section exposes billing plans entry`() {
        renderSettings()
        composeTestRule.onNodeWithText("Billing / Plans").assertExists()
        composeTestRule.onNodeWithText("VIEW BILLING / PLANS").assertExists()
    }

    @Test
    fun `billing plans entry button remains clickable`() {
        var openBillingClicks = 0

        renderSettings(onOpenBillingPlans = { openBillingClicks += 1 })

        composeTestRule.onNodeWithText("VIEW BILLING / PLANS").performScrollTo().performClick()

        assertEquals(1, openBillingClicks)
    }
}
