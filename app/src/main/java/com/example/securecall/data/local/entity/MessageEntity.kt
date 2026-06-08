package com.example.securecall.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.securecall.domain.model.MessageStatus
import com.example.securecall.domain.model.SyncStatus

@Entity(tableName = "messages",
    indices = [Index(value = ["chatId"])],
    foreignKeys = [ForeignKey(
        entity = ChatEntity::class,
        parentColumns = ["chatId"],
        childColumns = ["chatId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val status: MessageStatus,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val updatedAt: Long = System.currentTimeMillis()
)

