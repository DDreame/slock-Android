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
    val serverId: String? = null,
    val name: String? = null,
    val description: String? = null,
    val prompt: String? = null,
    val model: String? = null,
    val avatar: String? = null,
    val status: String? = null,
    val activity: String? = null,
    val activityDetail: String? = null,
    val createdAt: String? = null
)
