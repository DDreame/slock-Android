package com.slock.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenRouteRegistryTest {

    private val allScreens = Screen::class.sealedSubclasses.map { it.objectInstance!! }
    private val allRoutes = allScreens.map { it.route }.toSet()

    @Test
    fun `ghost routes Servers, ServerDetail, Channel, Messages, Agents are removed`() {
        val ghostRoutes = listOf("servers", "server/{serverId}", "channel/{channelId}", "messages/{channelId}", "agents")
        for (route in ghostRoutes) {
            assertFalse("Ghost route '$route' should not exist in Screen", allRoutes.contains(route))
        }
    }

    @Test
    fun `Screen sealed class contains only expected routes`() {
        val expectedRoutes = setOf(
            "splash", "login", "register", "forgot_password", "home",
            "agent/{agentId}", "profile", "profile/{userId}", "settings"
        )
        assertEquals(expectedRoutes, allRoutes)
    }

    @Test
    fun `bottomNavItems is not accessible as a top-level declaration`() {
        val topLevelFields = Class.forName("com.slock.app.ui.navigation.ScreenKt").declaredFields
        val names = topLevelFields.map { it.name }.toSet()
        assertFalse("bottomNavItems should be removed", names.contains("bottomNavItems"))
    }

    @Test
    fun `Routes agentDetailRoute produces correct path`() {
        assertEquals("agent/test-agent-123", Routes.agentDetailRoute("test-agent-123"))
    }

    @Test
    fun `Routes machineListRoute produces correct path`() {
        assertEquals("server/srv-1/machines", Routes.machineListRoute("srv-1"))
    }
}
