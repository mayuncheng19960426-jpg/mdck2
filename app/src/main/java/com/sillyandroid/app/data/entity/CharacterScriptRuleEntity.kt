package com.sillyandroid.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 角色脚本规则实体。
 *
 * MVU 模式中的 Update 函数：条件触发 → 执行动作修改变量状态。
 * 每个规则绑定到一个角色，在指定阶段执行。
 */
@Entity(
    tableName = "character_script_rules",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["character_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["character_id", "priority"])]
)
data class CharacterScriptRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "character_id")
    val characterId: Long,

    @ColumnInfo(name = "script_name")
    val scriptName: String = "",

    @ColumnInfo(name = "trigger_phase")
    val triggerPhase: String = "before_prompt", // "before_prompt" | "after_response"

    @ColumnInfo(name = "condition")
    val condition: String? = null, // e.g. "affection >= 50" or "always"

    @ColumnInfo(name = "action")
    val action: String, // e.g. "set mood happy", "add affection 5", "toggle flag_found"

    @ColumnInfo(name = "priority")
    val priority: Int = 0,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true
)
