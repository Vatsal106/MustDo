package com.example.todo.features.tasks.presentation

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.common.util.capitalizeFirstLetter
import com.example.todo.core.database.entity.*
import com.example.todo.core.datastore.UserPreferencesManager
import com.example.todo.features.gamification.domain.GamificationEngine
import com.example.todo.features.settings.domain.CategoryRepository
import com.example.todo.features.tasks.domain.TaskRepository
import com.example.todo.core.backup.BackupManager
import com.example.todo.core.backup.BackupEngine
import com.example.todo.core.backup.BackupInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val selectedCategoryId: String? = null,
    val sortOption: SortOption = SortOption.DUE_DATE,
    val isLoading: Boolean = true,
    val showRestorePrompt: Boolean = false,
    val backupTimestamp: Long? = null,
    val restoreErrorMessage: String? = null,

    // Task details dialog state
    val selectedTaskDetails: TaskEntity? = null,
    val selectedTaskResources: List<TaskResourceEntity> = emptyList(),
    val selectedTaskNoteBlocks: List<TaskNoteBlockEntity> = emptyList(),
    val taskResourcesMap: Map<String, List<TaskResourceEntity>> = emptyMap(),
    val taskNoteBlocksMap: Map<String, List<TaskNoteBlockEntity>> = emptyMap(),

    // Media Player Playback state inside details dialog
    val currentlyPlayingFilePath: String? = null,
    val isPlaying: Boolean = false,
    val playbackPositionMillis: Long = 0L,
    val playbackDurationMillis: Long = 0L
)

/**
 * One-shot event emitted when a task is completed, carrying all data
 * needed for the completion animation overlays (XP chip, streak banner, achievement sheet).
 */
data class CompletionEvent(
    val taskId: String,
    val xpAwarded: Int,
    val newStreak: Int? = null,         // Non-null only when streak increased
    val unlockedAchievement: AchievementEntity? = null
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val gamificationEngine: GamificationEngine,
    private val preferencesManager: UserPreferencesManager,
    private val backupManager: BackupManager,
    private val backupEngine: BackupEngine,
    private val reminderDao: com.example.todo.core.database.dao.ReminderDao,
    private val reminderScheduler: com.example.todo.core.notification.ReminderScheduler,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    // One-shot completion animation events
    private val _completionEvents = MutableSharedFlow<CompletionEvent>(extraBufferCapacity = 1)
    val completionEvents: SharedFlow<CompletionEvent> = _completionEvents.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _filter = MutableStateFlow(TaskFilter.ALL)
    private val _categoryFilter = MutableStateFlow<String?>(null)
    private val _sortOption = MutableStateFlow(SortOption.DUE_DATE)

    private var detectedBackupInfo: BackupInfo? = null
    private var mediaPlayer: MediaPlayer? = null

    init {
        loadData()
        checkForBackup()
    }

    private fun loadData() {
        // Categories
        viewModelScope.launch {
            categoryRepository.observeAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }

        // Resources
        viewModelScope.launch {
            taskRepository.observeAllTaskResources().collect { resources ->
                val grouped = resources.groupBy { it.taskId }
                _uiState.update { it.copy(taskResourcesMap = grouped) }
            }
        }

        // Note blocks
        viewModelScope.launch {
            taskRepository.observeAllTaskNoteBlocks().collect { blocks ->
                val grouped = blocks.groupBy { it.taskId }
                _uiState.update { it.copy(taskNoteBlocksMap = grouped) }
            }
        }

        // Tasks with reactive filters
        viewModelScope.launch {
            combine(
                _searchQuery,
                _filter,
                _categoryFilter,
                _sortOption
            ) { query, filter, categoryId, sort ->
                FilterParams(query, filter, categoryId, sort)
            }.collectLatest { params ->
                val tasksFlow = when {
                    params.query.isNotBlank() -> taskRepository.searchTasks(params.query)
                    params.categoryId != null -> taskRepository.observeTasksByCategory(params.categoryId)
                    params.filter == TaskFilter.ALL -> taskRepository.observeAllActiveTasks()
                    else -> taskRepository.observeTasksByStatus(params.filter.name)
                }

                tasksFlow.collect { tasks ->
                    val sorted = sortTasks(tasks, params.sort)
                    _uiState.update {
                        it.copy(
                            tasks = sorted,
                            searchQuery = params.query,
                            selectedFilter = params.filter,
                            selectedCategoryId = params.categoryId,
                            sortOption = params.sort,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
        _categoryFilter.value = null
    }

    fun setCategoryFilter(categoryId: String?) {
        _categoryFilter.value = categoryId
        if (categoryId != null) {
            _filter.value = TaskFilter.ALL
        }
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            // Read streak before completion
            val streakBefore = preferencesManager.currentStreak.first()

            val task = taskRepository.completeTask(taskId)
            if (task != null) {
                val xpAwarded = gamificationEngine.awardTaskCompletionXp(task)
                val totalCompleted = taskRepository.getTotalCompletedCount()
                val unlockedAchievement = gamificationEngine.checkFirstTaskAchievement(totalCompleted)

                // Read streak after completion
                val streakAfter = preferencesManager.currentStreak.first()
                val newStreak = if (streakAfter > streakBefore) streakAfter else null

                // Also check general achievements
                val generalUnlocked = gamificationEngine.checkAchievements()
                val achievement = unlockedAchievement ?: generalUnlocked.firstOrNull()

                // Emit completion event for UI animations
                _completionEvents.tryEmit(
                    CompletionEvent(
                        taskId = taskId,
                        xpAwarded = xpAwarded,
                        newStreak = newStreak,
                        unlockedAchievement = achievement
                    )
                )

                updateWidgets()
            }
        }
    }

    fun quickAddTask(title: String, dueDateMillis: Long?, priority: Priority, categoryId: String?) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                dueDateMillis = dueDateMillis,
                priority = priority.name,
                categoryId = categoryId
            )
            taskRepository.insertTask(task)
            updateWidgets()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
            updateWidgets()
        }
    }

    fun archiveTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.archiveTask(taskId)
        }
    }

    fun markTaskAsUndone(taskId: String) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            taskRepository.updateTask(task.copy(
                status = TaskStatus.PENDING.name,
                completedDateMillis = null
            ))

            // Re-enable and reschedule reminders
            val reminders = reminderDao.getByTaskId(taskId)
            reminders.forEach { reminder ->
                val updated = reminder.copy(isEnabled = true)
                reminderDao.update(updated)
                reminderScheduler.scheduleReminder(updated)
            }
            updateWidgets()
        }
    }

    private fun updateWidgets() {
        val intent = android.content.Intent("com.example.todo.action.UPDATE_WIDGETS")
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)
    }

    fun changeTaskCategory(taskId: String, categoryId: String?) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            taskRepository.updateTask(task.copy(categoryId = categoryId))
        }
    }

    fun unarchiveTask(taskId: String) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            val newStatus = if (task.completedDateMillis != null) TaskStatus.COMPLETED.name else TaskStatus.PENDING.name
            taskRepository.updateTask(task.copy(status = newStatus))
        }
    }

    fun updateTaskNotes(taskId: String, notes: String) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            taskRepository.updateTask(task.copy(notes = notes.capitalizeFirstLetter()))
        }
    }

    // --- Details Dialog Operations ---
    fun showTaskDetails(task: TaskEntity) {
        viewModelScope.launch {
            val resources = taskRepository.getTaskResources(task.id)
            val noteBlocks = taskRepository.getTaskNoteBlocks(task.id)
            _uiState.update {
                it.copy(
                    selectedTaskDetails = task,
                    selectedTaskResources = resources,
                    selectedTaskNoteBlocks = noteBlocks
                )
            }
        }
    }

    fun hideTaskDetails() {
        stopVoiceNotePlayback()
        _uiState.update {
            it.copy(
                selectedTaskDetails = null,
                selectedTaskResources = emptyList(),
                selectedTaskNoteBlocks = emptyList()
            )
        }
    }

    fun toggleDetailsChecklistBlock(blockId: String, currentContent: String) {
        viewModelScope.launch {
            val isChecked = currentContent.startsWith("[x] ")
            val textVal = if (isChecked) currentContent.removePrefix("[x] ") else currentContent.removePrefix("[ ] ")
            val prefix = if (!isChecked) "[x] " else "[ ] "
            val newContent = prefix + textVal

            _uiState.update { state ->
                state.copy(
                    selectedTaskNoteBlocks = state.selectedTaskNoteBlocks.map {
                        if (it.id == blockId) it.copy(content = newContent) else it
                    }
                )
            }

            val block = taskRepository.getTaskNoteBlocks(_uiState.value.selectedTaskDetails?.id ?: "").find { it.id == blockId }
            if (block != null) {
                taskRepository.updateTaskNoteBlock(block.copy(content = newContent))
                
                // Check if all checklist items are now checked
                val allBlocks = taskRepository.getTaskNoteBlocks(block.taskId)
                val checklistBlocks = allBlocks.filter { it.blockType == "CHECKLIST" }
                val allChecked = checklistBlocks.isNotEmpty() && checklistBlocks.all { 
                    if (it.id == blockId) !isChecked else it.content.startsWith("[x] ")
                }
                if (allChecked) {
                    val task = taskRepository.completeTask(block.taskId)
                    if (task != null) {
                        gamificationEngine.awardTaskCompletionXp(task)
                        val totalCompleted = taskRepository.getTotalCompletedCount()
                        gamificationEngine.checkFirstTaskAchievement(totalCompleted)
                    }
                }
            }
        }
    }

    // --- Voice Note Playback inside Details Dialog ---
    fun playVoiceNote(filePath: String) {
        viewModelScope.launch {
            if (_uiState.value.currentlyPlayingFilePath == filePath && _uiState.value.isPlaying) {
                mediaPlayer?.pause()
                _uiState.update { it.copy(isPlaying = false) }
            } else if (_uiState.value.currentlyPlayingFilePath == filePath && !_uiState.value.isPlaying) {
                mediaPlayer?.start()
                _uiState.update { it.copy(isPlaying = true) }
                observePlaybackPosition()
            } else {
                val file = java.io.File(filePath)
                if (!file.exists()) {
                    android.widget.Toast.makeText(context, "Audio file not found on device", android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }
                try {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(filePath)
                        prepare()
                        start()
                        setOnCompletionListener {
                            _uiState.update { it.copy(isPlaying = false, currentlyPlayingFilePath = null, playbackPositionMillis = 0L) }
                        }
                    }
                    _uiState.update {
                        it.copy(
                            currentlyPlayingFilePath = filePath,
                            isPlaying = true,
                            playbackDurationMillis = mediaPlayer?.duration?.toLong() ?: 0L,
                            playbackPositionMillis = 0L
                        )
                    }
                    observePlaybackPosition()
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Error playing audio file", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observePlaybackPosition() {
        viewModelScope.launch {
            while (mediaPlayer != null && _uiState.value.isPlaying && _uiState.value.currentlyPlayingFilePath != null) {
                val currentPos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                _uiState.update { it.copy(playbackPositionMillis = currentPos) }
                delay(200)
            }
        }
    }

    fun seekVoiceNote(positionMillis: Long) {
        mediaPlayer?.seekTo(positionMillis.toInt())
        _uiState.update { it.copy(playbackPositionMillis = positionMillis) }
    }

    fun stopVoiceNotePlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _uiState.update { it.copy(isPlaying = false, currentlyPlayingFilePath = null) }
    }

    fun checkForBackup() {
        viewModelScope.launch {
            val tasks = taskRepository.getAllTasks()
            if (tasks.isEmpty()) {
                val history = backupManager.getBackupHistory()
                val latest = history.firstOrNull()
                if (latest != null && latest.isValid) {
                    detectedBackupInfo = latest
                    _uiState.update {
                        it.copy(
                            showRestorePrompt = true,
                            backupTimestamp = latest.createdAt
                        )
                    }
                }
            }
        }
    }

    fun restoreBackup() {
        viewModelScope.launch {
            val info = detectedBackupInfo
            if (info != null) {
                val result = backupEngine.restoreFromFilePath(info.filePath)
                dismissRestorePrompt()
                if (result.isValid) {
                    Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_LONG).show()
                } else {
                    _uiState.update { it.copy(restoreErrorMessage = "Restore failed: ${result.message}") }
                }
            }
        }
    }

    fun clearRestoreErrorMessage() {
        _uiState.update { it.copy(restoreErrorMessage = null) }
    }

    fun dismissRestorePrompt() {
        _uiState.update {
            it.copy(
                showRestorePrompt = false,
                backupTimestamp = null
            )
        }
        detectedBackupInfo = null
    }

    private fun sortTasks(tasks: List<TaskEntity>, sort: SortOption): List<TaskEntity> {
        return when (sort) {
            SortOption.DUE_DATE -> tasks.sortedWith(
                compareBy { it.dueDateMillis ?: Long.MAX_VALUE }
            )
            SortOption.PRIORITY -> tasks.sortedWith(
                compareByDescending { priorityWeight(it.priority) }
            )
            SortOption.CREATED -> tasks.sortedByDescending { it.createdDateMillis }
            SortOption.TITLE -> tasks.sortedBy { it.title.lowercase() }
            SortOption.CUSTOM -> tasks.sortedBy { it.orderIndex }
        }
    }

    private fun priorityWeight(priority: String): Int = when (priority) {
        "URGENT" -> 4
        "HIGH" -> 3
        "MEDIUM" -> 2
        "LOW" -> 1
        else -> 0
    }
    
    fun reorderTasks(fromIndex: Int, toIndex: Int) {
        if (_sortOption.value != SortOption.CUSTOM) {
            _sortOption.value = SortOption.CUSTOM
        }
        
        val currentTasks = _uiState.value.tasks.toMutableList()
        if (fromIndex in currentTasks.indices && toIndex in currentTasks.indices) {
            val task = currentTasks.removeAt(fromIndex)
            currentTasks.add(toIndex, task)
            
            val updatedTasks = currentTasks.mapIndexed { index, t ->
                t.copy(orderIndex = index)
            }
            
            _uiState.update { it.copy(tasks = updatedTasks) }
            
            viewModelScope.launch {
                taskRepository.updateTasks(updatedTasks)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(800)
            // Just reloading by re-triggering checkForBackup
            checkForBackup()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }

    private data class FilterParams(
        val query: String,
        val filter: TaskFilter,
        val categoryId: String?,
        val sort: SortOption
    )
}

enum class SortOption(val label: String) {
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    CREATED("Date Created"),
    TITLE("Title"),
    CUSTOM("Custom Order")
}

enum class TaskFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    ARCHIVED("Archived")
}
