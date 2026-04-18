package com.slock.app.ui.release

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReleaseNotesComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleNotes = listOf(
        ReleaseNoteEntry(
            version = "1.0.0",
            date = "2026-04-01",
            highlights = listOf("Initial release", "Core features")
        ),
        ReleaseNoteEntry(
            version = "0.9.0",
            date = "2026-03-15",
            highlights = listOf("Beta improvements")
        )
    )

    @Test
    fun `header shows Release Notes title`() {
        composeTestRule.setContent {
            ReleaseNotesScreen(state = ReleaseNotesUiState(notes = sampleNotes))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Release Notes").assertIsDisplayed()
    }

    @Test
    fun `release note version and date are visible`() {
        composeTestRule.setContent {
            ReleaseNotesScreen(state = ReleaseNotesUiState(notes = sampleNotes))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("v1.0.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("2026-04-01").assertIsDisplayed()
    }

    @Test
    fun `release note highlights are visible`() {
        composeTestRule.setContent {
            ReleaseNotesScreen(state = ReleaseNotesUiState(notes = sampleNotes))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Initial release").assertIsDisplayed()
        composeTestRule.onNodeWithText("Core features").assertIsDisplayed()
    }

    @Test
    fun `empty state shows placeholder message`() {
        composeTestRule.setContent {
            ReleaseNotesScreen(state = ReleaseNotesUiState(notes = emptyList()))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No release notes available").assertIsDisplayed()
    }

    @Test
    fun `back button triggers onNavigateBack`() {
        var backCalled = false
        composeTestRule.setContent {
            ReleaseNotesScreen(
                state = ReleaseNotesUiState(notes = sampleNotes),
                onNavigateBack = { backCalled = true }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("\u2190").performClick()
        assertTrue("onNavigateBack should be called", backCalled)
    }
}
