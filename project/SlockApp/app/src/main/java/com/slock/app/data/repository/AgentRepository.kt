package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.dao.AgentDao
import com.slock.app.data.local.toEntity
import com.slock.app.data.local.toModel
import com.slock.app.data.model.*
import android.util.Log
import javax.inject.Inject

interface AgentRepository {
    suspend fun getAgents(serverId: String): Result<List<Agent>>
    suspend fun refreshAgents(serverId: String): Result<List<Agent>>
    suspend fun createAgent(serverId: String, name: String, description: String, prompt: String, model: String = "claude-sonnet-4-20250514", avatar: String? = null): Result<Agent>
    suspend fun updateAgent(serverId: String, agentId: String, name: String?, description: String?, prompt: String?): Result<Agent>
    suspend fun deleteAgent(serverId: String, agentId: String): Result<Unit>
    suspend fun startAgent(serverId: String, agentId: String): Result<Unit>
    suspend fun stopAgent(serverId: String, agentId: String): Result<Unit>
    suspend fun resetAgent(serverId: String, agentId: String, mode: String = "full"): Result<Unit>
    suspend fun getActivityLog(serverId: String, agentId: String, limit: Int = 50): Result<List<ActivityLogEntry>>
}

class AgentRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val activeServerHolder: ActiveServerHolder,
    private val agentDao: AgentDao
) : AgentRepository {

    override suspend fun getAgents(serverId: String): Result<List<Agent>> {
        activeServerHolder.serverId = serverId
        // Return cache for instant UI. ViewModel calls refreshAgents() separately.
        val cached = try {
            agentDao.getAgentsByServer(serverId).map { it.toModel() }
        } catch (e: Exception) {
            Log.w("AgentRepo", "Cache read failed", e)
            emptyList()
        }

        if (cached.isNotEmpty()) {
            return Result.success(cached)
        }

        return try {
            val response = apiService.getAgents()
            Log.d("AgentRepo", "getAgents API: code=${response.code()}, bodySize=${response.body()?.size}, serverId=$serverId")
            if (response.isSuccessful && response.body() != null) {
                val agents = response.body()!!
                if (agents.isNotEmpty()) {
                    agentDao.insertAgents(agents.map { it.toEntity(serverId) })
                }
                Result.success(agents)
            } else {
                val errorMsg = try { response.errorBody()?.string()?.take(200) } catch (_: Exception) { null }
                Log.e("AgentRepo", "getAgents failed: code=${response.code()}, error=$errorMsg")
                Result.failure(Exception("Get agents failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AgentRepo", "getAgents exception", e)
            Result.failure(e)
        }
    }

    override suspend fun refreshAgents(serverId: String): Result<List<Agent>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getAgents()
            Log.d("AgentRepo", "refreshAgents API: code=${response.code()}, bodySize=${response.body()?.size}, serverId=$serverId")
            if (response.isSuccessful && response.body() != null) {
                val agents = response.body()!!
                agentDao.deleteAgentsByServer(serverId)
                if (agents.isNotEmpty()) {
                    agentDao.insertAgents(agents.map { it.toEntity(serverId) })
                }
                Result.success(agents)
            } else {
                Result.failure(Exception("Refresh agents failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AgentRepo", "refreshAgents exception", e)
            Result.failure(e)
        }
    }

    override suspend fun createAgent(
        serverId: String,
        name: String,
        description: String,
        prompt: String,
        model: String,
        avatar: String?
    ): Result<Agent> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.createAgent(CreateAgentRequest(name, description, prompt, model, avatar))
            if (response.isSuccessful && response.body() != null) {
                val agent = response.body()!!
                agentDao.insertAgent(agent.toEntity(serverId))
                Result.success(agent)
            } else {
                Result.failure(Exception("Create agent failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAgent(
        serverId: String,
        agentId: String,
        name: String?,
        description: String?,
        prompt: String?
    ): Result<Agent> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.updateAgent(agentId, UpdateAgentRequest(name, description, prompt))
            if (response.isSuccessful && response.body() != null) {
                val agent = response.body()!!
                agentDao.insertAgent(agent.toEntity(serverId))
                Result.success(agent)
            } else {
                Result.failure(Exception("Update agent failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAgent(serverId: String, agentId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.deleteAgent(agentId)
            if (response.isSuccessful) {
                agentDao.deleteAgentById(agentId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete agent failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startAgent(serverId: String, agentId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.startAgent(agentId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Start agent failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopAgent(serverId: String, agentId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.stopAgent(agentId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Stop agent failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetAgent(serverId: String, agentId: String, mode: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.resetAgent(agentId, ResetAgentRequest(mode))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Reset agent failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActivityLog(serverId: String, agentId: String, limit: Int): Result<List<ActivityLogEntry>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getAgentActivityLog(agentId, limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get activity log failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
