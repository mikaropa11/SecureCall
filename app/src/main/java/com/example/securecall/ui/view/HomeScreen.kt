package com.example.securecall.ui.view

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecall.R
import com.example.securecall.ui.components.ChatItem
import com.example.securecall.ui.components.IncomingCallDialog
import com.example.securecall.ui.components.SearchResultsContent
import com.example.securecall.domain.model.CallType
import com.example.securecall.ui.viewmodel.CallViewModel
import com.example.securecall.ui.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

// ── Filter tab enum ──────────────────────────────────────────────────────────

enum class ChatFilter { ALL, VERIFIED, UNVERIFIED }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    callViewModel: CallViewModel = hiltViewModel(),
    onNavigateToChat: (String) -> Unit,
    onNavigateToCalls: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToIncomingCall: (String, String, CallType) -> Unit,
    onNavigateToNewCall: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onOpenMenu: () -> Unit
) {

    var selectedFilter by remember { mutableStateOf(ChatFilter.ALL) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val chats by viewModel.chats.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    val incomingCall by callViewModel.incomingCall.collectAsState()

    var searchExpanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var isOpeningChat by remember { mutableStateOf(false) }
    val results by viewModel.searchResults.collectAsState()

    val filteredChats = remember(chats, selectedFilter) {
        when (selectedFilter) {
            ChatFilter.ALL -> chats
            ChatFilter.VERIFIED -> chats.filter { it.isVerified }
            ChatFilter.UNVERIFIED -> chats.filter { !it.isVerified }
        }
    }

    incomingCall?.let { call ->
        IncomingCallDialog(
            callerName = call.callerId,
            onAccept = {
                callViewModel.clearIncomingCall()
                onNavigateToIncomingCall(call.callId, call.callerId, call.callType())
            },
            onReject = {
                callViewModel.clearIncomingCall()
                callViewModel.rejectCall(call.callId)
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToChat.collect { chatId ->
            isOpeningChat = false
            onNavigateToChat(chatId)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!searchExpanded) {

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.home_identity_verified),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = {
                                isOpeningChat = false
                                searchExpanded = true
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.home_search)
                                )
                            }
                            IconButton(onClick = onOpenMenu) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = stringResource(R.string.home_menu)
                                )
                            }
                        }

                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    searchExpanded = false
                                    query = ""
                                    isOpeningChat = false
                                    viewModel.clearSearch()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back)
                                )
                            }

                            OutlinedTextField(
                                value = query,
                                onValueChange = {
                                    query = it
                                    if (it.isNotBlank()) viewModel.searchUsers(it)
                                    else viewModel.clearSearch()
                                },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.search_users_placeholder),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                singleLine = true,
                                // forma tipo pill, más moderna
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    // fondo sutil para distinguir el campo del topbar
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.0f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // FIX 2: filter chips con weight(1f) para que los tres tengan el mismo ancho
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChatFilter.entries.forEach { filter ->

                        val label = when (filter) {
                            ChatFilter.ALL -> stringResource(R.string.filter_all)
                            ChatFilter.VERIFIED -> stringResource(R.string.filter_verified)
                            ChatFilter.UNVERIFIED -> stringResource(R.string.filter_unverified)
                        }

                        val isSelected = selectedFilter == filter

                        val bgColor by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            label = "chipBg"
                        )

                        val textColor by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "chipText"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)                    // ← todos iguales
                                .clip(RoundedCornerShape(50.dp))
                                .background(bgColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedFilter = filter }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = textColor,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(1.5.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
            }
        },

        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.navigationBarsPadding().height(72.dp)
            ) {

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            if (selectedTab == 0) Icons.Filled.Chat else Icons.Outlined.Chat,
                            null
                        )
                    },
                    label = { Text(stringResource(R.string.nav_chats)) }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onNavigateToCalls()
                    },
                    icon = {
                        Icon(
                            if (selectedTab == 1) Icons.Filled.Phone else Icons.Outlined.Phone,
                            null
                        )
                    },
                    label = { Text(stringResource(R.string.nav_calls)) }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        onNavigateToProfile()
                    },
                    icon = {
                        Icon(
                            if (selectedTab == 2) Icons.Filled.Person else Icons.Outlined.Person,
                            null
                        )
                    },
                    label = { Text(stringResource(R.string.nav_profile)) }
                )
            }
        },

        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewCall,
                icon = { Icon(Icons.Filled.Videocam, null) },
                text = { Text(stringResource(R.string.home_new_video_call)) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        if (searchExpanded) {
            SearchResultsContent(
                results = results,
                isOpening = isOpeningChat,
                onUserClick = { user ->
                    if (!isOpeningChat) {
                        isOpeningChat = true
                        searchExpanded = false
                        query = ""
                        viewModel.clearSearch()
                        viewModel.onUserClick(user)
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )

        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {

                if (filteredChats.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.home_no_chats))
                        }
                    }
                } else {
                    items(filteredChats, key = { it.chatId }) { chat ->
                        ChatItem(
                            chat = chat,
                            currentUserId = currentUserId,
                            onClick = {
                                Log.d("CHAT_CLICK", chat.chatId)
                                onNavigateToChat(chat.chatId) }
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick
    )

