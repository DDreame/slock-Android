package com.slock.app.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.slock.app.ui.channel.ChannelUiState
import com.slock.app.ui.member.MemberItem
import com.slock.app.ui.server.ServerUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NewDmDialogComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testMembers = listOf(
        MemberItem(
            id = "agent1", name = "Agent Bot", role = "agent",
            isAgent = true, isOnline = true, subtitle = "Working"
        ),
        MemberItem(
            id = "human1", userId = "user1", name = "Alice", role = "member",
            isAgent = false, isOnline = true, subtitle = ""
        ),
        MemberItem(
            id = "human2", userId = "user2", name = "Bob", role = "member",
            isAgent = false, isOnline = false, subtitle = ""
        )
    )

    private fun renderHomeScreen(
        members: List<MemberItem> = testMembers,
        onNewDmMemberSelected: (MemberItem) -> Unit = {}
    ) {
        composeTestRule.setContent {
            HomeScreen(
                serverState = ServerUiState(),
                channelState = ChannelUiState(),
                selectedServer = null,
                onServerSelect = {},
                onChannelClick = { _, _ -> },
                onDmClick = { _, _ -> },
                onCreateChannel = { _, _ -> },
                onCreateServer = { _, _ -> },
                onOpenSettings = {},
                members = members,
                onNewDmMemberSelected = onNewDmMemberSelected
            )
        }
    }

    private fun clickDmPlusButton() {
        val plusButtons = composeTestRule.onAllNodesWithText("+")
        plusButtons[1].performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `clicking DM section plus button opens NewDmDialog`() {
        renderHomeScreen()
        clickDmPlusButton()
        composeTestRule.onNodeWithText("New Message").assertIsDisplayed()
    }

    @Test
    fun `NewDmDialog shows all members`() {
        renderHomeScreen()
        clickDmPlusButton()
        composeTestRule.onNodeWithText("Agent Bot").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun `NewDmDialog shows AGENT badge for agent members`() {
        renderHomeScreen()
        clickDmPlusButton()
        composeTestRule.onAllNodesWithText("AGENT").fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun `search filters member list by name`() {
        renderHomeScreen()
        clickDmPlusButton()
        composeTestRule.onNodeWithText("Search members...").performTextInput("Ali")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertDoesNotExist()
        composeTestRule.onNodeWithText("Agent Bot").assertDoesNotExist()
    }

    @Test
    fun `search is case insensitive`() {
        renderHomeScreen()
        clickDmPlusButton()
        composeTestRule.onNodeWithText("Search members...").performTextInput("bob")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertDoesNotExist()
    }

    @Test
    fun `empty search shows all members`() {
        renderHomeScreen()
        clickDmPlusButton()
        composeTestRule.onNodeWithText("Agent Bot").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun `no match shows empty state`() {
        renderHomeScreen()
        clickDmPlusButton()
        composeTestRule.onNodeWithText("Search members...").performTextInput("zzzzz")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No members found").assertIsDisplayed()
    }

    @Test
    fun `cancel button closes dialog`() {
        renderHomeScreen()
        clickDmPlusButton()
        composeTestRule.onNodeWithText("New Message").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("New Message").assertDoesNotExist()
    }

    @Test
    fun `selecting a human member invokes callback with correct MemberItem`() {
        var selectedMember: MemberItem? = null
        renderHomeScreen(onNewDmMemberSelected = { selectedMember = it })
        clickDmPlusButton()
        composeTestRule.onNodeWithText("Alice").performClick()
        composeTestRule.waitForIdle()
        assertEquals("user1", selectedMember?.userId)
        assertEquals("Alice", selectedMember?.name)
        assertEquals(false, selectedMember?.isAgent)
    }

    @Test
    fun `selecting an agent member invokes callback with correct MemberItem`() {
        var selectedMember: MemberItem? = null
        renderHomeScreen(onNewDmMemberSelected = { selectedMember = it })
        clickDmPlusButton()
        composeTestRule.onNodeWithText("Agent Bot").performClick()
        composeTestRule.waitForIdle()
        assertEquals("agent1", selectedMember?.id)
        assertEquals("Agent Bot", selectedMember?.name)
        assertEquals(true, selectedMember?.isAgent)
    }

    @Test
    fun `selecting a member closes the dialog`() {
        renderHomeScreen()
        clickDmPlusButton()
        composeTestRule.onNodeWithText("New Message").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("New Message").assertDoesNotExist()
    }
}
