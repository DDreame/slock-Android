package com.slock.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String? = null,
    val name: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val emailVerified: Boolean = false
)
