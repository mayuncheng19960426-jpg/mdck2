package com.sillyandroid.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sillyandroid.app.data.entity.ChatEntity
import com.sillyandroid.app.ui.theme.ColorPalette
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatManagementDialog(
    chats: List<ChatEntity>,
    onSwitchToChat: (Long) -> Unit,
    onCloseChat: (Long) -> Unit,
    onDeleteChat: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("管理聊天文件", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        },
        text = {
            if (chats.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("暂无聊天记录", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(chats) { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSwitchToChat(chat.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null,
                                modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(chat.name.ifBlank { "聊天 ${chat.id}" },
                                    style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                Text(dateFormat.format(Date(chat.updatedAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (chat.isActive) {
                                Text("活跃", style = MaterialTheme.typography.labelSmall, color = ColorPalette.SuccessGreen)
                            }
                            Spacer(Modifier.width(4.dp))
                            TextButton(onClick = { onCloseChat(chat.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(16.dp))
                            }
                            TextButton(onClick = { onDeleteChat(chat.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除",
                                    modifier = Modifier.size(16.dp), tint = ColorPalette.ErrorRed)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}
