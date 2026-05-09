package com.sillyandroid.app.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sillyandroid.app.SillyApp
import com.sillyandroid.app.data.entity.CharacterEntity
import com.sillyandroid.app.domain.CharacterImporter
import com.sillyandroid.app.domain.PngCardHandler
import com.sillyandroid.app.ui.navigation.Routes
import com.sillyandroid.app.viewmodel.ChatViewModel
import com.sillyandroid.app.viewmodel.CharactersViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    innerPadding: PaddingValues,
    navController: NavHostController,
    viewModel: CharactersViewModel = viewModel(factory = CharactersViewModel.Factory())
) {
    val characters by viewModel.characters.collectAsState()
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory())
    var deleteTarget by remember { mutableStateOf<CharacterEntity?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var exportCharacter by remember { mutableStateOf<CharacterEntity?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Import file picker — accepts both PNG and JSON
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                // Try PNG first, then JSON
                val character = PngCardHandler.importFromPng(context, it)
                    ?: CharacterImporter.importFromJson(context, it)

                if (character != null) {
                    SillyApp.instance.container.characterRepository.insert(character)
                } else {
                    importError = "无法解析角色卡。请确认是有效的 PNG 角色卡或 JSON 文件。"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("角色") },
                actions = {
                    IconButton(onClick = { importLauncher.launch("*/*") }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "导入角色卡")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.createNew()
                navController.navigate(Routes.characterEdit(-1))
            }) {
                Icon(Icons.Default.Add, contentDescription = "添加角色")
            }
        },
        modifier = Modifier.padding(innerPadding)
    ) { padding ->
        if (characters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("暂无角色", style = MaterialTheme.typography.titleMedium)
                    Text("点击 + 创建角色，或从右上角导入 PNG/JSON", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(characters, key = { it.id }) { character ->
                    CharacterCard(
                        character = character,
                        onChat = {
                            chatViewModel.createChat(character.id, character.name)
                            navController.navigate(Routes.chatDetail(chatViewModel.currentChatId.value!!))
                        },
                        onEdit = { navController.navigate(Routes.characterEdit(character.id)) },
                        onExport = { exportCharacter = character },
                        onDelete = { deleteTarget = character }
                    )
                }
            }
        }
    }

    // Export dialog — PNG or JSON
    exportCharacter?.let { character ->
        AlertDialog(
            onDismissRequest = { exportCharacter = null },
            title = { Text("导出 ${character.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("选择导出格式：", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = {
                            exportCharacter = null
                            scope.launch {
                                try {
                                    val pngFile = PngCardHandler.exportToPng(character, context)
                                    val uri = FileProvider.getUriForFile(
                                        context, "${context.packageName}.fileprovider", pngFile
                                    )
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "导出 ${character.name} (PNG)"))
                                } catch (e: Exception) {
                                    importError = "PNG 导出失败: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("导出为 PNG 角色卡（可在 SillyTavern 中直接导入）")
                    }
                    OutlinedButton(
                        onClick = {
                            exportCharacter = null
                            val json = CharacterImporter.exportToJson(character)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_TEXT, json)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "导出 ${character.name} (JSON)"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("导出为 JSON 文件")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { exportCharacter = null }) { Text("取消") }
            }
        )
    }

    deleteTarget?.let { character ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除角色") },
            text = { Text("确认删除「${character.name}」？所有相关聊天也将被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCharacter(character.id)
                    deleteTarget = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }

    importError?.let { error ->
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text("导入失败") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { importError = null }) { Text("确定") }
            }
        )
    }
}

@Composable
fun CharacterCard(
    character: CharacterEntity,
    onChat: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onChat),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.Person, contentDescription = null,
                modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(character.name, style = MaterialTheme.typography.titleMedium)
                if (character.description.isNotBlank()) {
                    Text(
                        character.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.Share, contentDescription = "导出", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
