package com.slock.app.integration

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.slock.app.ui.theme.NeoButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComposeRobolectricSampleTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders composable inside real ComponentActivity`() {
        composeTestRule.setContent {
            Text("Hello from real Activity")
        }
        composeTestRule.onNodeWithText("Hello from real Activity").assertIsDisplayed()
    }

    @Test
    fun `renders NeoButton inside real ComponentActivity`() {
        composeTestRule.setContent {
            NeoButton(text = "Test Button", onClick = {})
        }
        composeTestRule.onNodeWithText("Test Button").assertIsDisplayed()
    }
}
