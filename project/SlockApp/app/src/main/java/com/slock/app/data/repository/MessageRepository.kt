package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.dao.MessageDao
import com.slock.app.data.local.toEntity
import com.slock.app.data.local.toModel
import com.slock.app.data.model.*
import android.util.Log
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

interface MessageRepository {
    suspend fun sendMessage(serverId: String, channelId: String, content: String, attachmentIds: List<String>? = null, asTask: Boolean = false, parentMessageId: String? = null): Result<Message>
    suspend fun getMessages(serverId: String, channelId: String, limit: Int = 50, before: String? = null, after: String? = null): Result<List<Message>>
    suspend fun refreshMessages(serverId: String, channelId: String, limit: Int = 50): Result<List<Message>>
    suspend fun searchMessages(serverId: String, query: String, searchServerId: String? = null, channelId: String? = null): Result<List<Message>>
    suspend fun getLatestMessagePerChannel(channelIds: List<String>): Map<String, Message>
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
        asTask: Boolean,
        parentMessageId: String?
    ): Result<Message> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.sendMessage(SendMessageRequest(channelId, content, attachmentIds, asTask, parentMessageId))
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
            val messages = fetchMessages(channelId, limit, before, after)
            if (messages != null) {
                messageDao.insertMessages(messages.map { it.toEntity() })
                messageDao.trimMessages(channelId, 200)
                Result.success(messages)
            } else {
                Result.failure(Exception("Get messages failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshMessages(serverId: String, channelId: String, limit: Int): Result<List<Message>> {
        return try {
            activeServerHolder.serverId = serverId
            val messages = fetchMessages(channelId, limit)
            if (messages != null) {
                messageDao.insertMessages(messages.map { it.toEntity() })
                Result.success(messages)
            } else {
                Result.failure(Exception("Refresh messages failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch messages handling both response formats:
     * - Wrapped: { "messages": [...] }
     * - Plain array: [...]
     * Matches JS frontend: `it.messages ?? it`
     */
    private suspend fun fetchMessages(channelId: String, limit: Int, before: String? = null, after: String? = null): List<Message>? {
        // Try wrapped format first
        try {
            val response = apiService.getMessages(channelId, limit, before, after)
            if (response.isSuccessful && response.body() != null) {
                val messages = response.body()!!.messages
                if (messages.isNotEmpty()) return messages
            }
        } catch (e: Exception) {
            Log.d("MessageRepo", "Wrapped format failed for $channelId, trying raw", e)
        }
        // Fallback: plain array (JS: it.messages ?? it)
        try {
            val response = apiService.getMessagesRaw(channelId, limit)
            if (response.isSuccessful && response.body() != null) {
                return response.body()!!
            }
        } catch (e: Exception) {
            Log.e("MessageRepo", "Raw format also failed for $channelId", e)
        }
        return null
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

    override suspend fun getLatestMessagePerChannel(channelIds: List<String>): Map<String, Message> {
        if (channelIds.isEmpty()) return emptyMap()
        return try {
            messageDao.getLatestMessagePerChannel(channelIds)
                .filter { !it.channelId.isNullOrEmpty() }
                .associate { it.channelId!! to it.toModel() }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
