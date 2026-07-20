package com.example.todo.features.tasks.data

import com.example.todo.core.database.dao.ReminderDao
import com.example.todo.core.database.entity.TaskReminderEntity
import com.example.todo.core.notification.ReminderScheduler
import com.example.todo.features.tasks.domain.ReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler
) : ReminderRepository {

    override fun observeRemindersForTask(taskId: String): Flow<List<TaskReminderEntity>> {
        return reminderDao.observeByTaskId(taskId)
    }

    override suspend fun getRemindersForTask(taskId: String): List<TaskReminderEntity> {
        return reminderDao.getByTaskId(taskId)
    }

    override suspend fun addReminder(reminder: TaskReminderEntity) {
        reminderDao.insert(reminder)
        reminderScheduler.scheduleReminder(reminder)
    }

    override suspend fun updateReminder(reminder: TaskReminderEntity) {
        reminderDao.update(reminder)
        reminderScheduler.scheduleReminder(reminder)
    }

    override suspend fun deleteReminder(reminder: TaskReminderEntity) {
        reminderScheduler.cancelReminder(reminder.id)
        reminderDao.delete(reminder)
    }

    override suspend fun deleteRemindersForTask(taskId: String) {
        reminderScheduler.cancelRemindersForTask(taskId)
        reminderDao.deleteByTaskId(taskId)
    }

    override suspend fun disableRemindersForTask(taskId: String) {
        reminderScheduler.cancelRemindersForTask(taskId)
        reminderDao.disableAllForTask(taskId)
    }

    override suspend fun getUpcomingReminders(): List<TaskReminderEntity> {
        return reminderDao.getUpcomingReminders()
    }

    override suspend fun getAllReminders(): List<TaskReminderEntity> {
        return reminderDao.getAll()
    }

    override suspend fun insertAllReminders(reminders: List<TaskReminderEntity>) {
        reminderDao.insertAll(reminders)
        reminders.forEach { reminder ->
            reminderScheduler.scheduleReminder(reminder)
        }
    }

    override suspend fun deleteAllReminders() {
        val reminders = reminderDao.getAll()
        reminders.forEach { reminderScheduler.cancelReminder(it.id) }
        reminderDao.deleteAll()
    }
}
