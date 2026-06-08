package com.example.securecall.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.securecall.domain.model.Chat
import com.example.securecall.domain.repository.ChatRepository
import androidx.lifecycle.viewModelScope
import com.example.securecall.domain.model.User
import com.example.securecall.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
): ViewModel() {

    private var _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats = _chats.asStateFlow()

    private var _chat = MutableStateFlow<Chat?>(null)
    val chat = _chat.asStateFlow()

    private val _openedUnreadCount = MutableStateFlow(0)
    val openedUnreadCount = _openedUnreadCount.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _navigateToChat = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val navigateToChat = _navigateToChat.asSharedFlow()

    init {
        getChats()
    }

    fun getChats() {
        viewModelScope.launch {

            try {

                chatRepository.getChats()
                    .collect { chatList ->
                        _chats.value = chatList
                    }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error getting chats", e)
            }
        }
    }

    fun resetUnread(chatId: String) {

        viewModelScope.launch {

            try {

                chatRepository.resetUnread(chatId)

            } catch (e: java.lang.Exception) {
                Log.e("ChatViewModel", "Error resetting unread", e)
            }
        }
    }

    fun openChat(chatId: String) {

        viewModelScope.launch {

            try {

                val selectedChat = chatRepository.getChat(chatId)
                _chat.value = selectedChat
                _openedUnreadCount.value = selectedChat?.unreadCount ?: 0
                chatRepository.resetUnread(chatId)

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error opening chat", e)
            }
        }
    }

    fun searchUsers(query: String) {

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {

            userRepository.searchUsers(query)
                .onSuccess { users ->

                    _searchResults.value = users
                    Log.d("Users", users.toString())
                }
                .onFailure { e ->
                    _searchResults.value = emptyList()
                    Log.e("ChatViewModel", "Error searching users", e)
                }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun onUserClick(user: User) {
        viewModelScope.launch {
            try {
                val chat = chatRepository.createOrGetChat(user)
                _navigateToChat.emit(chat.chatId)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error creating or getting chat", e)
            }
        }
    }

    suspend fun getChat(chatId: String) {
        viewModelScope.launch {
            try {
                _chat.value = chatRepository.getChat(chatId)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error getting chat", e)
                throw e
            }
        }
    }
}
