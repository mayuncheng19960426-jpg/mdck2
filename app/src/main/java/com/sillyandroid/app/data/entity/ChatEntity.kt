package com.sillyandroid.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 聊天会话实体。
 *
 * 分支机制：parentBranchId 指向派生来源的聊天，forkMessageId 指向派生时的消息点。
 * 检查点：checkpointMessageId 指向最后保存的检查点消息。
 */
@Entity(
    tableName = "chats",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["character_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["character_id"]),
        Index(value = ["parent_branch_id"]),
        Index(value = ["is_active"])
    ]
)
data class ChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "character_id")
    val characterId: Long,

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "checkpoint_message_id")
    val checkpointMessageId: Long? = null,

    @ColumnInfo(name = "parent_branch_id")
    val parentBranchId: Long? = null,

    @ColumnInfo(name = "fork_message_id")
    val forkMessageId: Long? = null,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String? = null // extensible metadata
)
