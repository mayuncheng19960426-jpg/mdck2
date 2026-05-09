package com.sillyandroid.app.cardbuilder

import com.google.gson.Gson
import com.sillyandroid.app.domain.AppContainer
import com.sillyandroid.app.network.ChatCompletionRequest
import com.sillyandroid.app.network.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 搓卡引擎：拼合模板 → 调用 API → 返回流式结果。
 */
class CardBuilderEngine(private val container: AppContainer) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    data class BuilderDelta(
        val content: String = "",
        val finishReason: String? = null
    )

    /**
     * 执行搓卡请求。
     *
     * @param selectedIds 用户选中的模板 ID 列表
     * @param userInput 用户的需求描述
     * @param characterName 可选的角色名（用作模板中的 {角色名} 占位符替换）
     */
    fun build(
        selectedIds: List<Int>,
        userInput: String,
        characterName: String? = null
    ): Flow<BuilderDelta> = flow {
        val config = container.settingsRepository.getDefaultApiConfig()
            ?: throw IllegalStateException("No API configuration found")

        val preset = container.settingsRepository.getDefaultPreset()
            ?: throw IllegalStateException("No preset found")

        // Assemble system prompt from templates
        var systemPrompt = CardBuilderTemplates.assembleSystemPrompt(selectedIds, userInput)

        // Replace {角色名} placeholder if available
        if (characterName != null) {
            systemPrompt = systemPrompt.replace("{角色名}", characterName)
        }

        val messages = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = userInput)
        )

        val request = ChatCompletionRequest(
            model = config.modelName,
            messages = messages,
            temperature = 0.3f, // Lower temperature for structured output
            maxTokens = preset.maxTokens.coerceAtLeast(2048),
            stream = true
        )

        val json = gson.toJson(request)
        val httpRequest = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/chat/completions")
            .post(json.toRequestBody(jsonMediaType))
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Accept", "text/event-stream")
            .build()

        val response = client.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw RuntimeException("API error ${response.code}: $errorBody")
        }

        val body = response.body ?: throw RuntimeException("Empty response")
        val reader = java.io.BufferedReader(java.io.InputStreamReader(body.byteStream()))

        try {
            var line: String?
            var buffer = StringBuilder()

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue

                if (currentLine.isEmpty()) {
                    processSseData(buffer.toString())?.let { emit(it) }
                    buffer = StringBuilder()
                } else if (currentLine.startsWith("data: ")) {
                    val data = currentLine.removePrefix("data: ")
                    if (data == "[DONE]") {
                        emit(BuilderDelta(finishReason = "stop"))
                        return@flow
                    }
                    buffer.append(data)
                }
            }
        } finally {
            reader.close()
            response.close()
        }
    }

    private fun processSseData(data: String): BuilderDelta? {
        if (data.isBlank()) return null
        return try {
            val response = gson.fromJson(
                data,
                com.sillyandroid.app.network.ChatCompletionResponse::class.java
            )
            val choice = response.choices?.firstOrNull() ?: return null
            BuilderDelta(
                content = choice.delta?.content ?: "",
                finishReason = choice.finishReason
            )
        } catch (e: Exception) {
            null
        }
    }
}
