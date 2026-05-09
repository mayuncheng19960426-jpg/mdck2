package com.sillyandroid.app.engine

import com.sillyandroid.app.domain.repository.WorldBookRepository

/**
 * 世界书匹配引擎。
 *
 * 核心逻辑：
 * 1. 检查角色是否有绑定的世界书条目——有则只匹配绑定条目，无则全量匹配
 * 2. 对用户消息和历史消息进行 key 触发词扫描
 * 3. 按优先级排序已触发条目
 * 4. 支持 depth 递归触发（已触发条目的 key 可以再触发其他条目）
 * 5. 按 position 字段插入到指定位置
 */
class WorldBookEngine(
    private val repository: WorldBookRepository
) {
    /**
     * 匹配世界书条目。
     *
     * @param currentMessage 当前用户消息
     * @param recentMessages 最近的对话历史（用于 context-aware 匹配）
     * @param worldBookId 世界书 ID
     * @param characterId 角色 ID（用于检查绑定）
     * @param position 筛选特定插入位置的条目，null 表示不过滤
     */
    suspend fun matchEntries(
        currentMessage: String,
        recentMessages: List<String>,
        worldBookId: Long,
        characterId: Long,
        position: String? = null
    ): List<MatchedEntry> {
        // Determine which entries to use
        val hasBindings = repository.hasBindings(characterId)
        val allEnabled = if (hasBindings) {
            repository.getBoundEntries(characterId)
        } else {
            repository.getEnabledEntries(worldBookId)
        }

        // Filter by position if specified
        val candidates = if (position != null) {
            allEnabled.filter { it.position == position }
        } else {
            allEnabled
        }

        val matched = mutableMapOf<Long, MatchedEntry>() // entry id -> result
        val scanText = buildScanText(currentMessage, recentMessages)
        val triggeredEntryIds = mutableSetOf<Long>()

        // First pass: match constant entries and key-based entries
        for (entry in candidates) {
            if (!entry.enabled) continue

            if (entry.isConstant) {
                matched[entry.id] = MatchedEntry(entry, entry.priority)
                continue
            }

            val keys = parseKeys(entry.key)
            val matchedKey = findMatchingKey(keys, scanText, entry.isSelective)

            if (matchedKey != null) {
                matched[entry.id] = MatchedEntry(entry, entry.priority)
                triggeredEntryIds.add(entry.id)
            }
        }

        // Second pass: recursive depth activation
        var currentDepth = 0
        var newlyTriggered = triggeredEntryIds.toSet()

        while (currentDepth < maxDepth(candidates) && newlyTriggered.isNotEmpty()) {
            val nextTriggered = mutableSetOf<Long>()

            for (entryId in newlyTriggered) {
                val entry = candidates.find { it.id == entryId } ?: continue
                if (entry.depth <= currentDepth) continue

                // Use the triggered entry's keys to find more entries
                val triggeredKeys = parseKeys(entry.key)
                for (subEntry in candidates) {
                    if (subEntry.id in matched || !subEntry.enabled) continue
                    if (subEntry.depth <= currentDepth) continue
                    if (subEntry.isConstant) continue

                    val subKeys = parseKeys(subEntry.key)
                    // Check if triggered entry content/key matches sub-entry's keys
                    val fullText = triggeredKeys.joinToString(" ") + " " + entry.content
                    val matchedKey = findMatchingKey(subKeys, fullText, subEntry.isSelective)
                    if (matchedKey != null) {
                        matched[subEntry.id] = MatchedEntry(subEntry, subEntry.priority)
                        nextTriggered.add(subEntry.id)
                    }
                }
            }

            newlyTriggered = nextTriggered
            currentDepth++
        }

        // Sort: constant first, then by priority descending
        return matched.values
            .sortedWith(compareByDescending<MatchedEntry> {
                if (it.entry.isConstant) Int.MAX_VALUE else it.priority
            }.thenBy { it.entry.order })
    }

    data class MatchedEntry(
        val entry: com.sillyandroid.app.data.entity.WorldBookEntryEntity,
        val priority: Int
    )

    private fun buildScanText(
        currentMessage: String,
        recentMessages: List<String>
    ): String {
        // Scan last N messages + current message
        val scanMessages = recentMessages.takeLast(10) + currentMessage
        return scanMessages.joinToString("\n")
    }

    private fun parseKeys(keyString: String): List<String> {
        return keyString
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * 查找匹配的触发键。
     *
     * @param selective 是否选择性匹配——选择性匹配用 word boundary 匹配，非选择性用 contains
     */
    private fun findMatchingKey(
        keys: List<String>,
        text: String,
        selective: Boolean
    ): String? {
        val lowerText = text.lowercase()
        for (key in keys) {
            val lowerKey = key.lowercase()
            if (selective) {
                // Word boundary match
                val regex = Regex("\\b${Regex.escape(lowerKey)}\\b")
                if (regex.containsMatchIn(lowerText)) return key
            } else {
                // Substring match
                if (lowerKey in lowerText) return key
            }
        }
        return null
    }

    private fun maxDepth(
        entries: List<com.sillyandroid.app.data.entity.WorldBookEntryEntity>
    ): Int = entries.maxOfOrNull { it.depth } ?: 0
}
