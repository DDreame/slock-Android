package com.slock.app.ui.channel

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UnreadBadgeApiTest {

    private val apiServiceSource: String = listOf(
        File("src/main/java/com/slock/app/data/api/ApiService.kt"),
        File("app/src/main/java/com/slock/app/data/api/ApiService.kt")
    ).first { it.exists() }.readText()

    private val channelRepoSource: String = listOf(
        File("src/main/java/com/slock/app/data/repository/ChannelRepository.kt"),
        File("app/src/main/java/com/slock/app/data/repository/ChannelRepository.kt")
    ).first { it.exists() }.readText()

    private val viewModelSource: String = listOf(
        File("src/main/java/com/slock/app/ui/channel/ChannelViewModel.kt"),
        File("app/src/main/java/com/slock/app/ui/channel/ChannelViewModel.kt")
    ).first { it.exists() }.readText()

    private val storeSource: String = listOf(
        File("src/main/java/com/slock/app/data/store/ChannelStore.kt"),
        File("app/src/main/java/com/slock/app/data/store/ChannelStore.kt")
    ).first { it.exists() }.readText()

    private val homeScreenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/home/HomeScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/home/HomeScreen.kt")
    ).first { it.exists() }.readText()

    private val navHostSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `ApiService getUnreadChannels returns Map of String to Int`() {
        assertTrue(
            "getUnreadChannels must return Response<Map<String, Int>>",
            apiServiceSource.contains("suspend fun getUnreadChannels(): Response<Map<String, Int>>")
        )
    }

    @Test
    fun `ChannelRepository interface returns Map of String to Int`() {
        val interfaceBlock = channelRepoSource
            .substringAfter("interface ChannelRepository")
            .substringBefore("class ChannelRepositoryImpl")
        assertTrue(
            "ChannelRepository getUnreadChannels must return Result<Map<String, Int>>",
            interfaceBlock.contains("Result<Map<String, Int>>")
        )
    }

    @Test
    fun `ChannelRepositoryImpl getUnreadChannels returns Map of String to Int`() {
        val implBlock = channelRepoSource
            .substringAfter("class ChannelRepositoryImpl")
        assertTrue(
            "ChannelRepositoryImpl must implement getUnreadChannels with Map return",
            implBlock.contains("override suspend fun getUnreadChannels") &&
                implBlock.contains("Result<Map<String, Int>>")
        )
    }

    @Test
    fun `ChannelUiState has unreadCounts field`() {
        assertTrue(
            "ChannelUiState must have val unreadCounts: Map<String, Int>",
            viewModelSource.contains("val unreadCounts: Map<String, Int>")
        )
    }

    @Test
    fun `loadChannels calls getUnreadChannels`() {
        val loadBlock = viewModelSource
            .substringAfter("fun loadChannels(")
            .substringBefore("fun createChannel(")
        assertTrue(
            "loadChannels must call getUnreadChannels",
            loadBlock.contains("getUnreadChannels")
        )
        assertTrue(
            "loadChannels must delegate unread counts to store",
            loadBlock.contains("channelStore.setUnreadCounts")
        )
    }

    @Test
    fun `MessageNew unread increment handled by ChannelStore`() {
        assertTrue(
            "ChannelStore must handle MessageNew for unread increment",
            storeSource.contains("MessageNew") && storeSource.contains("incrementUnread")
        )
    }

    @Test
    fun `ChannelStore incrementUnread guards on currentChannelId`() {
        val incrementBlock = storeSource
            .substringAfter("fun incrementUnread(")
            .substringBefore("}")
        assertTrue(
            "incrementUnread must check currentChannelId",
            incrementBlock.contains("currentChannelId")
        )
    }

    @Test
    fun `clearUnreadCount method exists and clears count`() {
        assertTrue(
            "ChannelViewModel must have clearUnreadCount method",
            viewModelSource.contains("fun clearUnreadCount(channelId: String)")
        )
        val method = viewModelSource
            .substringAfter("fun clearUnreadCount(")
            .substringBefore("fun loadChannelAgents(")
        assertTrue(
            "clearUnreadCount must remove from unreadCounts",
            method.contains("unreadCounts")
        )
        assertTrue(
            "clearUnreadCount must call markChannelRead",
            method.contains("markChannelRead")
        )
    }

    @Test
    fun `HomeScreen ChannelItem receives unreadCount from state`() {
        val channelListBlock = homeScreenSource
            .substringAfter("fun ChannelListContent(")
            .substringBefore("SectionHeader(title = \"DIRECT MESSAGES\"")
        assertTrue(
            "ChannelItem must receive unreadCount from channelState.unreadCounts",
            channelListBlock.contains("unreadCount = channelState.unreadCounts")
        )
    }

    @Test
    fun `NavHost message screen calls clearUnreadCount in LaunchedEffect`() {
        val messageBlock = navHostSource
            .substringAfter("// Message List Screen")
            .substringBefore("// Thread")
        assertTrue(
            "Message screen LaunchedEffect must call clearUnreadCount for all entry paths",
            messageBlock.contains("clearUnreadCount")
        )
    }

    @Test
    fun `clearCurrentChannel method exists`() {
        assertTrue(
            "ChannelViewModel must have clearCurrentChannel method",
            viewModelSource.contains("fun clearCurrentChannel()")
        )
        val method = viewModelSource
            .substringAfter("fun clearCurrentChannel()")
            .substringBefore("fun loadChannelAgents(")
        assertTrue(
            "clearCurrentChannel must delegate to channelStore",
            method.contains("channelStore.setCurrentChannel(null)")
        )
    }

    @Test
    fun `NavHost message screen clears current channel on dispose`() {
        val messageBlock = navHostSource
            .substringAfter("// Message List Screen")
            .substringBefore("// Thread")
        assertTrue(
            "Message screen must use DisposableEffect to clear current channel",
            messageBlock.contains("DisposableEffect") &&
                messageBlock.contains("clearCurrentChannel")
        )
    }
}
