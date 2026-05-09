package com.sillyandroid.app.network

import com.google.gson.annotations.SerializedName

// --- OpenAI-compatible Request ---
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    @SerializedName("top_p") val topP: Float = 1.0f,
    @SerializedName("top_k") val topK: Int = 0,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    @SerializedName("frequency_penalty") val frequencyPenalty: Float = 0.0f,
    @SerializedName("presence_penalty") val presencePenalty: Float = 0.0f,
    @SerializedName("repetition_penalty") val repetitionPenalty: Float = 1.0f,
    @SerializedName("min_p") val minP: Float = 0.0f,
    @SerializedName("top_a") val topA: Float = 0.0f,
    val seed: Int? = null,
    val stream: Boolean = true
)

data class ChatMessage(
    val role: String, // "system" | "user" | "assistant"
    val content: String
)

// --- OpenAI-compatible Response ---
data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null
)

data class Choice(
    val index: Int = 0,
    val delta: Delta? = null,
    val message: ChatMessage? = null,
    @SerializedName("finish_reason") val finishReason: String? = null
)

data class Delta(
    val role: String? = null,
    val content: String? = null
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int = 0,
    @SerializedName("completion_tokens") val completionTokens: Int = 0,
    @SerializedName("total_tokens") val totalTokens: Int = 0
)

// --- Embedding Request/Response ---
data class EmbeddingRequest(
    val model: String = "text-embedding-ada-002",
    val input: String
)

data class EmbeddingResponse(
    val data: List<EmbeddingData>? = null
)

data class EmbeddingData(
    val embedding: List<Float>? = null,
    val index: Int = 0
)
