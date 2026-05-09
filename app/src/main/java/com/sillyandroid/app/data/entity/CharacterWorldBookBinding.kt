package com.sillyandroid.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 角色与世界书条目的关联表。
 *
 * 这是新增功能：角色可以绑定特定的世界书条目。
 * 绑定了条目的角色只会触发其关联的条目，未绑定的角色保持全量匹配。
 */
@Entity(
    tableName = "character_world_book_bindings",
    primaryKeys = ["character_id", "entry_id"],
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["character_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorldBookEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["character_id"]),
        Index(value = ["entry_id"])
    ]
)
data class CharacterWorldBookBinding(
    @androidx.room.ColumnInfo(name = "character_id")
    val characterId: Long,

    @androidx.room.ColumnInfo(name = "entry_id")
    val entryId: Long
)
