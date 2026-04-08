package com.slock.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.slock.app.data.local.dao.ChannelDao
import com.slock.app.data.local.dao.MessageDao
import com.slock.app.data.local.dao.ServerDao
import com.slock.app.data.local.dao.UserDao
import com.slock.app.data.local.entity.ChannelEntity
import com.slock.app.data.local.entity.MessageEntity
import com.slock.app.data.local.entity.ServerEntity
import com.slock.app.data.local.entity.UserEntity

@Database(
    entities = [
        ServerEntity::class,
        ChannelEntity::class,
        MessageEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun channelDao(): ChannelDao
    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "slock_database"
    }
}
