package com.example.securecall.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.securecall.domain.model.UserStatus

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val chatId: String,

    val otherUserId: String,
    val otherUsername: String,
    val otherName: String,
    val otherPhotoUrl: String? = null,
    val otherStatus: UserStatus,
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long? = null,
    val unreadCount: Int = 0,
    val isVerified: Boolean = false
)