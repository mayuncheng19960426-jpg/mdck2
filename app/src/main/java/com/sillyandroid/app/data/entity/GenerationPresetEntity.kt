package com.sillyandroid.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 对话生成预设实体。
 *
 * 对应 SillyTavern 的 Generation Presets 功能。
 */
@Entity(tableName = "generation_presets")
data class GenerationPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String = "Default",

    @ColumnInfo(name = "temperature")
    val temperature: Float = 0.7f,

    @ColumnInfo(name = "top_p")
    val topP: Float = 1.0f,

    @ColumnInfo(name = "top_k")
    val topK: Int = 0,

    @ColumnInfo(name = "max_tokens")
    val maxTokens: Int = 1024,

    @ColumnInfo(name = "frequency_penalty")
    val frequencyPenalty: Float = 0.0f,

    @ColumnInfo(name = "presence_penalty")
    val presencePenalty: Float = 0.0f,

    @ColumnInfo(name = "repetition_penalty")
    val repetitionPenalty: Float = 1.0f,

    @ColumnInfo(name = "min_p")
    val minP: Float = 0.0f,

    @ColumnInfo(name = "top_a")
    val topA: Float = 0.0f,

    @ColumnInfo(name = "seed")
    val seed: Int? = null,

    @ColumnInfo(name = "stream")
    val stream: Boolean = true,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false
)
