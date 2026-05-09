package com.sillyandroid.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 聊天变量实体。
 *
 * 持久化 MVU 模型状态——每个聊天会话一组键值变量。
 * 使用分类型字段避免频繁反序列化。
 */
@Entity(
    tableName = "chat_variables",
    primaryKeys = ["chat_id", "name"],
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chat_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chat_id"])]
)
data class ChatVariableEntity(
    @ColumnInfo(name = "chat_id")
    val chatId: Long,

    @ColumnInfo(name = "name")
    val name: String, // variable name, e.g. "affection", "round", "flag_found"

    @ColumnInfo(name = "type")
    val type: String = "string", // "int" | "float" | "string" | "boolean"

    @ColumnInfo(name = "int_value")
    val intValue: Int = 0,

    @ColumnInfo(name = "float_value")
    val floatValue: Float = 0f,

    @ColumnInfo(name = "string_value")
    val stringValue: String = "",

    @ColumnInfo(name = "boolean_value")
    val booleanValue: Boolean = false
) {
    /** 获取当前值用于显示/条件比较 */
    val currentValue: Any
        get() = when (type) {
            "int" -> intValue
            "float" -> floatValue
            "boolean" -> booleanValue
            else -> stringValue
        }

    /** 用于模板替换的字符串表示 */
    val displayValue: String
        get() = when (type) {
            "int" -> intValue.toString()
            "float" -> floatValue.toString()
            "boolean" -> booleanValue.toString()
            else -> stringValue
        }
}
