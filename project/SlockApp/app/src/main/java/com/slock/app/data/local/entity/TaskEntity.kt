package com.slock.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [Index("channelId")]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val channelId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val createdBy: String? = null,
    val assigneeId: String? = null,
    val messageId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
