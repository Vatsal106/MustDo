package com.example.todo.core.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.todo.core.database.dao.ReminderDao
import com.example.todo.core.database.dao.TaskDao
import com.example.todo.core.database.entity.TaskReminderEntity
import com.example.todo.features.tasks.domain.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class TaskNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: TaskRepository

    @Inject
    lateinit var reminderDao: ReminderDao

    @Inject
    lateinit var taskDao: TaskDao

    @Inject
    lateinit var scheduler: ReminderScheduler

    @Inject
    lateinit var notificationBuilder: NotificationBuilder

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("TaskNotificationReceiver", "Received action: $action")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    scheduler.rescheduleAll()
                }
            }

            ACTION_TRIGGER_REMINDER -> {
                val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return

                CoroutineScope(Dispatchers.IO).launch {
                    val reminder = reminderDao.getById(reminderId) ?: return@launch
                    val task = taskDao.getTaskById(taskId) ?: return@launch

                    // Only show reminder if it is enabled and the task is not completed/archived
                    if (reminder.isEnabled && task.status != "COMPLETED" && task.status != "ARCHIVED") {
                        val notification = notificationBuilder.buildTaskReminder(task, reminder)
                        notificationManager.notify(reminder.id.hashCode(), notification)

                        // Update lastTriggeredAt
                        reminderDao.updateLastTriggered(reminder.id, System.currentTimeMillis())

                        // Compute and schedule next recurrence
                        val nextTrigger = scheduler.computeNextTrigger(reminder)
                        if (nextTrigger != null) {
                            val nextReminder = reminder.copy(
                                triggerTimestamp = nextTrigger,
                                lastTriggeredAt = System.currentTimeMillis()
                            )
                            reminderDao.update(nextReminder)
                            scheduler.scheduleReminder(nextReminder)
                        }
                    }
                }
            }

            ACTION_COMPLETE_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

                CoroutineScope(Dispatchers.IO).launch {
                    repository.completeTask(taskId)
                    scheduler.cancelRemindersForTask(taskId)
                    if (notificationId != -1) {
                        notificationManager.cancel(notificationId)
                    }
                    val updateIntent = Intent("com.example.todo.action.UPDATE_WIDGETS").apply {
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(updateIntent)
                }
            }

            ACTION_ARCHIVE_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

                CoroutineScope(Dispatchers.IO).launch {
                    repository.archiveTask(taskId)
                    scheduler.cancelRemindersForTask(taskId)
                    if (notificationId != -1) {
                        notificationManager.cancel(notificationId)
                    }
                }
            }

            ACTION_SNOOZE_15M, ACTION_SNOOZE_30M, ACTION_SNOOZE_1H, ACTION_SNOOZE_TOMORROW -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
                val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }

                val minutes = when (action) {
                    ACTION_SNOOZE_15M -> 15
                    ACTION_SNOOZE_30M -> 30
                    ACTION_SNOOZE_1H -> 60
                    ACTION_SNOOZE_TOMORROW -> 24 * 60
                    else -> 15
                }

                val snoozeTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
                scheduler.scheduleSnooze(reminderId, taskId, snoozeTime)
            }

            ACTION_TRIGGER_DAILY_AGENDA -> {
                val workRequest = OneTimeWorkRequestBuilder<DailyAgendaWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }

            ACTION_TRIGGER_WEEKLY_REVIEW -> {
                val workRequest = OneTimeWorkRequestBuilder<WeeklyReviewWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }

            ACTION_TRIGGER_OVERDUE_CHECK -> {
                val workRequest = OneTimeWorkRequestBuilder<OverdueCheckWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.todo.action.TRIGGER_REMINDER"
        const val ACTION_TRIGGER_DAILY_AGENDA = "com.example.todo.action.TRIGGER_DAILY_AGENDA"
        const val ACTION_TRIGGER_WEEKLY_REVIEW = "com.example.todo.action.TRIGGER_WEEKLY_REVIEW"
        const val ACTION_TRIGGER_OVERDUE_CHECK = "com.example.todo.action.TRIGGER_OVERDUE_CHECK"

        const val ACTION_COMPLETE_TASK = "com.example.todo.action.COMPLETE_TASK"
        const val ACTION_ARCHIVE_TASK = "com.example.todo.action.ARCHIVE_TASK"
        
        const val ACTION_SNOOZE_15M = "com.example.todo.action.SNOOZE_15M"
        const val ACTION_SNOOZE_30M = "com.example.todo.action.SNOOZE_30M"
        const val ACTION_SNOOZE_1H = "com.example.todo.action.SNOOZE_1H"
        const val ACTION_SNOOZE_TOMORROW = "com.example.todo.action.SNOOZE_TOMORROW"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
