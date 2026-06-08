package com.example.securecall.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.securecall.data.local.entity.MessageEntity
import com.example.securecall.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessages(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("UPDATE messages SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE messageId = :messageId")
    suspend fun updateSyncStatus(
        messageId: String,
        syncStatus: SyncStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM messages WHERE syncStatus != :synced ORDER BY timestamp ASC")
    suspend fun getPendingMessages(synced: SyncStatus = SyncStatus.SYNCED): List<MessageEntity>
}
