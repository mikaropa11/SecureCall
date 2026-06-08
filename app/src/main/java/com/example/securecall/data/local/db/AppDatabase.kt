package com.example.securecall.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun callDao(): CallDao
    abstract fun chatDao(): ChatDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chats ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE chats ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE messages ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE users ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chats ADD COLUMN lastMessageSenderId TEXT")
            }
        }
    }
}
