package com.sillyandroid.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 正则规则实体。
 *
 * 对应 SillyTavern 的 Regex 扩展：对提示词进行预处理和后处理。
 */
@Entity(tableName = "regex_rules")
data class RegexRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "script_name")
    val scriptName: String = "",

    @ColumnInfo(name = "find_regex")
    val findRegex: String = "",

    @ColumnInfo(name = "replace_string")
    val replaceString: String = "",

    @ColumnInfo(name = "trim_strings")
    val trimStrings: String? = null,

    @ColumnInfo(name = "placeholders_json")
    val placeholdersJson: String? = null,

    @ColumnInfo(name = "is_global")
    val isGlobal: Boolean = true,

    @ColumnInfo(name = "source")
    val source: String = "user", // "user" | "script"

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,

    @ColumnInfo(name = "sort_order")
    val order: Int = 0,

    @ColumnInfo(name = "run_on_edit")
    val runOnEdit: Boolean = false
)
