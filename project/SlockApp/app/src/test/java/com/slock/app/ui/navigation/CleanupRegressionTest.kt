package com.slock.app.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CleanupRegressionTest {

    private val srcRoot: File = listOf(
        File("src/main/java/com/slock/app/ui"),
        File("app/src/main/java/com/slock/app/ui")
    ).firstOrNull { it.isDirectory }
        ?: error("Cannot locate source root. Working dir: ${System.getProperty("user.dir")}")

    private fun readSource(relativePath: String): String {
        val file = File(srcRoot, relativePath)
        assertTrue("Source file must exist: ${file.absolutePath}", file.exists())
        return file.readText()
    }

    @Test
    fun `MessageListScreen does not contain removed inactive action sheet items`() {
        val source = readSource("message/MessageListScreen.kt")
        val removedLabels = listOf("Pin Message")
        for (label in removedLabels) {
            assertFalse(
                "MessageListScreen must not contain removed action '$label'",
                source.contains(label)
            )
        }
    }

    @Test
    fun `MessageListScreen does not contain inert overflow button`() {
        val source = readSource("message/MessageListScreen.kt")
        assertFalse(
            "MessageListScreen must not contain the vertical ellipsis overflow icon",
            source.contains("\\u22EE") || source.contains("\u22EE")
        )
    }

    @Test
    fun `ThreadReplyScreen does not contain Mark as resolved stub`() {
        val source = readSource("thread/ThreadReplyScreen.kt")
        assertFalse(
            "ThreadReplyScreen must not contain 'Mark as resolved' stub",
            source.contains("Mark as resolved")
        )
    }

    @Test
    fun `AgentListScreen does not contain Refresh Status button`() {
        val source = readSource("agent/AgentListScreen.kt")
        assertFalse(
            "AgentListScreen must not contain 'Refresh Status' action",
            source.contains("Refresh Status")
        )
    }

    @Test
    fun `AgentListScreen does not contain commented-out Machines section`() {
        val source = readSource("agent/AgentListScreen.kt")
        assertFalse(
            "AgentListScreen must not contain commented-out MachineCard references",
            source.contains("// MachineCard()")
        )
    }

    @Test
    fun `HomeScreen notification bell is removed from NeoTopBar`() {
        val source = readSource("home/HomeScreen.kt")
        assertFalse(
            "HomeScreen NeoTopBar must not contain onNotificationClick parameter",
            source.contains("onNotificationClick")
        )
    }

    @Test
    fun `MachineListScreen does not contain AddMachineCard`() {
        val source = readSource("machine/MachineListScreen.kt")
        assertFalse(
            "MachineListScreen must not contain AddMachineCard",
            source.contains("AddMachineCard")
        )
    }

    @Test
    fun `SettingsScreen does not contain placeholder theme text`() {
        val source = readSource("settings/SettingsScreen.kt")
        assertFalse(
            "SettingsScreen must not contain 'Theme switching is not exposed yet'",
            source.contains("Theme switching is not exposed yet")
        )
    }

    @Test
    fun `LoginScreen does not contain invite link dialog`() {
        val source = readSource("auth/LoginScreen.kt")
        assertFalse(
            "LoginScreen must not contain InviteLinkDialog",
            source.contains("InviteLinkDialog")
        )
    }

    @Test
    fun `RegisterScreen does not contain email verification dialog`() {
        val source = readSource("auth/RegisterScreen.kt")
        assertFalse(
            "RegisterScreen must not contain EmailVerificationDialog",
            source.contains("EmailVerificationDialog")
        )
    }

    @Test
    fun `NavHost standalone AgentList onAgentClick is wired to navigate`() {
        val source = readSource("navigation/NavHost.kt")
        assertFalse(
            "NavHost standalone AgentList onAgentClick must not be an empty lambda",
            source.contains("onAgentClick = { }")
        )
        assertTrue(
            "NavHost standalone AgentList onAgentClick must navigate to agentDetailRoute",
            source.contains("onAgentClick = { agentId ->") &&
                source.contains("Routes.agentDetailRoute(") &&
                source.contains("agentId = agentId") &&
                source.contains("serverId = serverId")
        )
    }

    @Test
    fun `NavHost AgentDetail DM click uses Routes dmMessagesRoute`() {
        val source = readSource("navigation/NavHost.kt")
        assertTrue(
            "NavHost AgentDetail DM must use Routes.dmMessagesRoute for navigation",
            source.contains("Routes.dmMessagesRoute(")
        )
    }
}
