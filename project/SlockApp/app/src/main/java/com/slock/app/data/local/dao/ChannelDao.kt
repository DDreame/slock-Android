package com.slock.app.data.local.dao

import androidx.room.*
import com.slock.app.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE serverId = :serverId ORDER BY createdAt ASC")
    fun getChannelsByServer(serverId: String): Flow<List<ChannelEntity>>

    @Query("SELECT id FROM channels WHERE serverId = :serverId")
    suspend fun getChannelIdsByServer(serverId: String): List<String>

    @Query("SELECT * FROM channels WHERE id = :channelId")
    suspend fun getChannelById(channelId: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Delete
    suspend fun deleteChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE id = :channelId")
    suspend fun deleteChannelById(channelId: String)

    @Query("DELETE FROM channels WHERE serverId = :serverId")
    suspend fun deleteChannelsByServer(serverId: String)
}
