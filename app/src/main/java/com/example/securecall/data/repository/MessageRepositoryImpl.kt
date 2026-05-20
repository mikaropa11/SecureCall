package com.example.securecall.data.repository

import android.util.Log
import com.example.securecall.data.local.dao.ChatDao
import com.example.securecall.data.local.dao.MessageDao
import com.example.securecall.data.mapper.toDomain
import com.example.securecall.data.mapper.toEntity
import com.example.securecall.domain.model.Message
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

        try {

            // 1. Guardar mensaje en Firestore
            firestore.collection("messages")
                .document(message.id)
                .set(message)
                .await()

            // 2. Actualizar metadata del chat
            firestore.collection("chats")
                .document(message.chatId)
                .set(
                    mapOf(
                        "lastMessage" to message.content,
                        "lastMessageSenderId" to message.senderId,
                        "lastMessageTimestamp" to message.timestamp
                    ),
                    SetOptions.merge()
                )
                .await()

            chatDao.updateLastMessage(
                chatId = message.chatId,
                lastMessage = message.content,
                lastMessageTimestamp = message.timestamp
            )

            // 3. Guardar local (Room)
            messageDao.insertMessage(message.toEntity())

            Log.d("MessageRepository", "Message sent: $message")

        } catch (e: Exception) {

            Log.e("MessageRepository", "Error sending message", e)

            throw e
        }
    }

    override suspend fun observeMessages(chatId: String) {
        val listener = firestore.collection("messages")
            .whereEqualTo("chatId", chatId)
            .addSnapshotListener { snapshot, _ ->

                snapshot?.documents?.forEach { doc ->
                    val message = doc.toObject(Message::class.java)
                    message?.let {
                        // guardar en Room
                        repositoryScope.launch {
                            messageDao.insertMessage(message = message.toEntity())
                        }
                    }
                }
            }

        listeners[chatId] = listener
    }

    override suspend fun stopObserving(chatId: String) {
        listeners[chatId]?.remove()
        listeners.remove(chatId)
    }
}