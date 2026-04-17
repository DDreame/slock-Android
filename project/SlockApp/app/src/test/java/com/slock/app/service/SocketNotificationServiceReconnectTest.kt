package com.slock.app.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SocketNotificationServiceReconnectTest {

    private fun readSource(vararg candidates: String): String {
        return candidates.map { File(it) }.first { it.exists() }.readText()
    }

    private val serviceSource = readSource(
        "src/main/java/com/slock/app/service/SocketNotificationService.kt",
        "app/src/main/java/com/slock/app/service/SocketNotificationService.kt"
    )

    @Test
    fun `rejoinCachedChannelsForServer rejoins every cached room for current server`() = runTest {
        val joined = mutableListOf<String>()

        val result = rejoinCachedChannelsForServer(
            serverId = "server-1",
            loadChannelIds = { serverId ->
                assertEquals("server-1", serverId)
                listOf("channel-1", "dm-1", "channel-2")
            },
            joinChannel = { joined += it }
        )

        assertEquals(listOf("channel-1", "dm-1", "channel-2"), result)
        assertEquals(listOf("channel-1", "dm-1", "channel-2"), joined)
    }

    @Test
    fun `rejoinCachedChannelsForServer skips when server is blank`() = runTest {
        var loaderCalls = 0
        val joined = mutableListOf<String>()

        val result = rejoinCachedChannelsForServer(
            serverId = "  ",
            loadChannelIds = {
                loaderCalls++
                listOf("should-not-join")
            },
            joinChannel = { joined += it }
        )

        assertTrue(result.isEmpty())
        assertEquals(0, loaderCalls)
        assertTrue(joined.isEmpty())
    }

    @Test
    fun `service observes connection state and rejoins cached channels on connect`() {
        val observeBlock = serviceSource
            .substringAfter("private fun observeConnectionState()")
            .substringBefore("private fun joinCachedChannels(")

        assertTrue(
            "SocketNotificationService must collect socketManager.connectionState",
            observeBlock.contains("socketManager.connectionState.collect")
        )
        assertTrue(
            "SocketNotificationService must rejoin cached rooms on CONNECTED",
            observeBlock.contains("SocketIOManager.ConnectionState.CONNECTED") &&
                observeBlock.contains("joinCachedChannels(activeServerHolder.serverId)")
        )
    }

    @Test
    fun `onStartCommand starts connection observer before socket ensure path`() {
        val startCommandBlock = serviceSource
            .substringAfter("override fun onStartCommand")
            .substringBefore("private fun ensureSocketConnected()")
        val observeIdx = startCommandBlock.indexOf("observeConnectionState()")
        val ensureIdx = startCommandBlock.indexOf("ensureSocketConnected()")

        assertTrue("onStartCommand must observe connection state", observeIdx >= 0)
        assertTrue("onStartCommand must still ensure socket connection", ensureIdx >= 0)
        assertTrue(
            "Connection observer must be attached before ensureSocketConnected()",
            observeIdx < ensureIdx
        )
    }
}
