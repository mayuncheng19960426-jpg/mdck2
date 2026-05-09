package com.sillyandroid.app.agent

import com.sillyandroid.app.data.entity.*
import com.sillyandroid.app.domain.AppContainer
import com.sillyandroid.app.engine.ChatPipeline
import com.sillyandroid.app.network.ChatCompletionRequest
import com.sillyandroid.app.network.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Agent 编排器。
 *
 * 管理多角色分步任务：按顺序执行步骤，每个步骤指派一个角色，
 * 上一步的输出作为下一步的上下文输入。
 */
class AgentOrchestrator(private val container: AppContainer) {

    private val chatPipeline = ChatPipeline(container)

    /**
     * 执行 Agent 任务。
     *
     * @param taskId 任务 ID
     * @param presetId 可选的生成预设覆盖
     * @param apiConfigId 可选的 API 配置覆盖
     * @return Flow<StepProgress> 逐步执行的进度流
     */
    fun executeTask(
        taskId: Long,
        presetId: Long? = null,
        apiConfigId: Long? = null
    ): Flow<StepProgress> = flow {
        val task = container.agentRepository.getTask(taskId) ?: return@flow
        val steps = container.agentRepository.getStepsList(taskId)

        if (steps.isEmpty()) {
            emit(StepProgress.Error("No steps defined for task"))
            return@flow
        }

        // Update task status
        container.agentRepository.updateTask(
            task.copy(status = "running", updatedAt = System.currentTimeMillis())
        )

        var previousResult: String? = task.globalContext

        for (step in steps) {
            emit(StepProgress.StepStarted(step))

            // Update step status
            container.agentRepository.updateStep(
                step.copy(
                    status = "running",
                    startedAt = System.currentTimeMillis(),
                    inputContext = previousResult
                )
            )

            try {
                val character = container.characterRepository.getById(step.characterId)
                if (character == null) {
                    emit(StepProgress.Error("Character not found for step ${step.order}"))
                    container.agentRepository.completeStep(step.id, "", false)
                    continue
                }

                // Build prompt for this step
                val prompt = buildStepPrompt(step, previousResult)

                // Assemble context for API call
                val preset = if (step.presetId != null) {
                    container.settingsRepository.getPreset(step.presetId)
                } else if (presetId != null) {
                    container.settingsRepository.getPreset(presetId)
                } else {
                    container.settingsRepository.getDefaultPreset()
                } ?: GenerationPresetEntity()

                val apiConfig = if (apiConfigId != null) {
                    container.settingsRepository.getApiConfig(apiConfigId)
                } else {
                    container.settingsRepository.getDefaultApiConfig()
                }

                val messages = listOf(
                    ChatMessage(role = "system", content = buildAgentSystemPrompt(character)),
                    ChatMessage(role = "user", content = prompt)
                )

                val request = ChatCompletionRequest(
                    model = apiConfig?.modelName ?: "gpt-4",
                    messages = messages,
                    temperature = preset.temperature,
                    topP = preset.topP,
                    maxTokens = preset.maxTokens,
                    stream = false // Agent steps use non-streaming for reliability
                )

                // Execute the API call
                val result = executeStep(request, apiConfigId)

                // Store result
                container.agentRepository.completeStep(step.id, result, true)
                previousResult = result

                emit(StepProgress.StepCompleted(step, result))

            } catch (e: Exception) {
                val errorMsg = "Step ${step.order} failed: ${e.message}"
                container.agentRepository.completeStep(step.id, errorMsg, false)
                emit(StepProgress.Error(errorMsg))

                // Update task as failed
                container.agentRepository.updateTask(
                    task.copy(status = "failed", updatedAt = System.currentTimeMillis())
                )
                return@flow
            }
        }

        // Task completed successfully
        container.agentRepository.updateTask(
            task.copy(status = "completed", updatedAt = System.currentTimeMillis())
        )
        emit(StepProgress.TaskCompleted(previousResult))
    }

    private suspend fun executeStep(
        request: ChatCompletionRequest,
        apiConfigId: Long?
    ): String {
        val config = if (apiConfigId != null) {
            container.settingsRepository.getApiConfig(apiConfigId)
        } else {
            container.settingsRepository.getDefaultApiConfig()
        } ?: throw IllegalStateException("No API configuration")

        val gson = com.google.gson.Gson()
        val json = gson.toJson(request)
        val requestBody = json.toRequestBody("application/json".toMediaType())

        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val httpRequest = okhttp3.Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/chat/completions")
            .post(requestBody)
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .build()

        val response = client.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            throw IllegalStateException("API error ${response.code}: ${response.body?.string()}")
        }

        val body = response.body?.string() ?: throw IllegalStateException("Empty response")
        val completion = gson.fromJson(
            body,
            com.sillyandroid.app.network.ChatCompletionResponse::class.java
        )

        return completion.choices?.firstOrNull()?.message?.content
            ?: throw IllegalStateException("No response content")
    }

    private fun buildAgentSystemPrompt(character: CharacterEntity): String {
        return buildString {
            appendLine("你正在扮演以下角色，这是多角色协作任务中的一个步骤。")
            appendLine()
            appendLine("[角色信息]")
            appendLine("名称: ${character.name}")
            if (character.description.isNotBlank()) {
                appendLine("描述: ${character.description}")
            }
            if (character.personality.isNotBlank()) {
                appendLine("性格: ${character.personality}")
            }
            appendLine()
            appendLine("请基于你的角色设定，完成分配给你的任务步骤。只输出任务结果，不要添加额外评论。")
        }
    }

    private fun buildStepPrompt(
        step: com.sillyandroid.app.data.entity.AgentStepEntity,
        previousResult: String?
    ): String {
        return buildString {
            if (previousResult != null) {
                appendLine("上一步的输出结果：")
                appendLine("---")
                appendLine(previousResult)
                appendLine("---")
                appendLine()
            }
            appendLine("当前任务步骤：")
            appendLine(step.instruction)
            appendLine()
            appendLine("请完成上述步骤并输出结果。")
        }
    }

    private fun String.toRequestBody(contentType: okhttp3.MediaType): okhttp3.RequestBody {
        return okhttp3.RequestBody.create(contentType, this)
    }

    private fun String.toMediaType(): okhttp3.MediaType {
        return okhttp3.MediaType.parse(this)!!
    }
}

sealed class StepProgress {
    data class StepStarted(val step: com.sillyandroid.app.data.entity.AgentStepEntity) : StepProgress()
    data class StepCompleted(
        val step: com.sillyandroid.app.data.entity.AgentStepEntity,
        val result: String
    ) : StepProgress()
    data class Error(val message: String) : StepProgress()
    data class TaskCompleted(val finalResult: String?) : StepProgress()
}
