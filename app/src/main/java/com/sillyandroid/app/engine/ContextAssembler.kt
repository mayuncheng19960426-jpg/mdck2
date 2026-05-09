package com.sillyandroid.app.engine

import com.google.gson.Gson
import com.sillyandroid.app.data.entity.*
import com.sillyandroid.app.network.ChatApiClient
import com.sillyandroid.app.network.ChatMessage
import com.sillyandroid.app.network.TokenCounter

/**
 * 聊天上下文组装器。
 *
 * 负责将角色卡、世界书条目、对话历史、向量记忆组装为最终提交给 LLM 的 prompt。
 */
class ContextAssembler(
    private val worldBookEngine: WorldBookEngine,
    private val vectorStore: VectorStore
) {
    private val gson = Gson()

    /**
     * 组装完整的消息列表，准备发送给 API。
     */
    suspend fun assembleMessages(
        character: CharacterEntity,
        messages: List<MessageEntity>,
        newUserMessage: String,
        worldBookId: Long? = null,
        maxContextTokens: Int = 8192
    ): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()

        // 1. System prompt
        val systemPrompt = buildSystemPrompt(character)
        result.add(ChatMessage(role = "system", content = systemPrompt))

        // 1.5 Vector memory — search for relevant past messages
        val chatMessages = messages.filter { it.role in listOf("user", "assistant") }
        if (chatMessages.isNotEmpty()) {
            try {
                val relevantMemories = vectorStore.getRelevantMemoriesText(
                    query = newUserMessage,
                    chatId = messages.firstOrNull()?.chatId ?: 0,
                    topK = 3
                )
                if (relevantMemories.isNotBlank()) {
                    result.add(ChatMessage(role = "system", content = relevantMemories))
                }
            } catch (_: Exception) {
                // Vector search failure shouldn't block chat
            }
        }

        // 2. World book entries (before character)
        val beforeEntries = if (worldBookId != null) {
            worldBookEngine.matchEntries(
                newUserMessage,
                messages.map { it.content },
                worldBookId,
                character.id,
                "before_char"
            )
        } else emptyList()

        for (entry in beforeEntries) {
            result.add(ChatMessage(role = "system", content = entry.content))
        }

        // 3. Chat history (trim to fit context window)
        val historyMessages = buildHistoryMessages(messages)
        val beforeTokens = TokenCounter.countMessages(
            result.map { mapOf("role" to it.role, "content" to it.content) }
        )

        val newMsgTokens = TokenCounter.count(newUserMessage) + 4
        val reserveForAfter = 500 // reserve for world book after entries + response
        val availableForHistory = maxContextTokens - beforeTokens - newMsgTokens - reserveForAfter

        val trimmedHistory = trimHistory(historyMessages, availableForHistory)
        result.addAll(trimmedHistory)

        // 4. Current user message
        result.add(ChatMessage(role = "user", content = newUserMessage))

        // 5. World book entries (after character)
        val afterEntries = if (worldBookId != null) {
            worldBookEngine.matchEntries(
                newUserMessage,
                messages.map { it.content },
                worldBookId,
                character.id,
                "after_char"
            )
        } else emptyList()

        for (entry in afterEntries) {
            result.add(ChatMessage(role = "system", content = entry.content))
        }

        return result
    }

    private fun buildSystemPrompt(character: CharacterEntity): String {
        val sb = StringBuilder()

        if (character.systemPrompt.isNotBlank()) {
            sb.appendLine(character.systemPrompt)
        }

        // Character card in SillyTavern format
        sb.appendLine("[角色设定]")
        sb.appendLine("名称: ${character.name}")
        if (character.description.isNotBlank()) {
            sb.appendLine("描述: ${character.description}")
        }
        if (character.personality.isNotBlank()) {
            sb.appendLine("性格: ${character.personality}")
        }
        if (character.scenario.isNotBlank()) {
            sb.appendLine("场景: ${character.scenario}")
        }
        if (character.exampleDialogue.isNotBlank()) {
            sb.appendLine("对话示例:")
            sb.appendLine(character.exampleDialogue)
        }

        if (character.firstMessage.isNotBlank()) {
            sb.appendLine()
            sb.appendLine("[开场消息]")
            sb.appendLine(character.firstMessage)
        }

        if (character.postHistoryInstructions.isNotBlank()) {
            sb.appendLine()
            sb.appendLine("[对话后指令]")
            sb.appendLine(character.postHistoryInstructions)
        }

        return sb.toString()
    }

    private fun buildHistoryMessages(messages: List<MessageEntity>): List<ChatMessage> {
        return messages
            .filter { !it.isHidden }
            .map { msg ->
                ChatMessage(
                    role = when (msg.role) {
                        "assistant" -> "assistant"
                        "system" -> "system"
                        else -> "user"
                    },
                    content = msg.content
                )
            }
    }

    private fun trimHistory(
        messages: List<ChatMessage>,
        maxTokens: Int
    ): List<ChatMessage> {
        val maxTokensClamped = maxTokens.coerceAtLeast(100)
        val kept = mutableListOf<ChatMessage>()
        var currentTokens = 0

        // Keep from newest to oldest until budget exhausted
        for (msg in messages.reversed()) {
            val msgTokens = TokenCounter.count(msg.content) + TokenCounter.count(msg.role) + 4
            if (currentTokens + msgTokens > maxTokensClamped) break
            kept.add(0, msg)
            currentTokens += msgTokens
        }

        return kept
    }
}
