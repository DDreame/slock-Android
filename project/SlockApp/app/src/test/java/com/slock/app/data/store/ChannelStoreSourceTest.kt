package com.slock.app.data.store

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChannelStoreSourceTest {

    private fun readSource(vararg candidates: String): String {
        return candidates.map { File(it) }.first { it.exists() }.readText()
    }

    private val storeSource = readSource(
        "src/main/java/com/slock/app/data/store/ChannelStore.kt",
        "app/src/main/java/com/slock/app/data/store/ChannelStore.kt"
    )

    private val vmSource = readSource(
        "src/main/java/com/slock/app/ui/channel/ChannelViewModel.kt",
        "app/src/main/java/com/slock/app/ui/channel/ChannelViewModel.kt"
    )

    // ── Store Shape ──

    @Test
    fun `ChannelStore is Singleton`() {
        assertTrue(storeSource.contains("@Singleton"))
    }

    @Test
    fun `ChannelStore has unreadCounts StateFlow`() {
        assertTrue(storeSource.contains("unreadCounts"))
        assertTrue(storeSource.contains("Map<String, Int>"))
    }

    @Test
    fun `ChannelStore has currentChannelId StateFlow`() {
        assertTrue(storeSource.contains("currentChannelId"))
        assertTrue(storeSource.contains("StateFlow<String?>"))
    }

    // ── Store Methods ──

    @Test
    fun `ChannelStore has setUnreadCounts method`() {
        assertTrue(storeSource.contains("fun setUnreadCounts("))
    }

    @Test
    fun `ChannelStore has incrementUnread method`() {
        assertTrue(storeSource.contains("fun incrementUnread("))
    }

    @Test
    fun `ChannelStore has clearUnread method`() {
        assertTrue(storeSource.contains("fun clearUnread("))
    }

    @Test
    fun `ChannelStore has setCurrentChannel method`() {
        assertTrue(storeSource.contains("fun setCurrentChannel("))
    }

    // ── Socket observation ──

    @Test
    fun `ChannelStore observes MessageNew socket event`() {
        assertTrue(storeSource.contains("MessageNew"))
    }

    @Test
    fun `ChannelStore increment guards on currentChannelId`() {
        val incrementBlock = storeSource
            .substringAfter("fun incrementUnread(")
            .substringBefore("}")
        assertTrue(
            "incrementUnread must check currentChannelId",
            incrementBlock.contains("currentChannelId")
        )
    }

    // ── ViewModel reads from store ──

    @Test
    fun `ChannelViewModel injects ChannelStore`() {
        assertTrue(
            vmSource.contains("channelStore") && vmSource.contains("ChannelStore")
        )
    }

    @Test
    fun `ChannelViewModel does not have mutable currentChannelId var`() {
        assertFalse(
            "ChannelViewModel must not have private var _currentChannelId",
            vmSource.contains("private var _currentChannelId")
        )
    }

    @Test
    fun `ChannelViewModel MessageNew handler does not update unreadCounts`() {
        val messageNewBlock = vmSource
            .substringAfter("is SocketIOManager.SocketEvent.MessageNew")
            .substringBefore("is SocketIOManager.SocketEvent.")
        assertFalse(
            "MessageNew handler must not directly update unreadCounts (store handles it)",
            messageNewBlock.contains("unreadCounts = updated") ||
                messageNewBlock.contains("unreadCounts = if")
        )
    }
}
