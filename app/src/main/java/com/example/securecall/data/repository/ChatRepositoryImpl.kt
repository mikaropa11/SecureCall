package com.example.securecall.data.repository

import android.util.Log
import com.example.securecall.data.local.dao.ChatDao
import com.example.securecall.data.local.dao.UserDao
import com.example.securecall.data.local.entity.ChatEntity
import com.example.securecall.data.mapper.toDomain
import com.example.securecall.data.mapper.toEntity
import com.example.securecall.data.mapper.toUserDto
import com.example.securecall.data.remote.dto.ChatDto
import com.example.securecall.domain.model.Chat
import com.example.securecall.domain.model.SyncStatus
import com.example.securecall.domain.model.User
import com.example.securecall.domain.model.UserStatus
import com.example.securecall.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ChatRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var chatsListener: ListenerRegistration? = null

    override fun getChats(): Flow<List<Chat>> {
        observeRemoteChats()
        return chatDao.getAllChats().map { list ->
            list.map { it.toDomain() }
        }
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

        // Local provisional chat. It stays hidden from the chat list until first activity.
        val chat = Chat(
            chatId = chatId,
            otherUserId = user.userId,
            otherUsername = user.username,
            otherName = user.name,
            otherPhotoUrl = user.photoUrl,
            otherStatus = user.status,
            lastMessage = null,
            lastMessageSenderId = null,
            lastMessageTimestamp = null,
            unreadCount = 0,
            isVerified = false
        )

        chatDao.insertChat(
            chat.toEntity().copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = System.currentTimeMillis()
            )
        )

        return chat
    }


    override suspend fun resetUnread(chatId: String) {
        chatDao.resetUnreadCount(chatId)
    }

    private fun observeRemoteChats() {
        val currentUserId = auth.currentUser?.uid ?: return
        if (chatsListener != null) return

        chatsListener = firestore.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error observing chats", error)
                    return@addSnapshotListener
                }

                snapshot?.documents.orEmpty().forEach { document ->
                    val dto = document.toObject(ChatDto::class.java) ?: return@forEach
                    repositoryScope.launch {
                        val entity = dto.toEntityForCurrentUser(currentUserId)
                        val existing = chatDao.getChatById(entity.chatId)
                        if (existing == null) {
                            chatDao.insertChat(entity)
                        } else {
                            chatDao.updateChat(entity)
                        }
                    }
                }
            }
    }

    private suspend fun ChatDto.toEntityForCurrentUser(currentUserId: String): ChatEntity {
        val existingChat = chatDao.getChatById(chatId)
        val otherUserId = participants.firstOrNull { it != currentUserId }.orEmpty()
        val cachedUser = userDao.getUser(otherUserId)
        val remoteUser = if (cachedUser == null && otherUserId.isNotBlank()) {
            runCatching {
                firestore.collection("users")
                    .document(otherUserId)
                    .get()
                    .await()
                    .data
                    ?.toUserDto()
            }.getOrNull()
        } else {
            null
        }

        return ChatEntity(
            chatId = chatId,
            otherUserId = otherUserId,
            otherUsername = cachedUser?.username ?: remoteUser?.username ?: otherUserId,
            otherName = cachedUser?.name ?: remoteUser?.name ?: remoteUser?.username ?: otherUserId,
            otherPhotoUrl = cachedUser?.photoUrl ?: remoteUser?.photoUrl,
            otherStatus = cachedUser?.status?.let { UserStatus.fromString(it) }
                ?: remoteUser?.status?.let { UserStatus.fromString(it) }
                ?: UserStatus.offline,
            lastMessage = lastMessage,
            lastMessageSenderId = lastMessageSenderId,
            lastMessageTimestamp = lastMessageTimestamp,
            unreadCount = resolveUnreadCount(existingChat, currentUserId),
            isVerified = false,
            syncStatus = SyncStatus.SYNCED,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun ChatDto.resolveUnreadCount(
        existingChat: ChatEntity?,
        currentUserId: String
    ): Int {
        val previousTimestamp = existingChat?.lastMessageTimestamp ?: 0L
        val remoteTimestamp = lastMessageTimestamp ?: 0L
        val hasNewIncomingMessage = remoteTimestamp > previousTimestamp &&
            lastMessageSenderId != null &&
            lastMessageSenderId != currentUserId

        return if (hasNewIncomingMessage) {
            (existingChat?.unreadCount ?: 0) + 1
        } else {
            existingChat?.unreadCount ?: 0
        }
    }
}
