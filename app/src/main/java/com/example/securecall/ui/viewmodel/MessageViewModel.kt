package com.example.securecall.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecall.domain.model.Message
import com.example.securecall.domain.repository.AuthenticationRepository
import com.example.securecall.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthenticationRepository,
): ViewModel() {

    private var _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _messagesLoaded = MutableStateFlow(false)
    val messagesLoaded = _messagesLoaded.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending = _isSending.asStateFlow()

    val currentUserId: String?
        get() = authRepository.currentUser?.uid

    private var messagesJob: Job? = null
    private var observerJob: Job? = null

    fun getMessages(chatId: String) {
        messagesJob?.cancel()
        observerJob?.cancel()
        _messagesLoaded.value = false

        messagesJob = viewModelScope.launch {
            try {
                messageRepository.getMessages(chatId)
                    .collect { messageList ->
                        _messages.value = messageList
                        _messagesLoaded.value = true
                    }
            } catch (e: Exception) {
                Log.e("MessageViewModel", "Error collecting local messages", e)
            }
        }

        observerJob = viewModelScope.launch {
            try {
                messageRepository.observeMessages(chatId)
            } catch (e: Exception) {
                Log.e("MessageViewModel", "Error observing remote messages", e)
            }
        }
    }

    fun sendMessage(message: Message) {

        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid
                ?: return@launch
            Log.d("MessageViewModel", "UID: '${authRepository.currentUser?.uid}'")

            try {
                _isSending.value = true

                val finalMessage = message.copy(
                    senderId = userId
                )
                Log.d("MessageViewModel", finalMessage.toString())

                messageRepository.sendMessage(finalMessage)
            } catch (e: Exception) {
                Log.e("MessageViewModel", "Error sending message", e)
            } finally {
                _isSending.value = false
            }
        }
    }



    fun stopObserving(chatId: String) {

        viewModelScope.launch {

            try {
                observerJob?.cancel()
                messagesJob?.cancel()

                messageRepository.stopObserving(chatId)

            } catch (e: Exception) {
                Log.e("MessageViewModel", "Error stopping listener", e)
            }
        }
    }
}
