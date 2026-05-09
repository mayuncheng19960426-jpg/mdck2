package com.sillyandroid.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 世界书条目实体。
 *
 * SillyTavern 核心字段：
 * - key: 逗号分隔的触发词列表
 * - content: 条目正文
 * - priority: 优先级（越大越靠前）
 * - position: 插入位置 "before_char" | "after_char" | "in_char"
 * - depth: 扫描深度（递归触发层级）
 * - isSelective: 是否选择性触发（vs 非选择性）
 * - isConstant: 是否常驻注入（忽略 key 匹配）
 */
@Entity(
    tableName = "world_book_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorldBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["world_book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["world_book_id"]),
        Index(value = ["world_book_id", "priority"])
    ]
)
data class WorldBookEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "world_book_id")
    val worldBookId: Long,

    @ColumnInfo(name = "entry_key")
    val key: String = "",

    @ColumnInfo(name = "content")
    val content: String = "",

    @ColumnInfo(name = "comment")
    val comment: String = "",

    @ColumnInfo(name = "priority")
    val priority: Int = 0,

    @ColumnInfo(name = "position")
    val position: String = "before_char", // "before_char" | "after_char" | "in_char"

    @ColumnInfo(name = "depth")
    val depth: Int = 0,

    @ColumnInfo(name = "selective")
    val isSelective: Boolean = true,

    @ColumnInfo(name = "constant")
    val isConstant: Boolean = false,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,

    @ColumnInfo(name = "sort_order")
    val order: Int = 0
)
