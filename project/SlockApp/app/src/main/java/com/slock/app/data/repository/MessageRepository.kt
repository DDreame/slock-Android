package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.dao.MessageDao
import com.slock.app.data.local.toEntity
import com.slock.app.data.local.toModel
import com.slock.app.data.model.*
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

interface MessageRepository {
    suspend fun sendMessage(serverId: String, channelId: String, content: String, attachmentIds: List<String>? = null, asTask: Boolean = false): Result<Message>
    suspend fun getMessages(serverId: String, channelId: String, limit: Int = 50, before: String? = null, after: String? = null): Result<List<Message>>
    suspend fun refreshMessages(serverId: String, channelId: String, limit: Int = 50): Result<List<Message>>
    suspend fun searchMessages(serverId: String, query: String, searchServerId: String? = null, channelId: String? = null): Result<List<Message>>
}

class MessageRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val activeServerHolder: ActiveServerHolder,
    private val messageDao: MessageDao
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
                val message = response.body()!!
                messageDao.insertMessage(message.toEntity())
                Result.success(message)
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
        activeServerHolder.serverId = serverId

        // Only use cache for initial load (no pagination params)
        if (before == null && after == null) {
            val cached = try {
                messageDao.getMessagesByChannel(channelId, limit).firstOrNull()?.map { it.toModel() } ?: emptyList()
            } catch (e: Exception) { emptyList() }

            if (cached.isNotEmpty()) {
                return Result.success(cached.sortedBy { it.seq })
            }
        }

        return try {
            val response = apiService.getMessages(channelId, limit, before, after)
            if (response.isSuccessful && response.body() != null) {
                val messages = response.body()!!.messages
                messageDao.insertMessages(messages.map { it.toEntity() })
                Result.success(messages)
            } else {
                Result.failure(Exception("Get messages failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshMessages(serverId: String, channelId: String, limit: Int): Result<List<Message>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getMessages(channelId, limit)
            if (response.isSuccessful && response.body() != null) {
                val messages = response.body()!!.messages
                messageDao.insertMessages(messages.map { it.toEntity() })
                Result.success(messages)
            } else {
                Result.failure(Exception("Refresh messages failed: ${response.code()}"))
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
                Result.success(response.body()!!.results)
            } else {
                Result.failure(Exception("Search messages failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
