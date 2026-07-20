package com.example.todo.core.backup

import com.example.todo.core.database.AppDatabase
import com.example.todo.core.datastore.UserPreferencesManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val db: AppDatabase,
    private val preferencesManager: UserPreferencesManager,
    private val reminderScheduler: com.example.todo.core.notification.ReminderScheduler
) {
    suspend fun getBackupPayload(appVersion: String): BackupPayload {
        val tasks = db.taskDao().getAllTasks()
        val subTasks = db.subTaskDao().getAllSubTasks()
        val categories = db.categoryDao().getAllCategories()
        val focusSessions = db.focusSessionDao().getAllSessions()
        val achievements = db.achievementDao().getAllAchievements()
        val reminders = db.reminderDao().getAll()
        val resources = db.taskResourceDao().getAll()
        val noteBlocks = db.taskNoteBlockDao().getAll()
        val activities = db.taskActivityDao().getAll()

        val settings = SettingsBackup(
            themeMode = preferencesManager.themeMode.first(),
            pomodoroDuration = preferencesManager.pomodoroDuration.first(),
            breakDuration = preferencesManager.breakDuration.first(),
            deepWorkDuration = preferencesManager.deepWorkDuration.first(),
            totalXp = preferencesManager.totalXp.first(),
            currentStreak = preferencesManager.currentStreak.first(),
            longestStreak = preferencesManager.longestStreak.first(),
            lastActiveDate = preferencesManager.lastActiveDate.first(),
            lastProductiveActionDate = preferencesManager.lastProductiveActionDate.first(),
            notificationsEnabled = preferencesManager.notificationsEnabled.first(),
            dailyReminderEnabled = preferencesManager.dailyAgendaEnabled.first(),
            dailyReminderTime = preferencesManager.dailyAgendaTime.first(),
            isFirstLaunch = preferencesManager.isFirstLaunch.first(),
            quietHoursEnabled = preferencesManager.quietHoursEnabled.first(),
            quietHoursStart = preferencesManager.quietHoursStart.first(),
            quietHoursEnd = preferencesManager.quietHoursEnd.first(),
            notificationSoundEnabled = preferencesManager.notificationSoundEnabled.first(),
            notificationVibrationEnabled = preferencesManager.notificationVibrationEnabled.first(),
            dailyAgendaEnabled = preferencesManager.dailyAgendaEnabled.first(),
            dailyAgendaUnified = preferencesManager.dailyAgendaUnified.first(),
            dailyAgendaTime = preferencesManager.dailyAgendaTime.first(),
            dailyAgendaTimeSecondary = preferencesManager.dailyAgendaTimeSecondary.first(),
            weeklyReviewEnabled = preferencesManager.weeklyReviewEnabled.first(),
            weeklyReviewDay = preferencesManager.weeklyReviewDay.first(),
            weeklyReviewTime = preferencesManager.weeklyReviewTime.first(),
            overdueReminderEnabled = preferencesManager.overdueReminderEnabled.first(),
            overdueReminderFrequency = preferencesManager.overdueReminderFrequency.first(),
            groupNotifications = preferencesManager.groupNotifications.first(),
            defaultSnoozeMinutes = preferencesManager.defaultSnoozeMinutes.first()
        )

        return BackupPayload(
            version = BackupMigrationManager.CURRENT_SCHEMA_VERSION,
            appVersion = appVersion,
            createdAt = System.currentTimeMillis(),
            settings = settings,
            categories = categories,
            tasks = tasks,
            subTasks = subTasks,
            focusSessions = focusSessions,
            achievements = achievements,
            reminders = reminders,
            resources = resources,
            noteBlocks = noteBlocks,
            activities = activities
        )
    }

    suspend fun restoreBackupPayload(payload: BackupPayload) {
        // Clear old database records in dependency order
        db.subTaskDao().deleteAll()
        db.reminderDao().deleteAll()
        db.taskResourceDao().deleteAll()
        db.taskNoteBlockDao().deleteAll()
        db.taskActivityDao().deleteAll()
        db.taskDao().deleteAllTasks()
        db.categoryDao().deleteAll()
        db.focusSessionDao().deleteAll()
        db.achievementDao().deleteAll()

        val categories = payload.categories ?: emptyList()
        val achievements = payload.achievements ?: emptyList()
        val tasks = payload.tasks ?: emptyList()
        val subTasks = payload.subTasks ?: emptyList()
        val reminders = payload.reminders ?: emptyList()
        val focusSessions = payload.focusSessions ?: emptyList()
        val resources = payload.resources ?: emptyList()
        val noteBlocks = payload.noteBlocks ?: emptyList()
        val activities = payload.activities ?: emptyList()

        // Insert new records in dependency order (categories first, then tasks/achievements/etc.)
        if (categories.isNotEmpty()) {
            db.categoryDao().insertAll(categories)
        }
        if (achievements.isNotEmpty()) {
            db.achievementDao().insertAll(achievements)
        }
        if (tasks.isNotEmpty()) {
            db.taskDao().insertAll(tasks)
        }
        if (subTasks.isNotEmpty()) {
            db.subTaskDao().insertAll(subTasks)
        }
        if (reminders.isNotEmpty()) {
            db.reminderDao().insertAll(reminders)
        }
        if (focusSessions.isNotEmpty()) {
            db.focusSessionDao().insertAll(focusSessions)
        }
        if (resources.isNotEmpty()) {
            db.taskResourceDao().insertAll(resources)
        }
        if (noteBlocks.isNotEmpty()) {
            db.taskNoteBlockDao().insertAll(noteBlocks)
        }
        if (activities.isNotEmpty()) {
            db.taskActivityDao().insertAll(activities)
        }

        // Restore settings/preferences
        val settings = payload.settings.sanitize()
        preferencesManager.setThemeMode(settings.themeMode!!)
        preferencesManager.setPomodoroDuration(settings.pomodoroDuration!!)
        preferencesManager.setBreakDuration(settings.breakDuration!!)
        preferencesManager.setDeepWorkDuration(settings.deepWorkDuration!!)
        preferencesManager.setTotalXp(settings.totalXp!!)
        preferencesManager.setCurrentStreak(settings.currentStreak!!)
        preferencesManager.setLongestStreak(settings.longestStreak!!)
        preferencesManager.setLastActiveDate(settings.lastActiveDate!!)
        preferencesManager.setLastProductiveActionDate(settings.lastProductiveActionDate!!)
        preferencesManager.setNotificationsEnabled(settings.notificationsEnabled!!)
        preferencesManager.setFirstLaunch(settings.isFirstLaunch!!)
        preferencesManager.setQuietHoursEnabled(settings.quietHoursEnabled!!)
        preferencesManager.setQuietHoursStart(settings.quietHoursStart!!)
        preferencesManager.setQuietHoursEnd(settings.quietHoursEnd!!)
        preferencesManager.setNotificationSoundEnabled(settings.notificationSoundEnabled!!)
        preferencesManager.setNotificationVibrationEnabled(settings.notificationVibrationEnabled!!)
        preferencesManager.setDailyAgendaEnabled(settings.dailyAgendaEnabled!!)
        preferencesManager.setDailyAgendaUnified(settings.dailyAgendaUnified!!)
        preferencesManager.setDailyAgendaTime(settings.dailyAgendaTime!!)
        preferencesManager.setDailyAgendaTimeSecondary(settings.dailyAgendaTimeSecondary!!)
        preferencesManager.setWeeklyReviewEnabled(settings.weeklyReviewEnabled!!)
        preferencesManager.setWeeklyReviewDay(settings.weeklyReviewDay!!)
        preferencesManager.setWeeklyReviewTime(settings.weeklyReviewTime!!)
        preferencesManager.setOverdueReminderEnabled(settings.overdueReminderEnabled!!)
        preferencesManager.setOverdueReminderFrequency(settings.overdueReminderFrequency!!)
        preferencesManager.setGroupNotifications(settings.groupNotifications!!)
        preferencesManager.setDefaultSnoozeMinutes(settings.defaultSnoozeMinutes!!)

        // Reschedule all restored reminders
        reminderScheduler.rescheduleAll()
    }
}
