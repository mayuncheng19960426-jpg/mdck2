package com.sillyandroid.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 向量记忆实体。
 *
 * 存储对话片段的文本表征向量，用于语义检索。
 * embeddingJson 存储 JSON 序列化的 FloatArray。
 */
@Entity(
    tableName = "vector_memories",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chat_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chat_id"])
    ]
)
data class VectorMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "chat_id")
    val chatId: Long,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "embedding_json")
    val embeddingJson: String? = null, // JSON array of floats

    @ColumnInfo(name = "source_message_id")
    val sourceMessageId: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "access_count")
    val accessCount: Int = 0,

    @ColumnInfo(name = "last_accessed_at")
    val lastAccessedAt: Long = 0
)
