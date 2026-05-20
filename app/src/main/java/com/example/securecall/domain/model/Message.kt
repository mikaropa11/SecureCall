package com.example.securecall.domain.model

data class Message(
    val id: String = "",
    val chatId: String = "",
    var senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val status: MessageStatus = MessageStatus.SENT
)

enum class MessageStatus {
    SENT,
    DELIVERED,
    READ
}
