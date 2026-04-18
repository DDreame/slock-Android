package com.slock.app.ui.agent

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.slock.app.data.model.Agent
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgentSettingsComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testAgent = Agent(
        id = "agent-1",
        name = "TestBot",
        description = "A helpful bot",
        prompt = "Be helpful",
        status = "active",
        model = "claude-sonnet-4-20250514"
    )

    private data class SaveCall(
        val name: String?,
        val description: String?,
        val prompt: String?,
        val runtime: String?,
        val reasoningEffort: String?,
        val envVars: Map<String, String>?
    )

    private fun renderContent(
        agent: Agent = testAgent,
        onSave: (String?, String?, String?, String, String?, Map<String, String>?) -> Unit = { _, _, _, _, _, _ -> },
        onDelete: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            AgentSettingsContent(
                agent = agent,
                onSave = onSave,
                onDelete = onDelete
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `settings sheet shows editable name field with current value`() {
        renderContent()
        composeTestRule.onNodeWithText("Agent Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("TestBot").assertIsDisplayed()
    }

    @Test
    fun `settings sheet shows editable description field`() {
        renderContent()
        composeTestRule.onNodeWithText("A helpful bot").assertIsDisplayed()
    }

    @Test
    fun `settings sheet shows editable prompt field`() {
        renderContent()
        composeTestRule.onNodeWithText("Be helpful").assertIsDisplayed()
    }

    @Test
    fun `settings sheet shows read-only model badge`() {
        renderContent()
        composeTestRule.onNodeWithText("Sonnet").assertIsDisplayed()
    }

    @Test
    fun `saving settings passes edited name to onSave`() {
        var captured: SaveCall? = null
        renderContent(
            onSave = { name, desc, prompt, runtime, reasoning, envVars ->
                captured = SaveCall(name, desc, prompt, runtime, reasoning, envVars)
            }
        )

        val nameFields = composeTestRule.onAllNodesWithText("TestBot")
        nameFields[0].performTextClearance()
        nameFields[0].performTextInput("NewName")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SAVE CONFIG").performClick()
        composeTestRule.waitForIdle()

        assertNotNull("onSave must have been called", captured)
        assertEquals("NewName", captured?.name)
    }

    @Test
    fun `saving settings passes edited description to onSave`() {
        var captured: SaveCall? = null
        renderContent(
            onSave = { name, desc, prompt, runtime, reasoning, envVars ->
                captured = SaveCall(name, desc, prompt, runtime, reasoning, envVars)
            }
        )

        val descFields = composeTestRule.onAllNodesWithText("A helpful bot")
        descFields[0].performTextClearance()
        descFields[0].performTextInput("New description")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SAVE CONFIG").performClick()
        composeTestRule.waitForIdle()

        assertNotNull("onSave must have been called", captured)
        assertEquals("New description", captured?.description)
    }

    @Test
    fun `saving settings passes edited prompt to onSave`() {
        var captured: SaveCall? = null
        renderContent(
            onSave = { name, desc, prompt, runtime, reasoning, envVars ->
                captured = SaveCall(name, desc, prompt, runtime, reasoning, envVars)
            }
        )

        val promptFields = composeTestRule.onAllNodesWithText("Be helpful")
        promptFields[0].performTextClearance()
        promptFields[0].performTextInput("New prompt")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SAVE CONFIG").performClick()
        composeTestRule.waitForIdle()

        assertNotNull("onSave must have been called", captured)
        assertEquals("New prompt", captured?.prompt)
    }

    @Test
    fun `model badge is displayed but not editable`() {
        renderContent()
        composeTestRule.onNodeWithText("Sonnet").assertIsDisplayed()
        composeTestRule.onNode(hasText("Sonnet") and hasSetTextAction()).assertDoesNotExist()
    }
}
