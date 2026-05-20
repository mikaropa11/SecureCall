package com.example.securecall.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.securecall.data.local.converter.Converters
import com.example.securecall.data.local.dao.CallDao
import com.example.securecall.data.local.dao.ChatDao
import com.example.securecall.data.local.dao.MessageDao
import com.example.securecall.data.local.dao.UserDao
import com.example.securecall.data.local.entity.CallEntity
import com.example.securecall.data.local.entity.ChatEntity
import com.example.securecall.data.local.entity.MessageEntity
import com.example.securecall.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        MessageEntity::class,
        CallEntity::class,
        ChatEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun callDao(): CallDao
    abstract fun chatDao(): ChatDao
}