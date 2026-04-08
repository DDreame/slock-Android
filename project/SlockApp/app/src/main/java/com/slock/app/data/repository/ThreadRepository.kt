package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.*
import javax.inject.Inject

interface ThreadRepository {
    suspend fun getThreadMessages(serverId: String, channelId: String, limit: Int = 50, before: String? = null): Result<List<Message>>
    suspend fun getThreadReplies(serverId: String, threadId: String, limit: Int = 50, before: String? = null): Result<List<Message>>
}

class ThreadRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val activeServerHolder: ActiveServerHolder
) : ThreadRepository {

    override suspend fun getThreadMessages(serverId: String, channelId: String, limit: Int, before: String?): Result<List<Message>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getThreadMessages(channelId, limit, before)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get thread messages failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getThreadReplies(serverId: String, threadId: String, limit: Int, before: String?): Result<List<Message>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getThreadReplies(threadId, limit, before)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get thread replies failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
