package com.sillyandroid.app.domain.repository

import com.sillyandroid.app.data.AppDatabase
import com.sillyandroid.app.data.entity.ChatEntity
import com.sillyandroid.app.data.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val db: AppDatabase) {

    private val chatDao = db.chatDao()
    private val messageDao = db.messageDao()

    // --- Chats ---
    fun getAll(): Flow<List<ChatEntity>> = chatDao.getAllFlow()

    fun getActive(): Flow<List<ChatEntity>> = chatDao.getActiveFlow()

    fun getByCharacter(characterId: Long): Flow<List<ChatEntity>> = chatDao.getByCharacterFlow(characterId)

    suspend fun getById(id: Long): ChatEntity? = chatDao.getById(id)

    suspend fun getBranches(chatId: Long): List<ChatEntity> = chatDao.getBranches(chatId)

    suspend fun createChat(characterId: Long, name: String = ""): Long {
        return chatDao.insert(
            ChatEntity(characterId = characterId, name = name)
        )
    }

    /**
     * 从指定消息处创建分支聊天。
     * 复制 fork 点之前的所有消息到新聊天。
     */
    suspend fun createBranch(sourceChatId: Long, forkMessageId: Long, name: String = ""): Long {
        val sourceChat = chatDao.getById(sourceChatId) ?: return -1
        val forkMessage = messageDao.getById(forkMessageId) ?: return -1

        val branchId = chatDao.insert(
            ChatEntity(
                characterId = sourceChat.characterId,
                name = name.ifBlank { "${sourceChat.name} (分支)" },
                parentBranchId = sourceChatId,
                forkMessageId = forkMessageId
            )
        )

        // Copy messages before fork point
        val messages = messageDao.getByChatBefore(sourceChatId, forkMessage.createdAt)
        for (msg in messages) {
            messageDao.insert(msg.copy(id = 0, chatId = branchId))
        }

        return branchId
    }

    suspend fun deactivateChat(chatId: Long) = chatDao.deactivate(chatId)

    suspend fun setCheckpoint(chatId: Long, messageId: Long) = chatDao.setCheckpoint(chatId, messageId)

    suspend fun deleteChat(chatId: Long) = chatDao.deleteById(chatId)

    // --- Messages ---
    fun getMessages(chatId: Long): Flow<List<MessageEntity>> = messageDao.getByChatFlow(chatId)

    suspend fun getMessagesList(chatId: Long): List<MessageEntity> = messageDao.getByChat(chatId)

    suspend fun sendMessage(
        chatId: Long,
        role: String,
        content: String,
        attachedFileUri: String? = null,
        attachedFileName: String? = null
    ): Long {
        chatDao.update(
            chatDao.getById(chatId)!!.copy(updatedAt = System.currentTimeMillis())
        )
        return messageDao.insert(
            MessageEntity(
                chatId = chatId,
                role = role,
                content = content,
                attachedFileUri = attachedFileUri,
                attachedFileName = attachedFileName
            )
        )
    }

    suspend fun updateMessage(message: MessageEntity) = messageDao.update(message)

    suspend fun deleteMessage(messageId: Long) = messageDao.deleteById(messageId)

    suspend fun deleteMessagesAfter(chatId: Long, timestamp: Long) =
        messageDao.deleteAfter(chatId, timestamp)

    suspend fun updateSwipe(messageId: Long, swipesJson: String, index: Int) =
        messageDao.updateSwipe(messageId, swipesJson, index)

    suspend fun setMessageCheckpoint(messageId: Long, isCheckpoint: Boolean = true) {
        messageDao.setCheckpoint(messageId, isCheckpoint)
    }
}
