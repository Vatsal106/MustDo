package com.example.todo.features.settings.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.core.backup.BackupEngine
import com.example.todo.core.backup.BackupInfo
import com.example.todo.core.backup.BackupManager
import com.example.todo.core.database.entity.AchievementEntity
import com.example.todo.core.database.entity.CategoryEntity
import com.example.todo.core.database.entity.FocusSessionEntity
import com.example.todo.core.database.entity.TaskEntity
import com.example.todo.core.datastore.UserPreferencesManager
import com.example.todo.features.focus.domain.FocusRepository
import com.example.todo.features.gamification.domain.GamificationEngine
import com.example.todo.features.settings.domain.CategoryRepository
import com.example.todo.features.tasks.domain.TaskRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: String = "SYSTEM",
    val amoledMode: Boolean = false,
    val useDynamicColor: Boolean = false,
    val accentColor: String = "",
    val colorTheme: String = "ORANGE",
    val pomodoroDuration: Int = 25,
    val breakDuration: Int = 5,
    val deepWorkDuration: Int = 90,
    val categories: List<CategoryEntity> = emptyList(),
    val totalXp: Int = 0,
    val currentLevel: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val achievements: List<AchievementEntity> = emptyList(),
    val showResetConfirm: Boolean = false,
    val backupMessage: String? = null,
    val lastBackupTime: Long = 0L,
    val lastBackupSuccess: Boolean = false,
    val lastBackupError: String? = null,
    val backupHistory: List<BackupInfo> = emptyList(),
    val backupPreview: BackupInfo? = null,
    val backupStorageUsage: Long = 0L
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: UserPreferencesManager,
    private val categoryRepository: CategoryRepository,
    private val taskRepository: TaskRepository,
    private val focusRepository: FocusRepository,
    private val gamificationEngine: GamificationEngine,
    private val backupEngine: BackupEngine,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val backupHistoryState: StateFlow<List<BackupInfo>> = backupManager.getBackupHistoryFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadSettings()
        viewModelScope.launch {
            backupHistoryState.collect { history ->
                val totalSize = history.sumOf { it.fileSizeBytes }
                _uiState.update {
                    it.copy(
                        backupHistory = history,
                        backupStorageUsage = totalSize
                    )
                }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            preferencesManager.themeMode.collect { _uiState.update { s -> s.copy(themeMode = it) } }
        }
        viewModelScope.launch {
            preferencesManager.amoledMode.collect { _uiState.update { s -> s.copy(amoledMode = it) } }
        }
        viewModelScope.launch {
            preferencesManager.useDynamicColor.collect { _uiState.update { s -> s.copy(useDynamicColor = it) } }
        }
        viewModelScope.launch {
            preferencesManager.accentColor.collect { _uiState.update { s -> s.copy(accentColor = it) } }
        }
        viewModelScope.launch {
            preferencesManager.colorTheme.collect { _uiState.update { s -> s.copy(colorTheme = it) } }
        }
        viewModelScope.launch {
            preferencesManager.pomodoroDuration.collect { _uiState.update { s -> s.copy(pomodoroDuration = it) } }
        }
        viewModelScope.launch {
            preferencesManager.breakDuration.collect { _uiState.update { s -> s.copy(breakDuration = it) } }
        }
        viewModelScope.launch {
            preferencesManager.deepWorkDuration.collect { _uiState.update { s -> s.copy(deepWorkDuration = it) } }
        }
        viewModelScope.launch {
            categoryRepository.observeAllCategories().collect { _uiState.update { s -> s.copy(categories = it) } }
        }
        viewModelScope.launch {
            preferencesManager.totalXp.collect { xp ->
                _uiState.update { it.copy(totalXp = xp, currentLevel = gamificationEngine.getLevel(xp)) }
            }
        }
        viewModelScope.launch {
            preferencesManager.currentStreak.collect { _uiState.update { s -> s.copy(currentStreak = it) } }
        }
        viewModelScope.launch {
            preferencesManager.longestStreak.collect { _uiState.update { s -> s.copy(longestStreak = it) } }
        }
        viewModelScope.launch {
            gamificationEngine.observeAchievements().collect { _uiState.update { s -> s.copy(achievements = it) } }
        }
        viewModelScope.launch {
            preferencesManager.lastBackupTime.collect { time -> _uiState.update { it.copy(lastBackupTime = time) } }
        }
        viewModelScope.launch {
            preferencesManager.lastBackupSuccess.collect { success -> _uiState.update { it.copy(lastBackupSuccess = success) } }
        }
        viewModelScope.launch {
            preferencesManager.lastBackupError.collect { err -> _uiState.update { it.copy(lastBackupError = err) } }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { preferencesManager.setThemeMode(mode) }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setAmoledMode(enabled) }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setUseDynamicColor(enabled) }
    }

    fun setAccentColor(hex: String) {
        viewModelScope.launch { preferencesManager.setAccentColor(hex) }
    }

    fun setColorTheme(theme: String) {
        viewModelScope.launch { preferencesManager.setColorTheme(theme) }
    }

    fun setPomodoroDuration(minutes: Int) {
        viewModelScope.launch { preferencesManager.setPomodoroDuration(minutes) }
    }

    fun setBreakDuration(minutes: Int) {
        viewModelScope.launch { preferencesManager.setBreakDuration(minutes) }
    }

    fun setDeepWorkDuration(minutes: Int) {
        viewModelScope.launch { preferencesManager.setDeepWorkDuration(minutes) }
    }

    fun addCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            categoryRepository.insertCategory(
                CategoryEntity(name = name, colorHex = colorHex)
            )
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { categoryRepository.deleteCategory(category) }
    }

    fun showResetConfirm() { _uiState.update { it.copy(showResetConfirm = true) } }
    fun hideResetConfirm() { _uiState.update { it.copy(showResetConfirm = false) } }

    fun resetAllData() {
        viewModelScope.launch {
            taskRepository.deleteAllTasks()
            focusRepository.deleteAllSessions()
            gamificationEngine.resetGamification()
            gamificationEngine.deleteAllAchievements()
            preferencesManager.clearAll()
            _uiState.update { it.copy(showResetConfirm = false, backupMessage = "All data has been reset") }
        }
    }

    fun loadBackupHistory() {
        viewModelScope.launch {
            backupManager.rotateBackups()
            backupManager.refreshBackupHistory()
        }
    }

    fun createManualBackup() {
        viewModelScope.launch {
            val success = backupManager.performAutoBackup(isManual = true)
            if (success) {
                _uiState.update { it.copy(backupMessage = "Backup created successfully in Downloads/MustDo/Backups") }
                loadBackupHistory()
            } else {
                _uiState.update { it.copy(backupMessage = "Failed to create backup: check storage") }
            }
        }
    }

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    backupEngine.exportToZip(stream)
                }
                _uiState.update { it.copy(backupMessage = "Backup exported successfully as ZIP archive!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(backupMessage = "Export failed: ${e.message}") }
            }
        }
    }

    fun loadImportPreview(uri: Uri) {
        viewModelScope.launch {
            val info = backupManager.getBackupInfoFromUri(uri)
            if (info != null) {
                if (info.isValid) {
                    _uiState.update { it.copy(backupPreview = info) }
                } else {
                    _uiState.update { it.copy(backupMessage = "Invalid Backup: ${info.validationMessage}") }
                }
            } else {
                _uiState.update { it.copy(backupMessage = "Could not parse backup preview") }
            }
        }
    }

    fun cancelImportPreview() {
        _uiState.update { it.copy(backupPreview = null) }
    }

    private fun openInputStreamForPath(context: Context, path: String): java.io.InputStream? {
        return if (path.startsWith("content://")) {
            context.contentResolver.openInputStream(Uri.parse(path))
        } else {
            val cleanPath = if (path.startsWith("file://")) {
                Uri.parse(path).path ?: path.substring(7)
            } else {
                path
            }
            java.io.File(cleanPath).inputStream()
        }
    }

    fun confirmImportRestore(context: Context) {
        val preview = _uiState.value.backupPreview ?: return
        _uiState.update { it.copy(backupPreview = null) }
        viewModelScope.launch {
            try {
                val isZip = try {
                    if (preview.filePath.startsWith("content://")) {
                        val mimeType = context.contentResolver.getType(Uri.parse(preview.filePath))
                        if (mimeType == "application/zip" || mimeType == "application/x-zip-compressed") {
                            true
                        } else {
                            openInputStreamForPath(context, preview.filePath)?.use { stream ->
                                val header = ByteArray(4)
                                val read = stream.read(header)
                                read == 4 &&
                                        header[0] == 0x50.toByte() &&
                                        header[1] == 0x4B.toByte() &&
                                        header[2] == 0x03.toByte() &&
                                        header[3] == 0x04.toByte()
                            } ?: false
                        }
                    } else {
                        openInputStreamForPath(context, preview.filePath)?.use { stream ->
                            val header = ByteArray(4)
                            val read = stream.read(header)
                            read == 4 &&
                                    header[0] == 0x50.toByte() &&
                                    header[1] == 0x4B.toByte() &&
                                    header[2] == 0x03.toByte() &&
                                    header[3] == 0x04.toByte()
                        } ?: false
                    }
                } catch (e: Exception) {
                    preview.fileName.endsWith(".zip") || preview.filePath.contains(".zip")
                }

                val result = openInputStreamForPath(context, preview.filePath)?.use { stream ->
                    if (isZip) {
                        backupEngine.restoreFromZip(stream)
                    } else {
                        val json = stream.bufferedReader().readText()
                        backupEngine.restoreFromString(json)
                    }
                } ?: throw Exception("Could not read file contents")

                if (result.isValid) {
                    _uiState.update { it.copy(backupMessage = "Restore completed successfully!") }
                    loadBackupHistory()
                } else {
                    _uiState.update { it.copy(backupMessage = "Restore failed: ${result.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(backupMessage = "Restore failed: ${e.message}") }
            }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(backupMessage = null) } }
}
