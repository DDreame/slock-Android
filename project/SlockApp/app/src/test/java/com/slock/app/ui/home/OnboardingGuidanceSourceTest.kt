package com.slock.app.ui.home

import com.slock.app.ui.navigation.Routes
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingGuidanceSourceTest {

    private val homeScreenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/home/HomeScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/home/HomeScreen.kt")
    ).first { it.exists() }.readText()

    private val navHostSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `HomeScreen accepts agentCount loading flag and onOpenAgents`() {
        val signature = homeScreenSource
            .substringAfter("fun HomeScreen(")
            .substringBefore(") {")

        assertTrue("HomeScreen must accept agentCount", signature.contains("agentCount: Int = 0"))
        assertTrue("HomeScreen must accept isAgentListLoading flag", signature.contains("isAgentListLoading: Boolean = false"))
        assertTrue("HomeScreen must accept onOpenAgents callback", signature.contains("onOpenAgents"))
    }

    @Test
    fun `HomeScreen contains zero server and first agent guidance copy`() {
        assertTrue(homeScreenSource.contains("Create your first server"))
        assertTrue(homeScreenSource.contains("Create your first agent"))
        assertTrue(homeScreenSource.contains("fun ZeroServerGuidanceCard("))
        assertTrue(homeScreenSource.contains("fun FirstAgentGuidanceCard("))
    }

    @Test
    fun `NavHost passes agentCount and wires Home open agents callback`() {
        val homeScreenCall = navHostSource
            .substringAfter("HomeScreen(")
            .substringBefore("threadsContent =")

        assertTrue(
            "NavHost must pass current agent count into HomeScreen",
            homeScreenCall.contains("agentCount = agentState.agents.size")
        )
        assertTrue(
            "NavHost must pass agent loading state into HomeScreen",
            homeScreenCall.contains("isAgentListLoading = agentState.isLoading")
        )
        assertTrue(
            "NavHost must wire onOpenAgents callback",
            homeScreenCall.contains("onOpenAgents =") && homeScreenCall.contains("Routes.agentListRoute(serverId)")
        )
    }

    @Test
    fun `Routes agentListRoute produces correct path`() {
        assertEquals("server/srv-1/agents", Routes.agentListRoute("srv-1"))
    }
}
