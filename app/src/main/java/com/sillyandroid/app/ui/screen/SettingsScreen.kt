package com.sillyandroid.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sillyandroid.app.data.entity.ApiConfigEntity
import com.sillyandroid.app.data.entity.GenerationPresetEntity
import com.sillyandroid.app.data.entity.WorldBookEntity
import com.sillyandroid.app.ui.navigation.Routes
import com.sillyandroid.app.ui.theme.ColorPalette
import com.sillyandroid.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    navController: NavHostController,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory())
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("API", "预设", "世界书")

    Column(modifier = Modifier.padding(innerPadding)) {
        TopAppBar(title = { Text("设置") })
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        when (selectedTab) {
            0 -> ApiSettingsTab(viewModel)
            1 -> PresetSettingsTab(viewModel)
            2 -> WorldBookSettingsTab(viewModel, navController)
        }
    }
}

@Composable
fun ApiSettingsTab(viewModel: SettingsViewModel) {
    val configs by viewModel.apiConfigs.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<ApiConfigEntity?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(configs) { config ->
            ApiConfigCard(config,
                onEdit = { editingConfig = config; showDialog = true },
                onSetDefault = { viewModel.setDefaultApiConfig(config.id) },
                onDelete = { viewModel.deleteApiConfig(config.id) })
        }
        item {
            Button(onClick = { editingConfig = null; showDialog = true },
                modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("添加 API 配置") }
        }
    }
    if (showDialog) {
        ApiConfigDialog(config = editingConfig,
            onDismiss = { showDialog = false },
            onSave = { viewModel.saveApiConfig(it); showDialog = false })
    }
}

@Composable
fun ApiConfigCard(config: ApiConfigEntity, onEdit: () -> Unit, onSetDefault: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row {
                    Text(config.name, style = MaterialTheme.typography.titleMedium)
                    if (config.isDefault) Text(" (默认)", style = MaterialTheme.typography.labelSmall, color = ColorPalette.Accent)
                }
                Text(config.baseUrl, style = MaterialTheme.typography.bodySmall)
                Text("模型: ${config.modelName}  上下文: ${config.maxContextLength}t", style = MaterialTheme.typography.bodySmall)
            }
            Column {
                TextButton(onClick = onEdit) { Text("编辑", style = MaterialTheme.typography.labelSmall) }
                if (!config.isDefault) TextButton(onClick = onSetDefault) { Text("设为默认", style = MaterialTheme.typography.labelSmall) }
                TextButton(onClick = onDelete) { Text("删除", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun ApiConfigDialog(config: ApiConfigEntity?, onDismiss: () -> Unit, onSave: (ApiConfigEntity) -> Unit) {
    var name by remember { mutableStateOf(config?.name ?: "") }
    var baseUrl by remember { mutableStateOf(config?.baseUrl ?: "https://api.openai.com/v1") }
    var apiKey by remember { mutableStateOf(config?.apiKey ?: "") }
    var modelName by remember { mutableStateOf(config?.modelName ?: "gpt-4") }
    var maxContextLength by remember { mutableStateOf((config?.maxContextLength ?: 8192).toString()) }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (config != null) "编辑 API 配置" else "添加 API 配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") })
                OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") })
                OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") })
                OutlinedTextField(value = modelName, onValueChange = { modelName = it }, label = { Text("模型名称") })
                OutlinedTextField(value = maxContextLength, onValueChange = { maxContextLength = it }, label = { Text("最大上下文长度") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(ApiConfigEntity(id = config?.id ?: 0, name = name, baseUrl = baseUrl,
                apiKey = apiKey, modelName = modelName, maxContextLength = maxContextLength.toIntOrNull() ?: 8192,
                isDefault = config?.isDefault ?: false)) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
fun PresetSettingsTab(viewModel: SettingsViewModel) {
    val presets by viewModel.presets.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<GenerationPresetEntity?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(presets) { preset ->
            PresetCard(preset,
                onEdit = { editingPreset = preset; showDialog = true },
                onSetDefault = { viewModel.setDefaultPreset(preset.id) },
                onDelete = { viewModel.deletePreset(preset.id) })
        }
        item {
            Button(onClick = { editingPreset = null; showDialog = true },
                modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("添加生成预设") }
        }
    }
    if (showDialog) {
        PresetDialog(preset = editingPreset,
            onDismiss = { showDialog = false },
            onSave = { viewModel.savePreset(it); showDialog = false })
    }
}

@Composable
fun PresetCard(preset: GenerationPresetEntity, onEdit: () -> Unit, onSetDefault: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row {
                    Text(preset.name, style = MaterialTheme.typography.titleMedium)
                    if (preset.isDefault) Text(" (默认)", style = MaterialTheme.typography.labelSmall, color = ColorPalette.Accent)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("T: ${preset.temperature}", style = MaterialTheme.typography.bodySmall)
                    Text("TopP: ${preset.topP}", style = MaterialTheme.typography.bodySmall)
                    Text("Max: ${preset.maxTokens}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Column {
                TextButton(onClick = onEdit) { Text("编辑", style = MaterialTheme.typography.labelSmall) }
                if (!preset.isDefault) TextButton(onClick = onSetDefault) { Text("设为默认", style = MaterialTheme.typography.labelSmall) }
                TextButton(onClick = onDelete) { Text("删除", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun PresetDialog(preset: GenerationPresetEntity?, onDismiss: () -> Unit, onSave: (GenerationPresetEntity) -> Unit) {
    var name by remember { mutableStateOf(preset?.name ?: "Default") }
    var temperature by remember { mutableStateOf((preset?.temperature ?: 0.7f).toString()) }
    var topP by remember { mutableStateOf((preset?.topP ?: 1.0f).toString()) }
    var maxTokens by remember { mutableStateOf((preset?.maxTokens ?: 1024).toString()) }
    var freqPenalty by remember { mutableStateOf((preset?.frequencyPenalty ?: 0f).toString()) }
    var presPenalty by remember { mutableStateOf((preset?.presencePenalty ?: 0f).toString()) }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (preset != null) "编辑预设" else "添加预设") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") })
                OutlinedTextField(value = temperature, onValueChange = { temperature = it }, label = { Text("温度") })
                OutlinedTextField(value = topP, onValueChange = { topP = it }, label = { Text("Top P") })
                OutlinedTextField(value = maxTokens, onValueChange = { maxTokens = it }, label = { Text("最大 Tokens") })
                OutlinedTextField(value = freqPenalty, onValueChange = { freqPenalty = it }, label = { Text("频率惩罚") })
                OutlinedTextField(value = presPenalty, onValueChange = { presPenalty = it }, label = { Text("存在惩罚") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(GenerationPresetEntity(id = preset?.id ?: 0, name = name,
                temperature = temperature.toFloatOrNull() ?: 0.7f, topP = topP.toFloatOrNull() ?: 1.0f,
                maxTokens = maxTokens.toIntOrNull() ?: 1024,
                frequencyPenalty = freqPenalty.toFloatOrNull() ?: 0f,
                presencePenalty = presPenalty.toFloatOrNull() ?: 0f,
                isDefault = preset?.isDefault ?: false)) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
fun WorldBookSettingsTab(viewModel: SettingsViewModel, navController: NavHostController) {
    val worldBooks by viewModel.worldBooks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newBookName by remember { mutableStateOf("") }
    var newBookDesc by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(worldBooks) { book ->
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(book.name, style = MaterialTheme.typography.titleMedium)
                        if (book.description.isNotBlank()) Text(book.description, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { navController.navigate(Routes.worldBook(book.id)) }) { Text("管理条目") }
                }
            }
        }
        item {
            Button(onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("添加世界书") }
        }
    }

    if (showAddDialog) {
        AlertDialog(onDismissRequest = { showAddDialog = false },
            title = { Text("添加世界书") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newBookName, onValueChange = { newBookName = it }, label = { Text("名称") })
                    OutlinedTextField(value = newBookDesc, onValueChange = { newBookDesc = it }, label = { Text("描述") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        com.sillyandroid.app.SillyApp.instance.container.worldBookRepository
                            .insertBook(WorldBookEntity(name = newBookName, description = newBookDesc))
                    }
                    showAddDialog = false
                }) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } })
    }
}
