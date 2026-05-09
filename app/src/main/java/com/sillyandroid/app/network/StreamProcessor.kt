package com.sillyandroid.app.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * SSE 流式响应处理器。
 * 将 SSE 事件流转换为 Kotlin Flow<StreamDelta>。
 */
class StreamProcessor {

    data class StreamDelta(
        val content: String = "",
        val finishReason: String? = null
    )

    private val gson = Gson()

    /**
     * 发起流式请求并返回 Flow。
     */
    fun stream(
        client: OkHttpClient,
        url: String,
        apiKey: String,
        requestBody: ChatCompletionRequest
    ): Flow<StreamDelta> = flow {
        val json = gson.toJson(requestBody)
        val request = Request.Builder()
            .url("$url/chat/completions")
            .post(json.toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw ApiException(response.code, errorBody)
        }

        val body = response.body ?: throw ApiException(0, "Empty response body")
        val reader = BufferedReader(InputStreamReader(body.byteStream()))

        try {
            var line: String?
            var buffer = StringBuilder()

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue

                if (currentLine.isEmpty()) {
                    // Empty line signals end of an SSE event
                    processSseData(buffer.toString())?.let { emit(it) }
                    buffer = StringBuilder()
                } else if (currentLine.startsWith("data: ")) {
                    val data = currentLine.removePrefix("data: ")
                    if (data == "[DONE]") {
                        emit(StreamDelta(finishReason = "stop"))
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

    private fun processSseData(data: String): StreamDelta? {
        if (data.isBlank()) return null
        return try {
            val response = gson.fromJson(data, ChatCompletionResponse::class.java)
            val choice = response.choices?.firstOrNull() ?: return null
            val content = choice.delta?.content ?: ""
            StreamDelta(
                content = content,
                finishReason = choice.finishReason
            )
        } catch (e: Exception) {
            null // Skip malformed lines
        }
    }
}

class ApiException(val code: Int, override val message: String) : Exception(message)
