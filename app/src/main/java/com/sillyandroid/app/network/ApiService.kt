package com.sillyandroid.app.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("chat/completions")
    suspend fun chatCompletion(
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>

    @POST("chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun chatCompletionStream(
        @Body request: ChatCompletionRequest
    ): Response<ResponseBody>

    @POST("embeddings")
    suspend fun createEmbedding(
        @Body request: EmbeddingRequest
    ): Response<EmbeddingResponse>
}
