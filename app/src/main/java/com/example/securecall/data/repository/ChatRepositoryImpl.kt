package com.example.securecall.data.repository

import android.util.Log
import com.example.securecall.data.local.dao.ChatDao
import com.example.securecall.data.mapper.toDomain
import com.example.securecall.data.mapper.toEntity
import com.example.securecall.data.remote.dto.ChatDto
import com.example.securecall.domain.model.Chat
import com.example.securecall.domain.model.User
import com.example.securecall.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ChatRepository {

    override fun getChats(): Flow<List<Chat>> =
        chatDao.getAllChats().map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun getChat(chatId: String): Chat? =
        chatDao.getChatById(chatId)?.toDomain()


    override suspend fun createOrGetChat(user: User): Chat {

        val currentUserId = auth.currentUser?.uid
            ?: throw Exception("User not authenticated")

        Log.d("CHAT", "currentUserId: '$currentUserId'")
        Log.d("CHAT", "otherUserId: '${user.userId}'")

        // deterministic id
        val chatId = listOf(currentUserId, user.userId)
            .sorted()
            .joinToString("_")

        // local cache
        chatDao.getChatById(chatId)?.let {
            return it.toDomain()
        }

        // firestore check
        val chatDoc = firestore.collection("chats")
            .document(chatId)
            .get()
            .await()

        // create remote if needed
        if (!chatDoc.exists()) {

            val chatDto = ChatDto(
                chatId = chatId,
                participants = listOf(currentUserId, user.userId),
                createdAt = System.currentTimeMillis()
            )

            firestore.collection("chats")
                .document(chatId)
                .set(chatDto)
                .await()
        }

        // local UI model
        val chat = Chat(
            chatId = chatId,
            otherUserId = user.userId,
            otherUsername = user.username,
            otherName = user.name,
            otherPhotoUrl = user.photoUrl,
            otherStatus = user.status,
            lastMessage = null,
            lastMessageTimestamp = null,
            unreadCount = 0,
            isVerified = false
        )

        // local cache
        chatDao.insertChat(chat.toEntity())

        return chat
    }


    override suspend fun resetUnread(chatId: String) {
        chatDao.resetUnreadCount(chatId)
    }
}