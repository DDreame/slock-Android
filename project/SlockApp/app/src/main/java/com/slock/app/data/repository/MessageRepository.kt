package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.*
import javax.inject.Inject

interface MessageRepository {
    suspend fun sendMessage(serverId: String, channelId: String, content: String, attachmentIds: List<String>? = null, asTask: Boolean = false): Result<Message>
    suspend fun getMessages(serverId: String, channelId: String, limit: Int = 50, before: String? = null, after: String? = null): Result<List<Message>>
    suspend fun searchMessages(serverId: String, query: String, searchServerId: String? = null, channelId: String? = null): Result<List<Message>>
}

class MessageRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val activeServerHolder: ActiveServerHolder
) : MessageRepository {

    override suspend fun sendMessage(
        serverId: String,
        channelId: String,
        content: String,
        attachmentIds: List<String>?,
        asTask: Boolean
    ): Result<Message> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.sendMessage(SendMessageRequest(channelId, content, attachmentIds, asTask))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Send message failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMessages(
        serverId: String,
        channelId: String,
        limit: Int,
        before: String?,
        after: String?
    ): Result<List<Message>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getMessages(channelId, limit, before, after)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get messages failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchMessages(
        serverId: String,
        query: String,
        searchServerId: String?,
        channelId: String?
    ): Result<List<Message>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.searchMessages(query, searchServerId, channelId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Search messages failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
