package com.sillyandroid.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * API 调用日志实体。
 *
 * 记录每次 API 请求的完整内容，用于日志查看器。
 */
@Entity(tableName = "api_logs")
data class ApiLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "direction")
    val direction: String = "request", // "request" | "response" | "error"

    @ColumnInfo(name = "endpoint")
    val endpoint: String = "",

    @ColumnInfo(name = "model")
    val model: String = "",

    @ColumnInfo(name = "request_json")
    val requestJson: String? = null,

    @ColumnInfo(name = "response_text")
    val responseText: String? = null,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "token_count")
    val tokenCount: Int = 0,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0
)
