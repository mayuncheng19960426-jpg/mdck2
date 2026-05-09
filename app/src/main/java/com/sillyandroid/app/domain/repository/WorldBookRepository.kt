package com.sillyandroid.app.domain.repository

import com.sillyandroid.app.data.AppDatabase
import com.sillyandroid.app.data.entity.CharacterWorldBookBinding
import com.sillyandroid.app.data.entity.WorldBookEntity
import com.sillyandroid.app.data.entity.WorldBookEntryEntity
import kotlinx.coroutines.flow.Flow

class WorldBookRepository(private val db: AppDatabase) {

    private val dao = db.worldBookDao()

    // --- Books ---
    fun getAllBooks(): Flow<List<WorldBookEntity>> = dao.getAllBooks()

    suspend fun getBook(id: Long): WorldBookEntity? = dao.getBookById(id)

    suspend fun insertBook(book: WorldBookEntity): Long = dao.insertBook(book)

    suspend fun updateBook(book: WorldBookEntity) {
        dao.updateBook(book.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteBook(book: WorldBookEntity) = dao.deleteBook(book)

    // --- Entries ---
    fun getEntries(bookId: Long): Flow<List<WorldBookEntryEntity>> = dao.getEntriesByBook(bookId)

    suspend fun getEnabledEntries(bookId: Long): List<WorldBookEntryEntity> =
        dao.getEnabledEntries(bookId)

    suspend fun getEntry(id: Long): WorldBookEntryEntity? = dao.getEntryById(id)

    suspend fun insertEntry(entry: WorldBookEntryEntity): Long = dao.insertEntry(entry)

    suspend fun updateEntry(entry: WorldBookEntryEntity) = dao.updateEntry(entry)

    suspend fun deleteEntry(entry: WorldBookEntryEntity) = dao.deleteEntry(entry)

    suspend fun deleteEntryById(id: Long) = dao.deleteEntryById(id)

    // --- Character Bindings (new feature) ---
    /**
     * 获取角色绑定的世界书条目。
     * 如果角色没有绑定任何条目，返回空列表——调用方应退回到全量匹配。
     */
    suspend fun getBoundEntries(characterId: Long): List<WorldBookEntryEntity> =
        dao.getBoundEntriesForCharacter(characterId)

    suspend fun hasBindings(characterId: Long): Boolean =
        dao.getBindingCount(characterId) > 0

    suspend fun getBoundEntryIds(characterId: Long): List<Long> =
        dao.getBoundEntryIds(characterId)

    suspend fun bindCharacterToEntry(characterId: Long, entryId: Long) {
        dao.insertBinding(CharacterWorldBookBinding(characterId, entryId))
    }

    suspend fun unbindCharacterFromEntry(characterId: Long, entryId: Long) {
        dao.deleteBinding(CharacterWorldBookBinding(characterId, entryId))
    }

    suspend fun clearAllBindings(characterId: Long) {
        dao.deleteAllBindingsForCharacter(characterId)
    }

    suspend fun setCharacterBindings(characterId: Long, entryIds: List<Long>) {
        dao.deleteAllBindingsForCharacter(characterId)
        for (entryId in entryIds) {
            dao.insertBinding(CharacterWorldBookBinding(characterId, entryId))
        }
    }
}
