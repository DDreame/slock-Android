package com.slock.app.ui.billing

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.slock.app.data.model.BillingPlanSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BillingPlansScreenComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `billing screen shows current plan and web upgrade guidance`() {
        var backClicks = 0
        composeTestRule.setContent {
            BillingPlansScreen(
                state = BillingPlansUiState(
                    isLoading = false,
                    planSummary = BillingPlanSummary(
                        planName = "Pro",
                        statusLabel = "Active",
                        renewalLabel = "Renews 2026-05-01"
                    )
                ),
                onNavigateBack = { backClicks += 1 },
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithText("Billing / Plans").assertIsDisplayed()
        composeTestRule.onNodeWithText("CURRENT PLAN").assertIsDisplayed()
        composeTestRule.onNodeWithText("PRO").assertIsDisplayed()
        composeTestRule.onNodeWithText("Active").assertIsDisplayed()
        composeTestRule.onNodeWithText("Billing changes and upgrades are still managed on web. Android now shows your current plan and status so this page is no longer a dead end.")
            .assertExists()
        composeTestRule.onNodeWithText("BACK TO SETTINGS").performScrollTo().performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun `billing screen keeps placeholder content when live data is unavailable`() {
        composeTestRule.setContent {
            BillingPlansScreen(
                state = BillingPlansUiState(
                    isLoading = false,
                    planSummary = BillingPlanSummary(),
                    notice = "Live billing details are temporarily unavailable. Android still shows a read-only plan placeholder.",
                    error = "Unable to refresh billing details"
                ),
                onNavigateBack = {},
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithText("FREE").assertIsDisplayed()
        composeTestRule.onNodeWithText("Read-only preview").assertIsDisplayed()
        composeTestRule.onNodeWithText("Live billing details are temporarily unavailable. Android still shows a read-only plan placeholder.")
            .assertExists()
        composeTestRule.onNodeWithText("Unable to refresh billing details").assertExists()
    }
}
