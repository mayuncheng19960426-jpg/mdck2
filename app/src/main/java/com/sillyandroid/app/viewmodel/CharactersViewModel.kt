package com.sillyandroid.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sillyandroid.app.SillyApp
import com.sillyandroid.app.data.entity.CharacterEntity
import com.sillyandroid.app.domain.AppContainer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CharactersViewModel(
    private val container: AppContainer = SillyApp.instance.container
) : ViewModel() {

    val characters: StateFlow<List<CharacterEntity>> = container.characterRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editingCharacter = MutableStateFlow<CharacterEntity?>(null)
    val editingCharacter: StateFlow<CharacterEntity?> = _editingCharacter.asStateFlow()

    fun loadCharacter(id: Long) {
        viewModelScope.launch {
            _editingCharacter.value = container.characterRepository.getById(id)
        }
    }

    fun createNew() {
        _editingCharacter.value = CharacterEntity()
    }

    fun saveCharacter(character: CharacterEntity) {
        viewModelScope.launch {
            if (character.id == 0L) {
                container.characterRepository.insert(character)
            } else {
                container.characterRepository.update(character)
            }
            _editingCharacter.value = null
        }
    }

    fun deleteCharacter(id: Long) {
        viewModelScope.launch {
            container.characterRepository.deleteById(id)
        }
    }

    fun updateEditingCharacter(character: CharacterEntity) {
        _editingCharacter.value = character
    }

    // --- World Book Bindings ---
    private val _boundEntryIds = MutableStateFlow<List<Long>>(emptyList())
    val boundEntryIds: StateFlow<List<Long>> = _boundEntryIds.asStateFlow()

    fun loadBindings(characterId: Long) {
        viewModelScope.launch {
            _boundEntryIds.value = container.worldBookRepository.getBoundEntryIds(characterId)
        }
    }

    fun toggleBinding(characterId: Long, entryId: Long) {
        viewModelScope.launch {
            val current = _boundEntryIds.value.toMutableList()
            if (current.contains(entryId)) {
                container.worldBookRepository.unbindCharacterFromEntry(characterId, entryId)
                current.remove(entryId)
            } else {
                container.worldBookRepository.bindCharacterToEntry(characterId, entryId)
                current.add(entryId)
            }
            _boundEntryIds.value = current
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CharactersViewModel() as T
        }
    }
}
