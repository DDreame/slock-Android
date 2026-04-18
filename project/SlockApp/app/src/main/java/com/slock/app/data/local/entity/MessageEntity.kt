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
    val channelId: String? = null,
    val content: String? = null,
    val senderId: String? = null,
    val senderType: String? = null,
    val senderName: String? = null,
    val seq: Long = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val threadId: String? = null,
    val taskStatus: String? = null,
    val taskNumber: Int? = null,
    val replyCount: Int = 0,
    val attachments: String? = null,
    val reactions: String? = null
)
