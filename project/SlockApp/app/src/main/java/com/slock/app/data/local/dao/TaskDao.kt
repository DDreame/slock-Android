package com.slock.app.data.local.dao

import androidx.room.*
import com.slock.app.data.local.entity.TaskEntity

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE channelId = :channelId ORDER BY createdAt DESC")
    suspend fun getTasksByChannel(channelId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE channelId IN (:channelIds) ORDER BY createdAt DESC")
    suspend fun getTasksByChannels(channelIds: List<String>): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM tasks WHERE channelId = :channelId")
    suspend fun deleteTasksByChannel(channelId: String)
}
