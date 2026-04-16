package com.slock.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("channelId"), Index("seq")]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val channelId: String = "",
    val content: String = "",
    val senderId: String = "",
    val senderType: String = "", // "user" or "agent"
    val senderName: String = "",
    val seq: Long = 0,
    val createdAt: String = "",
    val updatedAt: String? = null,
    val threadId: String? = null,
    val taskStatus: String? = null,
    val taskNumber: Int? = null,
    val attachments: String = "[]" // JSON array stored as string
)
