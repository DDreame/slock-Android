package com.slock.app.ui.navigation

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NavBackSemanticsTest {

    private val navHostSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `Home composable contains BackHandler for root back press`() {
        val homeBlock = navHostSource
            .substringAfter("composable(Routes.HOME)")
            .substringBefore("composable(Routes.SETTINGS)")
        assertTrue(
            "Home composable must contain a BackHandler",
            homeBlock.contains("BackHandler")
        )
    }

    @Test
    fun `Home BackHandler finishes the activity`() {
        val homeBlock = navHostSource
            .substringAfter("composable(Routes.HOME)")
            .substringBefore("composable(Routes.SETTINGS)")
        assertTrue(
            "Home BackHandler must call Activity.finish()",
            homeBlock.contains("Activity)?.finish()")
        )
    }

    @Test
    fun `BackHandler import is present`() {
        assertTrue(
            "NavHost must import BackHandler",
            navHostSource.contains("import androidx.activity.compose.BackHandler")
        )
    }

    @Test
    fun `warm-start deep link pops up to HOME`() {
        val warmStartBlock = navHostSource
            .substringAfter("shouldHandleWarmStartDeepLink")
            .substringBefore("onDeepLinkConsumed()")
        assertTrue(
            "Warm-start deep link must popUpTo HOME to keep it at stack bottom",
            warmStartBlock.contains("popUpTo(Routes.HOME)")
        )
    }

    @Test
    fun `warm-start deep link HOME popUpTo is not inclusive`() {
        val warmStartBlock = navHostSource
            .substringAfter("shouldHandleWarmStartDeepLink")
            .substringBefore("onDeepLinkConsumed()")
        val popUpToIdx = warmStartBlock.indexOf("popUpTo(Routes.HOME)")
        assertTrue("popUpTo(Routes.HOME) must exist", popUpToIdx >= 0)
        val afterPopUpTo = warmStartBlock.substring(popUpToIdx, minOf(popUpToIdx + 60, warmStartBlock.length))
        assertTrue(
            "popUpTo(Routes.HOME) must use inclusive = false",
            afterPopUpTo.contains("inclusive = false")
        )
    }

    @Test
    fun `AgentDetail to MachineList uses launchSingleTop`() {
        val agentDetailBlock = navHostSource
            .substringAfter("composable(\n            Routes.AGENT_DETAIL", "")
            .ifEmpty { navHostSource.substringAfter("composable(") }
        val agentBlock = navHostSource
            .substringAfter("// Agent Detail Screen")
            .substringBefore("// Machine List Screen")
        val machineNavIdx = agentBlock.indexOf("machineListRoute(serverId)")
        assertTrue("AgentDetail must navigate to machineListRoute", machineNavIdx > 0)
        val afterNav = agentBlock.substring(machineNavIdx, minOf(machineNavIdx + 120, agentBlock.length))
        assertTrue(
            "AgentDetail -> MachineList navigation must use launchSingleTop",
            afterNav.contains("launchSingleTop = true")
        )
    }

    @Test
    fun `MachineList to AgentDetail uses launchSingleTop`() {
        val machineBlock = navHostSource
            .substringAfter("// Machine List Screen")
        val agentNavIdx = machineBlock.indexOf("agentDetailRoute(agentId)")
        assertTrue("MachineList must navigate to agentDetailRoute", agentNavIdx > 0)
        val afterNav = machineBlock.substring(agentNavIdx, minOf(agentNavIdx + 120, machineBlock.length))
        assertTrue(
            "MachineList -> AgentDetail navigation must use launchSingleTop",
            afterNav.contains("launchSingleTop = true")
        )
    }
}
