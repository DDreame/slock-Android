package com.slock.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CrossNavStrategyTest {

    @Test
    fun `resolveAgentToMachineNav returns null for blank serverId`() {
        assertNull(resolveAgentToMachineNav(""))
        assertNull(resolveAgentToMachineNav("   "))
    }

    @Test
    fun `resolveAgentToMachineNav pops AgentDetail inclusively`() {
        val action = resolveAgentToMachineNav("srv-1")!!
        assertEquals(Routes.AGENT_DETAIL, action.popUpToRoute)
        assertTrue("Must pop AgentDetail inclusively to replace it", action.inclusive)
    }

    @Test
    fun `resolveAgentToMachineNav targets correct MachineList route`() {
        val action = resolveAgentToMachineNav("srv-1")!!
        assertEquals("server/srv-1/machines", action.route)
    }

    @Test
    fun `resolveMachineToAgentNav returns null for blank agentId`() {
        assertNull(resolveMachineToAgentNav(""))
        assertNull(resolveMachineToAgentNav("   "))
    }

    @Test
    fun `resolveMachineToAgentNav pops MachineList inclusively`() {
        val action = resolveMachineToAgentNav("agent-1")!!
        assertEquals(Routes.MACHINE_LIST, action.popUpToRoute)
        assertTrue("Must pop MachineList inclusively to replace it", action.inclusive)
    }

    @Test
    fun `resolveMachineToAgentNav targets correct AgentDetail route`() {
        val action = resolveMachineToAgentNav("agent-1")!!
        assertTrue(action.route.startsWith("agent/agent-1"))
    }

    @Test
    fun `resolveMachineToAgentNav carries serverId fallback when provided`() {
        val action = resolveMachineToAgentNav("agent-1", "srv-1")!!
        assertTrue(action.route.contains("serverId=srv-1"))
    }

    @Test
    fun `resolveAgentDetailServerId prefers route fallback over active holder`() {
        assertEquals("route-srv", resolveAgentDetailServerId("route-srv", "active-srv"))
        assertEquals("active-srv", resolveAgentDetailServerId(null, "active-srv"))
        assertNull(resolveAgentDetailServerId("   ", null))
    }

    @Test
    fun `cross-nav cycle produces constant stack depth`() {
        val a2m = resolveAgentToMachineNav("srv-1")!!
        val m2a = resolveMachineToAgentNav("agent-2")!!
        assertTrue(
            "AgentDetail->MachineList must pop AgentDetail (inclusive) so stack doesn't grow",
            a2m.inclusive && a2m.popUpToRoute == Routes.AGENT_DETAIL
        )
        assertTrue(
            "MachineList->AgentDetail must pop MachineList (inclusive) so stack doesn't grow",
            m2a.inclusive && m2a.popUpToRoute == Routes.MACHINE_LIST
        )
    }
}

class WarmDeepLinkNavStrategyTest {

    @Test
    fun `resolveWarmStartDeepLinkNav pops to HOME non-inclusively`() {
        val action = resolveWarmStartDeepLinkNav("ch-1", "general")
        assertEquals(Routes.HOME, action.popUpToRoute)
        assertFalse("HOME must stay in stack (inclusive=false)", action.inclusive)
    }

    @Test
    fun `resolveWarmStartDeepLinkNav uses singleTop`() {
        val action = resolveWarmStartDeepLinkNav("ch-1", "general")
        assertTrue(action.singleTop)
    }

    @Test
    fun `resolveWarmStartDeepLinkNav produces correct message route`() {
        val action = resolveWarmStartDeepLinkNav("ch-1", "general")
        assertTrue(action.route.startsWith("channel/ch-1/messages"))
        assertTrue(action.route.contains("name=general"))
    }

    @Test
    fun `resolveWarmStartDeepLinkNav handles null channelName`() {
        val action = resolveWarmStartDeepLinkNav("ch-1", null)
        assertTrue(action.route.startsWith("channel/ch-1/messages"))
    }
}

class NavBackSemanticsStructuralTest {

    private val navHostSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `Home composable contains BackHandler`() {
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
    fun `warm-start deep link delegates to resolveWarmStartDeepLinkNav`() {
        val warmStartBlock = navHostSource
            .substringAfter("shouldHandleWarmStartDeepLink")
            .substringBefore("onDeepLinkConsumed()")
        assertTrue(
            "Warm-start deep link must use resolveWarmStartDeepLinkNav helper",
            warmStartBlock.contains("resolveWarmStartDeepLinkNav(")
        )
    }

    @Test
    fun `AgentDetail to MachineList delegates to resolveAgentToMachineNav`() {
        val agentBlock = navHostSource
            .substringAfter("// Agent Detail Screen")
            .substringBefore("// Machine List Screen")
        assertTrue(
            "AgentDetail -> MachineList must use resolveAgentToMachineNav helper",
            agentBlock.contains("resolveAgentToMachineNav(")
        )
    }

    @Test
    fun `AgentDetail shows explicit server context unavailable toast when machine fallback is missing`() {
        val agentBlock = navHostSource
            .substringAfter("// Agent Detail Screen")
            .substringBefore("// Machine List Screen")
        assertTrue(
            "AgentDetail must show explicit server context unavailable feedback instead of silent fail",
            agentBlock.contains("Server context unavailable")
        )
    }

    @Test
    fun `MachineList to AgentDetail delegates to resolveMachineToAgentNav`() {
        val machineBlock = navHostSource
            .substringAfter("// Machine List Screen")
        assertTrue(
            "MachineList -> AgentDetail must use resolveMachineToAgentNav helper",
            machineBlock.contains("resolveMachineToAgentNav(")
        )
    }

    @Test
    fun `MachineList to AgentDetail passes serverId fallback into cross-nav helper`() {
        val machineBlock = navHostSource
            .substringAfter("// Machine List Screen")
        assertTrue(
            "MachineList -> AgentDetail must preserve serverId fallback when cross-navigating",
            machineBlock.contains("resolveMachineToAgentNav(agentId, serverId)")
        )
    }

    @Test
    fun `cross-nav uses popUpTo from action not launchSingleTop alone`() {
        val agentBlock = navHostSource
            .substringAfter("// Agent Detail Screen")
            .substringBefore("// Machine List Screen")
        val machineBlock = navHostSource
            .substringAfter("// Machine List Screen")

        assertTrue(
            "AgentDetail cross-nav must use action.popUpToRoute",
            agentBlock.contains("popUpTo(action.popUpToRoute)")
        )
        assertTrue(
            "MachineList cross-nav must use action.popUpToRoute",
            machineBlock.contains("popUpTo(action.popUpToRoute)")
        )
    }
}
