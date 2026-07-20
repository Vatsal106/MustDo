package com.example.todo.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createAllChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_TASK_REMINDERS,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for specific tasks when they are due or at scheduled times."
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_DAILY_AGENDA,
                    "Daily Agenda",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily digest of tasks pending and overdue."
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_WEEKLY_REVIEW,
                    "Weekly Planning",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Weekly summaries of your productivity and streaks."
                },
                NotificationChannel(
                    CHANNEL_OVERDUE_TASKS,
                    "Overdue Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts about tasks that are past their due dates."
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_FOCUS_SESSIONS,
                    "Focus Sessions",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for Pomodoro focus sessions and break timers."
                }
            )

            notificationManager.createNotificationChannels(channels)
        }
    }

    fun getChannelId(category: NotificationCategory): String {
        return when (category) {
            NotificationCategory.TASK_REMINDER -> CHANNEL_TASK_REMINDERS
            NotificationCategory.DAILY_AGENDA -> CHANNEL_DAILY_AGENDA
            NotificationCategory.WEEKLY_REVIEW -> CHANNEL_WEEKLY_REVIEW
            NotificationCategory.OVERDUE_REMINDER -> CHANNEL_OVERDUE_TASKS
            NotificationCategory.FOCUS_SESSION -> CHANNEL_FOCUS_SESSIONS
        }
    }

    companion object {
        const val CHANNEL_TASK_REMINDERS = "task_reminders"
        const val CHANNEL_DAILY_AGENDA = "daily_agenda"
        const val CHANNEL_WEEKLY_REVIEW = "weekly_review"
        const val CHANNEL_OVERDUE_TASKS = "overdue_tasks"
        const val CHANNEL_FOCUS_SESSIONS = "focus_sessions"
    }
}

enum class NotificationCategory {
    TASK_REMINDER, DAILY_AGENDA, WEEKLY_REVIEW, OVERDUE_REMINDER, FOCUS_SESSION
}
