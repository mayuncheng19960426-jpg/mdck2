package com.sillyandroid.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sillyandroid.app.data.entity.MessageEntity
import com.sillyandroid.app.ui.theme.ColorPalette
import com.sillyandroid.app.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: Long,
    innerPadding: PaddingValues,
    navController: NavHostController,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory())
) {
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val error by viewModel.error.collectAsState()
    val character by viewModel.currentCharacter.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showPromptViewer by remember { mutableStateOf(false) }
    var showLogViewer by remember { mutableStateOf(false) }
    var showManageDialog by remember { mutableStateOf(false) }
    var showVariablesDialog by remember { mutableStateOf(false) }
    var isCardBuilderMode by remember { mutableStateOf(false) }
    var showCardBuilderDialog by remember { mutableStateOf(false) }
    var showCardBuilderInput by remember { mutableStateOf(false) }
    var builderResultText by remember { mutableStateOf("") }
    var builderBlocks by remember { mutableStateOf<List<com.sillyandroid.app.cardbuilder.CardBuilderParser.ParsedBlock>>(emptyList()) }
    var attachedFileName by remember { mutableStateOf<String?>(null) }
    var attachedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            attachedFileUri = it
            attachedFileName = it.lastPathSegment ?: "附件"
        }
    }

    LaunchedEffect(chatId) {
        viewModel.loadChat(chatId)
    }

    LaunchedEffect(messages.size, streamingContent) {
        if (messages.isNotEmpty() || streamingContent.isNotEmpty()) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(character?.name ?: "聊天") },
                actions = {
                    IconButton(onClick = { viewModel.createCheckpoint() }) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "检查点")
                    }
                    IconButton(onClick = { viewModel.createBranch() }) {
                        Icon(Icons.Default.CallSplit, contentDescription = "创建分支")
                    }
                    IconButton(onClick = { viewModel.closeChat(); navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭聊天")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.padding(innerPadding)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Chat action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SmallActionChip("提示词", onClick = { showPromptViewer = true })
                SmallActionChip("日志", onClick = { showLogViewer = true })
                SmallActionChip("变量", onClick = { showVariablesDialog = true })
                SmallActionChip("管理", onClick = { showManageDialog = true })
                SmallActionChip(
                    if (isCardBuilderMode) "搓卡 ON" else "搓卡",
                    onClick = { showCardBuilderDialog = true },
                    enabled = true
                )
                SmallActionChip("继续", onClick = { viewModel.continueChat() }, enabled = !isStreaming)
                SmallActionChip("重试", onClick = { viewModel.retry() }, enabled = !isStreaming)
                SmallActionChip("附加文件", onClick = { filePickerLauncher.launch("text/*") })
                if (attachedFileName != null) {
                    SmallActionChip("清除附件", onClick = { attachedFileName = null; attachedFileUri = null })
                }
            }

            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }

                // Streaming indicator
                if (isStreaming && streamingContent.isNotEmpty()) {
                    item {
                        StreamingBubble(content = streamingContent)
                    }
                } else if (isStreaming) {
                    item {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(16.dp))
                    }
                }
            }

            // Input bar
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = !isStreaming
            )
        }
    }

    if (showPromptViewer) PromptViewerDialog(
        viewModel = viewModel,
        onDismiss = { showPromptViewer = false }
    )
    if (showLogViewer) LogViewerDialog(
        viewModel = viewModel,
        onDismiss = { showLogViewer = false }
    )

    if (showVariablesDialog) {
        VariablesDialog(
            chatId = chatId,
            onDismiss = { showVariablesDialog = false }
        )
    }

    val cardBuilderIds = remember { mutableStateListOf<Int>() }

    if (showCardBuilderDialog) {
        CardBuilderPanel(
            selectedIds = cardBuilderIds.toList(),
            onToggleItem = { id ->
                if (cardBuilderIds.contains(id)) cardBuilderIds.remove(id)
                else cardBuilderIds.add(id)
            },
            onGenerate = {
                showCardBuilderDialog = false
                showCardBuilderInput = true
            },
            onCancel = { showCardBuilderDialog = false }
        )
    }

    // Card builder input dialog
    val cardBuilderEngine = remember { com.sillyandroid.app.cardbuilder.CardBuilderEngine(com.sillyandroid.app.SillyApp.instance.container) }
    val cardBuilderAdapter = remember { com.sillyandroid.app.cardbuilder.CardBuilderAdapter(com.sillyandroid.app.SillyApp.instance.container) }

    if (showCardBuilderInput) {
        var inputText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCardBuilderInput = false },
            title = { Text("描述你的需求") },
            text = {
                Column {
                    Text("已选中 ${cardBuilderIds.size} 个条目", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputText, onValueChange = { inputText = it },
                        label = { Text("例如：帮我设计一个傲娇猫娘角色...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCardBuilderInput = false
                    if (inputText.isNotBlank()) {
                        isCardBuilderMode = true
                        builderResultText = ""
                        scope.launch {
                            try {
                                val characterName = character?.name
                                val stream = cardBuilderEngine.build(cardBuilderIds.toList(), inputText, characterName)
                                val sb = StringBuilder()
                                stream.collect { delta ->
                                    sb.append(delta.content)
                                    builderResultText = sb.toString()
                                }
                                // Parse the result
                                builderBlocks = com.sillyandroid.app.cardbuilder.CardBuilderParser.parse(builderResultText)
                                // Store as an assistant bubble
                                container.chatRepository.sendMessage(chatId, "assistant", builderResultText)
                            } catch (e: Exception) {
                                builderResultText = "搓卡失败: ${e.message}"
                            } finally {
                                isCardBuilderMode = false
                            }
                        }
                    }
                }) { Text("发送") }
            },
            dismissButton = { TextButton(onClick = { showCardBuilderInput = false }) { Text("取消") } }
        )
    }

    // Adopt results dialog
    if (builderBlocks.isNotEmpty() && !isCardBuilderMode) {
        AlertDialog(
            onDismissRequest = { builderBlocks = emptyList() },
            title = { Text("采纳结果") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(builderBlocks) { block ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text("<${block.tag}-${block.name}>", style = MaterialTheme.typography.labelMedium,
                                        color = ColorPalette.Accent)
                                    Text(block.content.take(100), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                }
                                TextButton(onClick = {
                                    scope.launch {
                                        val charId = viewModel.currentChatId.value?.let {
                                            container.chatRepository.getById(it)?.characterId
                                        } ?: 1L
                                        cardBuilderAdapter.adopt(block, charId)
                                    }
                                }) { Text("采纳") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { builderBlocks = emptyList() }) { Text("关闭") } }
        )
    }

    if (showManageDialog) {
        val allChats by viewModel.activeChats.collectAsState()
        ChatManagementDialog(
            chats = allChats,
            onSwitchToChat = { id ->
                showManageDialog = false
                viewModel.loadChat(id)
            },
            onCloseChat = { id ->
                if (id == chatId) navController.popBackStack()
                viewModel.closeChat()
            },
            onDeleteChat = { id ->
                // Delete is handled via DAO directly
                scope.launch {
                    com.sillyandroid.app.SillyApp.instance.database.chatDao().deleteById(id)
                }
            },
            onDismiss = { showManageDialog = false }
        )
    }
}

@Composable
fun SmallActionChip(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        enabled = enabled,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun MessageBubble(message: MessageEntity) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = when (message.role) {
        "user" -> ColorPalette.UserBubble
        "assistant" -> ColorPalette.BotBubble
        "system" -> ColorPalette.ThinkingBubble
        else -> ColorPalette.BotBubble
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = bubbleColor
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorPalette.OnPrimary
                )
                if (message.attachedFileName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, contentDescription = null,
                            modifier = Modifier.size(14.dp), tint = ColorPalette.OnPrimary.copy(alpha = 0.6f))
                        Text(" ${message.attachedFileName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorPalette.OnPrimary.copy(alpha = 0.6f))
                    }
                }
            }
        }
        if (message.isCheckpoint) {
            Text("检查点", style = MaterialTheme.typography.labelSmall,
                color = ColorPalette.WarningAmber)
        }
    }
}

@Composable
fun StreamingBubble(content: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = ColorPalette.BotBubble
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorPalette.OnPrimary
                )
                // Cursor indicator
                Text("▌", color = ColorPalette.Accent, fontStyle = FontStyle.Normal)
            }
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息...") },
                maxLines = 4,
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSend, enabled = enabled && text.isNotBlank()) {
                Icon(Icons.Default.Send, contentDescription = "发送",
                    tint = if (text.isNotBlank()) ColorPalette.Accent
                    else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PromptViewerDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val promptText = remember { viewModel.getPromptForViewer() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("提示词查看器", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                item {
                    Text(
                        text = promptText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun VariablesDialog(
    chatId: Long,
    onDismiss: () -> Unit
) {
    var variables by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newVarName by remember { mutableStateOf("") }
    var newVarValue by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val db = remember { com.sillyandroid.app.SillyApp.instance.database }

    LaunchedEffect(chatId) {
        variables = com.sillyandroid.app.engine.MvuEngine(db).getVariables(chatId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MVU 变量监控", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
        },
        text = {
            Column {
                // Manual variable set
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newVarName, onValueChange = { newVarName = it },
                        label = { Text("变量名") }, modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = newVarValue, onValueChange = { newVarValue = it },
                        label = { Text("值") }, modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = {
                        if (newVarName.isNotBlank()) {
                            scope.launch {
                                com.sillyandroid.app.engine.MvuEngine(db).setManualVariable(chatId, newVarName, newVarValue)
                                variables = com.sillyandroid.app.engine.MvuEngine(db).getVariables(chatId)
                                newVarName = ""; newVarValue = ""
                            }
                        }
                    }) { Icon(Icons.Default.Add, contentDescription = "设置") }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // Variable list
                if (variables.isEmpty()) {
                    Text("暂无变量。\n\n在角色编辑页添加脚本规则，规则会在对话时自动设置和修改变量。\n\n也可以在上方手动设置。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(variables.entries.toList()) { (name, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("{{$name}}", style = MaterialTheme.typography.labelLarge,
                                    color = ColorPalette.Accent, modifier = Modifier.width(100.dp))
                                Text("= $value", style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    scope.launch {
                                        db.chatVariableDao().delete(chatId, name)
                                        variables = com.sillyandroid.app.engine.MvuEngine(db).getVariables(chatId)
                                    }
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(14.dp),
                                        tint = ColorPalette.ErrorRed)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun LogViewerDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    var logs by remember { mutableStateOf<List<com.sillyandroid.app.data.entity.ApiLogEntity>>(emptyList()) }
    var selectedLog by remember { mutableStateOf<com.sillyandroid.app.data.entity.ApiLogEntity?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        logs = viewModel.getRecentLogs(50)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("日志查看器", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                if (logs.isNotEmpty()) {
                    TextButton(onClick = {
                        scope.launch {
                            com.sillyandroid.app.SillyApp.instance.database.apiLogDao().clearAll()
                            logs = emptyList()
                        }
                    }) { Text("清空", style = MaterialTheme.typography.labelSmall) }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
        },
        text = {
            if (selectedLog != null) {
                // Detail view
                val log = selectedLog!!
                Column {
                    TextButton(onClick = { selectedLog = null }) {
                        Text("← 返回列表")
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        item {
                            Text("${when (log.direction) {
                                "request" -> "请求"; "response" -> "响应"; "error" -> "错误"; else -> log.direction
                            }} — ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                .format(java.util.Date(log.timestamp))}",
                                style = MaterialTheme.typography.labelMedium)
                            Text("模型: ${log.model}  Tokens: ${log.tokenCount}  耗时: ${log.durationMs}ms",
                                style = MaterialTheme.typography.labelSmall)
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        }
                        if (log.direction == "request" && !log.requestJson.isNullOrBlank()) {
                            item {
                                Text("请求 JSON:", style = MaterialTheme.typography.labelMedium)
                                Text(log.requestJson, style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                        }
                        if (log.direction == "response" && !log.responseText.isNullOrBlank()) {
                            item {
                                Text("响应:", style = MaterialTheme.typography.labelMedium)
                                Text(log.responseText, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (log.direction == "error" && !log.errorMessage.isNullOrBlank()) {
                            item {
                                Text("错误:", style = MaterialTheme.typography.labelMedium,
                                    color = ColorPalette.ErrorRed)
                                Text(log.errorMessage, style = MaterialTheme.typography.bodySmall,
                                    color = ColorPalette.ErrorRed)
                            }
                        }
                    }
                }
            } else if (logs.isEmpty()) {
                Text("暂无日志记录。\n\n发送消息后这里会显示 API 请求和响应的详细内容。",
                    style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(logs) { log ->
                        val icon = when (log.direction) {
                            "request" -> "→"; "response" -> "←"; "error" -> "✗"; else -> "·"
                        }
                        val color = when (log.direction) {
                            "error" -> ColorPalette.ErrorRed; else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        TextButton(
                            onClick = { selectedLog = log },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$icon ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                .format(java.util.Date(log.timestamp))}  ${log.model}  ${if (log.direction == "request") "${log.tokenCount}tk" else "${log.durationMs}ms"}",
                                style = MaterialTheme.typography.bodySmall, color = color)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun VariablesDialog(
    chatId: Long,
    onDismiss: () -> Unit
) {
    var variables by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newVarName by remember { mutableStateOf("") }
    var newVarValue by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val db = remember { com.sillyandroid.app.SillyApp.instance.database }

    LaunchedEffect(chatId) {
        variables = com.sillyandroid.app.engine.MvuEngine(db).getVariables(chatId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MVU 变量监控", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
        },
        text = {
            Column {
                // Manual variable set
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newVarName, onValueChange = { newVarName = it },
                        label = { Text("变量名") }, modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = newVarValue, onValueChange = { newVarValue = it },
                        label = { Text("值") }, modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = {
                        if (newVarName.isNotBlank()) {
                            scope.launch {
                                com.sillyandroid.app.engine.MvuEngine(db).setManualVariable(chatId, newVarName, newVarValue)
                                variables = com.sillyandroid.app.engine.MvuEngine(db).getVariables(chatId)
                                newVarName = ""; newVarValue = ""
                            }
                        }
                    }) { Icon(Icons.Default.Add, contentDescription = "设置") }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // Variable list
                if (variables.isEmpty()) {
                    Text("暂无变量。\n\n在角色编辑页添加脚本规则，规则会在对话时自动设置和修改变量。\n\n也可以在上方手动设置。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(variables.entries.toList()) { (name, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("{{$name}}", style = MaterialTheme.typography.labelLarge,
                                    color = ColorPalette.Accent, modifier = Modifier.width(100.dp))
                                Text("= $value", style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    scope.launch {
                                        db.chatVariableDao().delete(chatId, name)
                                        variables = com.sillyandroid.app.engine.MvuEngine(db).getVariables(chatId)
                                    }
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(14.dp),
                                        tint = ColorPalette.ErrorRed)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
