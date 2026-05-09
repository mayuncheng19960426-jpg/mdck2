package com.sillyandroid.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * API 配置实体。
 *
 * 支持 OpenAI-compatible API 格式。
 */
@Entity(tableName = "api_configs")
data class ApiConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String = "Default",

    @ColumnInfo(name = "base_url")
    val baseUrl: String = "https://api.openai.com/v1",

    @ColumnInfo(name = "api_key")
    val apiKey: String = "",

    @ColumnInfo(name = "model_name")
    val modelName: String = "gpt-4",

    @ColumnInfo(name = "max_context_length")
    val maxContextLength: Int = 8192,

    @ColumnInfo(name = "custom_headers_json")
    val customHeadersJson: String? = null,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false
)
