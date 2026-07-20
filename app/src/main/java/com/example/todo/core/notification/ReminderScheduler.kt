package com.example.todo.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.todo.core.database.dao.ReminderDao
import com.example.todo.core.database.entity.TaskReminderEntity
import com.example.todo.core.database.entity.ReminderType
import com.example.todo.core.database.entity.RepeatPattern
import com.example.todo.core.datastore.UserPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao,
    private val preferencesManager: UserPreferencesManager
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleReminder(reminder: TaskReminderEntity) {
        if (!reminder.isEnabled) {
            cancelReminder(reminder.id)
            return
        }

        // Adjust for quiet hours
        val triggerTime = adjustForQuietHours(reminder.triggerTimestamp)
        
        // If trigger time is in the past, compute next trigger
        val finalTriggerTime = if (triggerTime <= System.currentTimeMillis()) {
            val next = computeNextTrigger(reminder)
            if (next != null) {
                reminderDao.updateTriggerTimestamp(reminder.id, next)
                adjustForQuietHours(next)
            } else {
                null
            }
        } else {
            triggerTime
        }

        if (finalTriggerTime == null) {
            Log.d("ReminderScheduler", "Skipping reminder ${reminder.id} as it is in the past and has no future recurrence")
            return
        }

        val pendingIntent = createPendingIntent(reminder)
        scheduleAlarmSafe(finalTriggerTime, pendingIntent)
        Log.d("ReminderScheduler", "Scheduled alarm for reminder ${reminder.id} at $finalTriggerTime")
    }

    fun cancelReminder(reminderId: String) {
        val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("ReminderScheduler", "Cancelled alarm for reminder $reminderId")
        }
    }

    suspend fun cancelRemindersForTask(taskId: String) {
        val reminders = reminderDao.getByTaskId(taskId)
        reminders.forEach { cancelReminder(it.id) }
        reminderDao.disableAllForTask(taskId)
    }

    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        val reminders = reminderDao.getAll().filter { it.isEnabled }
        reminders.forEach { reminder ->
            scheduleReminder(reminder)
        }
    }

    fun scheduleSnooze(reminderId: String, taskId: String, snoozeMillis: Long) {
        val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_TRIGGER_REMINDER
            putExtra(TaskNotificationReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(TaskNotificationReceiver.EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleAlarmSafe(snoozeMillis, pendingIntent)
        Log.d("ReminderScheduler", "Scheduled snooze alarm for reminder $reminderId at $snoozeMillis")
    }

    private suspend fun adjustForQuietHours(triggerMillis: Long): Long {
        val enabled = preferencesManager.quietHoursEnabled.first()
        if (!enabled) return triggerMillis

        val startStr = preferencesManager.quietHoursStart.first()
        val endStr = preferencesManager.quietHoursEnd.first()

        val startParts = startStr.split(":").map { it.toIntOrNull() ?: 0 }
        val endParts = endStr.split(":").map { it.toIntOrNull() ?: 0 }
        val startHour = startParts.getOrElse(0) { 22 }
        val startMinute = startParts.getOrElse(1) { 0 }
        val endHour = endParts.getOrElse(0) { 7 }
        val endMinute = endParts.getOrElse(1) { 0 }

        val calendar = Calendar.getInstance().apply { timeInMillis = triggerMillis }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val currentMinutes = hour * 60 + minute
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        var inQuietHours = false
        if (startMinutes < endMinutes) {
            if (currentMinutes in startMinutes until endMinutes) {
                inQuietHours = true
            }
        } else {
            if (currentMinutes >= startMinutes || currentMinutes < endMinutes) {
                inQuietHours = true
            }
        }

        if (inQuietHours) {
            val resultCalendar = Calendar.getInstance().apply {
                timeInMillis = triggerMillis
                set(Calendar.HOUR_OF_DAY, endHour)
                set(Calendar.MINUTE, endMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (resultCalendar.timeInMillis <= triggerMillis) {
                resultCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return resultCalendar.timeInMillis
        }

        return triggerMillis
    }

    fun computeNextTrigger(reminder: TaskReminderEntity): Long? {
        val type = try {
            ReminderType.valueOf(reminder.reminderType)
        } catch (e: Exception) {
            ReminderType.BEFORE_DUE_DATE
        }
        val repeatPattern = try {
            RepeatPattern.valueOf(reminder.repeatPattern)
        } catch (e: Exception) {
            RepeatPattern.NONE
        }

        val baseTime = reminder.lastTriggeredAt ?: reminder.triggerTimestamp
        val now = System.currentTimeMillis()

        return when (type) {
            ReminderType.EXACT_TIME, ReminderType.BEFORE_DUE_DATE -> {
                if (baseTime > now) baseTime else null
            }
            ReminderType.RECURRING -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = baseTime }
                while (calendar.timeInMillis <= now) {
                    when (repeatPattern) {
                        RepeatPattern.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                        RepeatPattern.WEEKLY -> {
                            val daysOfWeek = reminder.repeatDaysOfWeek?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
                            if (!daysOfWeek.isNullOrEmpty()) {
                                var found = false
                                for (i in 1..8) {
                                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                                    val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                                    if (daysOfWeek.contains(currentDay)) {
                                        found = true
                                        break
                                    }
                                }
                                if (!found) calendar.add(Calendar.WEEK_OF_YEAR, 1)
                            } else {
                                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                            }
                        }
                        RepeatPattern.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                        RepeatPattern.YEARLY -> calendar.add(Calendar.YEAR, 1)
                        else -> return null
                    }
                }
                calendar.timeInMillis
            }
            ReminderType.CUSTOM_INTERVAL -> {
                val interval = reminder.customIntervalMinutes ?: return null
                val calendar = Calendar.getInstance().apply { timeInMillis = baseTime }
                while (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.MINUTE, interval)
                }
                val elapsed = calendar.timeInMillis - reminder.createdAt
                if (elapsed > 24 * 60 * 60 * 1000L) {
                    null
                } else {
                    calendar.timeInMillis
                }
            }
        }
    }

    private fun createPendingIntent(reminder: TaskReminderEntity): PendingIntent {
        val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_TRIGGER_REMINDER
            putExtra(TaskNotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(TaskNotificationReceiver.EXTRA_TASK_ID, reminder.taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleAlarmSafe(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w("ReminderScheduler", "SecurityException scheduling exact alarm, falling back to inexact", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }
}
