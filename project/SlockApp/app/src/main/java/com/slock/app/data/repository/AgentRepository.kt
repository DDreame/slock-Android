package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.model.*
import javax.inject.Inject

interface AgentRepository {
    suspend fun getAgents(): Result<List<Agent>>
    suspend fun createAgent(name: String, description: String, prompt: String, model: String = "gpt-4", avatar: String? = null): Result<Agent>
    suspend fun updateAgent(agentId: String, name: String?, description: String?, prompt: String?): Result<Agent>
    suspend fun deleteAgent(agentId: String): Result<Unit>
    suspend fun startAgent(agentId: String): Result<Unit>
    suspend fun stopAgent(agentId: String): Result<Unit>
    suspend fun resetAgent(agentId: String, mode: String = "full"): Result<Unit>
}

class AgentRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : AgentRepository {

    override suspend fun getAgents(): Result<List<Agent>> {
        return try {
            val response = apiService.getAgents()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get agents failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createAgent(
        name: String,
        description: String,
        prompt: String,
        model: String,
        avatar: String?
    ): Result<Agent> {
        return try {
            val response = apiService.createAgent(CreateAgentRequest(name, description, prompt, model, avatar))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Create agent failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAgent(
        agentId: String,
        name: String?,
        description: String?,
        prompt: String?
    ): Result<Agent> {
        return try {
            val response = apiService.updateAgent(agentId, UpdateAgentRequest(name, description, prompt))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Update agent failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAgent(agentId: String): Result<Unit> {
        return try {
            val response = apiService.deleteAgent(agentId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete agent failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startAgent(agentId: String): Result<Unit> {
        return try {
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

    override suspend fun stopAgent(agentId: String): Result<Unit> {
        return try {
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

    override suspend fun resetAgent(agentId: String, mode: String): Result<Unit> {
        return try {
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
}
