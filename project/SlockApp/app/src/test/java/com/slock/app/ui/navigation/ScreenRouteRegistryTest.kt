package com.slock.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenRouteRegistryTest {

    private val allScreens: List<Screen> = listOf(
        Screen.Splash, Screen.Login, Screen.Register, Screen.ForgotPassword,
        Screen.Home, Screen.AgentDetail, Screen.Profile, Screen.UserProfile, Screen.Settings
    )
    private val allRoutes = allScreens.map { it.route }.toSet()

    @Test
    fun `ghost routes Servers, ServerDetail, Channel, Messages, Agents are removed`() {
        val ghostRoutes = listOf("servers", "server/{serverId}", "channel/{channelId}", "messages/{channelId}", "agents")
        for (route in ghostRoutes) {
            assertFalse("Ghost route '$route' should not exist in Screen", allRoutes.contains(route))
        }
        val srcFile = listOf(
            java.io.File("src/main/java/com/slock/app/ui/navigation/Screen.kt"),
            java.io.File("app/src/main/java/com/slock/app/ui/navigation/Screen.kt")
        ).first { it.exists() }.readText()
        for (name in listOf("Servers", "ServerDetail", "Channel", "Messages", "Agents")) {
            assertFalse("Screen.$name should not exist in source", srcFile.contains("data object $name"))
        }
    }

    @Test
    fun `Screen sealed class contains only expected routes`() {
        val expectedRoutes = setOf(
            "splash", "login", "register", "forgot_password", "home",
            "agent/{agentId}", "profile", "profile/{userId}", "settings"
        )
        assertEquals(expectedRoutes, allRoutes)
        val srcFile = listOf(
            java.io.File("src/main/java/com/slock/app/ui/navigation/Screen.kt"),
            java.io.File("app/src/main/java/com/slock/app/ui/navigation/Screen.kt")
        ).first { it.exists() }.readText()
        val dataObjects = Regex("""data object (\w+)""").findAll(srcFile).map { it.groupValues[1] }.toSet()
        assertEquals(
            setOf("Splash", "Login", "Register", "ForgotPassword", "Home", "AgentDetail", "Profile", "UserProfile", "Settings"),
            dataObjects
        )
    }

    @Test
    fun `bottomNavItems is not accessible as a top-level declaration`() {
        try {
            val topLevelFields = Class.forName("com.slock.app.ui.navigation.ScreenKt").declaredFields
            val names = topLevelFields.map { it.name }.toSet()
            assertFalse("bottomNavItems should be removed", names.contains("bottomNavItems"))
        } catch (_: ClassNotFoundException) {
            // ScreenKt class doesn't exist = no top-level declarations remain = pass
        }
    }

    @Test
    fun `Routes agentDetailRoute produces correct path`() {
        assertEquals("agent/test-agent-123", Routes.agentDetailRoute("test-agent-123"))
    }

    @Test
    fun `Routes machineListRoute produces correct path`() {
        assertEquals("server/srv-1/machines", Routes.machineListRoute("srv-1"))
    }

    @Test
    fun `Routes dmMessagesRoute produces correct path`() {
        assertEquals("channel/dm-1/messages?name=Claude", Routes.dmMessagesRoute("dm-1", "Claude"))
    }

    @Test
    fun `Routes dmMessagesRoute encodes special characters in name`() {
        assertEquals("channel/dm-2/messages?name=Agent%20Bot", Routes.dmMessagesRoute("dm-2", "Agent Bot"))
    }

    @Test
    fun `Routes dmMessagesRoute matches MESSAGES template structure`() {
        val route = Routes.dmMessagesRoute("ch-123", "TestName")
        assertTrue("DM route must start with channel/ prefix", route.startsWith("channel/"))
        assertTrue("DM route must contain /messages?name=", route.contains("/messages?name="))
    }
}
