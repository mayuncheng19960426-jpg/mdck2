package com.sillyandroid.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sillyandroid.app.SillyApp
import com.sillyandroid.app.data.entity.*
import com.sillyandroid.app.domain.AppContainer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val container: AppContainer = SillyApp.instance.container
) : ViewModel() {

    val presets: StateFlow<List<GenerationPresetEntity>> = container.settingsRepository.getAllPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val apiConfigs: StateFlow<List<ApiConfigEntity>> = container.settingsRepository.getAllApiConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val worldBooks: StateFlow<List<WorldBookEntity>> = container.worldBookRepository.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Presets ---
    fun savePreset(preset: GenerationPresetEntity) {
        viewModelScope.launch {
            if (preset.id == 0L) {
                container.settingsRepository.insertPreset(preset)
            } else {
                container.settingsRepository.updatePreset(preset)
            }
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch {
            container.settingsRepository.deletePreset(container.settingsRepository.getPreset(id)!!)
        }
    }

    fun setDefaultPreset(id: Long) {
        viewModelScope.launch {
            container.settingsRepository.setDefaultPreset(id)
        }
    }

    // --- API Configs ---
    fun saveApiConfig(config: ApiConfigEntity) {
        viewModelScope.launch {
            if (config.id == 0L) {
                container.settingsRepository.insertApiConfig(config)
            } else {
                container.settingsRepository.updateApiConfig(config)
            }
        }
    }

    fun deleteApiConfig(id: Long) {
        viewModelScope.launch {
            container.settingsRepository.deleteApiConfig(container.settingsRepository.getApiConfig(id)!!)
        }
    }

    fun setDefaultApiConfig(id: Long) {
        viewModelScope.launch {
            container.settingsRepository.setDefaultApiConfig(id)
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel() as T
        }
    }
}
