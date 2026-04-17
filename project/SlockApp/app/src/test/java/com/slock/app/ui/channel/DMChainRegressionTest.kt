package com.slock.app.ui.channel

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DMChainRegressionTest {

    private fun readSource(vararg candidates: String): String {
        return candidates.map { File(it) }.first { it.exists() }.readText()
    }

    private val channelRepoSource = readSource(
        "src/main/java/com/slock/app/data/repository/ChannelRepository.kt",
        "app/src/main/java/com/slock/app/data/repository/ChannelRepository.kt"
    )

    private val viewModelSource = readSource(
        "src/main/java/com/slock/app/ui/channel/ChannelViewModel.kt",
        "app/src/main/java/com/slock/app/ui/channel/ChannelViewModel.kt"
    )

    private val notificationServiceSource = readSource(
        "src/main/java/com/slock/app/service/SocketNotificationService.kt",
        "app/src/main/java/com/slock/app/service/SocketNotificationService.kt"
    )

    private val navHostSource = readSource(
        "src/main/java/com/slock/app/ui/navigation/NavHost.kt",
        "app/src/main/java/com/slock/app/ui/navigation/NavHost.kt"
    )

    @Test
    fun `createDM persists new channel to Room via channelDao`() {
        val createDMBlock = channelRepoSource
            .substringAfter("override suspend fun createDM(")
            .substringBefore("override suspend fun")
        assertTrue(
            "createDM must persist new DM to Room via channelDao.insertChannel",
            createDMBlock.contains("channelDao.insertChannel(")
        )
    }

    @Test
    fun `createDM in ViewModel awaits dmsLoaded before checking`() {
        val createDMBlock = viewModelSource
            .substringAfter("fun createDM(")
            .substringBefore("fun findExistingDM(")
        val awaitIdx = createDMBlock.indexOf("dmsLoaded.await()")
        val findExistingIdx = createDMBlock.indexOf("findExistingDM(")
        assertTrue("createDM must await dmsLoaded", awaitIdx >= 0)
        assertTrue("findExistingDM must be called after await", findExistingIdx > awaitIdx)
    }

    @Test
    fun `createDM in ViewModel checks findExistingDM before API call`() {
        val createDMBlock = viewModelSource
            .substringAfter("fun createDM(")
            .substringBefore("fun findExistingDM(")
        val findExistingIdx = createDMBlock.indexOf("findExistingDM(")
        val repoCallIdx = createDMBlock.indexOf("channelRepository.createDM(")
        assertTrue("findExistingDM must be called in createDM", findExistingIdx >= 0)
        assertTrue("channelRepository.createDM must be called in createDM", repoCallIdx >= 0)
        assertTrue(
            "findExistingDM must be checked BEFORE channelRepository.createDM",
            findExistingIdx < repoCallIdx
        )
    }

    @Test
    fun `createDM in ViewModel calls socketIOManager joinChannel on success`() {
        val createDMBlock = viewModelSource
            .substringAfter("fun createDM(")
            .substringBefore("fun findExistingDM(")
        assertTrue(
            "createDM must join socket channel for new DM",
            createDMBlock.contains("socketIOManager.joinChannel(")
        )
    }

    @Test
    fun `findExistingDM function exists and checks members`() {
        assertTrue(
            "findExistingDM must exist in ChannelViewModel",
            viewModelSource.contains("fun findExistingDM(")
        )
        val findBlock = viewModelSource.substringAfter("fun findExistingDM(")
        assertTrue(
            "findExistingDM must check members for matching agentId",
            findBlock.contains("member.agentId")
        )
        assertTrue(
            "findExistingDM must check members for matching userId",
            findBlock.contains("member.userId")
        )
    }

    @Test
    fun `SocketNotificationService handles DMNew event`() {
        assertTrue(
            "SocketNotificationService must handle DMNew socket events",
            notificationServiceSource.contains("SocketEvent.DMNew")
        )
        val dmNewBlock = notificationServiceSource
            .substringAfter("SocketEvent.DMNew")
            .substringBefore("else ->")
        assertTrue(
            "DMNew handler must join socket channel",
            dmNewBlock.contains("joinChannel(")
        )
        assertTrue(
            "DMNew handler must persist to channelDao",
            dmNewBlock.contains("channelDao.insertChannel(")
        )
    }

    @Test
    fun `ChannelViewModel handles DMNew event with full DM refresh`() {
        assertTrue(
            "ChannelViewModel must handle DMNew socket events",
            viewModelSource.contains("SocketEvent.DMNew")
        )
        val dmNewBlock = viewModelSource
            .substringAfter("SocketEvent.DMNew")
            .substringBefore("else ->")
        assertTrue(
            "DMNew handler must join socket channel for new DM",
            dmNewBlock.contains("joinChannel(")
        )
        assertTrue(
            "DMNew handler must refresh full DM list to get members for reopen matching",
            dmNewBlock.contains("refreshDMs()")
        )
    }

    @Test
    fun `loadDMs signals dmsLoaded completion`() {
        val loadDMsBlock = viewModelSource
            .substringAfter("fun loadDMs()")
            .substringBefore("fun createDM(")
        assertTrue(
            "loadDMs must signal dmsLoaded.complete on success",
            loadDMsBlock.contains("dmsLoaded.complete(")
        )
    }

    @Test
    fun `AgentList screen ensures DMs are loaded before createDM`() {
        val agentListIdx = navHostSource.indexOf("AgentListScreen(")
        assertTrue("AgentListScreen composable must exist", agentListIdx > 0)
        val beforeAgentList = navHostSource.substring(0, agentListIdx)
        assertTrue(
            "Agent screens must call ensureDMsLoaded before createDM can be invoked",
            beforeAgentList.contains("ensureDMsLoaded()")
        )
    }

    @Test
    fun `AgentDetail screen ensures DMs are loaded before createDM`() {
        val agentDetailIdx = navHostSource.indexOf("Routes.AGENT_DETAIL")
        assertTrue("Routes.AGENT_DETAIL composable must exist", agentDetailIdx > 0)
        val agentDetailSection = navHostSource.substring(agentDetailIdx)
            .substringBefore("AgentDetailScreen(")
        assertTrue(
            "AgentDetail must call ensureDMsLoaded before createDM can be invoked",
            agentDetailSection.contains("ensureDMsLoaded()")
        )
    }
}
