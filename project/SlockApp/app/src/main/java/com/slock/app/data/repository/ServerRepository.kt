package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.model.*
import javax.inject.Inject

interface ServerRepository {
    suspend fun getServers(): Result<List<Server>>
    suspend fun createServer(name: String, slug: String): Result<Server>
    suspend fun deleteServer(serverId: String): Result<Unit>
    suspend fun getServerMembers(serverId: String): Result<List<Member>>
    suspend fun updateMemberRole(serverId: String, memberId: String, role: String): Result<Unit>
}

class ServerRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ServerRepository {

    override suspend fun getServers(): Result<List<Server>> {
        return try {
            val response = apiService.getServers()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get servers failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createServer(name: String, slug: String): Result<Server> {
        return try {
            val response = apiService.createServer(CreateServerRequest(name, slug))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Create server failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteServer(serverId: String): Result<Unit> {
        return try {
            val response = apiService.deleteServer(serverId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete server failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getServerMembers(serverId: String): Result<List<Member>> {
        return try {
            val response = apiService.getServerMembers(serverId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get members failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMemberRole(serverId: String, memberId: String, role: String): Result<Unit> {
        return try {
            val response = apiService.updateMemberRole(serverId, memberId, UpdateMemberRoleRequest(role))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Update role failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
