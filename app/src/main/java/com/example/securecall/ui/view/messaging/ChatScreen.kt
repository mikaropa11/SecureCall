package com.example.securecall.ui.view.messaging

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecall.domain.model.Message
import com.example.securecall.ui.components.ChatBottomBar
import com.example.securecall.ui.components.ChatTopBar
import com.example.securecall.ui.components.MessageBubble
import com.example.securecall.ui.viewmodel.MessageViewModel
import java.util.UUID
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.securecall.domain.model.MessageStatus
import com.example.securecall.domain.model.UserStatus
import com.example.securecall.ui.viewmodel.AuthenticationViewModel
import com.example.securecall.ui.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    chatId: String,
    viewModel: MessageViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    userViewModel: AuthenticationViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onCall: (receiverId: String) -> Unit,
    onMore: () -> Unit
) {

    val messages by viewModel.messages.collectAsState()
    val chat by chatViewModel.chat.collectAsState()

    val currentUser = userViewModel.user.collectAsState()

    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(chatId) {
        viewModel.getMessages(chatId)

        chatViewModel.getChat(chatId)

    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ChatTopBar(
                username = chat?.otherUsername ?: "",
                photoUrl = chat?.otherPhotoUrl ?: "",
                userId = chat?.otherUserId ?: "",
                isVerified = false, // o user.isVerified si lo tienes
                status = chat?.otherStatus ?: UserStatus.offline,
                onBack = onBack,
                onCall = onCall,
                onMore = onMore
            )
        },

        bottomBar = {
            ChatBottomBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(
                            Message(
                                id = UUID.randomUUID().toString(),
                                chatId = chatId,
                                senderId = "",
                                content = inputText,
                                timestamp = System.currentTimeMillis(),
                                status = MessageStatus.SENT
                            )
                        )
                        inputText = ""
                    }
                },
                onCamera = { }
            )
        }

    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            reverseLayout = false,
            contentPadding = PaddingValues(8.dp)
        ) {

            items(messages, key = { it.id }) { message ->
                MessageBubble(message, currentUser.value?.userId)
            }
        }
    }
}