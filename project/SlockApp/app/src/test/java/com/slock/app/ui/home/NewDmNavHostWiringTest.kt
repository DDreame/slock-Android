package com.slock.app.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NewDmNavHostWiringTest {

    private val navHostSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `NavHost passes members to HomeScreen`() {
        val homeScreenCall = navHostSource
            .substringAfter("HomeScreen(")
            .substringBefore("threadsContent =")
        assertTrue(
            "NavHost must pass members = membersState.members to HomeScreen",
            homeScreenCall.contains("members = membersState.members")
        )
    }

    @Test
    fun `NavHost wires onNewDmMemberSelected with createDM`() {
        val homeScreenCall = navHostSource
            .substringAfter("HomeScreen(")
            .substringBefore("threadsContent =")
        assertTrue(
            "NavHost must wire onNewDmMemberSelected callback",
            homeScreenCall.contains("onNewDmMemberSelected =")
        )
        assertTrue(
            "onNewDmMemberSelected must call channelViewModel.createDM",
            homeScreenCall.contains("channelViewModel.createDM(") ||
                homeScreenCall.contains("createDM(")
        )
    }

    @Test
    fun `onNewDmMemberSelected navigates to DM messages on success`() {
        val homeScreenCall = navHostSource
            .substringAfter("onNewDmMemberSelected =")
            .substringBefore("threadsContent =")
        assertTrue(
            "onNewDmMemberSelected must navigate to dmMessagesRoute on success",
            homeScreenCall.contains("Routes.dmMessagesRoute(")
        )
    }

    @Test
    fun `onNewDmMemberSelected passes agentId for agents and userId for humans`() {
        val callback = navHostSource
            .substringAfter("onNewDmMemberSelected =")
            .substringBefore("threadsContent =")
        assertTrue(
            "Must determine agentId from member.isAgent",
            callback.contains("member.isAgent") && callback.contains("agentId")
        )
        assertTrue(
            "Must determine userId for non-agent members",
            callback.contains("userId")
        )
    }

    @Test
    fun `onNewDmMemberSelected shows toast on error`() {
        val callback = navHostSource
            .substringAfter("onNewDmMemberSelected =")
            .substringBefore("threadsContent =")
        assertTrue(
            "Must show Toast on DM creation error",
            callback.contains("Toast.makeText(")
        )
    }
}
