package com.example.securecall.data.repository

import android.util.Log
import com.example.securecall.data.local.dao.ChatDao
import com.example.securecall.data.local.dao.MessageDao
import com.example.securecall.data.local.entity.ChatEntity
import com.example.securecall.data.mapper.toDomain
import com.example.securecall.data.mapper.toEntity
import com.example.securecall.data.remote.dto.MessageDto
import com.example.securecall.data.remote.dto.toDomain
import com.example.securecall.data.remote.dto.toDto
import com.example.securecall.domain.model.Message
import com.example.securecall.domain.model.SyncStatus
import com.example.securecall.domain.model.UserStatus
import com.example.securecall.domain.repository.MessageRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val firestore: FirebaseFirestore,
    private val chatDao: ChatDao
) : MessageRepository {

    private val listeners = mutableMapOf<String, ListenerRegistration>()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getMessages(chatId: String): Flow<List<Message>> =
        messageDao.getMessages(chatId)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun sendMessage(message: Message) {

        require(message.senderId.isNotBlank()) {
            "Message sender cannot be blank"
        }
        require(message.chatId.isNotBlank()) {
            "Message chatId cannot be blank"
        }

        val localMessage = message.copy(
            id = message.id.ifBlank { UUID.randomUUID().toString() },
            timestamp = message.timestamp.takeIf { it > 0L } ?: System.currentTimeMillis()
        )

        val pendingEntity = localMessage.toEntity().copy(
            syncStatus = SyncStatus.PENDING,
            updatedAt = System.currentTimeMillis()
        )

        ensureLocalChatForMessage(localMessage, SyncStatus.PENDING)
        messageDao.insertMessage(pendingEntity)
        val insertedMessage = messageDao.getMessageById(localMessage.id)
        if (insertedMessage == null) {
            Log.e("MessageRepository", "Local insert failed for message ${localMessage.id}")
            throw IllegalStateException("Message was not inserted locally")
        }

        try {
            syncMessageToFirestore(localMessage)

            messageDao.updateSyncStatus(localMessage.id, SyncStatus.SYNCED)
            chatDao.updateSyncStatus(localMessage.chatId, SyncStatus.SYNCED)

            Log.d("MessageRepository", "Message sent: $localMessage")

        } catch (e: Exception) {
            Log.e("MessageRepository", "Error sending message", e)
            if (messageDao.getMessageById(localMessage.id) == null) {
                messageDao.insertMessage(
                    pendingEntity.copy(
                        syncStatus = SyncStatus.FAILED,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                messageDao.updateSyncStatus(localMessage.id, SyncStatus.FAILED)
            }
            chatDao.updateSyncStatus(localMessage.chatId, SyncStatus.FAILED)
        }
    }

    override suspend fun observeMessages(chatId: String) {
        syncPendingMessages()

        listeners[chatId]?.remove()

        val subcollectionListener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.forEach { doc ->
                    val message = doc.toObject(MessageDto::class.java)?.toDomain()
                    message?.let {
                        repositoryScope.launch {
                            ensureLocalChatForMessage(it, SyncStatus.SYNCED)
                            messageDao.insertMessage(
                                it.toEntity().copy(syncStatus = SyncStatus.SYNCED)
                            )
                        }
                    }
                }
                repositoryScope.launch {
                    chatDao.resetUnreadCount(chatId)
                }
            }

        val legacyListener = firestore.collection("messages")
            .whereEqualTo("chatId", chatId)
            .addSnapshotListener { snapshot, _ ->

                snapshot?.documents?.forEach { doc ->
                    val message = doc.toObject(MessageDto::class.java)?.toDomain()
                    message?.let {
                        repositoryScope.launch {
                            ensureLocalChatForMessage(it, SyncStatus.SYNCED)
                            messageDao.insertMessage(
                                it.toEntity().copy(syncStatus = SyncStatus.SYNCED)
                            )
                        }
                    }
                }
                repositoryScope.launch {
                    chatDao.resetUnreadCount(chatId)
                }
            }

        listeners[chatId] = object : ListenerRegistration {
            override fun remove() {
                subcollectionListener.remove()
                legacyListener.remove()
            }
        }
    }

    override suspend fun stopObserving(chatId: String) {
        listeners[chatId]?.remove()
        listeners.remove(chatId)
    }

    private suspend fun syncPendingMessages() {
        messageDao.getPendingMessages().forEach { entity ->
            runCatching {
                syncMessageToFirestore(entity.toDomain())
                messageDao.updateSyncStatus(entity.messageId, SyncStatus.SYNCED)
                chatDao.updateSyncStatus(entity.chatId, SyncStatus.SYNCED)
            }.onFailure {
                Log.e("MessageRepository", "Pending message sync failed: ${entity.messageId}", it)
            }
        }
    }

    private suspend fun syncMessageToFirestore(message: Message) {
        val localChat = chatDao.getChatById(message.chatId)
        val participants = listOf(message.senderId, localChat?.otherUserId.orEmpty())
            .filter { it.isNotBlank() }
            .distinct()
        val chatData = mapOf(
            "chatId" to message.chatId,
            "participants" to participants,
            "lastMessage" to message.content,
            "lastMessageSenderId" to message.senderId,
            "lastMessageTimestamp" to message.timestamp,
            "updatedAt" to System.currentTimeMillis()
        )
        val messageDto = message.toDto()

        firestore.collection("chats")
            .document(message.chatId)
            .set(chatData, SetOptions.merge())
            .await()

        firestore.collection("chats")
            .document(message.chatId)
            .collection("messages")
            .document(message.id)
            .set(messageDto)
            .await()

        // Legacy write kept during migration, but it must not fail the main send path.
        runCatching {
            firestore.collection("messages")
                .document(message.id)
                .set(messageDto)
                .await()
        }.onFailure {
            Log.w("MessageRepository", "Legacy message write failed: ${message.id}", it)
        }
    }

    private suspend fun ensureLocalChatForMessage(message: Message, syncStatus: SyncStatus) {
        val existing = chatDao.getChatById(message.chatId)
        val updatedAt = System.currentTimeMillis()
        if (existing == null) {
            val otherUserId = message.chatId
                .split("_")
                .firstOrNull { it != message.senderId }
                .orEmpty()
            chatDao.insertChat(
                ChatEntity(
                    chatId = message.chatId,
                    otherUserId = otherUserId,
                    otherUsername = otherUserId,
                    otherName = otherUserId,
                    otherPhotoUrl = null,
                    otherStatus = UserStatus.offline,
                    lastMessage = message.content,
                    lastMessageSenderId = message.senderId,
                    lastMessageTimestamp = message.timestamp,
                    unreadCount = 0,
                    isVerified = false,
                    syncStatus = syncStatus,
                    updatedAt = updatedAt
                )
            )
        } else {
            val shouldUpdatePreview = existing.lastMessageTimestamp == null ||
                message.timestamp >= existing.lastMessageTimestamp

            if (!shouldUpdatePreview) {
                return
            }

            chatDao.updateChat(
                existing.copy(
                    lastMessage = message.content,
                    lastMessageSenderId = message.senderId,
                    lastMessageTimestamp = message.timestamp,
                    syncStatus = syncStatus,
                    updatedAt = updatedAt
                )
            )
        }
    }
}
