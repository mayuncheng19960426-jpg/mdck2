package com.sillyandroid.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Agent 任务实体。
 *
 * 一个任务包含多个步骤，每个步骤指定一个角色来执行。
 */
@Entity(tableName = "agent_tasks")
data class AgentTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "status")
    val status: String = "pending", // "pending" | "running" | "paused" | "completed" | "failed"

    @ColumnInfo(name = "current_step_order")
    val currentStepOrder: Int = 0,

    @ColumnInfo(name = "global_context")
    val globalContext: String? = null, // shared context across steps

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
