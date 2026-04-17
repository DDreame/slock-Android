package com.slock.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThreadListServerNameTest {

    @Test
    fun `resolveThreadListServerName returns server name when available`() {
        assertEquals("Acme Server", resolveThreadListServerName("srv-1", "Acme Server"))
    }

    @Test
    fun `resolveThreadListServerName falls back to serverId when name is null`() {
        assertEquals("srv-1", resolveThreadListServerName("srv-1", null))
    }

    @Test
    fun `resolveThreadListServerName falls back to serverId when name is blank`() {
        assertEquals("srv-1", resolveThreadListServerName("srv-1", ""))
        assertEquals("srv-1", resolveThreadListServerName("srv-1", "   "))
    }

    @Test
    fun `thread context label uses server name not raw id`() {
        val serverName = resolveThreadListServerName("abc123", "My Team")
        val label = Routes.buildContextLabel(serverName, "Threads")
        assertEquals("My Team · Threads", label)
    }
}

class ThreadListContextStructuralTest {

    private val navHostSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `standalone ThreadList uses resolveThreadListServerName not raw serverId`() {
        val threadBlock = navHostSource
            .substringAfter("// Thread List Screen")
            .substringBefore("// Task List Screen")
        assertTrue(
            "ThreadList context must use resolveThreadListServerName helper",
            threadBlock.contains("resolveThreadListServerName(")
        )
    }

    @Test
    fun `standalone ThreadList looks up server name from ServerViewModel state`() {
        val threadBlock = navHostSource
            .substringAfter("// Thread List Screen")
            .substringBefore("// Task List Screen")
        assertTrue(
            "ThreadList must get ServerViewModel to resolve server name",
            threadBlock.contains("ServerViewModel")
        )
    }
}
