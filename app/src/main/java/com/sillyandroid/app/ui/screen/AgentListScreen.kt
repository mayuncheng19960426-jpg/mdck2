package com.sillyandroid.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sillyandroid.app.data.entity.AgentTaskEntity
import com.sillyandroid.app.ui.navigation.Routes
import com.sillyandroid.app.ui.theme.ColorPalette
import com.sillyandroid.app.viewmodel.AgentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentListScreen(
    innerPadding: PaddingValues,
    navController: NavHostController,
    viewModel: AgentViewModel = viewModel(factory = AgentViewModel.Factory())
) {
    val tasks by viewModel.tasks.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<AgentTaskEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Agent 任务") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新建任务")
            }
        },
        modifier = Modifier.padding(innerPadding)
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("暂无 Agent 任务", style = MaterialTheme.typography.titleMedium)
                    Text("创建多角色协作任务", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(tasks, key = { it.id }) { task ->
                    AgentTaskCard(
                        task = task,
                        onClick = { navController.navigate(Routes.agentTask(task.id)) },
                        onDelete = { deleteTarget = task }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        var taskName by remember { mutableStateOf("") }
        var taskDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建 Agent 任务") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = taskName, onValueChange = { taskName = it },
                        label = { Text("任务名称") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = taskDesc, onValueChange = { taskDesc = it },
                        label = { Text("描述") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createTask(taskName, taskDesc, emptyList())
                    showCreateDialog = false
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("取消") }
            }
        )
    }

    deleteTarget?.let { task ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除任务") },
            text = { Text("确认删除「${task.name}」？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task.id)
                    deleteTarget = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

@Composable
fun AgentTaskCard(
    task: AgentTaskEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (task.status) {
        "running" -> ColorPalette.WarningAmber
        "completed" -> ColorPalette.SuccessGreen
        "failed" -> ColorPalette.ErrorRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusText = when (task.status) {
        "pending" -> "待执行"
        "running" -> "运行中"
        "completed" -> "已完成"
        "failed" -> "失败"
        "paused" -> "已暂停"
        else -> task.status
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(statusText, style = MaterialTheme.typography.labelSmall, color = statusColor)
                }
                if (task.description.isNotBlank()) {
                    Text(task.description, style = MaterialTheme.typography.bodySmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onDelete) {
                Text("删除", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
