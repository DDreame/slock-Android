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
    val channelId: String,
    val title: String,
    val description: String? = null,
    val status: String = "todo",
    val createdBy: String,
    val assigneeId: String? = null,
    val messageId: String? = null,
    val createdAt: String,
    val updatedAt: String? = null
)
