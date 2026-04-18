package com.slock.app.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.slock.app.data.model.Server
import com.slock.app.ui.channel.ChannelUiState
import com.slock.app.ui.server.ServerUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingGuidanceComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testServer = Server(
        id = "srv-1",
        name = "Acme",
        slug = "acme"
    )

    @Test
    fun `zero server guidance shows create server CTA and opens dialog`() {
        composeTestRule.setContent {
            HomeScreen(
                serverState = ServerUiState(servers = emptyList()),
                channelState = ChannelUiState(),
                selectedServer = null,
                onServerSelect = {},
                onChannelClick = { _, _ -> },
                onDmClick = { _, _ -> },
                onCreateChannel = { _, _ -> },
                onCreateServer = { _, _ -> },
                onOpenSettings = {}
            )
        }

        composeTestRule.onNodeWithText("Create your first server").assertIsDisplayed()
        composeTestRule.onNodeWithText("CREATE SERVER").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Create Server").assertIsDisplayed()
        composeTestRule.onNodeWithText("Server Name").assertIsDisplayed()
    }

    @Test
    fun `empty workspace guidance shows first agent copy and stable open agents entry`() {
        var openAgentsClicks = 0

        composeTestRule.setContent {
            HomeScreen(
                serverState = ServerUiState(servers = listOf(testServer)),
                channelState = ChannelUiState(),
                selectedServer = testServer,
                agentCount = 0,
                onServerSelect = {},
                onChannelClick = { _, _ -> },
                onDmClick = { _, _ -> },
                onCreateChannel = { _, _ -> },
                onCreateServer = { _, _ -> },
                onOpenAgents = { openAgentsClicks++ },
                onOpenSettings = {}
            )
        }

        composeTestRule.onNodeWithText("Create your first agent").assertIsDisplayed()
        composeTestRule.onNodeWithText("OPEN AGENTS").assertIsDisplayed()
        composeTestRule.onNodeWithText("OPEN AGENTS").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, openAgentsClicks)
    }

    @Test
    fun `first agent guidance hides once workspace already has agents`() {
        composeTestRule.setContent {
            HomeScreen(
                serverState = ServerUiState(servers = listOf(testServer)),
                channelState = ChannelUiState(),
                selectedServer = testServer,
                agentCount = 1,
                onServerSelect = {},
                onChannelClick = { _, _ -> },
                onDmClick = { _, _ -> },
                onCreateChannel = { _, _ -> },
                onCreateServer = { _, _ -> },
                onOpenAgents = {},
                onOpenSettings = {}
            )
        }

        composeTestRule.onNodeWithText("Create your first agent").assertDoesNotExist()
        composeTestRule.onNodeWithText("OPEN AGENTS").assertIsDisplayed()
    }
}
