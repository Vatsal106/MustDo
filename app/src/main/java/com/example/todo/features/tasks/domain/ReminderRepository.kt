package com.example.todo.features.tasks.domain

import com.example.todo.core.database.entity.TaskReminderEntity
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeRemindersForTask(taskId: String): Flow<List<TaskReminderEntity>>
    suspend fun getRemindersForTask(taskId: String): List<TaskReminderEntity>
    suspend fun addReminder(reminder: TaskReminderEntity)
    suspend fun updateReminder(reminder: TaskReminderEntity)
    suspend fun deleteReminder(reminder: TaskReminderEntity)
    suspend fun deleteRemindersForTask(taskId: String)
    suspend fun disableRemindersForTask(taskId: String)
    suspend fun getUpcomingReminders(): List<TaskReminderEntity>
    suspend fun getAllReminders(): List<TaskReminderEntity>
    suspend fun insertAllReminders(reminders: List<TaskReminderEntity>)
    suspend fun deleteAllReminders()
}
