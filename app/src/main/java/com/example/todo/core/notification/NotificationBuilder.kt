package com.example.todo.core.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.todo.MainActivity
import com.example.todo.core.database.dao.CategoryDao
import com.example.todo.core.database.entity.TaskEntity
import com.example.todo.core.database.entity.TaskReminderEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.todo.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val channelManager: NotificationChannelManager,
    private val categoryDao: CategoryDao,
    private val taskResourceDao: com.example.todo.core.database.dao.TaskResourceDao
) {

    suspend fun buildTaskReminder(task: TaskEntity, reminder: TaskReminderEntity): Notification {
        val channelId = channelManager.getChannelId(NotificationCategory.TASK_REMINDER)
        val notificationId = reminder.id.hashCode()

        val dueStr = task.dueDateMillis?.let {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
        } ?: "No due date"

        val category = task.categoryId?.let { categoryDao.getCategoryById(it) }
        val categoryStr = category?.let { " | Category: ${it.name}" } ?: ""
        val hasVoiceNote = taskResourceDao.getByTaskId(task.id).any { it.resourceType == "VOICE_NOTE" }
        val suffix = if (hasVoiceNote) " | 🎤 Voice Note Attached" else ""
        val contentText = "Priority: ${task.priority}$categoryStr | Due: $dueStr$suffix"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(TaskNotificationReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskNotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_TASKS)

        // Add actions
        val completeIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_COMPLETE_TASK
            putExtra(TaskNotificationReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskNotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(TaskNotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            android.R.drawable.checkbox_on_background,
            "Complete",
            completePendingIntent
        )

        val snoozeIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_SNOOZE_15M
            putExtra(TaskNotificationReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskNotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(TaskNotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 3,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            android.R.drawable.ic_lock_idle_alarm,
            "Snooze 15m",
            snoozePendingIntent
        )

        val archiveIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_ARCHIVE_TASK
            putExtra(TaskNotificationReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskNotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(TaskNotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val archivePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 4,
            archiveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            android.R.drawable.ic_menu_save,
            "Archive",
            archivePendingIntent
        )

        return builder.build()
    }

    fun buildDailyAgenda(tasks: List<TaskEntity>): Notification {
        val channelId = channelManager.getChannelId(NotificationCategory.DAILY_AGENDA)
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val summaryTitle = if (hour < 12) "🌅 Morning Daily Agenda" else "☀️ Noon Daily Agenda"
        
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(summaryTitle)
            .setSummaryText("${tasks.size} tasks pending today")
        
        tasks.take(5).forEach { task ->
            inboxStyle.addLine("• ${task.title}")
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            DAILY_AGENDA_REQ_CODE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val taskListStr = tasks.take(3).joinToString(", ") { it.title }
        val bodyText = if (tasks.size > 3) {
            "Pending: $taskListStr and ${tasks.size - 3} more"
        } else {
            "Pending: $taskListStr"
        }

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(summaryTitle)
            .setContentText(bodyText)
            .setStyle(inboxStyle)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }

    suspend fun buildDailyAgendaTask(task: TaskEntity): Notification {
        val channelId = channelManager.getChannelId(NotificationCategory.DAILY_AGENDA)
        val notificationId = task.id.hashCode()

        val dueStr = task.dueDateMillis?.let {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
        } ?: "No due date"

        val category = task.categoryId?.let { categoryDao.getCategoryById(it) }
        val categoryStr = category?.let { " | Category: ${it.name}" } ?: ""
        val hasVoiceNote = taskResourceDao.getByTaskId(task.id).any { it.resourceType == "VOICE_NOTE" }
        val suffix = if (hasVoiceNote) " | 🎤 Voice Note Attached" else ""
        val contentText = "Priority: ${task.priority}$categoryStr | Due: $dueStr$suffix"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(TaskNotificationReceiver.EXTRA_TASK_ID, task.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)

        val completeIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_COMPLETE_TASK
            putExtra(TaskNotificationReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskNotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            android.R.drawable.checkbox_on_background,
            "Complete",
            completePendingIntent
        )

        val archiveIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = TaskNotificationReceiver.ACTION_ARCHIVE_TASK
            putExtra(TaskNotificationReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskNotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val archivePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 4,
            archiveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            android.R.drawable.ic_menu_save,
            "Archive",
            archivePendingIntent
        )

        return builder.build()
    }

    fun buildWeeklyReview(stats: WeeklyStats): Notification {
        val channelId = channelManager.getChannelId(NotificationCategory.WEEKLY_REVIEW)
        val title = "📈 Weekly Planning & Review"
        
        val content = "Completed: ${stats.completedCount} | Focus: ${stats.focusMinutes}m | Streak: ${stats.currentStreak}d"
        
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .addLine("Completed Tasks: ${stats.completedCount}")
            .addLine("Remaining Tasks: ${stats.remainingCount}")
            .addLine("Focus Minutes: ${stats.focusMinutes}m")
            .addLine("Current Streak: ${stats.currentStreak} days")
 
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            WEEKLY_REVIEW_REQ_CODE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
 
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(inboxStyle)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
    }

    fun buildOverdueReminder(tasks: List<TaskEntity>): Notification {
        val channelId = channelManager.getChannelId(NotificationCategory.OVERDUE_REMINDER)
        val title = "⚠️ Overdue Tasks Reminder"
        
        val bodyText = "You have ${tasks.size} overdue tasks needing attention!"
 
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText("${tasks.size} overdue tasks")
        
        tasks.take(5).forEach { task ->
            inboxStyle.addLine("• ${task.title}")
        }
 
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            OVERDUE_REQ_CODE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
 
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(inboxStyle)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
    }

    fun buildFocusReminder(message: String): Notification {
        val channelId = channelManager.getChannelId(NotificationCategory.FOCUS_SESSION)
        
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            FOCUS_REQ_CODE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
 
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Focus Session")
            .setContentText(message)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }

    companion object {
        const val GROUP_KEY_TASKS = "com.example.todo.TASK_GROUP"
        private const val DAILY_AGENDA_REQ_CODE = 4001
        private const val WEEKLY_REVIEW_REQ_CODE = 4002
        private const val OVERDUE_REQ_CODE = 4003
        private const val FOCUS_REQ_CODE = 4004
    }
}

data class WeeklyStats(
    val completedCount: Int,
    val remainingCount: Int,
    val focusMinutes: Long,
    val currentStreak: Int
)
