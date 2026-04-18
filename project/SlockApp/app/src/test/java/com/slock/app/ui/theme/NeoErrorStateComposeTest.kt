package com.slock.app.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NeoErrorStateComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `log actions are visible and send action is clickable when feedback hook exists`() {
        var retryClicks = 0
        var copyLogClicks = 0
        var sendLogClicks = 0

        composeTestRule.setContent {
            SlockAppTheme {
                NeoErrorState(
                    message = "消息加载失败",
                    onRetry = { retryClicks++ },
                    onCopyLog = { copyLogClicks++ },
                    onSendLog = { sendLogClicks++ }
                )
            }
        }

        composeTestRule.onNodeWithText("重试").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("复制错误日志").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("发送错误报告").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, retryClicks)
        assertEquals(1, copyLogClicks)
        assertEquals(1, sendLogClicks)
    }

    @Test
    fun `log actions are hidden when no feedback hook or log context exists`() {
        composeTestRule.setContent {
            SlockAppTheme {
                NeoErrorState(message = "消息加载失败")
            }
        }

        composeTestRule.onNodeWithText("复制错误日志").assertDoesNotExist()
        composeTestRule.onNodeWithText("发送错误报告").assertDoesNotExist()
    }
}
