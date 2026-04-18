package com.slock.app.ui.agent

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.slock.app.data.model.Agent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgentCardClickComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val activeAgent = Agent(
        id = "agent-1",
        name = "TestBot",
        status = "active",
        model = "claude-sonnet-4-20250514"
    )

    private val inactiveAgent = Agent(
        id = "agent-2",
        name = "SleepBot",
        status = "stopped",
        model = "claude-sonnet-4-20250514"
    )

    private fun renderAgentList(
        agents: List<Agent> = listOf(activeAgent, inactiveAgent),
        onAgentClick: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            AgentListScreen(
                state = AgentUiState(agents = agents),
                onCreateAgent = { _, _, _, _ -> },
                onStartAgent = {},
                onStopAgent = {},
                onResetAgent = {},
                onDeleteAgent = {},
                onUpdateAgent = { _, _, _, _ -> },
                onDmAgent = {},
                onAgentClick = onAgentClick,
                onNavigateBack = {},
                showHeader = false
            )
        }
    }

    @Test
    fun `clicking active agent card invokes onAgentClick with correct agentId`() {
        var clickedId: String? = null
        renderAgentList(onAgentClick = { clickedId = it })

        composeTestRule.onNodeWithText("TestBot").assertIsDisplayed()
        composeTestRule.onNodeWithText("TestBot").performClick()
        composeTestRule.waitForIdle()

        assertEquals("agent-1", clickedId)
    }

    @Test
    fun `clicking inactive agent card invokes onAgentClick with correct agentId`() {
        var clickedId: String? = null
        renderAgentList(onAgentClick = { clickedId = it })

        composeTestRule.onNodeWithText("SleepBot").assertIsDisplayed()
        composeTestRule.onNodeWithText("SleepBot").performClick()
        composeTestRule.waitForIdle()

        assertEquals("agent-2", clickedId)
    }

    @Test
    fun `both active and inactive agents are displayed`() {
        renderAgentList()

        composeTestRule.onNodeWithText("TestBot").assertIsDisplayed()
        composeTestRule.onNodeWithText("SleepBot").assertIsDisplayed()
    }

    @Test
    fun `clicking agent with null id does not invoke onAgentClick`() {
        var clickedId: String? = null
        val nullIdAgent = Agent(
            id = null,
            name = "NullBot",
            status = "active",
            model = "claude-sonnet-4-20250514"
        )
        renderAgentList(
            agents = listOf(nullIdAgent),
            onAgentClick = { clickedId = it }
        )

        composeTestRule.onNodeWithText("NullBot").assertIsDisplayed()
        composeTestRule.onNodeWithText("NullBot").performClick()
        composeTestRule.waitForIdle()

        assertEquals(null, clickedId)
    }

    @Test
    fun `clicking agent with blank id does not invoke onAgentClick`() {
        var clickedId: String? = null
        val blankIdAgent = Agent(
            id = "",
            name = "BlankBot",
            status = "stopped",
            model = "claude-sonnet-4-20250514"
        )
        renderAgentList(
            agents = listOf(blankIdAgent),
            onAgentClick = { clickedId = it }
        )

        composeTestRule.onNodeWithText("BlankBot").assertIsDisplayed()
        composeTestRule.onNodeWithText("BlankBot").performClick()
        composeTestRule.waitForIdle()

        assertEquals(null, clickedId)
    }
}
