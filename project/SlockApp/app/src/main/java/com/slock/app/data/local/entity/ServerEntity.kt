package com.slock.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey
    val id: String,
    val name: String? = null,
    val slug: String? = null,
    val role: String? = null,
    val createdAt: String? = null
)
