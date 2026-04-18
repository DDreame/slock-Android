package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.*
import javax.inject.Inject

interface ThreadRepository {
    suspend fun getFollowedThreads(serverId: String): Result<List<ThreadSummary>>
    suspend fun getThreadMessages(serverId: String, channelId: String, limit: Int = 50, before: String? = null): Result<List<Message>>
    suspend fun getThreadReplies(serverId: String, threadId: String, limit: Int = 50, before: String? = null): Result<List<Message>>
    suspend fun followThread(serverId: String, parentMessageId: String): Result<Unit>
    suspend fun unfollowThread(serverId: String, threadChannelId: String): Result<Unit>
    suspend fun markThreadDone(serverId: String, threadChannelId: String): Result<Unit>
    suspend fun undoThreadDone(serverId: String, threadChannelId: String): Result<Unit>
}

class ThreadRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val activeServerHolder: ActiveServerHolder
) : ThreadRepository {

    override suspend fun getFollowedThreads(serverId: String): Result<List<ThreadSummary>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getFollowedThreads()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.threads)
            } else {
                Result.failure(Exception("Get followed threads failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getThreadMessages(serverId: String, channelId: String, limit: Int, before: String?): Result<List<Message>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getThreadMessages(channelId, limit, before)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.messages)
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
                Result.success(response.body()!!.messages)
            } else {
                Result.failure(Exception("Get thread replies failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun followThread(serverId: String, parentMessageId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.followThread(FollowThreadRequest(parentMessageId))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Follow thread failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfollowThread(serverId: String, threadChannelId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.unfollowThread(ThreadChannelIdRequest(threadChannelId))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Unfollow thread failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markThreadDone(serverId: String, threadChannelId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.markThreadDone(ThreadChannelIdRequest(threadChannelId))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Mark thread done failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun undoThreadDone(serverId: String, threadChannelId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.undoThreadDone(ThreadChannelIdRequest(threadChannelId))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Undo thread done failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
