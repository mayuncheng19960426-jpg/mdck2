package com.sillyandroid.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sillyandroid.app.data.entity.CharacterEntity
import com.sillyandroid.app.ui.navigation.Routes
import com.sillyandroid.app.viewmodel.CharactersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditScreen(
    characterId: Long,
    innerPadding: PaddingValues,
    navController: NavHostController,
    viewModel: CharactersViewModel = viewModel(factory = CharactersViewModel.Factory())
) {
    val editingCharacter by viewModel.editingCharacter.collectAsState()
    val boundIds by viewModel.boundEntryIds.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var personality by remember { mutableStateOf("") }
    var firstMessage by remember { mutableStateOf("") }
    var scenario by remember { mutableStateOf("") }
    var exampleDialogue by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("") }
    var postHistoryInstructions by remember { mutableStateOf("") }

    LaunchedEffect(characterId) {
        if (characterId > 0) {
            viewModel.loadCharacter(characterId)
            viewModel.loadBindings(characterId)
        } else {
            viewModel.createNew()
        }
    }

    LaunchedEffect(editingCharacter) {
        editingCharacter?.let {
            name = it.name
            description = it.description
            personality = it.personality
            firstMessage = it.firstMessage
            scenario = it.scenario
            exampleDialogue = it.exampleDialogue
            systemPrompt = it.systemPrompt
            postHistoryInstructions = it.postHistoryInstructions
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (characterId > 0) "编辑角色" else "新建角色") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val character = (editingCharacter ?: CharacterEntity()).copy(
                            name = name,
                            description = description,
                            personality = personality,
                            firstMessage = firstMessage,
                            scenario = scenario,
                            exampleDialogue = exampleDialogue,
                            systemPrompt = systemPrompt,
                            postHistoryInstructions = postHistoryInstructions
                        )
                        viewModel.saveCharacter(character)
                        navController.popBackStack()
                    }) { Text("保存") }
                }
            )
        },
        modifier = Modifier.padding(innerPadding)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("角色名称 *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = personality,
                onValueChange = { personality = it },
                label = { Text("性格") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = firstMessage,
                onValueChange = { firstMessage = it },
                label = { Text("开场消息") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = scenario,
                onValueChange = { scenario = it },
                label = { Text("场景设定") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = exampleDialogue,
                onValueChange = { exampleDialogue = it },
                label = { Text("对话示例") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("系统提示词") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = postHistoryInstructions,
                onValueChange = { postHistoryInstructions = it },
                label = { Text("对话后指令") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // World Book Bindings section
            if (characterId > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("角色关联世界书条目", style = MaterialTheme.typography.titleMedium)
                Text("未关联任何条目时将使用全量匹配", style = MaterialTheme.typography.bodySmall)

                WorldBookBindingSection(
                    characterId = characterId,
                    boundIds = boundIds,
                    onToggle = { entryId -> viewModel.toggleBinding(characterId, entryId) }
                )

                // Script Rules section (MVU)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ScriptRulesSection(characterId = characterId)
            }
        }
    }
}

// ==================== Script Rules Section ====================

@Composable
fun ScriptRulesSection(characterId: Long) {
    var rules by remember { mutableStateOf<List<com.sillyandroid.app.data.entity.CharacterScriptRuleEntity>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<com.sillyandroid.app.data.entity.CharacterScriptRuleEntity?>(null) }
    val scope = rememberCoroutineScope()
    val db = remember { com.sillyandroid.app.SillyApp.instance.database }

    LaunchedEffect(characterId) {
        rules = db.characterScriptRuleDao().getByCharacter(characterId)
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("角色脚本规则 (MVU)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("{{变量}} 绑定", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { editingRule = null; showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加规则")
            }
        }

        if (rules.isEmpty()) {
            Text("暂未绑定脚本规则。", style = MaterialTheme.typography.bodySmall)
            Text("规则可以在对话前/后修改变量，变量通过 {{name}} 注入 prompt。",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            rules.forEachIndexed { _, rule ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(rule.scriptName, style = MaterialTheme.typography.labelLarge)
                            Text("条件: ${rule.condition ?: "always"}", style = MaterialTheme.typography.labelSmall)
                            Text("动作: ${rule.action}", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("阶段: ${if (rule.triggerPhase == "before_prompt") "发送前" else "响应后"}",
                                    style = MaterialTheme.typography.labelSmall)
                                Text("优先: ${rule.priority}", style = MaterialTheme.typography.labelSmall)
                                if (!rule.enabled) Text("已禁用", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Column {
                            IconButton(onClick = { editingRule = rule }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                scope.launch { db.characterScriptRuleDao().deleteById(rule.id); rules = db.characterScriptRuleDao().getByCharacter(characterId) }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp),
                                    tint = ColorPalette.ErrorRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingRule != null) {
        ScriptRuleDialog(
            rule = editingRule,
            characterId = characterId,
            onDismiss = { showAddDialog = false; editingRule = null },
            onSave = { rule ->
                scope.launch {
                    if (rule.id == 0L) db.characterScriptRuleDao().insert(rule)
                    else db.characterScriptRuleDao().update(rule)
                    rules = db.characterScriptRuleDao().getByCharacter(characterId)
                }
                showAddDialog = false; editingRule = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptRuleDialog(
    rule: com.sillyandroid.app.data.entity.CharacterScriptRuleEntity?,
    characterId: Long,
    onDismiss: () -> Unit,
    onSave: (com.sillyandroid.app.data.entity.CharacterScriptRuleEntity) -> Unit
) {
    var name by remember { mutableStateOf(rule?.scriptName ?: "") }
    var phase by remember { mutableStateOf(rule?.triggerPhase ?: "before_prompt") }
    var condition by remember { mutableStateOf(rule?.condition ?: "") }
    var action by remember { mutableStateOf(rule?.action ?: "") }
    var priority by remember { mutableStateOf((rule?.priority ?: 0).toString()) }
    var enabled by remember { mutableStateOf(rule?.enabled ?: true) }
    var phaseExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (rule != null) "编辑规则" else "添加规则") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("规则名称") })
                ExposedDropdownMenuBox(expanded = phaseExpanded, onExpandedChange = { phaseExpanded = it }) {
                    OutlinedTextField(
                        value = if (phase == "before_prompt") "发送前 (before_prompt)" else "响应后 (after_response)",
                        onValueChange = {}, readOnly = true, label = { Text("触发阶段") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = phaseExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = phaseExpanded, onDismissRequest = { phaseExpanded = false }) {
                        DropdownMenuItem(text = { Text("发送前 (before_prompt)") }, onClick = { phase = "before_prompt"; phaseExpanded = false })
                        DropdownMenuItem(text = { Text("响应后 (after_response)") }, onClick = { phase = "after_response"; phaseExpanded = false })
                    }
                }
                OutlinedTextField(value = condition, onValueChange = { condition = it },
                    label = { Text("条件 (ex: affection >= 50, always)") })
                OutlinedTextField(value = action, onValueChange = { action = it },
                    label = { Text("动作 (ex: add affection 5, set mood happy)") })
                OutlinedTextField(value = priority, onValueChange = { priority = it }, label = { Text("优先级") })
                Row { Text("启用"); Spacer(Modifier.width(8.dp)); Switch(checked = enabled, onCheckedChange = { enabled = it }) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave((rule ?: com.sillyandroid.app.data.entity.CharacterScriptRuleEntity(characterId = characterId, action = "")).copy(
                    scriptName = name, triggerPhase = phase, condition = condition.ifBlank { null },
                    action = action, priority = priority.toIntOrNull() ?: 0, enabled = enabled
                ))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun WorldBookBindingSection(
    characterId: Long,
    boundIds: List<Long>,
    onToggle: (Long) -> Unit
) {
    var entries by remember { mutableStateOf<List<com.sillyandroid.app.data.entity.WorldBookEntryEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        entries = com.sillyandroid.app.SillyApp.instance.container
            .worldBookRepository.getEnabledEntries(1L)
    }

    if (entries.isEmpty()) {
        Text("暂无世界书条目", style = MaterialTheme.typography.bodyMedium)
        return
    }

    LazyColumn(
        modifier = Modifier.heightIn(max = 200.dp)
    ) {
        items(entries) { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = boundIds.contains(entry.id),
                    onCheckedChange = { onToggle(entry.id) }
                )
                Column(Modifier.weight(1f)) {
                    Text(entry.key.ifBlank { "(无触发键)" }, style = MaterialTheme.typography.bodyMedium)
                    Text(entry.content.take(80), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ==================== Script Rules Section ====================

@Composable
fun ScriptRulesSection(characterId: Long) {
    var rules by remember { mutableStateOf<List<com.sillyandroid.app.data.entity.CharacterScriptRuleEntity>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<com.sillyandroid.app.data.entity.CharacterScriptRuleEntity?>(null) }
    val scope = rememberCoroutineScope()
    val db = remember { com.sillyandroid.app.SillyApp.instance.database }

    LaunchedEffect(characterId) {
        rules = db.characterScriptRuleDao().getByCharacter(characterId)
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("角色脚本规则 (MVU)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("{{变量}} 绑定", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { editingRule = null; showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加规则")
            }
        }

        if (rules.isEmpty()) {
            Text("暂未绑定脚本规则。", style = MaterialTheme.typography.bodySmall)
            Text("规则可以在对话前/后修改变量，变量通过 {{name}} 注入 prompt。",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            rules.forEachIndexed { _, rule ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(rule.scriptName, style = MaterialTheme.typography.labelLarge)
                            Text("条件: ${rule.condition ?: "always"}", style = MaterialTheme.typography.labelSmall)
                            Text("动作: ${rule.action}", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("阶段: ${if (rule.triggerPhase == "before_prompt") "发送前" else "响应后"}",
                                    style = MaterialTheme.typography.labelSmall)
                                Text("优先: ${rule.priority}", style = MaterialTheme.typography.labelSmall)
                                if (!rule.enabled) Text("已禁用", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Column {
                            IconButton(onClick = { editingRule = rule }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                scope.launch { db.characterScriptRuleDao().deleteById(rule.id); rules = db.characterScriptRuleDao().getByCharacter(characterId) }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp),
                                    tint = ColorPalette.ErrorRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingRule != null) {
        ScriptRuleDialog(
            rule = editingRule,
            characterId = characterId,
            onDismiss = { showAddDialog = false; editingRule = null },
            onSave = { rule ->
                scope.launch {
                    if (rule.id == 0L) db.characterScriptRuleDao().insert(rule)
                    else db.characterScriptRuleDao().update(rule)
                    rules = db.characterScriptRuleDao().getByCharacter(characterId)
                }
                showAddDialog = false; editingRule = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptRuleDialog(
    rule: com.sillyandroid.app.data.entity.CharacterScriptRuleEntity?,
    characterId: Long,
    onDismiss: () -> Unit,
    onSave: (com.sillyandroid.app.data.entity.CharacterScriptRuleEntity) -> Unit
) {
    var name by remember { mutableStateOf(rule?.scriptName ?: "") }
    var phase by remember { mutableStateOf(rule?.triggerPhase ?: "before_prompt") }
    var condition by remember { mutableStateOf(rule?.condition ?: "") }
    var action by remember { mutableStateOf(rule?.action ?: "") }
    var priority by remember { mutableStateOf((rule?.priority ?: 0).toString()) }
    var enabled by remember { mutableStateOf(rule?.enabled ?: true) }
    var phaseExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (rule != null) "编辑规则" else "添加规则") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("规则名称") })
                ExposedDropdownMenuBox(expanded = phaseExpanded, onExpandedChange = { phaseExpanded = it }) {
                    OutlinedTextField(
                        value = if (phase == "before_prompt") "发送前 (before_prompt)" else "响应后 (after_response)",
                        onValueChange = {}, readOnly = true, label = { Text("触发阶段") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = phaseExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = phaseExpanded, onDismissRequest = { phaseExpanded = false }) {
                        DropdownMenuItem(text = { Text("发送前 (before_prompt)") }, onClick = { phase = "before_prompt"; phaseExpanded = false })
                        DropdownMenuItem(text = { Text("响应后 (after_response)") }, onClick = { phase = "after_response"; phaseExpanded = false })
                    }
                }
                OutlinedTextField(value = condition, onValueChange = { condition = it },
                    label = { Text("条件 (ex: affection >= 50, always)") })
                OutlinedTextField(value = action, onValueChange = { action = it },
                    label = { Text("动作 (ex: add affection 5, set mood happy)") })
                OutlinedTextField(value = priority, onValueChange = { priority = it }, label = { Text("优先级") })
                Row { Text("启用"); Spacer(Modifier.width(8.dp)); Switch(checked = enabled, onCheckedChange = { enabled = it }) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave((rule ?: com.sillyandroid.app.data.entity.CharacterScriptRuleEntity(characterId = characterId, action = "")).copy(
                    scriptName = name, triggerPhase = phase, condition = condition.ifBlank { null },
                    action = action, priority = priority.toIntOrNull() ?: 0, enabled = enabled
                ))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
