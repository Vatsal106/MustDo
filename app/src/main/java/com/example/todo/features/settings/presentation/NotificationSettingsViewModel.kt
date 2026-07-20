package com.example.todo.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.core.datastore.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsUiState(
    val notificationsEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val notificationSoundEnabled: Boolean = true,
    val notificationVibrationEnabled: Boolean = true,
    val dailyAgendaEnabled: Boolean = true,
    val dailyAgendaUnified: Boolean = true,
    val dailyAgendaTime: String = "08:00",
    val dailyAgendaTimeSecondary: String = "12:00",
    val weeklyReviewEnabled: Boolean = true,
    val weeklyReviewDay: Int = 1, // Sunday
    val weeklyReviewTime: String = "19:00",
    val overdueReminderEnabled: Boolean = true,
    val overdueReminderFrequency: String = "DAILY",
    val groupNotifications: Boolean = true,
    val defaultSnoozeMinutes: Int = 15
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            preferencesManager.notificationsEnabled.collect { v -> _uiState.update { it.copy(notificationsEnabled = v) } }
        }
        viewModelScope.launch {
            preferencesManager.quietHoursEnabled.collect { v -> _uiState.update { it.copy(quietHoursEnabled = v) } }
        }
        viewModelScope.launch {
            preferencesManager.quietHoursStart.collect { v -> _uiState.update { it.copy(quietHoursStart = v) } }
        }
        viewModelScope.launch {
            preferencesManager.quietHoursEnd.collect { v -> _uiState.update { it.copy(quietHoursEnd = v) } }
        }
        viewModelScope.launch {
            preferencesManager.notificationSoundEnabled.collect { v -> _uiState.update { it.copy(notificationSoundEnabled = v) } }
        }
        viewModelScope.launch {
            preferencesManager.notificationVibrationEnabled.collect { v -> _uiState.update { it.copy(notificationVibrationEnabled = v) } }
        }
        viewModelScope.launch {
            preferencesManager.dailyAgendaEnabled.collect { v -> _uiState.update { it.copy(dailyAgendaEnabled = v) } }
        }
        viewModelScope.launch {
            preferencesManager.dailyAgendaUnified.collect { v -> _uiState.update { it.copy(dailyAgendaUnified = v) } }
        }
        viewModelScope.launch {
            preferencesManager.dailyAgendaTime.collect { v -> _uiState.update { it.copy(dailyAgendaTime = v) } }
        }
        viewModelScope.launch {
            preferencesManager.dailyAgendaTimeSecondary.collect { v -> _uiState.update { it.copy(dailyAgendaTimeSecondary = v) } }
        }
        viewModelScope.launch {
            preferencesManager.weeklyReviewEnabled.collect { v -> _uiState.update { it.copy(weeklyReviewEnabled = v) } }
        }
        viewModelScope.launch {
            preferencesManager.weeklyReviewDay.collect { v -> _uiState.update { it.copy(weeklyReviewDay = v) } }
        }
        viewModelScope.launch {
            preferencesManager.weeklyReviewTime.collect { v -> _uiState.update { it.copy(weeklyReviewTime = v) } }
        }
        viewModelScope.launch {
            preferencesManager.overdueReminderEnabled.collect { v -> _uiState.update { it.copy(overdueReminderEnabled = v) } }
        }
        viewModelScope.launch {
            preferencesManager.overdueReminderFrequency.collect { v -> _uiState.update { it.copy(overdueReminderFrequency = v) } }
        }
        viewModelScope.launch {
            preferencesManager.groupNotifications.collect { v -> _uiState.update { it.copy(groupNotifications = v) } }
        }
        viewModelScope.launch {
            preferencesManager.defaultSnoozeMinutes.collect { v -> _uiState.update { it.copy(defaultSnoozeMinutes = v) } }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setNotificationsEnabled(enabled) }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setQuietHoursEnabled(enabled) }
    }

    fun setQuietHoursStart(time: String) {
        viewModelScope.launch { preferencesManager.setQuietHoursStart(time) }
    }

    fun setQuietHoursEnd(time: String) {
        viewModelScope.launch { preferencesManager.setQuietHoursEnd(time) }
    }

    fun setNotificationSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setNotificationSoundEnabled(enabled) }
    }

    fun setNotificationVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setNotificationVibrationEnabled(enabled) }
    }

    fun setDailyAgendaEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setDailyAgendaEnabled(enabled) }
    }

    fun setDailyAgendaUnified(unified: Boolean) {
        viewModelScope.launch { preferencesManager.setDailyAgendaUnified(unified) }
    }

    fun setDailyAgendaTime(time: String) {
        viewModelScope.launch { preferencesManager.setDailyAgendaTime(time) }
    }

    fun setDailyAgendaTimeSecondary(time: String) {
        viewModelScope.launch { preferencesManager.setDailyAgendaTimeSecondary(time) }
    }

    fun setWeeklyReviewEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setWeeklyReviewEnabled(enabled) }
    }

    fun setWeeklyReviewDay(day: Int) {
        viewModelScope.launch { preferencesManager.setWeeklyReviewDay(day) }
    }

    fun setWeeklyReviewTime(time: String) {
        viewModelScope.launch { preferencesManager.setWeeklyReviewTime(time) }
    }

    fun setOverdueReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setOverdueReminderEnabled(enabled) }
    }

    fun setOverdueReminderFrequency(frequency: String) {
        viewModelScope.launch { preferencesManager.setOverdueReminderFrequency(frequency) }
    }

    fun setGroupNotifications(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setGroupNotifications(enabled) }
    }

    fun setDefaultSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { preferencesManager.setDefaultSnoozeMinutes(minutes) }
    }
}
