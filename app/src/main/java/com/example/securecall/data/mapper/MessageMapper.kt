package com.example.securecall.data.mapper

import com.example.securecall.data.local.entity.MessageEntity
import com.example.securecall.domain.model.Message

fun MessageEntity.toDomain(): Message {
    return Message(
        id = messageId,
        chatId = chatId,
        senderId = senderId,
        content = content,
        timestamp = timestamp,
        status = status
    )
}

fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        messageId = id,
        chatId = chatId,
        senderId = senderId,
        content = content,
        timestamp = timestamp,
        status = status
    )
}
