package com.slock.app.data.local.dao

import androidx.room.*
import com.slock.app.data.local.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY createdAt DESC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers ORDER BY createdAt DESC")
    suspend fun getAllServersSync(): List<ServerEntity>

    @Query("SELECT * FROM servers WHERE id = :serverId")
    suspend fun getServerById(serverId: String): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<ServerEntity>)

    @Delete
    suspend fun deleteServer(server: ServerEntity)

    @Query("DELETE FROM servers WHERE id = :serverId")
    suspend fun deleteServerById(serverId: String)

    @Query("DELETE FROM servers")
    suspend fun deleteAllServers()
}
