package com.example.todo.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mustdo_settings")

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Theme
    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
    private val ACCENT_COLOR = stringPreferencesKey("accent_color")
    private val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")

    // Timer settings
    private val POMODORO_DURATION = intPreferencesKey("pomodoro_duration")
    private val BREAK_DURATION = intPreferencesKey("break_duration")
    private val DEEP_WORK_DURATION = intPreferencesKey("deep_work_duration")

    // Gamification
    private val TOTAL_XP = intPreferencesKey("total_xp")
    private val CURRENT_STREAK = intPreferencesKey("current_streak")
    private val LONGEST_STREAK = intPreferencesKey("longest_streak")
    private val LAST_ACTIVE_DATE = longPreferencesKey("last_active_date")
    private val LAST_PRODUCTIVE_ACTION_DATE = longPreferencesKey("last_productive_action_date")

    // First launch
    private val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")

    // Backup health status
    private val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
    private val LAST_BACKUP_SUCCESS = booleanPreferencesKey("last_backup_success")
    private val LAST_BACKUP_ERROR = stringPreferencesKey("last_backup_error")

    // Redesigned Notifications Settings
    private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    private val QUIET_HOURS_START = stringPreferencesKey("quiet_hours_start")
    private val QUIET_HOURS_END = stringPreferencesKey("quiet_hours_end")
    private val NOTIFICATION_SOUND_ENABLED = booleanPreferencesKey("notification_sound_enabled")
    private val NOTIFICATION_VIBRATION_ENABLED = booleanPreferencesKey("notification_vibration_enabled")
    private val DAILY_AGENDA_ENABLED = booleanPreferencesKey("daily_agenda_enabled")
    private val DAILY_AGENDA_UNIFIED = booleanPreferencesKey("daily_agenda_unified")
    private val DAILY_AGENDA_TIME = stringPreferencesKey("daily_agenda_time")
    private val DAILY_AGENDA_TIME_SECONDARY = stringPreferencesKey("daily_agenda_time_secondary")
    private val WEEKLY_REVIEW_ENABLED = booleanPreferencesKey("weekly_review_enabled")
    private val WEEKLY_REVIEW_DAY = intPreferencesKey("weekly_review_day")
    private val WEEKLY_REVIEW_TIME = stringPreferencesKey("weekly_review_time")
    private val OVERDUE_REMINDER_ENABLED = booleanPreferencesKey("overdue_reminder_enabled")
    private val OVERDUE_REMINDER_FREQUENCY = stringPreferencesKey("overdue_reminder_frequency")
    private val GROUP_NOTIFICATIONS = booleanPreferencesKey("group_notifications")
    private val DEFAULT_SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
    
    // Calendar settings
    private val CALENDAR_MODE = stringPreferencesKey("calendar_mode")
    private val CALENDAR_FILTER_STATUS = stringSetPreferencesKey("calendar_filter_status")
    private val CALENDAR_FILTER_PRIORITY = stringSetPreferencesKey("calendar_filter_priority")
    private val CALENDAR_FILTER_CATEGORIES = stringSetPreferencesKey("calendar_filter_categories")
    private val CALENDAR_FILTER_RECURRENCE = stringSetPreferencesKey("calendar_filter_recurrence")
    private val CALENDAR_FILTER_DUE_STATE = stringSetPreferencesKey("calendar_filter_due_state")

    // --- Backup Flows & Setters ---
    val lastBackupTime: Flow<Long> = context.dataStore.data.map { it[LAST_BACKUP_TIME] ?: 0L }
    val lastBackupSuccess: Flow<Boolean> = context.dataStore.data.map { it[LAST_BACKUP_SUCCESS] ?: false }
    val lastBackupError: Flow<String?> = context.dataStore.data.map { it[LAST_BACKUP_ERROR] }

    suspend fun setBackupStatus(time: Long, success: Boolean, error: String?) {
        context.dataStore.edit { prefs ->
            prefs[LAST_BACKUP_TIME] = time
            prefs[LAST_BACKUP_SUCCESS] = success
            if (error != null) {
                prefs[LAST_BACKUP_ERROR] = error
            } else {
                prefs.remove(LAST_BACKUP_ERROR)
            }
        }
    }

    // --- Theme ---
    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "SYSTEM" }
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }
    
    val amoledMode: Flow<Boolean> = context.dataStore.data.map { it[AMOLED_MODE] ?: false }
    suspend fun setAmoledMode(enabled: Boolean) {
        context.dataStore.edit { it[AMOLED_MODE] = enabled }
    }
    
    val accentColor: Flow<String> = context.dataStore.data.map { it[ACCENT_COLOR] ?: "" }
    suspend fun setAccentColor(hex: String) {
        context.dataStore.edit { it[ACCENT_COLOR] = hex }
    }
    
    val useDynamicColor: Flow<Boolean> = context.dataStore.data.map { it[USE_DYNAMIC_COLOR] ?: false }
    suspend fun setUseDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[USE_DYNAMIC_COLOR] = enabled }
    }

    private val COLOR_THEME = stringPreferencesKey("color_theme")
    val colorTheme: Flow<String> = context.dataStore.data.map { it[COLOR_THEME] ?: "ORANGE" }
    suspend fun setColorTheme(theme: String) {
        context.dataStore.edit { it[COLOR_THEME] = theme }
    }

    // --- Timer ---
    val pomodoroDuration: Flow<Int> = context.dataStore.data.map { it[POMODORO_DURATION] ?: 25 }
    suspend fun setPomodoroDuration(minutes: Int) {
        context.dataStore.edit { it[POMODORO_DURATION] = minutes }
    }

    val breakDuration: Flow<Int> = context.dataStore.data.map { it[BREAK_DURATION] ?: 5 }
    suspend fun setBreakDuration(minutes: Int) {
        context.dataStore.edit { it[BREAK_DURATION] = minutes }
    }

    val deepWorkDuration: Flow<Int> = context.dataStore.data.map { it[DEEP_WORK_DURATION] ?: 90 }
    suspend fun setDeepWorkDuration(minutes: Int) {
        context.dataStore.edit { it[DEEP_WORK_DURATION] = minutes }
    }

    // --- Gamification ---
    val totalXp: Flow<Int> = context.dataStore.data.map { it[TOTAL_XP] ?: 0 }
    suspend fun setTotalXp(xp: Int) {
        context.dataStore.edit { it[TOTAL_XP] = xp }
    }
    suspend fun addXp(xp: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[TOTAL_XP] ?: 0
            prefs[TOTAL_XP] = current + xp
        }
    }

    val currentStreak: Flow<Int> = context.dataStore.data.map { it[CURRENT_STREAK] ?: 0 }
    suspend fun setCurrentStreak(streak: Int) {
        context.dataStore.edit { it[CURRENT_STREAK] = streak }
    }

    val longestStreak: Flow<Int> = context.dataStore.data.map { it[LONGEST_STREAK] ?: 0 }
    suspend fun setLongestStreak(streak: Int) {
        context.dataStore.edit { it[LONGEST_STREAK] = streak }
    }

    val lastActiveDate: Flow<Long> = context.dataStore.data.map { it[LAST_ACTIVE_DATE] ?: 0L }
    suspend fun setLastActiveDate(dateMillis: Long) {
        context.dataStore.edit { it[LAST_ACTIVE_DATE] = dateMillis }
    }

    val lastProductiveActionDate: Flow<Long> = context.dataStore.data.map { it[LAST_PRODUCTIVE_ACTION_DATE] ?: 0L }
    suspend fun setLastProductiveActionDate(dateMillis: Long) {
        context.dataStore.edit { it[LAST_PRODUCTIVE_ACTION_DATE] = dateMillis }
    }

    // --- Notifications Redesign Flows & Setters ---
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    val quietHoursEnabled: Flow<Boolean> = context.dataStore.data.map { it[QUIET_HOURS_ENABLED] ?: false }
    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.dataStore.edit { it[QUIET_HOURS_ENABLED] = enabled }
    }

    val quietHoursStart: Flow<String> = context.dataStore.data.map { it[QUIET_HOURS_START] ?: "22:00" }
    suspend fun setQuietHoursStart(time: String) {
        context.dataStore.edit { it[QUIET_HOURS_START] = time }
    }

    val quietHoursEnd: Flow<String> = context.dataStore.data.map { it[QUIET_HOURS_END] ?: "07:00" }
    suspend fun setQuietHoursEnd(time: String) {
        context.dataStore.edit { it[QUIET_HOURS_END] = time }
    }

    val notificationSoundEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATION_SOUND_ENABLED] ?: true }
    suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATION_SOUND_ENABLED] = enabled }
    }

    val notificationVibrationEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATION_VIBRATION_ENABLED] ?: true }
    suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATION_VIBRATION_ENABLED] = enabled }
    }

    val dailyAgendaEnabled: Flow<Boolean> = context.dataStore.data.map { it[DAILY_AGENDA_ENABLED] ?: true }
    suspend fun setDailyAgendaEnabled(enabled: Boolean) {
        context.dataStore.edit { it[DAILY_AGENDA_ENABLED] = enabled }
    }

    val dailyAgendaUnified: Flow<Boolean> = context.dataStore.data.map { it[DAILY_AGENDA_UNIFIED] ?: true }
    suspend fun setDailyAgendaUnified(unified: Boolean) {
        context.dataStore.edit { it[DAILY_AGENDA_UNIFIED] = unified }
    }

    val dailyAgendaTime: Flow<String> = context.dataStore.data.map { it[DAILY_AGENDA_TIME] ?: "08:00" }
    suspend fun setDailyAgendaTime(time: String) {
        context.dataStore.edit { it[DAILY_AGENDA_TIME] = time }
    }

    val dailyAgendaTimeSecondary: Flow<String> = context.dataStore.data.map { it[DAILY_AGENDA_TIME_SECONDARY] ?: "12:00" }
    suspend fun setDailyAgendaTimeSecondary(time: String) {
        context.dataStore.edit { it[DAILY_AGENDA_TIME_SECONDARY] = time }
    }

    val weeklyReviewEnabled: Flow<Boolean> = context.dataStore.data.map { it[WEEKLY_REVIEW_ENABLED] ?: true }
    suspend fun setWeeklyReviewEnabled(enabled: Boolean) {
        context.dataStore.edit { it[WEEKLY_REVIEW_ENABLED] = enabled }
    }

    val weeklyReviewDay: Flow<Int> = context.dataStore.data.map { it[WEEKLY_REVIEW_DAY] ?: 1 } // Sunday
    suspend fun setWeeklyReviewDay(day: Int) {
        context.dataStore.edit { it[WEEKLY_REVIEW_DAY] = day }
    }

    val weeklyReviewTime: Flow<String> = context.dataStore.data.map { it[WEEKLY_REVIEW_TIME] ?: "19:00" }
    suspend fun setWeeklyReviewTime(time: String) {
        context.dataStore.edit { it[WEEKLY_REVIEW_TIME] = time }
    }

    val overdueReminderEnabled: Flow<Boolean> = context.dataStore.data.map { it[OVERDUE_REMINDER_ENABLED] ?: true }
    suspend fun setOverdueReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[OVERDUE_REMINDER_ENABLED] = enabled }
    }

    val overdueReminderFrequency: Flow<String> = context.dataStore.data.map { it[OVERDUE_REMINDER_FREQUENCY] ?: "DAILY" }
    suspend fun setOverdueReminderFrequency(frequency: String) {
        context.dataStore.edit { it[OVERDUE_REMINDER_FREQUENCY] = frequency }
    }

    val groupNotifications: Flow<Boolean> = context.dataStore.data.map { it[GROUP_NOTIFICATIONS] ?: true }
    suspend fun setGroupNotifications(enabled: Boolean) {
        context.dataStore.edit { it[GROUP_NOTIFICATIONS] = enabled }
    }

    val defaultSnoozeMinutes: Flow<Int> = context.dataStore.data.map { it[DEFAULT_SNOOZE_MINUTES] ?: 15 }
    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[DEFAULT_SNOOZE_MINUTES] = minutes }
    }

    // --- First Launch ---
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { it[IS_FIRST_LAUNCH] ?: true }
    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { it[IS_FIRST_LAUNCH] = false }
    }
    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { it[IS_FIRST_LAUNCH] = isFirst }
    }

    // --- Calendar Settings ---
    val calendarMode: Flow<String> = context.dataStore.data.map { it[CALENDAR_MODE] ?: "MONTH" }
    suspend fun setCalendarMode(mode: String) {
        context.dataStore.edit { it[CALENDAR_MODE] = mode }
    }

    val calendarFilterStatus: Flow<Set<String>> = context.dataStore.data.map { it[CALENDAR_FILTER_STATUS] ?: emptySet() }
    suspend fun setCalendarFilterStatus(status: Set<String>) {
        context.dataStore.edit { it[CALENDAR_FILTER_STATUS] = status }
    }

    val calendarFilterPriority: Flow<Set<String>> = context.dataStore.data.map { it[CALENDAR_FILTER_PRIORITY] ?: emptySet() }
    suspend fun setCalendarFilterPriority(priority: Set<String>) {
        context.dataStore.edit { it[CALENDAR_FILTER_PRIORITY] = priority }
    }

    val calendarFilterCategories: Flow<Set<String>> = context.dataStore.data.map { it[CALENDAR_FILTER_CATEGORIES] ?: emptySet() }
    suspend fun setCalendarFilterCategories(categories: Set<String>) {
        context.dataStore.edit { it[CALENDAR_FILTER_CATEGORIES] = categories }
    }

    val calendarFilterRecurrence: Flow<Set<String>> = context.dataStore.data.map { it[CALENDAR_FILTER_RECURRENCE] ?: emptySet() }
    suspend fun setCalendarFilterRecurrence(recurrence: Set<String>) {
        context.dataStore.edit { it[CALENDAR_FILTER_RECURRENCE] = recurrence }
    }

    val calendarFilterDueState: Flow<Set<String>> = context.dataStore.data.map { it[CALENDAR_FILTER_DUE_STATE] ?: emptySet() }
    suspend fun setCalendarFilterDueState(dueState: Set<String>) {
        context.dataStore.edit { it[CALENDAR_FILTER_DUE_STATE] = dueState }
    }

    // --- Reset all ---
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
