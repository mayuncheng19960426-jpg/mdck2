package com.sillyandroid.app.engine

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sillyandroid.app.data.entity.VectorMemoryEntity
import com.sillyandroid.app.domain.AppContainer
import com.sillyandroid.app.network.ChatApiClient
import kotlin.math.sqrt

/**
 * 向量存储器。
 *
 * 功能：
 * - 存储对话片段及其 embedding 向量
 * - 通过余弦相似度检索相关记忆
 * - 将检索结果注入聊天上下文
 */
class VectorStore(
    private val apiClient: ChatApiClient,
    private val container: AppContainer
) {
    private val gson = Gson()

    /**
     * 为文本创建 embedding 并存储。
     */
    suspend fun storeMemory(
        chatId: Long,
        content: String,
        sourceMessageId: Long? = null
    ): Long {
        val embedding = try {
            apiClient.getEmbedding(content)
        } catch (e: Exception) {
            // Embedding API failed; fallback to storing without vector
            null
        }

        val entity = VectorMemoryEntity(
            chatId = chatId,
            content = content,
            embeddingJson = embedding?.let { gson.toJson(it) },
            sourceMessageId = sourceMessageId
        )

        return container.database.vectorMemoryDao().insert(entity)
    }

    /**
     * 批量存储记忆。
     */
    suspend fun storeMemories(
        chatId: Long,
        contents: List<String>
    ) {
        val entities = mutableListOf<VectorMemoryEntity>()

        for (content in contents) {
            if (content.isBlank()) continue
            val embedding = try {
                apiClient.getEmbedding(content)
            } catch (e: Exception) {
                null
            }
            entities.add(
                VectorMemoryEntity(
                    chatId = chatId,
                    content = content,
                    embeddingJson = embedding?.let { gson.toJson(it) }
                )
            )
        }

        if (entities.isNotEmpty()) {
            container.database.vectorMemoryDao().insertAll(entities)
        }
    }

    /**
     * 语义检索相关记忆。
     *
     * @param query 查询文本
     * @param chatId 聊天 ID
     * @param topK 返回最相关的前 K 条
     * @param threshold 余弦相似度阈值
     */
    suspend fun search(
        query: String,
        chatId: Long,
        topK: Int = 3,
        threshold: Float = 0.7f
    ): List<SearchResult> {
        val queryEmbedding = try {
            apiClient.getEmbedding(query)
        } catch (e: Exception) {
            return emptyList()
        }

        val memories = container.database.vectorMemoryDao().getByChat(chatId)

        val scored = memories
            .filter { !it.embeddingJson.isNullOrBlank() }
            .mapNotNull { memory ->
                val embedding = parseEmbedding(memory.embeddingJson!!) ?: return@mapNotNull null
                val similarity = cosineSimilarity(queryEmbedding, embedding)
                if (similarity >= threshold) {
                    SearchResult(memory, similarity)
                } else null
            }
            .sortedByDescending { it.score }
            .take(topK)

        // Record access
        for (result in scored) {
            container.database.vectorMemoryDao().recordAccess(result.memory.id)
        }

        return scored
    }

    /**
     * 获取相关记忆的文本摘要，用于注入聊天上下文。
     */
    suspend fun getRelevantMemoriesText(
        query: String,
        chatId: Long,
        topK: Int = 3
    ): String {
        val results = search(query, chatId, topK)
        if (results.isEmpty()) return ""

        return buildString {
            appendLine("[相关记忆]")
            for ((index, result) in results.withIndex()) {
                appendLine("${index + 1}. ${result.memory.content}")
            }
        }
    }

    private fun parseEmbedding(json: String): List<Float>? {
        return try {
            val type = object : TypeToken<List<Float>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    private fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        require(a.size == b.size) { "Embedding dimensions must match" }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        if (normA == 0f || normB == 0f) return 0f
        return dotProduct / (sqrt(normA) * sqrt(normB))
    }

    data class SearchResult(
        val memory: VectorMemoryEntity,
        val score: Float
    )
}
