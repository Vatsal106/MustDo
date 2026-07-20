package com.example.todo.core.database.dao

import androidx.room.*
import com.example.todo.core.database.entity.TaskReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: TaskReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<TaskReminderEntity>)

    @Update
    suspend fun update(reminder: TaskReminderEntity)

    @Delete
    suspend fun delete(reminder: TaskReminderEntity)

    @Query("DELETE FROM task_reminders WHERE id = :reminderId")
    suspend fun deleteById(reminderId: String)

    @Query("DELETE FROM task_reminders WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    @Query("DELETE FROM task_reminders")
    suspend fun deleteAll()

    @Query("SELECT * FROM task_reminders WHERE taskId = :taskId ORDER BY triggerTimestamp ASC")
    fun observeByTaskId(taskId: String): Flow<List<TaskReminderEntity>>

    @Query("SELECT * FROM task_reminders WHERE taskId = :taskId ORDER BY triggerTimestamp ASC")
    suspend fun getByTaskId(taskId: String): List<TaskReminderEntity>

    @Query("SELECT * FROM task_reminders WHERE isEnabled = 1 AND triggerTimestamp > :now ORDER BY triggerTimestamp ASC")
    suspend fun getUpcomingReminders(now: Long = System.currentTimeMillis()): List<TaskReminderEntity>

    @Query("SELECT * FROM task_reminders WHERE isEnabled = 1 AND triggerTimestamp BETWEEN :start AND :end ORDER BY triggerTimestamp ASC")
    suspend fun getRemindersInRange(start: Long, end: Long): List<TaskReminderEntity>

    @Query("SELECT * FROM task_reminders WHERE id = :id")
    suspend fun getById(id: String): TaskReminderEntity?

    @Query("SELECT * FROM task_reminders")
    suspend fun getAll(): List<TaskReminderEntity>

    @Query("UPDATE task_reminders SET isEnabled = 0 WHERE taskId = :taskId")
    suspend fun disableAllForTask(taskId: String)

    @Query("UPDATE task_reminders SET isEnabled = 1 WHERE taskId = :taskId")
    suspend fun enableAllForTask(taskId: String)

    @Query("UPDATE task_reminders SET lastTriggeredAt = :timestamp WHERE id = :reminderId")
    suspend fun updateLastTriggered(reminderId: String, timestamp: Long)

    @Query("UPDATE task_reminders SET triggerTimestamp = :newTrigger WHERE id = :reminderId")
    suspend fun updateTriggerTimestamp(reminderId: String, newTrigger: Long)
}
