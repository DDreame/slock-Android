package com.slock.app.ui.theme

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
class TaskListComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `unchecked task item renders with empty checkbox symbol`() {
        composeTestRule.setContent {
            NeoMessageContent(content = "- [ ] buy milk")
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("☐", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("buy milk", substring = true).assertIsDisplayed()
    }

    @Test
    fun `checked task item renders with checked checkbox symbol`() {
        composeTestRule.setContent {
            NeoMessageContent(content = "- [x] buy milk")
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("☑", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("buy milk", substring = true).assertIsDisplayed()
    }

    @Test
    fun `mixed task list renders both checkbox states`() {
        composeTestRule.setContent {
            NeoMessageContent(content = "- [ ] pending\n- [x] done")
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("☐", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("pending", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("☑", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("done", substring = true).assertIsDisplayed()
    }

    @Test
    fun `regular list item renders bullet not checkbox`() {
        composeTestRule.setContent {
            NeoMessageContent(content = "- regular item")
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("•", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("regular item", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("☐").assertDoesNotExist()
        composeTestRule.onNodeWithText("☑").assertDoesNotExist()
    }
}
