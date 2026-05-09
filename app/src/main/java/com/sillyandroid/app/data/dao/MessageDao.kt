package com.sillyandroid.app.data.dao

import androidx.room.*
import com.sillyandroid.app.data.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY created_at ASC")
    fun getByChatFlow(chatId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chat_id = :chatId AND created_at <= :beforeTimestamp ORDER BY created_at ASC")
    suspend fun getByChatBefore(chatId: Long, beforeTimestamp: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY created_at ASC")
    suspend fun getByChat(chatId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: Long): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageEntity): Long

    @Update
    suspend fun update(entity: MessageEntity)

    @Delete
    suspend fun delete(entity: MessageEntity)

    @Query("DELETE FROM messages WHERE chat_id = :chatId AND created_at > :sinceTimestamp")
    suspend fun deleteAfter(chatId: Long, sinceTimestamp: Long)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE messages SET swipes_json = :swipesJson, selected_swipe_index = :index WHERE id = :id")
    suspend fun updateSwipe(id: Long, swipesJson: String, index: Int)

    @Query("UPDATE messages SET is_checkpoint = :isCheckpoint WHERE id = :id")
    suspend fun setCheckpoint(id: Long, isCheckpoint: Boolean = true)

    @Query("SELECT COUNT(*) FROM messages WHERE chat_id = :chatId")
    suspend fun countByChat(chatId: Long): Int

    @Query("SELECT * FROM messages WHERE chat_id = :chatId AND role = 'system' LIMIT 1")
    suspend fun getSystemMessage(chatId: Long): MessageEntity?
}
