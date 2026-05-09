package com.sillyandroid.app.data.dao

import androidx.room.*
import com.sillyandroid.app.data.entity.VectorMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VectorMemoryDao {

    @Query("SELECT * FROM vector_memories WHERE chat_id = :chatId ORDER BY created_at DESC")
    fun getByChatFlow(chatId: Long): Flow<List<VectorMemoryEntity>>

    @Query("SELECT * FROM vector_memories WHERE chat_id = :chatId")
    suspend fun getByChat(chatId: Long): List<VectorMemoryEntity>

    @Query("SELECT * FROM vector_memories WHERE id = :id")
    suspend fun getById(id: Long): VectorMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VectorMemoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<VectorMemoryEntity>)

    @Update
    suspend fun update(entity: VectorMemoryEntity)

    @Delete
    suspend fun delete(entity: VectorMemoryEntity)

    @Query("DELETE FROM vector_memories WHERE chat_id = :chatId")
    suspend fun deleteByChat(chatId: Long)

    @Query("DELETE FROM vector_memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE vector_memories SET access_count = access_count + 1, last_accessed_at = :timestamp WHERE id = :id")
    suspend fun recordAccess(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM vector_memories WHERE chat_id = :chatId")
    suspend fun countByChat(chatId: Long): Int
}
