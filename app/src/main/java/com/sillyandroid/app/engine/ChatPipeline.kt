package com.sillyandroid.app.engine

import com.google.gson.Gson
import com.sillyandroid.app.data.entity.*
import com.sillyandroid.app.domain.AppContainer
import com.sillyandroid.app.network.ChatCompletionRequest
import com.sillyandroid.app.network.ChatMessage
import com.sillyandroid.app.network.StreamProcessor.StreamDelta
import com.sillyandroid.app.network.TokenCounter
import kotlinx.coroutines.flow.Flow

/**
 * 聊天处理管线。
 *
 * 管线顺序：Regex 预处理 → MVU before_prompt (规则 + 变量替换) →
 * 上下文组装 → Regex → API → MVU after_response → Regex 后处理 →
 * 日志 + 向量记忆
 */
class ChatPipeline(private val container: AppContainer) {

    private val contextAssembler = ContextAssembler(
        worldBookEngine = WorldBookEngine(container.worldBookRepository),
        vectorStore = VectorStore(container.chatApiClient, container)
    )
    private val regexEngine = RegexEngine()
    private val apiClient = container.chatApiClient
    private val gson = Gson()
    private val mvuEngine = MvuEngine(container.database)

    @Volatile
    var lastAssembledMessages: List<ChatMessage> = emptyList()
        private set

    @Volatile
    var lastRequestJson: String = ""
        private set

    suspend fun sendMessageStream(
        chatId: Long,
        character: CharacterEntity,
        userMessage: String,
        presetId: Long? = null,
        worldBookId: Long? = null,
        apiConfigId: Long? = null
    ): Flow<StreamDelta> {
        val messages = container.chatRepository.getMessagesList(chatId)
        val regexRules = container.regexRepository.getEnabledList()

        // --- MVU: before_prompt phase ---
        val messageCount = messages.size
        mvuEngine.executeRules(character.id, chatId, "before_prompt", messageCount)

        // Auto-increment round counter
        mvuEngine.setManualVariable(chatId, "round", "${messageCount / 2}")
        mvuEngine.executeAction("inc message_count", chatId)

        // Expand {{var}} in user message
        val expandedMessage = mvuEngine.expandVariables(userMessage, chatId)

        val processedMessage = regexEngine.preProcess(expandedMessage, regexRules)
        container.chatRepository.sendMessage(chatId, "user", expandedMessage)

        val preset = if (presetId != null) {
            container.settingsRepository.getPreset(presetId)
        } else {
            container.settingsRepository.getDefaultPreset()
        } ?: GenerationPresetEntity()

        val apiConfig = if (apiConfigId != null) {
            container.settingsRepository.getApiConfig(apiConfigId)
        } else {
            container.settingsRepository.getDefaultApiConfig()
        }

        val maxContext = apiConfig?.maxContextLength ?: 8192
        val assembledMessages = contextAssembler.assembleMessages(
            character = character, messages = messages,
            newUserMessage = processedMessage, worldBookId = worldBookId,
            maxContextTokens = maxContext
        )

        // Expand {{var}} in all assembled messages
        val varExpanded = assembledMessages.map { msg ->
            msg.copy(content = mvuEngine.expandVariables(msg.content, chatId))
        }

        val processedMessages = varExpanded.map { msg ->
            msg.copy(content = regexEngine.preProcess(msg.content, regexRules))
        }

        lastAssembledMessages = processedMessages

        val request = ChatCompletionRequest(
            model = apiConfig?.modelName ?: "gpt-4",
            messages = processedMessages,
            temperature = preset.temperature,
            topP = preset.topP, topK = preset.topK,
            maxTokens = preset.maxTokens,
            frequencyPenalty = preset.frequencyPenalty,
            presencePenalty = preset.presencePenalty,
            repetitionPenalty = preset.repetitionPenalty,
            minP = preset.minP, topA = preset.topA,
            seed = preset.seed, stream = true
        )

        val requestJson = gson.toJson(request)
        lastRequestJson = requestJson
        val tokenEstimate = TokenCounter.countMessages(
            processedMessages.map { mapOf("role" to it.role, "content" to it.content) }
        )
        ApiLogger.logRequest(
            endpoint = apiConfig?.baseUrl ?: "unknown",
            model = request.model,
            requestJson = requestJson,
            tokenCount = tokenEstimate
        )

        val startTime = System.currentTimeMillis()
        val rawStream = apiClient.streamCompletion(request, apiConfigId)

        return kotlinx.coroutines.flow.flow {
            val responseBuilder = StringBuilder()
            try {
                rawStream.collect { delta ->
                    responseBuilder.append(delta.content)
                    emit(delta)
                }
                val fullResponse = responseBuilder.toString()

                // --- MVU: after_response phase ---
                val newMessageCount = messageCount + 2
                mvuEngine.executeRules(character.id, chatId, "after_response", newMessageCount)

                // Expand {{var}} in response
                val expandedResponse = mvuEngine.expandVariables(fullResponse, chatId)
                responseBuilder.clear()
                responseBuilder.append(expandedResponse)

                ApiLogger.logResponse(responseBuilder.toString(), System.currentTimeMillis() - startTime)
            } catch (e: Exception) {
                ApiLogger.logError(e.message ?: "Unknown error")
                throw e
            }
        }
    }

    suspend fun postProcessResponse(response: String): String {
        val regexRules = container.regexRepository.getEnabledList()
        return regexEngine.postProcess(response, regexRules)
    }

    suspend fun storeAssistantMessage(chatId: Long, content: String, tokenCount: Int = 0): Long {
        return container.chatRepository.sendMessage(chatId, "assistant", content)
    }

    suspend fun generateVectorMemories(chatId: Long, messages: List<MessageEntity>) {
        val vectorStore = VectorStore(container.chatApiClient, container)
        val contents = messages.filter { it.role in listOf("user", "assistant") }.map { it.content }
        if (contents.isNotEmpty()) {
            try { vectorStore.storeMemories(chatId, contents) } catch (_: Exception) {}
        }
    }

    suspend fun getRelevantMemories(query: String, chatId: Long): String {
        val vectorStore = VectorStore(container.chatApiClient, container)
        return try { vectorStore.getRelevantMemoriesText(query, chatId) } catch (e: Exception) { "" }
    }

    fun formatPromptForViewer(): String {
        if (lastAssembledMessages.isEmpty()) return "尚未发送消息。"
        return buildString {
            for ((i, msg) in lastAssembledMessages.withIndex()) {
                appendLine("─── 消息 ${i + 1} [${msg.role}] ───")
                val display = if (msg.content.length > 2000) {
                    msg.content.take(2000) + "\n\n... (截断，共 ${msg.content.length} 字符)"
                } else msg.content
                appendLine(display)
                appendLine()
            }
            if (lastRequestJson.isNotEmpty()) {
                appendLine("─── API 请求参数 ───")
                val prettyJson = try {
                    val jsonObj = com.google.gson.JsonParser.parseString(lastRequestJson)
                    com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(jsonObj)
                } catch (_: Exception) { lastRequestJson }
                appendLine(prettyJson)
            }
        }
    }

    /**
     * 获取 MVU 变量快照（供 UI 监控面板使用）。
     */
    suspend fun getMvuVariables(chatId: Long): Map<String, String> {
        return mvuEngine.getVariables(chatId)
    }
}
