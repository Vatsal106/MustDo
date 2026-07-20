package com.example.todo.features.dashboard.presentation

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.common.util.DateUtils
import com.example.todo.core.database.entity.*
import com.example.todo.core.datastore.UserPreferencesManager
import com.example.todo.features.focus.domain.FocusRepository
import com.example.todo.features.gamification.domain.GamificationEngine
import com.example.todo.features.tasks.domain.TaskRepository
import com.example.todo.features.settings.domain.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val greeting: String = DateUtils.getGreeting(),
    val todayTasks: List<TaskEntity> = emptyList(),
    val overdueTasks: List<TaskEntity> = emptyList(),
    val allTasks: List<TaskEntity> = emptyList(),
    val completedToday: Int = 0,
    val totalToday: Int = 0,
    val completionPercentage: Float = 0f,
    
    // Accountability & Debt
    val taskDebt: Int = 0,
    val backlogHealth: String = "Healthy",
    val unscheduledCount: Int = 0,
    val inboxHealth: String = "Healthy",
    val todayWorkload: String = "Light",
    val taskDebtTrend: List<Int> = emptyList(),
    val healthScoreTrend: List<Int> = emptyList(),
    
    // Performance Metrics
    val productivityHealthScore: Int? = null,
    val onTimeCompletionRate30Days: Int? = null,
    val missRate30Days: Int = 0,
    val completionQuality: String = "Good",
    val recoveryScore: String = "Neutral",
    val healthScoreExplanation: List<String> = emptyList(),
    
    // Weekly Review Stats
    val weeklyCompletedCount: Int = 0,
    val weeklyMissedCount: Int = 0,
    val weeklyOnTimeRate: Int? = null,
    val weeklyFocusHours: Float = 0f,
    
    // Most Delayed Task
    val mostDelayedTask: TaskEntity? = null,
    val mostDelayedDays: Int = 0,
    
    // Focus Consistency
    val focusDaysInLast7: Int = 0,
    
    // Gamification
    val currentStreak: Int = 0,
    val totalXp: Int = 0,
    val currentLevel: Int = 0,
    val xpProgress: Float = 0f,
    val xpNeededToNextLevel: Int = 100,
    val unlockedAchievements: List<AchievementEntity> = emptyList(),
    val lockedAchievements: List<AchievementEntity> = emptyList(),
    val nextAchievement: AchievementEntity? = null,
    val nextAchievementProgress: Float = 0f,
    
    val motivationalQuote: String = "",
    val isLoading: Boolean = true,

    // Task details dialog state
    val categories: List<CategoryEntity> = emptyList(),
    val taskResourcesMap: Map<String, List<TaskResourceEntity>> = emptyMap(),
    val taskNoteBlocksMap: Map<String, List<TaskNoteBlockEntity>> = emptyMap(),
    val selectedTaskDetails: TaskEntity? = null,
    val selectedTaskResources: List<TaskResourceEntity> = emptyList(),
    val selectedTaskNoteBlocks: List<TaskNoteBlockEntity> = emptyList(),
    
    // Media Player Playback state inside details dialog
    val currentlyPlayingFilePath: String? = null,
    val isPlaying: Boolean = false,
    val playbackPositionMillis: Long = 0L,
    val playbackDurationMillis: Long = 0L,

    // Global Search
    val searchQuery: String = "",
    val searchResults: List<TaskEntity> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val focusRepository: FocusRepository,
    private val gamificationEngine: GamificationEngine,
    private val preferencesManager: UserPreferencesManager,
    private val categoryRepository: CategoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    private val quotes = listOf(
        "The secret of getting ahead is getting started.",
        "Focus on being productive instead of busy.",
        "Don't watch the clock; do what it does. Keep going.",
        "Small daily improvements over time lead to stunning results.",
        "Your future is created by what you do today.",
        "Progress, not perfection.",
        "One task at a time. One day at a time.",
        "Discipline is choosing between what you want now and what you want most.",
        "The way to get started is to quit talking and begin doing.",
        "It's not about having time, it's about making time.",
        "Consistency is the true foundation of trust.",
        "Start where you are. Use what you have. Do what you can.",
        "Action is the foundational key to all success.",
        "You don't have to be great to start, but you have to start to be great.",
        "Every accomplishment starts with the decision to try."
    )

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        val now = System.currentTimeMillis()
        val todayStart = DateUtils.startOfDay(now)
        val todayEnd = DateUtils.endOfDay(now)
        val thirtyDaysAgo = todayStart - (30L * 24 * 60 * 60 * 1000)
        val sevenDaysAgo = todayStart - (7L * 24 * 60 * 60 * 1000)

        // Get start of the current week (Sunday start)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = todayStart
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        val startOfWeek = calendar.timeInMillis

        // Launch categories observation
        viewModelScope.launch {
            categoryRepository.observeAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }

        // Launch resources observation
        viewModelScope.launch {
            taskRepository.observeAllTaskResources().collect { resources ->
                val grouped = resources.groupBy { it.taskId }
                _uiState.update { it.copy(taskResourcesMap = grouped) }
            }
        }

        // Launch note blocks observation
        viewModelScope.launch {
            taskRepository.observeAllTaskNoteBlocks().collect { blocks ->
                val grouped = blocks.groupBy { it.taskId }
                _uiState.update { it.copy(taskNoteBlocksMap = grouped) }
            }
        }

        viewModelScope.launch {
            combine(
                taskRepository.observeAllTasks(),
                focusRepository.observeCompletedSessions(),
                preferencesManager.currentStreak,
                preferencesManager.totalXp,
                gamificationEngine.observeAchievements()
            ) { allTasks, focusSessions, streak, xp, achievements ->
                val currentTime = System.currentTimeMillis()
                val calculated = com.example.todo.features.dashboard.domain.DashboardAnalyticsCalculator.calculate(
                    allTasks = allTasks,
                    focusSessions = focusSessions,
                    streak = streak,
                    xp = xp,
                    achievements = achievements,
                    now = currentTime,
                    motivationalQuote = QuotesOfDay()
                )
                // Retain current UI State dialog states, categories, and resources maps
                calculated.copy(
                    categories = _uiState.value.categories,
                    taskResourcesMap = _uiState.value.taskResourcesMap,
                    taskNoteBlocksMap = _uiState.value.taskNoteBlocksMap,
                    selectedTaskDetails = _uiState.value.selectedTaskDetails,
                    selectedTaskResources = _uiState.value.selectedTaskResources,
                    selectedTaskNoteBlocks = _uiState.value.selectedTaskNoteBlocks,
                    currentlyPlayingFilePath = _uiState.value.currentlyPlayingFilePath,
                    isPlaying = _uiState.value.isPlaying,
                    playbackPositionMillis = _uiState.value.playbackPositionMillis,
                    playbackDurationMillis = _uiState.value.playbackDurationMillis
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun QuotesOfDay(): String {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return quotes[dayOfYear % quotes.size]
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            val task = taskRepository.completeTask(taskId)
            if (task != null) {
                gamificationEngine.awardTaskCompletionXp(task)
                val totalCompleted = taskRepository.getTotalCompletedCount()
                gamificationEngine.checkFirstTaskAchievement(totalCompleted)
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
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            taskRepository.searchTasks(query).collect { results ->
                if (_uiState.value.searchQuery == query) {
                    _uiState.update { it.copy(searchResults = results) }
                }
            }
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
            val newContent = if (isChecked) {
                "[ ] " + currentContent.substring(4)
            } else {
                "[x] " + currentContent.substring(4)
            }
            val block = taskRepository.getTaskNoteBlocks(_uiState.value.selectedTaskDetails?.id ?: "").find { it.id == blockId }
            if (block != null) {
                taskRepository.updateTaskNoteBlock(block.copy(content = newContent))
                // Refresh local state inside details dialog
                val updatedBlocks = taskRepository.getTaskNoteBlocks(_uiState.value.selectedTaskDetails?.id ?: "")
                _uiState.update { it.copy(selectedTaskNoteBlocks = updatedBlocks) }

                // Check if all checklist items are now checked
                val checklistBlocks = updatedBlocks.filter { it.blockType == "CHECKLIST" }
                val allChecked = checklistBlocks.isNotEmpty() && checklistBlocks.all { it.content.startsWith("[x] ") }
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

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
