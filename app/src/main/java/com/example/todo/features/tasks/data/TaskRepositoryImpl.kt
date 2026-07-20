package com.example.todo.features.tasks.data

import com.example.todo.common.util.DateUtils
import com.example.todo.core.database.dao.SubTaskDao
import com.example.todo.core.database.dao.TaskDao
import com.example.todo.core.database.entity.Recurrence
import com.example.todo.core.database.entity.SubTaskEntity
import com.example.todo.core.database.entity.TaskEntity
import com.example.todo.core.database.entity.TaskStatus
import com.example.todo.features.tasks.domain.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import com.example.todo.core.database.entity.TaskResourceEntity
import com.example.todo.core.database.entity.TaskNoteBlockEntity
import com.example.todo.core.database.entity.TaskActivityEntity
import com.example.todo.core.database.dao.TaskResourceDao
import com.example.todo.core.database.dao.TaskNoteBlockDao
import com.example.todo.core.database.dao.TaskActivityDao
import com.example.todo.core.datastore.UserPreferencesManager

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val subTaskDao: SubTaskDao,
    private val reminderDao: com.example.todo.core.database.dao.ReminderDao,
    private val reminderScheduler: com.example.todo.core.notification.ReminderScheduler,
    private val taskResourceDao: TaskResourceDao,
    private val taskNoteBlockDao: TaskNoteBlockDao,
    private val taskActivityDao: TaskActivityDao,
    private val preferencesManager: UserPreferencesManager
) : TaskRepository {

    override fun observeAllActiveTasks(): Flow<List<TaskEntity>> =
        taskDao.observeAllActiveTasks()

    override fun observeActivePendingTasks(): Flow<List<TaskEntity>> =
        taskDao.observeActivePendingTasks()

    override fun observeAllTasks(): Flow<List<TaskEntity>> =
        taskDao.observeAllTasks()

    override fun observeTasksByStatus(status: String): Flow<List<TaskEntity>> =
        taskDao.observeTasksByStatus(status)

    override fun observeTasksByCategory(categoryId: String): Flow<List<TaskEntity>> =
        taskDao.observeTasksByCategory(categoryId)

    override fun observeTasksForDate(dateMillis: Long): Flow<List<TaskEntity>> =
        taskDao.observeTasksForDate(DateUtils.startOfDay(dateMillis), DateUtils.endOfDay(dateMillis))

    override fun observeOverdueTasks(): Flow<List<TaskEntity>> =
        taskDao.observeOverdueTasks(DateUtils.startOfDay())

    override fun observeCompletedTasksForDate(dateMillis: Long): Flow<List<TaskEntity>> =
        taskDao.observeCompletedTasksForDate(DateUtils.startOfDay(dateMillis), DateUtils.endOfDay(dateMillis))

    override fun searchTasks(query: String): Flow<List<TaskEntity>> =
        taskDao.searchTasks(query)

    override fun countCompletedTasksForDate(dateMillis: Long): Flow<Int> =
        taskDao.countCompletedTasksForDate(DateUtils.startOfDay(dateMillis), DateUtils.endOfDay(dateMillis))

    override fun countTasksForDate(dateMillis: Long): Flow<Int> =
        taskDao.countTasksForDate(DateUtils.startOfDay(dateMillis), DateUtils.endOfDay(dateMillis))

    override fun observeTaskById(taskId: String): Flow<TaskEntity?> =
        taskDao.observeTaskById(taskId)

    override fun observeSubTasks(taskId: String): Flow<List<SubTaskEntity>> =
        subTaskDao.observeSubTasksByTaskId(taskId)

    override suspend fun getTaskById(taskId: String): TaskEntity? =
        taskDao.getTaskById(taskId)

    override suspend fun insertTask(task: TaskEntity) {
        taskDao.insertTask(task)
        taskActivityDao.insert(TaskActivityEntity(taskId = task.id, activityType = "CREATED", details = "Created task"))
    }

    override suspend fun updateTask(task: TaskEntity) {
        val existingTask = taskDao.getTaskById(task.id)
        val finalTask = if (existingTask != null && task.dueDateMillis != null && existingTask.dueDateMillis != null && task.dueDateMillis > existingTask.dueDateMillis) {
            task.copy(rescheduleCount = existingTask.rescheduleCount + 1)
        } else {
            task
        }
        taskDao.updateTask(finalTask)
        taskActivityDao.insert(TaskActivityEntity(taskId = finalTask.id, activityType = "UPDATED", details = "Updated task details"))
    }
    
    override suspend fun updateTasks(tasks: List<TaskEntity>) {
        taskDao.updateTasks(tasks)
    }

    override suspend fun deleteTask(taskId: String) {
        reminderScheduler.cancelRemindersForTask(taskId)
        reminderDao.deleteByTaskId(taskId)
        taskDao.deleteTaskById(taskId)
    }

    override suspend fun completeTask(taskId: String): TaskEntity? {
        val task = taskDao.getTaskById(taskId) ?: return null
        if (task.status == TaskStatus.COMPLETED.name) {
            return task
        }
        val completedTask = task.copy(
            status = TaskStatus.COMPLETED.name,
            completedDateMillis = System.currentTimeMillis()
        )
        taskDao.updateTask(completedTask)
        taskActivityDao.insert(TaskActivityEntity(taskId = taskId, activityType = "COMPLETED", details = "Completed task"))
        
        // Record productive action date
        val todayStart = DateUtils.startOfDay(System.currentTimeMillis())
        preferencesManager.setLastProductiveActionDate(todayStart)
        
        // Cancel reminders for completed task
        reminderScheduler.cancelRemindersForTask(taskId)

        // Handle recurring tasks
        if (task.recurrence != Recurrence.NONE.name && task.dueDateMillis != null) {
            val baseDateForRecurrence = if (task.autoReschedule) {
                System.currentTimeMillis()
            } else {
                task.dueDateMillis
            }
            val newDueDate = DateUtils.advanceDate(baseDateForRecurrence, task.recurrence)
            val newDueTime = task.dueTimeMillis?.let {
                if (task.autoReschedule) {
                    val timeDiff = newDueDate - baseDateForRecurrence
                    it + timeDiff
                } else {
                    DateUtils.advanceDate(it, task.recurrence)
                }
            }
            val newTask = task.copy(
                id = UUID.randomUUID().toString(),
                status = TaskStatus.PENDING.name,
                dueDateMillis = newDueDate,
                dueTimeMillis = newDueTime,
                completedDateMillis = null,
                createdDateMillis = System.currentTimeMillis()
            )
            taskDao.insertTask(newTask)

            // Clone reminders
            val oldReminders = reminderDao.getByTaskId(taskId)
            val newReminders = oldReminders.map { reminder ->
                val newTrigger = if (reminder.reminderType == "BEFORE_DUE_DATE" && reminder.offsetMinutes != null && newDueDate != null) {
                    newDueDate - (reminder.offsetMinutes * 60 * 1000L)
                } else {
                    DateUtils.advanceDate(reminder.triggerTimestamp, task.recurrence)
                }
                reminder.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    taskId = newTask.id,
                    triggerTimestamp = newTrigger,
                    isEnabled = true,
                    lastTriggeredAt = null,
                    createdAt = System.currentTimeMillis()
                )
            }
            reminderDao.insertAll(newReminders)
            newReminders.forEach { reminderScheduler.scheduleReminder(it) }

            // Clone subtasks
            val subTasks = subTaskDao.getSubTasksByTaskId(taskId)
            val clonedSubTasks = subTasks.map {
                it.copy(
                    id = UUID.randomUUID().toString(),
                    taskId = newTask.id,
                    isCompleted = false
                )
            }
            subTaskDao.insertAll(clonedSubTasks)
        }

        return completedTask
    }

    override suspend fun skipTask(taskId: String): TaskEntity? {
        val task = taskDao.getTaskById(taskId) ?: return null
        if (task.status == TaskStatus.COMPLETED.name || task.status == TaskStatus.ARCHIVED.name) {
            return task
        }
        
        // Mark current task as ARCHIVED so it disappears but retains its history,
        // and increment skippedCount
        val skippedTask = task.copy(
            status = TaskStatus.ARCHIVED.name,
            skippedCount = task.skippedCount + 1
        )
        taskDao.updateTask(skippedTask)
        taskActivityDao.insert(TaskActivityEntity(taskId = taskId, activityType = "SKIPPED", details = "Skipped task occurrence"))
        
        // Cancel reminders for skipped task
        reminderScheduler.cancelRemindersForTask(taskId)

        // Handle recurring tasks exactly like completeTask
        if (task.recurrence != Recurrence.NONE.name && task.dueDateMillis != null) {
            val baseDateForRecurrence = if (task.autoReschedule) {
                System.currentTimeMillis()
            } else {
                task.dueDateMillis
            }
            val newDueDate = DateUtils.advanceDate(baseDateForRecurrence, task.recurrence)
            val newDueTime = task.dueTimeMillis?.let {
                if (task.autoReschedule) {
                    val timeDiff = newDueDate - baseDateForRecurrence
                    it + timeDiff
                } else {
                    DateUtils.advanceDate(it, task.recurrence)
                }
            }
            val newTask = task.copy(
                id = UUID.randomUUID().toString(),
                status = TaskStatus.PENDING.name,
                dueDateMillis = newDueDate,
                dueTimeMillis = newDueTime,
                completedDateMillis = null,
                createdDateMillis = System.currentTimeMillis(),
                skippedCount = 0 // Reset for the new instance
            )
            taskDao.insertTask(newTask)

            // Clone reminders
            val oldReminders = reminderDao.getByTaskId(taskId)
            val newReminders = oldReminders.map { reminder ->
                val newTrigger = if (reminder.reminderType == "BEFORE_DUE_DATE" && reminder.offsetMinutes != null && newDueDate != null) {
                    newDueDate - (reminder.offsetMinutes * 60 * 1000L)
                } else {
                    DateUtils.advanceDate(reminder.triggerTimestamp, task.recurrence)
                }
                reminder.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    taskId = newTask.id,
                    triggerTimestamp = newTrigger,
                    isEnabled = true,
                    lastTriggeredAt = null,
                    createdAt = System.currentTimeMillis()
                )
            }
            reminderDao.insertAll(newReminders)
            newReminders.forEach { reminderScheduler.scheduleReminder(it) }

            // Clone subtasks
            val subTasks = subTaskDao.getSubTasksByTaskId(taskId)
            val clonedSubTasks = subTasks.map {
                it.copy(
                    id = UUID.randomUUID().toString(),
                    taskId = newTask.id,
                    isCompleted = false
                )
            }
            subTaskDao.insertAll(clonedSubTasks)
        }

        return skippedTask
    }

    override suspend fun archiveTask(taskId: String) {
        val task = taskDao.getTaskById(taskId) ?: return
        reminderScheduler.cancelRemindersForTask(taskId)
        taskDao.updateTask(task.copy(status = TaskStatus.ARCHIVED.name))
    }

    override suspend fun insertSubTask(subTask: SubTaskEntity) {
        subTaskDao.insertSubTask(subTask)
        taskActivityDao.insert(TaskActivityEntity(
            taskId = subTask.taskId,
            activityType = "SUBTASK_ADDED",
            details = "Todo added: \"${subTask.title}\""
        ))
    }

    override suspend fun updateSubTask(subTask: SubTaskEntity) {
        val existing = subTaskDao.getSubTasksByTaskId(subTask.taskId).find { it.id == subTask.id }
        subTaskDao.updateSubTask(subTask)
        if (existing != null && existing.isCompleted != subTask.isCompleted) {
            val statusStr = if (subTask.isCompleted) "completed" else "uncompleted"
            taskActivityDao.insert(TaskActivityEntity(
                taskId = subTask.taskId,
                activityType = "SUBTASK_TOGGLED",
                details = "Todo $statusStr: \"${subTask.title}\""
            ))
        } else {
            taskActivityDao.insert(TaskActivityEntity(
                taskId = subTask.taskId,
                activityType = "SUBTASK_UPDATED",
                details = "Todo updated: \"${subTask.title}\""
            ))
        }
    }

    override suspend fun deleteSubTask(subTask: SubTaskEntity) {
        subTaskDao.deleteSubTask(subTask)
        taskActivityDao.insert(TaskActivityEntity(
            taskId = subTask.taskId,
            activityType = "SUBTASK_DELETED",
            details = "Todo deleted: \"${subTask.title}\""
        ))
    }

    override suspend fun getSubTasksByTaskId(taskId: String): List<SubTaskEntity> =
        subTaskDao.getSubTasksByTaskId(taskId)

    override suspend fun getTotalCompletedCount(): Int =
        taskDao.getTotalCompletedCount()

    override suspend fun getAllTasks(): List<TaskEntity> =
        taskDao.getAllTasks()

    override suspend fun getAllSubTasks(): List<SubTaskEntity> =
        subTaskDao.getAllSubTasks()

    override suspend fun deleteAllTasks() {
        subTaskDao.deleteAll()
        taskDao.deleteAllTasks()
    }

    override suspend fun insertAllTasks(tasks: List<TaskEntity>) =
        taskDao.insertAll(tasks)

    override suspend fun insertAllSubTasks(subTasks: List<SubTaskEntity>) =
        subTaskDao.insertAll(subTasks)

    override fun observeUnscheduledTasks(): Flow<List<TaskEntity>> =
        taskDao.observeUnscheduledTasks()

    override fun observeAllTaskResources(): Flow<List<TaskResourceEntity>> =
        taskResourceDao.observeAll()

    override suspend fun bulkUpdateTasks(taskIds: List<String>, dueDateMillis: Long?, priority: String?, categoryId: String?) {
        if (priority != null) {
            taskDao.updatePriorityForTasks(taskIds, priority)
        }
        if (dueDateMillis != null) {
            val targetDate = if (dueDateMillis == -1L) null else dueDateMillis
            taskIds.forEach { id ->
                val task = taskDao.getTaskById(id)
                if (task != null) {
                    val isPostponed = targetDate != null && task.dueDateMillis != null && targetDate > task.dueDateMillis
                    val updatedTask = task.copy(
                        dueDateMillis = targetDate,
                        rescheduleCount = if (isPostponed) task.rescheduleCount + 1 else task.rescheduleCount
                    )
                    taskDao.updateTask(updatedTask)
                }
            }
        }
        if (categoryId != null) {
            val cat = if (categoryId == "CLEAR") null else categoryId
            taskDao.updateCategoryForTasks(taskIds, cat)
        }
    }

    override suspend fun bulkCompleteTasks(taskIds: List<String>) {
        taskIds.forEach { completeTask(it) }
    }

    override suspend fun bulkArchiveTasks(taskIds: List<String>) {
        taskIds.forEach { archiveTask(it) }
    }

    override fun observeTaskResources(taskId: String): Flow<List<TaskResourceEntity>> =
        taskResourceDao.observeByTaskId(taskId)

    override suspend fun getTaskResources(taskId: String): List<TaskResourceEntity> =
        taskResourceDao.getByTaskId(taskId)

    override suspend fun insertTaskResource(resource: TaskResourceEntity) {
        taskResourceDao.insert(resource)
        val typeStr = when (resource.resourceType) {
            "CONTACT" -> "Contact"
            "LINK" -> "Link"
            "LOCATION" -> "Location"
            "FILE" -> "File"
            "VOICE_NOTE" -> "Voice note"
            else -> "Resource"
        }
        taskActivityDao.insert(TaskActivityEntity(
            taskId = resource.taskId,
            activityType = "RESOURCE_ADDED",
            details = "$typeStr added"
        ))
    }

    override suspend fun deleteTaskResource(resourceId: String) {
        val resource = taskResourceDao.getAll().find { it.id == resourceId }
        taskResourceDao.deleteById(resourceId)
        if (resource != null) {
            val typeStr = when (resource.resourceType) {
                "CONTACT" -> "Contact"
                "LINK" -> "Link"
                "LOCATION" -> "Location"
                "FILE" -> "File"
                "VOICE_NOTE" -> "Voice note"
                else -> "Resource"
            }
            taskActivityDao.insert(TaskActivityEntity(
                taskId = resource.taskId,
                activityType = "RESOURCE_DELETED",
                details = "$typeStr removed"
            ))
        }
    }

    override suspend fun deleteTaskResourceByTaskAndType(taskId: String, resourceType: String) {
        val matching = taskResourceDao.getByTaskId(taskId).filter { it.resourceType == resourceType }
        matching.forEach { taskResourceDao.delete(it) }
    }

    override suspend fun deleteTaskResourcesByTaskId(taskId: String) {
        taskResourceDao.deleteByTaskId(taskId)
    }

    override fun observeTaskNoteBlocks(taskId: String): Flow<List<TaskNoteBlockEntity>> =
        taskNoteBlockDao.observeByTaskId(taskId)

    override fun observeAllTaskNoteBlocks(): Flow<List<TaskNoteBlockEntity>> =
        taskNoteBlockDao.observeAll()

    override suspend fun getTaskNoteBlocks(taskId: String): List<TaskNoteBlockEntity> =
        taskNoteBlockDao.getByTaskId(taskId)

    override suspend fun insertTaskNoteBlock(block: TaskNoteBlockEntity) {
        taskNoteBlockDao.insert(block)
        val detailsStr = when (block.blockType) {
            "CHECKLIST" -> "Checklist item added: \"${block.content}\""
            "TEXT" -> "Note added"
            "LINK" -> "Link block added"
            "QUOTE" -> "Quote added"
            else -> "Note block added"
        }
        taskActivityDao.insert(TaskActivityEntity(
            taskId = block.taskId,
            activityType = "NOTE_BLOCK_ADDED",
            details = detailsStr
        ))
    }

    override suspend fun updateTaskNoteBlock(block: TaskNoteBlockEntity) {
        val existing = taskNoteBlockDao.getByTaskId(block.taskId).find { it.id == block.id }
        taskNoteBlockDao.update(block)
        if (existing != null && block.blockType == "CHECKLIST" && existing.isChecked != block.isChecked) {
            val statusStr = if (block.isChecked) "checked" else "unchecked"
            taskActivityDao.insert(TaskActivityEntity(
                taskId = block.taskId,
                activityType = "CHECKLIST_TOGGLED",
                details = "Checklist item $statusStr: \"${block.content}\""
            ))
        }
    }

    override suspend fun deleteTaskNoteBlock(blockId: String) {
        val block = taskNoteBlockDao.getAll().find { it.id == blockId }
        taskNoteBlockDao.deleteById(blockId)
        if (block != null) {
            val detailsStr = when (block.blockType) {
                "CHECKLIST" -> "Checklist item removed: \"${block.content}\""
                "TEXT" -> "Note removed"
                "LINK" -> "Link block removed"
                "QUOTE" -> "Quote removed"
                else -> "Note block removed"
            }
            taskActivityDao.insert(TaskActivityEntity(
                taskId = block.taskId,
                activityType = "NOTE_BLOCK_DELETED",
                details = detailsStr
            ))
        }
    }

    override suspend fun updateTaskNoteBlocks(blocks: List<TaskNoteBlockEntity>) =
        taskNoteBlockDao.insertAll(blocks)

    override suspend fun deleteTaskNoteBlocksByTaskId(taskId: String) {
        taskNoteBlockDao.deleteByTaskId(taskId)
    }

    override fun observeTaskActivities(taskId: String): Flow<List<TaskActivityEntity>> =
        taskActivityDao.observeByTaskId(taskId)

    override suspend fun getTaskActivities(taskId: String): List<TaskActivityEntity> =
        taskActivityDao.getByTaskId(taskId)

    override suspend fun insertTaskActivity(activity: TaskActivityEntity) =
        taskActivityDao.insert(activity)

    override suspend fun getAllTaskResources(): List<TaskResourceEntity> = taskResourceDao.getAll()
    override suspend fun getAllTaskNoteBlocks(): List<TaskNoteBlockEntity> = taskNoteBlockDao.getAll()
    override suspend fun getAllTaskActivities(): List<TaskActivityEntity> = taskActivityDao.getAll()
    override suspend fun insertAllTaskResources(resources: List<TaskResourceEntity>) = taskResourceDao.insertAll(resources)
    override suspend fun insertAllTaskNoteBlocks(blocks: List<TaskNoteBlockEntity>) = taskNoteBlockDao.insertAll(blocks)
    override suspend fun insertAllTaskActivities(activities: List<TaskActivityEntity>) = taskActivityDao.insertAll(activities)
    override suspend fun deleteAllTaskResources() = taskResourceDao.deleteAll()
    override suspend fun deleteAllTaskNoteBlocks() = taskNoteBlockDao.deleteAll()
    override suspend fun deleteAllTaskActivities() = taskActivityDao.deleteAll()
}
