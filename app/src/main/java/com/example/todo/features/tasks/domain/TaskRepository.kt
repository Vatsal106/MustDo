package com.example.todo.features.tasks.domain

import com.example.todo.core.database.entity.SubTaskEntity
import com.example.todo.core.database.entity.TaskEntity
import com.example.todo.core.database.entity.TaskResourceEntity
import com.example.todo.core.database.entity.TaskNoteBlockEntity
import com.example.todo.core.database.entity.TaskActivityEntity
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeAllActiveTasks(): Flow<List<TaskEntity>>
    fun observeActivePendingTasks(): Flow<List<TaskEntity>>
    fun observeAllTasks(): Flow<List<TaskEntity>>
    fun observeTasksByStatus(status: String): Flow<List<TaskEntity>>
    fun observeTasksByCategory(categoryId: String): Flow<List<TaskEntity>>
    fun observeTasksForDate(dateMillis: Long): Flow<List<TaskEntity>>
    fun observeOverdueTasks(): Flow<List<TaskEntity>>
    fun observeCompletedTasksForDate(dateMillis: Long): Flow<List<TaskEntity>>
    fun searchTasks(query: String): Flow<List<TaskEntity>>
    fun countCompletedTasksForDate(dateMillis: Long): Flow<Int>
    fun countTasksForDate(dateMillis: Long): Flow<Int>
    fun observeTaskById(taskId: String): Flow<TaskEntity?>
    fun observeSubTasks(taskId: String): Flow<List<SubTaskEntity>>

    suspend fun getTaskById(taskId: String): TaskEntity?
    suspend fun insertTask(task: TaskEntity)
    suspend fun updateTask(task: TaskEntity)
    suspend fun updateTasks(tasks: List<TaskEntity>)
    suspend fun deleteTask(taskId: String)
    suspend fun completeTask(taskId: String): TaskEntity?
    suspend fun skipTask(taskId: String): TaskEntity?
    suspend fun archiveTask(taskId: String)

    suspend fun insertSubTask(subTask: SubTaskEntity)
    suspend fun updateSubTask(subTask: SubTaskEntity)
    suspend fun deleteSubTask(subTask: SubTaskEntity)
    suspend fun getSubTasksByTaskId(taskId: String): List<SubTaskEntity>

    suspend fun getTotalCompletedCount(): Int
    suspend fun getAllTasks(): List<TaskEntity>
    suspend fun getAllSubTasks(): List<SubTaskEntity>
    suspend fun deleteAllTasks()
    suspend fun insertAllTasks(tasks: List<TaskEntity>)
    suspend fun insertAllSubTasks(subTasks: List<SubTaskEntity>)

    fun observeUnscheduledTasks(): Flow<List<TaskEntity>>
    fun observeAllTaskResources(): Flow<List<TaskResourceEntity>>
    suspend fun bulkUpdateTasks(taskIds: List<String>, dueDateMillis: Long?, priority: String?, categoryId: String?)
    suspend fun bulkCompleteTasks(taskIds: List<String>)
    suspend fun bulkArchiveTasks(taskIds: List<String>)

    fun observeTaskResources(taskId: String): Flow<List<TaskResourceEntity>>
    suspend fun getTaskResources(taskId: String): List<TaskResourceEntity>
    suspend fun insertTaskResource(resource: TaskResourceEntity)
    suspend fun deleteTaskResource(resourceId: String)
    suspend fun deleteTaskResourceByTaskAndType(taskId: String, resourceType: String)
    suspend fun deleteTaskResourcesByTaskId(taskId: String)

    fun observeTaskNoteBlocks(taskId: String): Flow<List<TaskNoteBlockEntity>>
    fun observeAllTaskNoteBlocks(): Flow<List<TaskNoteBlockEntity>>
    suspend fun getTaskNoteBlocks(taskId: String): List<TaskNoteBlockEntity>
    suspend fun insertTaskNoteBlock(block: TaskNoteBlockEntity)
    suspend fun updateTaskNoteBlock(block: TaskNoteBlockEntity)
    suspend fun deleteTaskNoteBlock(blockId: String)
    suspend fun updateTaskNoteBlocks(blocks: List<TaskNoteBlockEntity>)
    suspend fun deleteTaskNoteBlocksByTaskId(taskId: String)

    fun observeTaskActivities(taskId: String): Flow<List<TaskActivityEntity>>
    suspend fun getTaskActivities(taskId: String): List<TaskActivityEntity>
    suspend fun insertTaskActivity(activity: TaskActivityEntity)

    suspend fun getAllTaskResources(): List<TaskResourceEntity>
    suspend fun getAllTaskNoteBlocks(): List<TaskNoteBlockEntity>
    suspend fun getAllTaskActivities(): List<TaskActivityEntity>
    suspend fun insertAllTaskResources(resources: List<TaskResourceEntity>)
    suspend fun insertAllTaskNoteBlocks(blocks: List<TaskNoteBlockEntity>)
    suspend fun insertAllTaskActivities(activities: List<TaskActivityEntity>)
    suspend fun deleteAllTaskResources()
    suspend fun deleteAllTaskNoteBlocks()
    suspend fun deleteAllTaskActivities()
}
