package com.slock.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agents",
    indices = [Index("serverId")]
)
data class AgentEntity(
    @PrimaryKey
    val id: String,
    val serverId: String,
    val name: String,
    val description: String,
    val prompt: String,
    val model: String = "claude-sonnet-4-20250514",
    val avatar: String? = null,
    val status: String = "stopped",
    val activity: String? = null,
    val activityDetail: String? = null,
    val createdAt: String
)
