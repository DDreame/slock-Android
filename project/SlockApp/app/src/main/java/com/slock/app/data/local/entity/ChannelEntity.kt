package com.slock.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = ServerEntity::class,
            parentColumns = ["id"],
            childColumns = ["serverId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("serverId")]
)
data class ChannelEntity(
    @PrimaryKey
    val id: String,
    val serverId: String? = null,
    val name: String? = null,
    val type: String? = null,
    val seq: Long = 0,
    val createdAt: String? = null,
    val description: String? = null,
    val joined: Boolean = true
)
