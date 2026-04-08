package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.*
import javax.inject.Inject

interface TaskRepository {
    suspend fun getTasks(serverId: String, channelId: String): Result<List<Task>>
    suspend fun createTask(serverId: String, channelId: String, title: String, description: String?, assigneeId: String?, messageId: String?): Result<Task>
    suspend fun updateTaskStatus(serverId: String, taskId: String, status: String): Result<Task>
    suspend fun deleteTask(serverId: String, taskId: String): Result<Unit>
}

class TaskRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val activeServerHolder: ActiveServerHolder
) : TaskRepository {

    override suspend fun getTasks(serverId: String, channelId: String): Result<List<Task>> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.getTasks(channelId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get tasks failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createTask(
        serverId: String,
        channelId: String,
        title: String,
        description: String?,
        assigneeId: String?,
        messageId: String?
    ): Result<Task> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.createTask(CreateTaskRequest(channelId, title, description, assigneeId, messageId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Create task failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTaskStatus(serverId: String, taskId: String, status: String): Result<Task> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.updateTask(taskId, UpdateTaskStatusRequest(status))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Update task failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(serverId: String, taskId: String): Result<Unit> {
        return try {
            activeServerHolder.serverId = serverId
            val response = apiService.deleteTask(taskId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete task failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
