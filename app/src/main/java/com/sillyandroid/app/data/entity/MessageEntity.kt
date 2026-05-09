package com.sillyandroid.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 对话消息实体。
 *
 * swipe 机制：swipesJson 存储 JSON 数组，每个元素为一份备选回复；
 * selectedSwipeIndex 指示当前选中哪个备选。
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chat_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chat_id"]),
        Index(value = ["chat_id", "created_at"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "chat_id")
    val chatId: Long,

    @ColumnInfo(name = "role")
    val role: String, // "user" | "assistant" | "system"

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "swipes_json")
    val swipesJson: String? = null, // JSON array of alternative responses

    @ColumnInfo(name = "selected_swipe_index")
    val selectedSwipeIndex: Int = 0,

    @ColumnInfo(name = "token_count")
    val tokenCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_checkpoint")
    val isCheckpoint: Boolean = false,

    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean = false,

    @ColumnInfo(name = "attached_file_uri")
    val attachedFileUri: String? = null,

    @ColumnInfo(name = "attached_file_name")
    val attachedFileName: String? = null
)
