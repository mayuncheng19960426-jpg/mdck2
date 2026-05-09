package com.sillyandroid.app.data.dao

import androidx.room.*
import com.sillyandroid.app.data.entity.AgentTaskEntity
import com.sillyandroid.app.data.entity.AgentStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {

    // --- Tasks ---
    @Query("SELECT * FROM agent_tasks ORDER BY updated_at DESC")
    fun getAllTasksFlow(): Flow<List<AgentTaskEntity>>

    @Query("SELECT * FROM agent_tasks ORDER BY updated_at DESC")
    suspend fun getAllTasks(): List<AgentTaskEntity>

    @Query("SELECT * FROM agent_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): AgentTaskEntity?

    @Query("SELECT * FROM agent_tasks WHERE id = :id")
    fun getTaskByIdFlow(id: Long): Flow<AgentTaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(entity: AgentTaskEntity): Long

    @Update
    suspend fun updateTask(entity: AgentTaskEntity)

    @Delete
    suspend fun deleteTask(entity: AgentTaskEntity)

    @Query("DELETE FROM agent_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    // --- Steps ---
    @Query("SELECT * FROM agent_steps WHERE task_id = :taskId ORDER BY sort_order ASC")
    fun getStepsByTaskFlow(taskId: Long): Flow<List<AgentStepEntity>>

    @Query("SELECT * FROM agent_steps WHERE task_id = :taskId ORDER BY sort_order ASC")
    suspend fun getStepsByTask(taskId: Long): List<AgentStepEntity>

    @Query("SELECT * FROM agent_steps WHERE id = :id")
    suspend fun getStepById(id: Long): AgentStepEntity?

    @Query("SELECT * FROM agent_steps WHERE task_id = :taskId AND sort_order = :order")
    suspend fun getStepByOrder(taskId: Long, order: Int): AgentStepEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(entity: AgentStepEntity): Long

    @Update
    suspend fun updateStep(entity: AgentStepEntity)

    @Delete
    suspend fun deleteStep(entity: AgentStepEntity)

    @Query("DELETE FROM agent_steps WHERE task_id = :taskId")
    suspend fun deleteStepsByTask(taskId: Long)

    @Query("DELETE FROM agent_steps WHERE id = :id")
    suspend fun deleteStepById(id: Long)

    @Query("UPDATE agent_steps SET status = :status, result = :result, completed_at = :timestamp WHERE id = :id")
    suspend fun completeStep(id: Long, status: String, result: String, timestamp: Long = System.currentTimeMillis())

    // --- Cross-entity queries ---
    @Transaction
    @Query("SELECT * FROM agent_tasks WHERE id = :taskId")
    suspend fun getTaskWithSteps(taskId: Long): TaskWithSteps?
}

data class TaskWithSteps(
    @Embedded val task: AgentTaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "task_id",
        entity = AgentStepEntity::class
    )
    val steps: List<AgentStepEntity>
)
