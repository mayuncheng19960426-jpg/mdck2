package com.sillyandroid.app.data.dao

import androidx.room.*
import com.sillyandroid.app.data.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {

    @Query("SELECT * FROM characters ORDER BY updated_at DESC")
    fun getAllFlow(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters ORDER BY updated_at DESC")
    suspend fun getAll(): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getById(id: Long): CharacterEntity?

    @Query("SELECT * FROM characters WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<CharacterEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CharacterEntity): Long

    @Update
    suspend fun update(entity: CharacterEntity)

    @Delete
    suspend fun delete(entity: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM characters")
    suspend fun count(): Int
}
