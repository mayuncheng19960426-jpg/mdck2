package com.sillyandroid.app.domain.repository

import com.sillyandroid.app.data.AppDatabase
import com.sillyandroid.app.data.entity.AgentTaskEntity
import com.sillyandroid.app.data.entity.AgentStepEntity
import com.sillyandroid.app.data.dao.TaskWithSteps
import kotlinx.coroutines.flow.Flow

class AgentRepository(private val db: AppDatabase) {

    private val dao = db.agentDao()

    // --- Tasks ---
    fun getAllTasks(): Flow<List<AgentTaskEntity>> = dao.getAllTasksFlow()

    suspend fun getAllTasksList(): List<AgentTaskEntity> = dao.getAllTasks()

    suspend fun getTask(id: Long): AgentTaskEntity? = dao.getTaskById(id)

    fun getTaskFlow(id: Long): Flow<AgentTaskEntity?> = dao.getTaskByIdFlow(id)

    suspend fun getTaskWithSteps(id: Long): TaskWithSteps? = dao.getTaskWithSteps(id)

    suspend fun insertTask(task: AgentTaskEntity): Long = dao.insertTask(task)

    suspend fun updateTask(task: AgentTaskEntity) = dao.updateTask(task)

    suspend fun deleteTask(id: Long) = dao.deleteTaskById(id)

    // --- Steps ---
    fun getSteps(taskId: Long): Flow<List<AgentStepEntity>> = dao.getStepsByTaskFlow(taskId)

    suspend fun getStepsList(taskId: Long): List<AgentStepEntity> = dao.getStepsByTask(taskId)

    suspend fun insertStep(step: AgentStepEntity): Long = dao.insertStep(step)

    suspend fun updateStep(step: AgentStepEntity) = dao.updateStep(step)

    suspend fun deleteStep(id: Long) = dao.deleteStepById(id)

    suspend fun deleteAllSteps(taskId: Long) = dao.deleteStepsByTask(taskId)

    suspend fun completeStep(id: Long, result: String, success: Boolean = true) {
        val status = if (success) "completed" else "failed"
        dao.completeStep(id, status, result)
    }

    /**
     * 创建任务及其所有步骤。
     */
    suspend fun createTaskWithSteps(
        name: String,
        description: String,
        steps: List<AgentStepEntity>
    ): Long {
        val taskId = dao.insertTask(
            AgentTaskEntity(name = name, description = description)
        )
        for ((index, step) in steps.withIndex()) {
            dao.insertStep(step.copy(taskId = taskId, order = index))
        }
        return taskId
    }
}
