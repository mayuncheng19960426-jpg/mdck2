package com.sillyandroid.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sillyandroid.app.SillyApp
import com.sillyandroid.app.agent.AgentOrchestrator
import com.sillyandroid.app.agent.StepProgress
import com.sillyandroid.app.data.entity.*
import com.sillyandroid.app.domain.AppContainer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AgentViewModel(
    private val container: AppContainer = SillyApp.instance.container
) : ViewModel() {

    private val orchestrator = AgentOrchestrator(container)

    val tasks: StateFlow<List<AgentTaskEntity>> = container.agentRepository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTask = MutableStateFlow<AgentTaskEntity?>(null)
    val currentTask: StateFlow<AgentTaskEntity?> = _currentTask.asStateFlow()

    private val _steps = MutableStateFlow<List<AgentStepEntity>>(emptyList())
    val steps: StateFlow<List<AgentStepEntity>> = _steps.asStateFlow()

    private val _executionLog = MutableStateFlow<List<String>>(emptyList())
    val executionLog: StateFlow<List<String>> = _executionLog.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            val task = container.agentRepository.getTask(taskId)
            _currentTask.value = task
            if (task != null) {
                container.agentRepository.getSteps(taskId).collect { stepList ->
                    _steps.value = stepList
                }
            }
        }
    }

    fun createTask(name: String, description: String, steps: List<AgentStepEntity>) {
        viewModelScope.launch {
            val taskId = container.agentRepository.createTaskWithSteps(name, description, steps)
            loadTask(taskId)
        }
    }

    fun addStep(characterId: Long, instruction: String) {
        val taskId = _currentTask.value?.id ?: return
        viewModelScope.launch {
            val order = _steps.value.size
            container.agentRepository.insertStep(
                AgentStepEntity(
                    taskId = taskId,
                    characterId = characterId,
                    instruction = instruction,
                    order = order
                )
            )
            _steps.value = container.agentRepository.getStepsList(taskId)
        }
    }

    fun removeStep(stepId: Long) {
        val taskId = _currentTask.value?.id ?: return
        viewModelScope.launch {
            container.agentRepository.deleteStep(stepId)
            _steps.value = container.agentRepository.getStepsList(taskId)
        }
    }

    fun runTask(presetId: Long? = null, apiConfigId: Long? = null) {
        val taskId = _currentTask.value?.id ?: return
        viewModelScope.launch {
            _isRunning.value = true
            _executionLog.value = emptyList()

            orchestrator.executeTask(taskId, presetId, apiConfigId).collect { progress ->
                when (progress) {
                    is StepProgress.StepStarted -> {
                        _executionLog.value = _executionLog.value +
                                "▶ 步骤 ${progress.step.order + 1}: ${progress.step.instruction.take(50)}..."
                    }
                    is StepProgress.StepCompleted -> {
                        _executionLog.value = _executionLog.value +
                                "✓ 步骤 ${progress.step.order + 1} 完成\n${progress.result.take(200)}..."
                        // Refresh steps
                        _steps.value = container.agentRepository.getStepsList(taskId)
                    }
                    is StepProgress.Error -> {
                        _executionLog.value = _executionLog.value +
                                "✗ 错误: ${progress.message}"
                    }
                    is StepProgress.TaskCompleted -> {
                        _executionLog.value = _executionLog.value +
                                "✓ 任务完成"
                        _currentTask.value = container.agentRepository.getTask(taskId)
                    }
                }
            }

            _isRunning.value = false
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            container.agentRepository.deleteTask(taskId)
            _currentTask.value = null
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AgentViewModel() as T
        }
    }
}
