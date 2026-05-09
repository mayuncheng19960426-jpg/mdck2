package com.sillyandroid.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Agent 步骤实体。
 *
 * 每个步骤指派一个角色，包含执行指令和结果。
 */
@Entity(
    tableName = "agent_steps",
    foreignKeys = [
        ForeignKey(
            entity = AgentTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["character_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["task_id"]),
        Index(value = ["task_id", "sort_order"], unique = true)
    ]
)
data class AgentStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "task_id")
    val taskId: Long,

    @ColumnInfo(name = "character_id")
    val characterId: Long,

    @ColumnInfo(name = "instruction")
    val instruction: String = "",

    @ColumnInfo(name = "sort_order")
    val order: Int = 0,

    @ColumnInfo(name = "status")
    val status: String = "pending", // "pending" | "running" | "completed" | "failed"

    @ColumnInfo(name = "result")
    val result: String? = null,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "input_context")
    val inputContext: String? = null, // context passed from previous step

    @ColumnInfo(name = "preset_id")
    val presetId: Long? = null, // optional per-step generation preset override

    @ColumnInfo(name = "started_at")
    val startedAt: Long? = null,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
)
