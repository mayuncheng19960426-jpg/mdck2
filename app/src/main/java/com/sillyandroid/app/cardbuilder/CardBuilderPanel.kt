package com.sillyandroid.app.cardbuilder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.sillyandroid.app.ui.theme.ColorPalette

/**
 * 搓卡模式侧滑面板。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardBuilderPanel(
    selectedIds: List<Int>,
    onToggleItem: (Int) -> Unit,
    onGenerate: () -> Unit,
    onCancel: () -> Unit,
    isGenerating: Boolean = false
) {
    var showDetailId by remember { mutableIntStateOf(-1) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, tint = ColorPalette.Accent)
                Spacer(Modifier.width(8.dp))
                Text("搓卡模式", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
        },
        text = {
            Column {
                Text(
                    "勾选要生成的条目，点击「生成」发送给 AI。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    itemsIndexed(CardBuilderTemplates.templates) { _, template ->
                        val isSelected = selectedIds.contains(template.id)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { onToggleItem(template.id) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleItem(template.id) }
                                )
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${template.id}. ${template.title}",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        if (template.worldBookHint != null) {
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "→世界书",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ColorPalette.Accent
                                            )
                                        }
                                    }
                                    Text(
                                        template.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                IconButton(onClick = { showDetailId = template.id }) {
                                    Icon(Icons.Default.Info, contentDescription = "详情",
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel) { Text("取消") }
                Button(
                    onClick = onGenerate,
                    enabled = selectedIds.isNotEmpty() && !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("生成 (${selectedIds.size})")
                }
            }
        }
    )

    // Detail dialog
    if (showDetailId > 0) {
        val template = CardBuilderTemplates.getById(showDetailId)
        if (template != null) {
            AlertDialog(
                onDismissRequest = { showDetailId = -1 },
                title = { Text("${template.id}. ${template.title}") },
                text = {
                    Column {
                        Text("目标: ${template.targetEntity}", style = MaterialTheme.typography.labelMedium)
                        template.worldBookHint?.let { hint ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("世界书位置: ${hint.position}", style = MaterialTheme.typography.labelSmall)
                                Text("优先级: ${hint.priority}", style = MaterialTheme.typography.labelSmall)
                                Text("深度: ${hint.depth}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text(
                            template.prompt.take(800) + if (template.prompt.length > 800) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontStyle = FontStyle.Normal
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDetailId = -1 }) { Text("关闭") }
                }
            )
        }
    }
}
