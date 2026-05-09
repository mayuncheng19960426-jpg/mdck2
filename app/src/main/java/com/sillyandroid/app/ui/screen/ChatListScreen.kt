package com.sillyandroid.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sillyandroid.app.data.entity.CharacterEntity
import com.sillyandroid.app.data.entity.ChatEntity
import com.sillyandroid.app.ui.navigation.Routes
import com.sillyandroid.app.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    innerPadding: PaddingValues,
    navController: NavHostController,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory())
) {
    val chats by viewModel.activeChats.collectAsState()
    var showNewChatDialog by remember { mutableStateOf(false) }
    var characters by remember { mutableStateOf<List<CharacterEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        characters = com.sillyandroid.app.SillyApp.instance.container.characterRepository.getAllList()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("聊天") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewChatDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新建聊天")
            }
        },
        modifier = Modifier.padding(innerPadding)
    ) { padding ->
        if (chats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("暂无聊天", style = MaterialTheme.typography.titleMedium)
                    Text("点击 + 新建聊天", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(chats, key = { it.id }) { chat ->
                    ChatListItem(
                        chat = chat,
                        onClick = { navController.navigate(Routes.chatDetail(chat.id)) },
                        onClose = { viewModel.closeChat() }
                    )
                }
            }
        }
    }

    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("选择角色") },
            text = {
                LazyColumn {
                    if (characters.isEmpty()) {
                        item { Text("暂无角色，请先创建角色", style = MaterialTheme.typography.bodyMedium) }
                    }
                    items(characters) { character ->
                        TextButton(
                            onClick = {
                                viewModel.createChat(character.id, character.name)
                                showNewChatDialog = false
                                navController.navigate(Routes.chatDetail(viewModel.currentChatId.value!!))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(character.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ChatListItem(
    chat: ChatEntity,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    chat.name.ifBlank { "聊天 ${chat.id}" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    dateFormat.format(Date(chat.updatedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onClose) {
                Text("关闭", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
