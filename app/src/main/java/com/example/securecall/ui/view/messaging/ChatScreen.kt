package com.example.securecall.ui.view.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecall.R
import com.example.securecall.domain.model.Message
import com.example.securecall.ui.components.ChatBottomBar
import com.example.securecall.ui.components.ChatTopBar
import com.example.securecall.ui.components.MessageBubble
import com.example.securecall.ui.viewmodel.MessageViewModel
import java.util.UUID
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.securecall.domain.model.MessageStatus
import com.example.securecall.domain.model.CallType
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
    onCall: (receiverId: String, callType: CallType) -> Unit,
    onMore: () -> Unit
) {

    val messages by viewModel.messages.collectAsState()
    val messagesLoaded by viewModel.messagesLoaded.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val chat by chatViewModel.chat.collectAsState()
    val openedUnreadCount by chatViewModel.openedUnreadCount.collectAsState()

    val listState = rememberLazyListState()
    val currentUserId = viewModel.currentUserId

    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(chatId) {
        viewModel.getMessages(chatId)
        chatViewModel.openChat(chatId)

    }

    DisposableEffect(chatId) {
        onDispose {
            viewModel.stopObserving(chatId)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
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

        if (messages.isEmpty() && messagesLoaded && !isSending) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.chat_empty_messages),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                reverseLayout = false,
                contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp)
            ) {

                itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                    val firstUnreadIndex = messages.size - openedUnreadCount
                    if (
                        openedUnreadCount > 0 &&
                        firstUnreadIndex in messages.indices &&
                        index == firstUnreadIndex
                    ) {
                        NewMessagesDivider()
                    }
                    MessageBubble(message, currentUserId)
                }
            }
        }
    }
}

@Composable
private fun NewMessagesDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
        Text(
            text = stringResource(R.string.chat_new_messages),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    }
}
