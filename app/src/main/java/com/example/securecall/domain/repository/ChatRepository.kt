package com.example.securecall.domain.repository

import com.example.securecall.domain.model.Chat
import com.example.securecall.domain.model.User
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    fun getChats(): Flow<List<Chat>>

    suspend fun getChat(chatId: String): Chat?

    suspend fun createOrGetChat(user: User): Chat

    suspend fun resetUnread(chatId: String)
}