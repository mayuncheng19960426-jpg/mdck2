package com.sillyandroid.app.data.dao

import androidx.room.*
import com.sillyandroid.app.data.entity.ChatVariableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatVariableDao {

    @Query("SELECT * FROM chat_variables WHERE chat_id = :chatId")
    suspend fun getByChat(chatId: Long): List<ChatVariableEntity>

    @Query("SELECT * FROM chat_variables WHERE chat_id = :chatId")
    fun getByChatFlow(chatId: Long): Flow<List<ChatVariableEntity>>

    @Query("SELECT * FROM chat_variables WHERE chat_id = :chatId AND name = :name LIMIT 1")
    suspend fun get(chatId: Long, name: String): ChatVariableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChatVariableEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ChatVariableEntity>)

    @Query("DELETE FROM chat_variables WHERE chat_id = :chatId AND name = :name")
    suspend fun delete(chatId: Long, name: String)

    @Query("DELETE FROM chat_variables WHERE chat_id = :chatId")
    suspend fun deleteByChat(chatId: Long)
}
