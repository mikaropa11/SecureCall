package com.example.securecall.ui.viewmodel

import android.util.Log
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecall.domain.model.Message
import com.example.securecall.domain.repository.AuthenticationRepository
import com.example.securecall.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun getMessages(chatId: String) {

        viewModelScope.launch {

            try {

                messageRepository.observeMessages(chatId)

                messageRepository.getMessages(chatId)
                    .collect { messageList ->
                        _messages.value = messageList
                    }

            } catch (e: Exception) {
                Log.e("MessageViewModel", "Error getting messages", e)
            }
        }
    }

    fun sendMessage(message: Message) {

        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid
                ?: return@launch
            Log.d("MessageViewModel", "UID: '${authRepository.currentUser?.uid}'")

            try {

                val finalMessage = message.copy(
                    senderId = userId
                )
                Log.d("MessageViewModel", finalMessage.toString())

                messageRepository.sendMessage(finalMessage)
            } catch (e: Exception) {
                Log.e("MessageViewModel", "Error sending message", e)
            }
        }
    }



    fun stopObserving(chatId: String) {

        viewModelScope.launch {

            try {

                messageRepository.stopObserving(chatId)

            } catch (e: Exception) {
                Log.e("MessageViewModel", "Error stopping listener", e)
            }
        }
    }
}