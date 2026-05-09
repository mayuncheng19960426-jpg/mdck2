package com.sillyandroid.app.data.dao

import androidx.room.*
import com.sillyandroid.app.data.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chats ORDER BY updated_at DESC")
    fun getAllFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE is_active = 1 ORDER BY updated_at DESC")
    fun getActiveFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE character_id = :characterId ORDER BY updated_at DESC")
    fun getByCharacterFlow(characterId: Long): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getById(id: Long): ChatEntity?

    @Query("SELECT * FROM chats WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE parent_branch_id = :branchId")
    suspend fun getBranches(branchId: Long): List<ChatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChatEntity): Long

    @Update
    suspend fun update(entity: ChatEntity)

    @Delete
    suspend fun delete(entity: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE chats SET is_active = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun deactivate(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET checkpoint_message_id = :messageId, updated_at = :timestamp WHERE id = :chatId")
    suspend fun setCheckpoint(chatId: Long, messageId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM chats WHERE is_active = 1")
    suspend fun activeCount(): Int
}
