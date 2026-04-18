package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.dao.ChannelDao
import com.slock.app.data.local.toEntity
import com.slock.app.data.local.toModel
import com.slock.app.data.model.*
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

interface ChannelRepository {
    suspend fun getChannels(serverId: String): Result<List<Channel>>
    suspend fun refreshChannels(serverId: String): Result<List<Channel>>
    suspend fun createChannel(serverId: String, name: String, type: String = "text"): Result<Channel>
    suspend fun updateChannel(serverId: String, channelId: String, name: String): Result<Channel>
    suspend fun deleteChannel(serverId: String, channelId: String): Result<Unit>
    suspend fun joinChannel(serverId: String, channelId: String): Result<Unit>
    suspend fun leaveChannel(serverId: String, channelId: String): Result<Unit>
    suspend fun markChannelRead(serverId: String, channelId: String, seq: Long): Result<Unit>
    suspend fun getDMs(serverId: String): Result<List<Channel>>
    suspend fun createDM(serverId: String, agentId: String? = null, userId: String? = null): Result<Channel>
    suspend fun getChannelMembers(serverId: String, channelId: String): Result<List<ChannelMember>>
    suspend fun stopAllChannelAgents(serverId: String, channelId: String): Result<Unit>
    suspend fun resumeAllChannelAgents(serverId: String, channelId: String, prompt: String): Result<Unit>
    suspend fun getUnreadChannels(serverId: String): Result<Map<String, Int>>
    suspend fun getSavedChannels(serverId: String): Result<List<Channel>> = Result.failure(NotImplementedError())
    suspend fun saveChannel(serverId: String, channelId: String): Result<Unit> = Result.failure(NotImplementedError())
    suspend fun removeSavedChannel(serverId: String, channelId: String): Result<Unit> = Result.failure(NotImplementedError())
    suspend fun isChannelSaved(serverId: String, channelId: String): Result<Boolean> = Result.failure(NotImplementedError())
}

class ChannelRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val activeServerHolder: ActiveServerHolder,
    private val channelDao: ChannelDao
) : ChannelRepository {

    override suspend fun getChannels(serverId: String): Result<List<Channel>> {
        activeServerHolder.serverId = serverId
        // Return cache for instant UI. ViewModel calls refreshChannels() separately.
        val cached = try {
            channelDao.getChannelsByServer(serverId).firstOrNull()?.map { it.toModel() } ?: emptyList()
        } catch (e: Exception) { emptyList() }

        if (cached.isNotEmpty()) {
            return Result.success(cached)
        }

        return try {
            val response = apiService.getChannels()
            if (response.isSuccessful && response.body() != null) {
                val channels = response.body()!!
                channelDao.insertChannels(channels.map { it.toEntity(serverId) })
                Result.success(channels)
            } else {
                Result.failure(Exception("Get channels failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshChannels(serverId: String): Result<List<Channel>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getChannels()
            if (response.isSuccessful && response.body() != null) {
                val channels = response.body()!!
                channelDao.deleteChannelsByServer(serverId)
                channelDao.insertChannels(channels.map { it.toEntity(serverId) })
                Result.success(channels)
            } else {
                Result.failure(Exception("Refresh channels failed: ${response.code()}"))
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
                val channel = response.body()!!
                channelDao.insertChannel(channel.toEntity(serverId))
                Result.success(channel)
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
                val channel = response.body()!!
                channelDao.insertChannel(channel.toEntity(serverId))
                Result.success(channel)
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
                channelDao.deleteChannelById(channelId)
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
                val dms = response.body()!!
                channelDao.insertChannels(dms.map { it.toEntity(serverId) })
                Result.success(dms)
            } else {
                Result.failure(Exception("Get DMs failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createDM(serverId: String, agentId: String?, userId: String?): Result<Channel> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.createDM(CreateDMRequest(agentId = agentId, userId = userId))
            if (response.isSuccessful && response.body() != null) {
                val channel = response.body()!!
                channelDao.insertChannel(channel.toEntity(serverId))
                Result.success(channel)
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

    override suspend fun stopAllChannelAgents(serverId: String, channelId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.stopAllChannelAgents(channelId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Stop all agents failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resumeAllChannelAgents(serverId: String, channelId: String, prompt: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.resumeAllChannelAgents(channelId, ResumeAllAgentsRequest(prompt))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Resume all agents failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnreadChannels(serverId: String): Result<Map<String, Int>> {
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

    override suspend fun getSavedChannels(serverId: String): Result<List<Channel>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getSavedChannels()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get saved channels failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveChannel(serverId: String, channelId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.saveChannel(SaveChannelRequest(channelId))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Save channel failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeSavedChannel(serverId: String, channelId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.removeSavedChannel(channelId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Remove saved channel failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isChannelSaved(serverId: String, channelId: String): Result<Boolean> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.checkSavedChannel(SaveChannelRequest(channelId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.isSaved)
            } else {
                Result.failure(Exception("Check saved channel failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
