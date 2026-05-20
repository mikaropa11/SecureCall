package com.example.securecall.data.mapper

import com.example.securecall.data.local.entity.ChatEntity
import com.example.securecall.domain.model.Chat

fun ChatEntity.toDomain(): Chat {
    return Chat(
        chatId = chatId,
        otherUserId = otherUserId,
        otherUsername = otherUsername,
        otherName = otherName,
        otherPhotoUrl = otherPhotoUrl,
        otherStatus = otherStatus,
        lastMessage = lastMessage,
        lastMessageTimestamp = lastMessageTimestamp,
        unreadCount = unreadCount,
        isVerified = isVerified
    )
}

fun Chat.toEntity(): ChatEntity {
    return ChatEntity(
        chatId = chatId,
        otherUserId = otherUserId,
        otherUsername = otherUsername,
        otherName = otherName,
        otherPhotoUrl = otherPhotoUrl,
        otherStatus = otherStatus,
        lastMessage = lastMessage,
        lastMessageTimestamp = lastMessageTimestamp,
        unreadCount = unreadCount,
        isVerified = isVerified
    )
}