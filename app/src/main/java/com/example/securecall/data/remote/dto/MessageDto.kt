package com.example.securecall.data.remote.dto

import com.example.securecall.domain.model.Message
import com.example.securecall.domain.model.MessageStatus

data class MessageDto(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val status: String = MessageStatus.SENT.name,
    val updatedAt: Long = System.currentTimeMillis()
)

fun Message.toDto(): MessageDto {
    return MessageDto(
        id = id,
        chatId = chatId,
        senderId = senderId,
        content = content,
        timestamp = timestamp,
        status = status.name
    )
}

fun MessageDto.toDomain(): Message {
    return Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        content = content,
        timestamp = timestamp,
        status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.SENT)
    )
}
