package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.*
import javax.inject.Inject

interface ChannelRepository {
    suspend fun getChannels(serverId: String): Result<List<Channel>>
    suspend fun createChannel(serverId: String, name: String, type: String = "text"): Result<Channel>
    suspend fun updateChannel(serverId: String, channelId: String, name: String): Result<Channel>
    suspend fun deleteChannel(serverId: String, channelId: String): Result<Unit>
    suspend fun joinChannel(serverId: String, channelId: String): Result<Unit>
    suspend fun leaveChannel(serverId: String, channelId: String): Result<Unit>
    suspend fun markChannelRead(serverId: String, channelId: String, seq: Long): Result<Unit>
    suspend fun getDMs(serverId: String): Result<List<Channel>>
    suspend fun createDM(serverId: String, userId: String): Result<Channel>
    suspend fun getChannelMembers(serverId: String, channelId: String): Result<List<ChannelMember>>
    suspend fun getUnreadChannels(serverId: String): Result<List<Channel>>
}

class ChannelRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val activeServerHolder: ActiveServerHolder
) : ChannelRepository {

    override suspend fun getChannels(serverId: String): Result<List<Channel>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getChannels()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get channels failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createChannel(serverId: String, name: String, type: String): Result<Channel> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.createChannel(CreateChannelRequest(name, type))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Create channel failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateChannel(serverId: String, channelId: String, name: String): Result<Channel> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.updateChannel(channelId, UpdateChannelRequest(name))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Update channel failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteChannel(serverId: String, channelId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.deleteChannel(channelId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete channel failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinChannel(serverId: String, channelId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.joinChannel(channelId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Join channel failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveChannel(serverId: String, channelId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.leaveChannel(channelId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Leave channel failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markChannelRead(serverId: String, channelId: String, seq: Long): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.markChannelRead(channelId, MarkReadRequest(seq))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Mark read failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDMs(serverId: String): Result<List<Channel>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getDMs()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get DMs failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createDM(serverId: String, userId: String): Result<Channel> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.createDM(CreateDMRequest(userId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Create DM failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getChannelMembers(serverId: String, channelId: String): Result<List<ChannelMember>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getChannelMembers(channelId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get members failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnreadChannels(serverId: String): Result<List<Channel>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getUnreadChannels()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get unread failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
