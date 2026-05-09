package com.sillyandroid.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sillyandroid.app.SillyApp
import com.sillyandroid.app.data.entity.WorldBookEntryEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldBookScreen(
    bookId: Long,
    innerPadding: PaddingValues,
    navController: NavHostController
) {
    var entries by remember { mutableStateOf<List<WorldBookEntryEntity>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<WorldBookEntryEntity?>(null) }
    val scope = rememberCoroutineScope()
    val repo = remember { SillyApp.instance.container.worldBookRepository }

    LaunchedEffect(bookId) {
        entries = repo.getEnabledEntries(bookId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("世界书") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加条目")
            }
        },
        modifier = Modifier.padding(innerPadding)
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无世界书条目", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(entries) { entry ->
                    WorldBookEntryCard(
                        entry = entry,
                        onEdit = { editingEntry = entry }
                    )
                }
            }
        }
    }

    if (showAddDialog || editingEntry != null) {
        WorldBookEntryDialog(
            entry = editingEntry,
            bookId = bookId,
            onDismiss = {
                showAddDialog = false
                editingEntry = null
            },
            onSave = { entry ->
                scope.launch {
                    if (entry.id == 0L) {
                        repo.insertEntry(entry)
                    } else {
                        repo.updateEntry(entry)
                    }
                    entries = repo.getEnabledEntries(bookId)
                }
                showAddDialog = false
                editingEntry = null
            }
        )
    }
}

@Composable
fun WorldBookEntryCard(
    entry: WorldBookEntryEntity,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Row {
                    Text("Key: ", style = MaterialTheme.typography.labelMedium)
                    Text(entry.key.ifBlank { "(无)" }, style = MaterialTheme.typography.bodyMedium)
                }
                if (entry.content.isNotBlank()) {
                    Text(
                        entry.content,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("优先: ${entry.priority}", style = MaterialTheme.typography.labelSmall)
                    Text("位置: ${entry.position}", style = MaterialTheme.typography.labelSmall)
                    Text("深度: ${entry.depth}", style = MaterialTheme.typography.labelSmall)
                    if (entry.isConstant) Text("常驻", style = MaterialTheme.typography.labelSmall)
                    if (!entry.enabled) Text("已禁用", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldBookEntryDialog(
    entry: WorldBookEntryEntity?,
    bookId: Long,
    onDismiss: () -> Unit,
    onSave: (WorldBookEntryEntity) -> Unit
) {
    var key by remember { mutableStateOf(entry?.key ?: "") }
    var content by remember { mutableStateOf(entry?.content ?: "") }
    var comment by remember { mutableStateOf(entry?.comment ?: "") }
    var priority by remember { mutableStateOf((entry?.priority ?: 0).toString()) }
    var position by remember { mutableStateOf(entry?.position ?: "before_char") }
    var depth by remember { mutableStateOf((entry?.depth ?: 0).toString()) }
    var selective by remember { mutableStateOf(entry?.isSelective ?: true) }
    var constant by remember { mutableStateOf(entry?.isConstant ?: false) }
    var enabled by remember { mutableStateOf(entry?.enabled ?: true) }
    var positionExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry != null) "编辑条目" else "添加条目") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = key, onValueChange = { key = it },
                    label = { Text("触发键 (逗号分隔)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = content, onValueChange = { content = it },
                    label = { Text("内容") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(value = comment, onValueChange = { comment = it },
                    label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = priority, onValueChange = { priority = it },
                        label = { Text("优先级") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = depth, onValueChange = { depth = it },
                        label = { Text("深度") }, modifier = Modifier.weight(1f))
                }
                ExposedDropdownMenuBox(
                    expanded = positionExpanded,
                    onExpandedChange = { positionExpanded = it }
                ) {
                    OutlinedTextField(
                        value = position, onValueChange = {}, readOnly = true,
                        label = { Text("插入位置") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = positionExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = positionExpanded, onDismissRequest = { positionExpanded = false }) {
                        listOf("before_char" to "角色前", "after_char" to "角色后", "in_char" to "角色内").forEach { (v, l) ->
                            DropdownMenuItem(text = { Text(l) }, onClick = { position = v; positionExpanded = false })
                        }
                    }
                }
                Row { Text("选择性", modifier = Modifier.weight(1f)); Switch(checked = selective, onCheckedChange = { selective = it }) }
                Row { Text("常驻", modifier = Modifier.weight(1f)); Switch(checked = constant, onCheckedChange = { constant = it }) }
                Row { Text("启用", modifier = Modifier.weight(1f)); Switch(checked = enabled, onCheckedChange = { enabled = it }) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave((entry ?: WorldBookEntryEntity(worldBookId = bookId)).copy(
                    key = key, content = content, comment = comment,
                    priority = priority.toIntOrNull() ?: 0,
                    depth = depth.toIntOrNull() ?: 0,
                    position = position, isSelective = selective,
                    isConstant = constant, enabled = enabled
                ))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
