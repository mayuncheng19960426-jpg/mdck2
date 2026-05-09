package com.sillyandroid.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 角色卡实体，兼容 SillyTavern character card v2 格式。
 */
@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "personality")
    val personality: String = "",

    @ColumnInfo(name = "first_message")
    val firstMessage: String = "",

    @ColumnInfo(name = "scenario")
    val scenario: String = "",

    @ColumnInfo(name = "example_dialogue")
    val exampleDialogue: String = "",

    @ColumnInfo(name = "system_prompt")
    val systemPrompt: String = "",

    @ColumnInfo(name = "post_history_instructions")
    val postHistoryInstructions: String = "",

    @ColumnInfo(name = "creator_notes")
    val creatorNotes: String = "",

    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String? = null,

    @ColumnInfo(name = "tags")
    val tags: String? = null, // JSON array of strings

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "import_format")
    val importFormat: String = "json" // "json" | "png"
)
