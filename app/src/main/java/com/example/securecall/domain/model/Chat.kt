package com.example.securecall.domain.model

data class Chat(
    val chatId: String,
    val otherUserId: String,
    val otherUsername: String,
    val otherName: String,
    val otherPhotoUrl: String? = null,
    val otherStatus: UserStatus = UserStatus.offline,
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long? = null,
    val unreadCount: Int = 0,
    val isVerified: Boolean = false
)