package com.example.securecall.di

import android.content.Context
import androidx.room.Room
import com.example.securecall.data.local.dao.CallDao
import com.example.securecall.data.local.dao.ChatDao
import com.example.securecall.data.local.dao.MessageDao
import com.example.securecall.data.local.dao.UserDao
import com.example.securecall.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "securecall_db"
        )
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideCallDao(db: AppDatabase): CallDao = db.callDao()

    @Provides
    fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()
}
