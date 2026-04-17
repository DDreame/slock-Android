package com.slock.app.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NewDmEntryUiTest {

    private val homeScreenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/home/HomeScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/home/HomeScreen.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `HomeScreen accepts members parameter`() {
        val signature = homeScreenSource
            .substringAfter("fun HomeScreen(")
            .substringBefore(") {")
        assertTrue(
            "HomeScreen must accept members: List<MemberItem> parameter",
            signature.contains("members: List<MemberItem>")
        )
    }

    @Test
    fun `HomeScreen accepts onNewDmMemberSelected callback`() {
        val signature = homeScreenSource
            .substringAfter("fun HomeScreen(")
            .substringBefore(") {")
        assertTrue(
            "HomeScreen must accept onNewDmMemberSelected callback",
            signature.contains("onNewDmMemberSelected")
        )
    }

    @Test
    fun `DM section header has onAdd callback for new DM`() {
        val channelListBlock = homeScreenSource
            .substringAfter("private fun ChannelListContent(")
            .substringBefore("private fun NeoTopBar(")
        assertTrue(
            "DIRECT MESSAGES SectionHeader must have onAdd for new DM entry",
            channelListBlock.contains("SectionHeader(title = \"DIRECT MESSAGES\", onAdd =")
        )
    }

    @Test
    fun `ChannelListContent accepts onShowNewDm callback`() {
        val signature = homeScreenSource
            .substringAfter("private fun ChannelListContent(")
            .substringBefore(") {")
        assertTrue(
            "ChannelListContent must accept onShowNewDm callback",
            signature.contains("onShowNewDm")
        )
    }

    @Test
    fun `ChannelsTabContent accepts onShowNewDm callback`() {
        val signature = homeScreenSource
            .substringAfter("private fun ChannelsTabContent(")
            .substringBefore(") {")
        assertTrue(
            "ChannelsTabContent must accept onShowNewDm callback",
            signature.contains("onShowNewDm")
        )
    }

    @Test
    fun `NewDmDialog composable exists`() {
        assertTrue(
            "HomeScreen must contain NewDmDialog composable",
            homeScreenSource.contains("fun NewDmDialog(")
        )
    }

    @Test
    fun `NewDmDialog has search field`() {
        val dialogBlock = homeScreenSource
            .substringAfter("fun NewDmDialog(")
            .substringBefore("fun NewDmMemberRow(")
        assertTrue(
            "NewDmDialog must contain a search text field",
            dialogBlock.contains("NeoTextField(") && dialogBlock.contains("searchQuery")
        )
    }

    @Test
    fun `NewDmDialog filters members by search query`() {
        val dialogBlock = homeScreenSource
            .substringAfter("fun NewDmDialog(")
            .substringBefore("fun NewDmMemberRow(")
        assertTrue(
            "NewDmDialog must filter members list",
            dialogBlock.contains("filteredMembers")
        )
    }

    @Test
    fun `NewDmDialog shows member rows with click handler`() {
        val dialogBlock = homeScreenSource
            .substringAfter("fun NewDmDialog(")
            .substringBefore("fun NewDmMemberRow(")
        assertTrue(
            "NewDmDialog must render NewDmMemberRow for each member",
            dialogBlock.contains("NewDmMemberRow(")
        )
        assertTrue(
            "NewDmDialog must call onMemberSelected on click",
            dialogBlock.contains("onMemberSelected(")
        )
    }

    @Test
    fun `NewDmMemberRow composable exists with member display`() {
        assertTrue(
            "HomeScreen must contain NewDmMemberRow composable",
            homeScreenSource.contains("fun NewDmMemberRow(")
        )
        val rowBlock = homeScreenSource
            .substringAfter("fun NewDmMemberRow(")
        assertTrue(
            "NewDmMemberRow must display member name",
            rowBlock.contains("member.name")
        )
    }

    @Test
    fun `NewDmMemberRow shows agent badge for agents`() {
        val rowBlock = homeScreenSource
            .substringAfter("fun NewDmMemberRow(")
        assertTrue(
            "NewDmMemberRow must show AGENT badge for agent members",
            rowBlock.contains("member.isAgent") && rowBlock.contains("\"AGENT\"")
        )
    }

    @Test
    fun `NewDmMemberRow shows online status indicator`() {
        val rowBlock = homeScreenSource
            .substringAfter("fun NewDmMemberRow(")
        assertTrue(
            "NewDmMemberRow must show online/offline status",
            rowBlock.contains("member.isOnline")
        )
    }

    @Test
    fun `showNewDmDialog state is declared in HomeScreen`() {
        val homeScreenBody = homeScreenSource
            .substringAfter("fun HomeScreen(")
        assertTrue(
            "HomeScreen must declare showNewDmDialog state",
            homeScreenBody.contains("showNewDmDialog")
        )
    }
}
