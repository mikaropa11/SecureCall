package com.example.securecall.data.remote.dto

data class ChatDto(

    val chatId: String = "",

    val participants: List<String> = emptyList(),

    val lastMessage: String? = null,

    val lastMessageSenderId: String? = null,

    val lastMessageTimestamp: Long? = null,

    val createdAt: Long = System.currentTimeMillis()
)