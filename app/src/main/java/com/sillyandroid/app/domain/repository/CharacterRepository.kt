package com.sillyandroid.app.domain.repository

import com.sillyandroid.app.data.AppDatabase
import com.sillyandroid.app.data.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

class CharacterRepository(private val db: AppDatabase) {

    private val dao = db.characterDao()

    fun getAll(): Flow<List<CharacterEntity>> = dao.getAllFlow()

    suspend fun getAllList(): List<CharacterEntity> = dao.getAll()

    suspend fun getById(id: Long): CharacterEntity? = dao.getById(id)

    fun getByIdFlow(id: Long): Flow<CharacterEntity?> = dao.getByIdFlow(id)

    suspend fun insert(character: CharacterEntity): Long = dao.insert(character)

    suspend fun update(character: CharacterEntity) {
        dao.update(character.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(character: CharacterEntity) = dao.delete(character)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun count(): Int = dao.count()
}
