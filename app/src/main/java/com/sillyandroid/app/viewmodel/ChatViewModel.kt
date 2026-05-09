package com.sillyandroid.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sillyandroid.app.SillyApp
import com.sillyandroid.app.data.entity.*
import com.sillyandroid.app.domain.AppContainer
import com.sillyandroid.app.engine.ChatPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val container: AppContainer = SillyApp.instance.container
) : ViewModel() {

    private val chatPipeline = ChatPipeline(container)

    // --- State ---
    val activeChats: StateFlow<List<ChatEntity>> = container.chatRepository.getActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentChatId = MutableStateFlow<Long?>(null)
    val currentChatId: StateFlow<Long?> = _currentChatId.asStateFlow()

    private val _currentCharacter = MutableStateFlow<CharacterEntity?>(null)
    val currentCharacter: StateFlow<CharacterEntity?> = _currentCharacter.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadChat(chatId: Long) {
        _currentChatId.value = chatId
        viewModelScope.launch {
            val chat = container.chatRepository.getById(chatId)
            if (chat != null) {
                _currentCharacter.value = container.characterRepository.getById(chat.characterId)
                container.chatRepository.getMessages(chatId).collect { msgs ->
                    _messages.value = msgs
                }
            }
        }
    }

    fun createChat(characterId: Long, name: String = "") {
        viewModelScope.launch {
            val chatId = container.chatRepository.createChat(characterId, name)
            val character = container.characterRepository.getById(characterId)
            if (character?.firstMessage?.isNotBlank() == true) {
                container.chatRepository.sendMessage(chatId, "assistant", character.firstMessage)
            }
            _currentChatId.value = chatId
            loadChat(chatId)
        }
    }

    fun sendMessage(text: String) {
        val chatId = _currentChatId.value ?: return
        val character = _currentCharacter.value ?: return

        viewModelScope.launch {
            _isStreaming.value = true
            _streamingContent.value = ""
            _error.value = null

            try {
                val stream = chatPipeline.sendMessageStream(
                    chatId = chatId,
                    character = character,
                    userMessage = text,
                    worldBookId = 1L // Default world book
                )

                val responseBuilder = StringBuilder()
                stream.collect { delta ->
                    responseBuilder.append(delta.content)
                    _streamingContent.value = responseBuilder.toString()
                }

                // Post-process and store
                val processed = chatPipeline.postProcessResponse(responseBuilder.toString())

                // Clean up thinking tags if present
                val cleaned = removeThinkingTags(processed)

                container.chatRepository.sendMessage(chatId, "assistant", cleaned)

                // Generate vector memories asynchronously
                launch(Dispatchers.IO) {
                    try {
                        chatPipeline.generateVectorMemories(chatId, container.chatRepository.getMessagesList(chatId))
                    } catch (_: Exception) {}
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isStreaming.value = false
                _streamingContent.value = ""
            }
        }
    }

    fun continueChat() {
        val chatId = _currentChatId.value ?: return
        val character = _currentCharacter.value ?: return
        viewModelScope.launch {
            _isStreaming.value = true
            _streamingContent.value = ""
            _error.value = null

            try {
                val stream = chatPipeline.sendMessageStream(
                    chatId = chatId,
                    character = character,
                    userMessage = "[继续]",
                    worldBookId = 1L
                )
                val sb = StringBuilder()
                stream.collect { delta -> sb.append(delta.content) }
                val cleaned = removeThinkingTags(sb.toString())
                container.chatRepository.sendMessage(chatId, "assistant", cleaned)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isStreaming.value = false
            }
        }
    }

    fun createCheckpoint() {
        val chatId = _currentChatId.value ?: return
        val lastMsg = _messages.value.lastOrNull() ?: return
        viewModelScope.launch {
            container.chatRepository.setCheckpoint(chatId, lastMsg.id)
            container.chatRepository.setMessageCheckpoint(lastMsg.id)
        }
    }

    fun createBranch(name: String = "") {
        val chatId = _currentChatId.value ?: return
        val forkMsg = _messages.value.lastOrNull() ?: return
        viewModelScope.launch {
            val branchId = container.chatRepository.createBranch(chatId, forkMsg.id, name)
            _currentChatId.value = branchId
            loadChat(branchId)
        }
    }

    fun closeChat() {
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch {
            container.chatRepository.deactivateChat(chatId)
            _currentChatId.value = null
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            container.chatRepository.deleteMessage(messageId)
        }
    }

    fun retry() {
        // Remove last assistant message and regenerate
        val lastAssistant = _messages.value.lastOrNull { it.role == "assistant" } ?: return
        viewModelScope.launch {
            container.chatRepository.deleteMessage(lastAssistant.id)
            continueChat()
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun getPromptForViewer(): String = chatPipeline.formatPromptForViewer()

    suspend fun getRecentLogs(limit: Int = 50): List<com.sillyandroid.app.data.entity.ApiLogEntity> {
        return container.database.apiLogDao().getRecent(limit)
    }

    companion object {
        fun removeThinkingTags(text: String): String {
            // Remove <｜end▁of▁thinking｜>... content
            return text
                .replace(Regex("(?s)"), "")
                .replace(Regex("(?s)"), "")
                .replace(Regex(">\\s*$"), "")
                .trim()
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel() as T
        }
    }
}
