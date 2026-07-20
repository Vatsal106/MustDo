package com.example.todo.core.backup

import com.example.todo.core.database.entity.*

data class SettingsBackup(
    val themeMode: String? = "SYSTEM",
    val pomodoroDuration: Int? = 25,
    val breakDuration: Int? = 5,
    val deepWorkDuration: Int? = 90,
    val notificationsEnabled: Boolean? = true,
    val dailyReminderEnabled: Boolean? = false,
    val dailyReminderTime: String? = "09:00",
    val totalXp: Int? = 0,
    val currentStreak: Int? = 0,
    val longestStreak: Int? = 0,
    val lastActiveDate: Long? = 0L,
    val lastProductiveActionDate: Long? = 0L,
    val isFirstLaunch: Boolean? = true,
    val quietHoursEnabled: Boolean? = false,
    val quietHoursStart: String? = "22:00",
    val quietHoursEnd: String? = "07:00",
    val notificationSoundEnabled: Boolean? = true,
    val notificationVibrationEnabled: Boolean? = true,
    val dailyAgendaEnabled: Boolean? = true,
    val dailyAgendaUnified: Boolean? = true,
    val dailyAgendaTime: String? = "08:00",
    val dailyAgendaTimeSecondary: String? = "12:00",
    val weeklyReviewEnabled: Boolean? = true,
    val weeklyReviewDay: Int? = 1,
    val weeklyReviewTime: String? = "19:00",
    val overdueReminderEnabled: Boolean? = true,
    val overdueReminderFrequency: String? = "DAILY",
    val groupNotifications: Boolean? = true,
    val defaultSnoozeMinutes: Int? = 15
) {
    fun sanitize(): SettingsBackup {
        return SettingsBackup(
            themeMode = themeMode ?: "SYSTEM",
            pomodoroDuration = pomodoroDuration ?: 25,
            breakDuration = breakDuration ?: 5,
            deepWorkDuration = deepWorkDuration ?: 90,
            notificationsEnabled = notificationsEnabled ?: true,
            dailyReminderEnabled = dailyReminderEnabled ?: false,
            dailyReminderTime = dailyReminderTime ?: "09:00",
            totalXp = totalXp ?: 0,
            currentStreak = currentStreak ?: 0,
            longestStreak = longestStreak ?: 0,
            lastActiveDate = lastActiveDate ?: 0L,
            lastProductiveActionDate = lastProductiveActionDate ?: 0L,
            isFirstLaunch = isFirstLaunch ?: true,
            quietHoursEnabled = quietHoursEnabled ?: false,
            quietHoursStart = quietHoursStart ?: "22:00",
            quietHoursEnd = quietHoursEnd ?: "07:00",
            notificationSoundEnabled = notificationSoundEnabled ?: true,
            notificationVibrationEnabled = notificationVibrationEnabled ?: true,
            dailyAgendaEnabled = dailyAgendaEnabled ?: true,
            dailyAgendaUnified = dailyAgendaUnified ?: true,
            dailyAgendaTime = dailyAgendaTime ?: "08:00",
            weeklyReviewEnabled = weeklyReviewEnabled ?: true,
            weeklyReviewDay = weeklyReviewDay ?: 1,
            weeklyReviewTime = weeklyReviewTime ?: "19:00",
            overdueReminderEnabled = overdueReminderEnabled ?: true,
            overdueReminderFrequency = overdueReminderFrequency ?: "DAILY",
            groupNotifications = groupNotifications ?: true,
            defaultSnoozeMinutes = defaultSnoozeMinutes ?: 15
        )
    }
}

data class BackupPayload(
    val version: Int,
    val appVersion: String,
    val createdAt: Long,
    val checksum: String = "",
    val settings: SettingsBackup,
    val categories: List<CategoryEntity>? = emptyList(),
    val tasks: List<TaskEntity>? = emptyList(),
    val subTasks: List<SubTaskEntity>? = emptyList(),
    val focusSessions: List<FocusSessionEntity>? = emptyList(),
    val achievements: List<AchievementEntity>? = emptyList(),
    val reminders: List<TaskReminderEntity>? = emptyList(),
    val resources: List<TaskResourceEntity>? = emptyList(),
    val noteBlocks: List<TaskNoteBlockEntity>? = emptyList(),
    val activities: List<TaskActivityEntity>? = emptyList()
)
