package com.slock.app.data.repository

import com.slock.app.data.api.ApiService
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.dao.TaskDao
import com.slock.app.data.local.toEntity
import com.slock.app.data.local.toModel
import com.slock.app.data.model.*
import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

interface TaskRepository {
    suspend fun getTasks(serverId: String, channelId: String): Result<List<Task>>
    suspend fun getAllServerTasks(serverId: String, channelIds: List<String>): Result<List<Task>>
    suspend fun getServerTasks(serverId: String): Result<List<Task>>
    suspend fun createTask(serverId: String, channelId: String, title: String, description: String?, assigneeId: String?, messageId: String?): Result<Task>
    suspend fun updateTaskStatus(serverId: String, taskId: String, status: String): Result<Task>
    suspend fun deleteTask(serverId: String, taskId: String): Result<Unit>
}

class TaskRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val activeServerHolder: ActiveServerHolder,
    private val taskDao: TaskDao
) : TaskRepository {

    override suspend fun getTasks(serverId: String, channelId: String): Result<List<Task>> {
        activeServerHolder.serverId = serverId
        // Return cache for instant UI. ViewModel calls refresh separately.
        val cached = try {
            taskDao.getTasksByChannel(channelId).map { it.toModel() }
        } catch (e: Exception) { emptyList() }

        if (cached.isNotEmpty()) {
            return Result.success(cached)
        }

        return try {
            val response = apiService.getTasks(channelId)
            if (response.isSuccessful && response.body() != null) {
                val tasks = response.body()!!.tasks
                taskDao.insertTasks(tasks.map { it.toEntity() })
                Result.success(tasks)
            } else {
                Result.failure(Exception("Get tasks failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllServerTasks(serverId: String, channelIds: List<String>): Result<List<Task>> {
        activeServerHolder.serverId = serverId
        Log.d("TaskRepo", "getAllServerTasks: serverId=$serverId, channelCount=${channelIds.size}")
        // Return cache for instant UI. ViewModel calls refresh separately.
        val cached = try {
            taskDao.getTasksByChannels(channelIds).map { it.toModel() }
        } catch (e: Exception) {
            Log.w("TaskRepo", "Cache read failed", e)
            emptyList()
        }

        if (cached.isNotEmpty()) {
            return Result.success(cached)
        }

        if (channelIds.isEmpty()) {
            Log.w("TaskRepo", "No channel IDs provided, returning empty")
            return Result.success(emptyList())
        }

        return try {
            coroutineScope {
                val results = channelIds.map { channelId ->
                    async {
                        try {
                            val response = apiService.getTasks(channelId)
                            if (response.isSuccessful && response.body() != null) {
                                val tasks = response.body()!!.tasks
                                if (tasks.isNotEmpty()) {
                                    taskDao.insertTasks(tasks.map { it.toEntity() })
                                }
                                tasks
                            } else {
                                Log.w("TaskRepo", "getTasks failed for channel=$channelId: code=${response.code()}")
                                emptyList()
                            }
                        } catch (e: Exception) {
                            Log.w("TaskRepo", "getTasks exception for channel=$channelId", e)
                            emptyList()
                        }
                    }
                }.awaitAll()
                val allTasks = results.flatten()
                Log.d("TaskRepo", "getAllServerTasks total: ${allTasks.size} tasks from ${channelIds.size} channels")
                Result.success(allTasks)
            }
        } catch (e: Exception) {
            Log.e("TaskRepo", "getAllServerTasks exception", e)
            Result.failure(e)
        }
    }

    override suspend fun getServerTasks(serverId: String): Result<List<Task>> {
        activeServerHolder.serverId = serverId
        return try {
            val response = apiService.getServerTasks()
            Log.d("TaskRepo", "getServerTasks API: code=${response.code()}, bodySize=${response.body()?.tasks?.size}")
            if (response.isSuccessful && response.body() != null) {
                val tasks = response.body()!!.tasks
                if (tasks.isNotEmpty()) {
                    taskDao.insertTasks(tasks.map { it.toEntity() })
                }
                Result.success(tasks)
            } else {
                val errorMsg = try { response.errorBody()?.string()?.take(200) } catch (_: Exception) { null }
                Log.e("TaskRepo", "getServerTasks failed: code=${response.code()}, error=$errorMsg")
                Result.failure(Exception("Get server tasks failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("TaskRepo", "getServerTasks exception", e)
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
                val task = response.body()!!
                taskDao.insertTask(task.toEntity())
                Result.success(task)
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
                val task = response.body()!!
                taskDao.insertTask(task.toEntity())
                Result.success(task)
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
                taskDao.deleteTaskById(taskId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete task failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
