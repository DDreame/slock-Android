package com.slock.app.integration

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.slock.app.ui.theme.NeoButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComposeRobolectricSampleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders Text composable and asserts content`() {
        composeTestRule.setContent {
            Text("Hello from Robolectric")
        }
        composeTestRule.onNodeWithText("Hello from Robolectric").assertIsDisplayed()
    }

    @Test
    fun `renders NeoButton and asserts label`() {
        composeTestRule.setContent {
            NeoButton(text = "Test Button", onClick = {})
        }
        composeTestRule.onNodeWithText("Test Button").assertIsDisplayed()
    }
}
