package com.slock.app.data.local.dao

import androidx.room.*
import com.slock.app.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE channelId = :channelId ORDER BY seq DESC LIMIT :limit")
    fun getMessagesByChannel(channelId: String, limit: Int = 50): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE channelId = :channelId AND seq < :beforeSeq ORDER BY seq DESC LIMIT :limit")
    suspend fun getMessagesBefore(channelId: String, beforeSeq: Long, limit: Int = 50): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT MAX(seq) FROM messages WHERE channelId = :channelId")
    suspend fun getLatestSeq(channelId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE channelId = :channelId AND id NOT IN (SELECT id FROM messages WHERE channelId = :channelId ORDER BY seq DESC LIMIT :keep)")
    suspend fun trimMessages(channelId: String, keep: Int = 200)

    @Query("DELETE FROM messages WHERE channelId = :channelId")
    suspend fun deleteMessagesByChannel(channelId: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("""
        SELECT * FROM messages
        WHERE id IN (
            SELECT id FROM messages
            WHERE channelId IN (:channelIds)
            GROUP BY channelId
            HAVING seq = MAX(seq)
        )
    """)
    suspend fun getLatestMessagePerChannel(channelIds: List<String>): List<MessageEntity>

    @Query("""
        SELECT m.* FROM messages m
        INNER JOIN channels c ON m.channelId = c.id
        WHERE c.serverId = :serverId AND m.content LIKE '%' || :query || '%'
        ORDER BY m.seq DESC LIMIT :limit
    """)
    suspend fun searchMessages(serverId: String, query: String, limit: Int = 30): List<MessageEntity>
}
