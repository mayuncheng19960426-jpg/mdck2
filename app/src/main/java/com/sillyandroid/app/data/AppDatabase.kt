package com.sillyandroid.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sillyandroid.app.data.dao.*
import com.sillyandroid.app.data.entity.*

@Database(
    entities = [
        CharacterEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        WorldBookEntity::class,
        WorldBookEntryEntity::class,
        CharacterWorldBookBinding::class,
        RegexRuleEntity::class,
        GenerationPresetEntity::class,
        ApiConfigEntity::class,
        VectorMemoryEntity::class,
        AgentTaskEntity::class,
        AgentStepEntity::class,
        ApiLogEntity::class,
        ChatVariableEntity::class,
        CharacterScriptRuleEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun characterDao(): CharacterDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun worldBookDao(): WorldBookDao
    abstract fun regexRuleDao(): RegexRuleDao
    abstract fun presetDao(): PresetDao
    abstract fun apiConfigDao(): ApiConfigDao
    abstract fun vectorMemoryDao(): VectorMemoryDao
    abstract fun agentDao(): AgentDao
    abstract fun apiLogDao(): ApiLogDao
    abstract fun chatVariableDao(): ChatVariableDao
    abstract fun characterScriptRuleDao(): CharacterScriptRuleDao

    companion object {
        private const val DB_NAME = "sillyandroid.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun create(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
