package com.example.securecall.domain.repository

import com.example.securecall.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {

    fun getMessages(chatId: String): Flow<List<Message>>

    suspend fun sendMessage(message: Message)

    suspend fun observeMessages(chatId: String)

    suspend fun stopObserving(chatId: String)
}