package com.sillyandroid.app.data.dao

import androidx.room.*
import com.sillyandroid.app.data.entity.CharacterWorldBookBinding
import com.sillyandroid.app.data.entity.WorldBookEntity
import com.sillyandroid.app.data.entity.WorldBookEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldBookDao {

    // --- World Book ---
    @Query("SELECT * FROM world_books ORDER BY updated_at DESC")
    fun getAllBooks(): Flow<List<WorldBookEntity>>

    @Query("SELECT COUNT(*) FROM world_books")
    suspend fun countBooks(): Int

    @Query("SELECT * FROM world_books WHERE id = :id")
    suspend fun getBookById(id: Long): WorldBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(entity: WorldBookEntity): Long

    @Update
    suspend fun updateBook(entity: WorldBookEntity)

    @Delete
    suspend fun deleteBook(entity: WorldBookEntity)

    // --- Entries ---
    @Query("SELECT * FROM world_book_entries WHERE world_book_id = :bookId ORDER BY sort_order ASC")
    fun getEntriesByBook(bookId: Long): Flow<List<WorldBookEntryEntity>>

    @Query("SELECT * FROM world_book_entries WHERE world_book_id = :bookId AND enabled = 1 ORDER BY priority DESC")
    suspend fun getEnabledEntries(bookId: Long): List<WorldBookEntryEntity>

    @Query("SELECT * FROM world_book_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): WorldBookEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entity: WorldBookEntryEntity): Long

    @Update
    suspend fun updateEntry(entity: WorldBookEntryEntity)

    @Delete
    suspend fun deleteEntry(entity: WorldBookEntryEntity)

    @Query("DELETE FROM world_book_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    // --- Character Bindings (new feature) ---
    @Query("""
        SELECT e.* FROM world_book_entries e 
        INNER JOIN character_world_book_bindings b ON e.id = b.entry_id 
        WHERE b.character_id = :characterId AND e.enabled = 1
        ORDER BY e.priority DESC
    """)
    suspend fun getBoundEntriesForCharacter(characterId: Long): List<WorldBookEntryEntity>

    @Query("SELECT COUNT(*) FROM character_world_book_bindings WHERE character_id = :characterId")
    suspend fun getBindingCount(characterId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBinding(binding: CharacterWorldBookBinding)

    @Delete
    suspend fun deleteBinding(binding: CharacterWorldBookBinding)

    @Query("DELETE FROM character_world_book_bindings WHERE character_id = :characterId")
    suspend fun deleteAllBindingsForCharacter(characterId: Long)

    @Query("DELETE FROM character_world_book_bindings WHERE entry_id = :entryId")
    suspend fun deleteAllBindingsForEntry(entryId: Long)

    @Query("SELECT entry_id FROM character_world_book_bindings WHERE character_id = :characterId")
    suspend fun getBoundEntryIds(characterId: Long): List<Long>
}
