package com.sillyandroid.app.network

import com.google.gson.Gson
import com.sillyandroid.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 聊天 API 客户端。
 *
 * 支持 OpenAI-compatible API，含流式和非流式两种调用方式。
 */
class ChatApiClient(
    private val settingsRepository: SettingsRepository
) {
    private val streamProcessor = StreamProcessor()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    suspend fun streamCompletion(
        request: ChatCompletionRequest,
        apiConfigId: Long? = null
    ): Flow<StreamProcessor.StreamDelta> {
        val config = if (apiConfigId != null) {
            settingsRepository.getApiConfig(apiConfigId)
        } else {
            settingsRepository.getDefaultApiConfig()
        } ?: throw ApiException(0, "No API configuration found")

        return streamProcessor.stream(
            client = client,
            url = config.baseUrl.trimEnd('/'),
            apiKey = config.apiKey,
            requestBody = request.copy(model = config.modelName, stream = true)
        )
    }

    /**
     * 获取文本 embedding。
     * 用于向量存储的语义检索。
     */
    suspend fun getEmbedding(text: String): List<Float> {
        val config = settingsRepository.getDefaultApiConfig()
            ?: throw ApiException(0, "No API configuration found")

        val json = gson.toJson(EmbeddingRequest(input = text))
        val requestBody = json.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/embeddings")
            .post(requestBody)
            .header("Authorization", "Bearer ${config.apiKey}")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw ApiException(response.code, response.body?.string() ?: "Unknown error")
        }

        val body = response.body?.string() ?: throw ApiException(0, "Empty response")
        val embeddingResponse = gson.fromJson(body, EmbeddingResponse::class.java)

        return embeddingResponse.data?.firstOrNull()?.embedding
            ?: throw ApiException(0, "No embedding data in response")
    }
}
