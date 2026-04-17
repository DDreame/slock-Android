package com.slock.app.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.slock.app.ui.channel.ChannelUiState
import com.slock.app.ui.member.MemberItem
import com.slock.app.ui.server.ServerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `NewDmDialog has search input field`() {
        renderHomeScreen()
        clickDmPlusButton()
        composeTestRule.onNodeWithText("Search members...").assertIsDisplayed()
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

class NewDmFilterTest {

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

    @Test
    fun `blank query returns all members`() {
        val result = filterMembersByQuery(testMembers, "")
        assertEquals(3, result.size)
    }

    @Test
    fun `whitespace query returns all members`() {
        val result = filterMembersByQuery(testMembers, "   ")
        assertEquals(3, result.size)
    }

    @Test
    fun `filters by partial name match`() {
        val result = filterMembersByQuery(testMembers, "Ali")
        assertEquals(1, result.size)
        assertEquals("Alice", result[0].name)
    }

    @Test
    fun `filter is case insensitive`() {
        val result = filterMembersByQuery(testMembers, "bob")
        assertEquals(1, result.size)
        assertEquals("Bob", result[0].name)
    }

    @Test
    fun `filters agents by name`() {
        val result = filterMembersByQuery(testMembers, "Agent")
        assertEquals(1, result.size)
        assertTrue(result[0].isAgent)
    }

    @Test
    fun `no match returns empty list`() {
        val result = filterMembersByQuery(testMembers, "zzzzz")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `full name match returns single result`() {
        val result = filterMembersByQuery(testMembers, "Alice")
        assertEquals(1, result.size)
        assertEquals("Alice", result[0].name)
    }
}
