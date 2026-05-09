package com.sillyandroid.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sillyandroid.app.SillyApp
import com.sillyandroid.app.data.entity.AgentStepEntity
import com.sillyandroid.app.data.entity.CharacterEntity
import com.sillyandroid.app.ui.theme.ColorPalette
import com.sillyandroid.app.viewmodel.AgentViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentTaskScreen(
    taskId: Long,
    innerPadding: PaddingValues,
    navController: NavHostController,
    viewModel: AgentViewModel = viewModel(factory = AgentViewModel.Factory())
) {
    val task by viewModel.currentTask.collectAsState()
    val steps by viewModel.steps.collectAsState()
    val executionLog by viewModel.executionLog.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddStepDialog by remember { mutableStateOf(false) }
    var showCreateForm by remember { mutableStateOf(taskId == -1L) }
    var characters by remember { mutableStateOf<List<CharacterEntity>>(emptyList()) }

    LaunchedEffect(taskId) {
        if (taskId > 0) {
            viewModel.loadTask(taskId)
            showCreateForm = false
        }
        characters = SillyApp.instance.container.characterRepository.getAllList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(task?.name ?: "Agent 任务") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            if (taskId > 0 && !isRunning) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FloatingActionButton(onClick = { showAddStepDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加步骤")
                    }
                    FloatingActionButton(
                        onClick = { viewModel.runTask() },
                        containerColor = ColorPalette.SuccessGreen
                    ) { Icon(Icons.Default.PlayArrow, contentDescription = "运行") }
                }
            }
        },
        modifier = Modifier.padding(innerPadding)
    ) { padding ->
        if (showCreateForm) {
            CreateTaskForm(
                onCreated = { name, desc ->
                    viewModel.createTask(name, desc, emptyList())
                    showCreateForm = false
                    scope.launch {
                        val tasks = SillyApp.instance.container.agentRepository.getAllTasksList()
                        val newTask = tasks.firstOrNull { it.name == name }
                        if (newTask != null) viewModel.loadTask(newTask.id)
                    }
                },
                padding = padding
            )
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            task?.let { t ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(t.name, style = MaterialTheme.typography.titleLarge)
                        if (t.description.isNotBlank()) {
                            Text(t.description, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val statusLabel = when (t.status) {
                            "pending" -> "待执行"; "running" -> "运行中"
                            "completed" -> "已完成"; "failed" -> "失败"; "paused" -> "已暂停"
                            else -> t.status
                        }
                        Text("状态: $statusLabel", style = MaterialTheme.typography.labelMedium,
                            color = when (t.status) {
                                "running" -> ColorPalette.WarningAmber
                                "completed" -> ColorPalette.SuccessGreen
                                "failed" -> ColorPalette.ErrorRed
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            })
                    }
                }
            }

            Text("步骤", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                itemsIndexed(steps) { index, step ->
                    StepCard(index = index, step = step, onDelete = { viewModel.removeStep(step.id) })
                }
            }

            if (executionLog.isNotEmpty()) {
                Text("执行日志", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    itemsIndexed(executionLog) { _, log ->
                        Text(log, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }

            if (isRunning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
        }
    }

    if (showAddStepDialog) {
        AddStepDialog(
            characters = characters,
            onDismiss = { showAddStepDialog = false },
            onAdd = { characterId, instruction ->
                viewModel.addStep(characterId, instruction)
                showAddStepDialog = false
            }
        )
    }
}

@Composable
fun StepCard(index: Int, step: AgentStepEntity, onDelete: () -> Unit) {
    val statusColor = when (step.status) {
        "running" -> ColorPalette.WarningAmber
        "completed" -> ColorPalette.SuccessGreen
        "failed" -> ColorPalette.ErrorRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Text("${index + 1}", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
            Column(Modifier.weight(1f)) {
                Text(step.instruction, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                if (step.result != null) {
                    Text("结果: ${step.result.take(100)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column {
                Text(step.status, style = MaterialTheme.typography.labelSmall, color = statusColor)
                if (step.status == "pending") {
                    TextButton(onClick = onDelete) { Text("删除", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStepDialog(
    characters: List<CharacterEntity>,
    onDismiss: () -> Unit,
    onAdd: (Long, String) -> Unit
) {
    var selectedCharacterId by remember { mutableStateOf<Long?>(null) }
    var instruction by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加步骤") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    val selectedName = characters.find { it.id == selectedCharacterId }?.name ?: "选择角色"
                    OutlinedTextField(
                        value = selectedName, onValueChange = {}, readOnly = true,
                        label = { Text("角色") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        characters.forEach { character ->
                            DropdownMenuItem(
                                text = { Text(character.name) },
                                onClick = { selectedCharacterId = character.id; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(value = instruction, onValueChange = { instruction = it },
                    label = { Text("步骤指令") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                selectedCharacterId?.let { charId ->
                    if (instruction.isNotBlank()) onAdd(charId, instruction)
                }
            }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun CreateTaskForm(
    onCreated: (String, String) -> Unit,
    padding: PaddingValues
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("创建 Agent 任务", style = MaterialTheme.typography.headlineSmall)
        Text("定义多角色协作的步骤流程", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = name, onValueChange = { name = it },
            label = { Text("任务名称") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it },
            label = { Text("描述") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Button(
            onClick = { onCreated(name, description) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("创建任务") }
    }
}
