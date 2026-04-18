package com.slock.app.ui.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.slock.app.ui.theme.SlockAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForgotPasswordScreenComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `forgot password form renders neo actions and keeps submit back clickable`() {
        var sendClicks = 0
        var backClicks = 0

        composeTestRule.setContent {
            SlockAppTheme {
                ForgotPasswordScreen(
                    state = AuthUiState(email = "person@example.com"),
                    onEmailChange = {},
                    onSendReset = { sendClicks++ },
                    onNavigateBack = { backClicks++ }
                )
            }
        }

        composeTestRule.onNodeWithText("Forgot Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("EMAIL").assertIsDisplayed()
        composeTestRule.onNodeWithText("SEND RESET LINK").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Back to Sign In").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, sendClicks)
        assertEquals(1, backClicks)
    }

    @Test
    fun `forgot password success state shows confirmation and back action`() {
        var backClicks = 0

        composeTestRule.setContent {
            SlockAppTheme {
                ForgotPasswordScreen(
                    state = AuthUiState(
                        email = "person@example.com",
                        resetEmailSent = true
                    ),
                    onEmailChange = {},
                    onSendReset = {},
                    onNavigateBack = { backClicks++ }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Check your inbox").assertIsDisplayed()
        composeTestRule.onNodeWithText("person@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("BACK TO SIGN IN").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, backClicks)
    }
}
