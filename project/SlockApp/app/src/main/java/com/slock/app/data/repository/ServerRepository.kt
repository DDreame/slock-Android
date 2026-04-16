package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.local.dao.ServerDao
import com.slock.app.data.local.toEntity
import com.slock.app.data.local.toModel
import com.slock.app.data.model.*
import android.util.Log
import javax.inject.Inject

interface ServerRepository {
    suspend fun getServers(): Result<List<Server>>
    suspend fun refreshServers(): Result<List<Server>>
    suspend fun createServer(name: String, slug: String): Result<Server>
    suspend fun deleteServer(serverId: String): Result<Unit>
    suspend fun getServerMembers(serverId: String): Result<List<Member>>
    suspend fun updateMemberRole(serverId: String, memberId: String, role: String): Result<Unit>
}

class ServerRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val serverDao: ServerDao
) : ServerRepository {

    override suspend fun getServers(): Result<List<Server>> {
        // Return cache for instant UI. ViewModel calls refreshServers() separately.
        val cached = try {
            serverDao.getAllServersSync().map { it.toModel() }
        } catch (e: Exception) {
            Log.e("ServerRepo", "Cache read failed", e)
            emptyList()
        }

        if (cached.isNotEmpty()) {
            return Result.success(cached)
        }

        // No cache — must wait for network
        return try {
            val response = apiService.getServers()
            Log.d("ServerRepo", "getServers API: code=${response.code()}, bodySize=${response.body()?.size}")
            if (response.isSuccessful && response.body() != null) {
                val servers = response.body()!!
                if (servers.isNotEmpty()) {
                    serverDao.insertServers(servers.map { it.toEntity() })
                }
                Result.success(servers)
            } else {
                val errorMsg = try { response.errorBody()?.string()?.take(200) } catch (_: Exception) { null }
                Log.e("ServerRepo", "getServers failed: code=${response.code()}, error=$errorMsg")
                Result.failure(Exception("Get servers failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ServerRepo", "getServers exception", e)
            Result.failure(e)
        }
    }

    override suspend fun refreshServers(): Result<List<Server>> {
        return try {
            val response = apiService.getServers()
            Log.d("ServerRepo", "refreshServers API: code=${response.code()}, bodySize=${response.body()?.size}")
            if (response.isSuccessful && response.body() != null) {
                val servers = response.body()!!
                serverDao.deleteAllServers()
                serverDao.insertServers(servers.map { it.toEntity() })
                Result.success(servers)
            } else {
                val errorMsg = try { response.errorBody()?.string()?.take(200) } catch (_: Exception) { null }
                Log.e("ServerRepo", "refreshServers failed: code=${response.code()}, error=$errorMsg")
                Result.failure(Exception("Refresh servers failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ServerRepo", "refreshServers exception", e)
            Result.failure(e)
        }
    }

    override suspend fun createServer(name: String, slug: String): Result<Server> {
        return try {
            val response = apiService.createServer(CreateServerRequest(name, slug))
            if (response.isSuccessful && response.body() != null) {
                val server = response.body()!!
                serverDao.insertServer(server.toEntity())
                Result.success(server)
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
                serverDao.deleteServerById(serverId)
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
