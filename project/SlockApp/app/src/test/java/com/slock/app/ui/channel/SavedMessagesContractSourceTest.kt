package com.slock.app.ui.channel

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedMessagesContractSourceTest {

    private fun channelModelSource(): String = listOf(
        File("src/main/java/com/slock/app/data/model/Channel.kt"),
        File("app/src/main/java/com/slock/app/data/model/Channel.kt")
    ).first { it.exists() }.readText()

    private fun apiResponseSource(): String = listOf(
        File("src/main/java/com/slock/app/data/model/ApiResponse.kt"),
        File("app/src/main/java/com/slock/app/data/model/ApiResponse.kt")
    ).first { it.exists() }.readText()

    private fun apiServiceSource(): String = listOf(
        File("src/main/java/com/slock/app/data/api/ApiService.kt"),
        File("app/src/main/java/com/slock/app/data/api/ApiService.kt")
    ).first { it.exists() }.readText()

    private fun repositorySource(): String = listOf(
        File("src/main/java/com/slock/app/data/repository/ChannelRepository.kt"),
        File("app/src/main/java/com/slock/app/data/repository/ChannelRepository.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `saved message dto names use message ids not channel ids`() {
        val source = channelModelSource()
        assertTrue(source.contains("data class SaveMessageRequest("))
        assertTrue(source.contains("@SerializedName(\"messageId\")"))
        assertTrue(source.contains("data class SavedMessagesCheckRequest("))
        assertTrue(source.contains("@SerializedName(\"messageIds\")"))
        assertTrue(source.contains("data class SavedMessagesCheckResponse("))
        assertTrue(source.contains("@SerializedName(\"savedIds\")"))
    }

    @Test
    fun `saved messages page response carries saved plus hasMore`() {
        val source = apiResponseSource()
        assertTrue(source.contains("data class SavedMessageItem("))
        assertTrue(source.contains("val messageId: String"))
        assertTrue(source.contains("data class SavedMessagesPageResponse("))
        assertTrue(source.contains("val saved: List<SavedMessageItem> = emptyList()"))
        assertTrue(source.contains("val hasMore: Boolean = false"))
    }

    @Test
    fun `ApiService saved endpoints match message-level contract`() {
        val source = apiServiceSource()
        assertTrue(source.contains("suspend fun getSavedMessages("))
        assertTrue(source.contains("@Query(\"limit\") limit: Int"))
        assertTrue(source.contains("@Query(\"offset\") offset: Int"))
        assertTrue(source.contains("Response<SavedMessagesPageResponse>"))
        assertTrue(source.contains("suspend fun saveMessage(@Body request: SaveMessageRequest): Response<Unit>"))
        assertTrue(source.contains("suspend fun checkSavedMessages(@Body request: SavedMessagesCheckRequest): Response<SavedMessagesCheckResponse>"))
        assertTrue(source.contains("@DELETE(\"channels/saved/{messageId}\")"))
        assertTrue(source.contains("@Path(\"messageId\") messageId: String"))
    }

    @Test
    fun `ChannelRepository exposes saved message page and batch check surface`() {
        val interfaceBlock = repositorySource()
            .substringAfter("interface ChannelRepository")
            .substringBefore("class ChannelRepositoryImpl")

        assertTrue(interfaceBlock.contains("suspend fun getSavedMessages(serverId: String, limit: Int, offset: Int): Result<SavedMessagesPageResponse>"))
        assertTrue(interfaceBlock.contains("suspend fun saveMessage(serverId: String, messageId: String): Result<Unit>"))
        assertTrue(interfaceBlock.contains("suspend fun removeSavedMessage(serverId: String, messageId: String): Result<Unit>"))
        assertTrue(interfaceBlock.contains("suspend fun checkSavedMessages(serverId: String, messageIds: List<String>): Result<List<String>>"))
    }

    @Test
    fun `ChannelRepositoryImpl delegates saved message contract to ApiService`() {
        val implBlock = repositorySource().substringAfter("class ChannelRepositoryImpl")

        assertTrue(implBlock.contains("override suspend fun getSavedMessages(serverId: String, limit: Int, offset: Int)"))
        assertTrue(implBlock.contains("apiService.getSavedMessages(limit = limit, offset = offset)"))
        assertTrue(implBlock.contains("override suspend fun saveMessage(serverId: String, messageId: String)"))
        assertTrue(implBlock.contains("apiService.saveMessage(SaveMessageRequest(messageId))"))
        assertTrue(implBlock.contains("override suspend fun checkSavedMessages(serverId: String, messageIds: List<String>)"))
        assertTrue(implBlock.contains("apiService.checkSavedMessages(SavedMessagesCheckRequest(messageIds))"))
        assertTrue(implBlock.contains("Result.success(response.body()!!.savedIds)"))
        assertTrue(implBlock.contains("override suspend fun removeSavedMessage(serverId: String, messageId: String)"))
        assertTrue(implBlock.contains("apiService.removeSavedMessage(messageId)"))
    }
}
