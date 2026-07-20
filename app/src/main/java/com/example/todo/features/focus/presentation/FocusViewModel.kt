package com.example.todo.features.focus.presentation

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.core.database.entity.FocusSessionEntity
import com.example.todo.core.database.entity.SessionType
import com.example.todo.core.datastore.UserPreferencesManager
import com.example.todo.features.focus.domain.FocusRepository
import com.example.todo.features.gamification.domain.GamificationEngine
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusUiState(
    val sessionType: String = SessionType.POMODORO.name,
    val totalDurationMinutes: Int = 25,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val sessionsCompleted: Int = 0,
    val totalFocusMinutes: Int = 0,
    val todayFocusMinutes: Int = 0,
    val progress: Float = 0f,
    val pomodoroDuration: Int = 25,
    val breakDuration: Int = 5,
    val deepWorkDuration: Int = 90,
    val isBreak: Boolean = false,
    val endTimeDisplay: String? = null,
    val isSoundPlaying: Boolean = false,
    
    // Task Linking
    val linkedTaskId: String? = null,
    val linkedTask: com.example.todo.core.database.entity.TaskEntity? = null,
    val pendingTasks: List<com.example.todo.core.database.entity.TaskEntity> = emptyList(),
    val showTaskSelector: Boolean = false
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val focusRepository: FocusRepository,
    private val taskRepository: com.example.todo.features.tasks.domain.TaskRepository,
    private val gamificationEngine: GamificationEngine,
    private val preferencesManager: UserPreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var mediaPlayer: android.media.MediaPlayer? = null

    private var countDownTimer: CountDownTimer? = null

    init {
        loadStats()
        loadSettings()
    }

    private fun loadStats() {
        viewModelScope.launch {
            focusRepository.observeTotalSessionCount().collect { count ->
                _uiState.update { it.copy(sessionsCompleted = count) }
            }
        }
        viewModelScope.launch {
            focusRepository.observeTotalFocusMinutes().collect { minutes ->
                _uiState.update { it.copy(totalFocusMinutes = minutes ?: 0) }
            }
        }
        viewModelScope.launch {
            focusRepository.observeFocusMinutesForDate(System.currentTimeMillis()).collect { minutes ->
                _uiState.update { it.copy(todayFocusMinutes = minutes ?: 0) }
            }
        }
        viewModelScope.launch {
            taskRepository.observeActivePendingTasks().collect { tasks ->
                _uiState.update { state ->
                    val linkedId = state.linkedTaskId
                    val task = if (linkedId != null) tasks.find { t -> t.id == linkedId } else null
                    val estimated = task?.estimatedMinutes
                    val newDuration = estimated ?: if (state.sessionType == SessionType.DEEP_WORK.name) state.deepWorkDuration else state.pomodoroDuration
                    state.copy(
                        pendingTasks = tasks,
                        linkedTask = task,
                        totalDurationMinutes = if (!state.isRunning && linkedId != null && estimated != null) newDuration else state.totalDurationMinutes,
                        remainingSeconds = if (!state.isRunning && linkedId != null && estimated != null) newDuration * 60 else state.remainingSeconds
                    )
                }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            preferencesManager.pomodoroDuration.collect { duration ->
                _uiState.update {
                    it.copy(
                        pomodoroDuration = duration,
                        totalDurationMinutes = if (it.sessionType == SessionType.POMODORO.name && !it.isRunning) duration else it.totalDurationMinutes,
                        remainingSeconds = if (it.sessionType == SessionType.POMODORO.name && !it.isRunning) duration * 60 else it.remainingSeconds
                    )
                }
            }
        }
        viewModelScope.launch {
            preferencesManager.breakDuration.collect { duration ->
                _uiState.update { it.copy(breakDuration = duration) }
            }
        }
        viewModelScope.launch {
            preferencesManager.deepWorkDuration.collect { duration ->
                _uiState.update {
                    it.copy(
                        deepWorkDuration = duration,
                        totalDurationMinutes = if (it.sessionType == SessionType.DEEP_WORK.name && !it.isRunning) duration else it.totalDurationMinutes,
                        remainingSeconds = if (it.sessionType == SessionType.DEEP_WORK.name && !it.isRunning) duration * 60 else it.remainingSeconds
                    )
                }
            }
        }
    }

    fun setSessionType(type: String) {
        if (_uiState.value.isRunning) return
        val duration = when (type) {
            SessionType.POMODORO.name -> _uiState.value.pomodoroDuration
            SessionType.DEEP_WORK.name -> _uiState.value.deepWorkDuration
            else -> 25
        }
        _uiState.update {
            it.copy(
                sessionType = type,
                isBreak = false,
                totalDurationMinutes = duration,
                remainingSeconds = duration * 60,
                isCompleted = false,
                progress = 0f,
                endTimeDisplay = null
            )
        }
    }

    fun startSession() {
        stopSound()
        val state = _uiState.value
        if (state.isRunning && !state.isPaused) return

        _uiState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                isCompleted = false,
                endTimeDisplay = calculateEndTime(it.remainingSeconds)
            )
        }

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(
            _uiState.value.remainingSeconds * 1000L,
            1000L
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = (millisUntilFinished / 1000).toInt()
                val total = _uiState.value.totalDurationMinutes * 60
                val progress = 1f - (remaining.toFloat() / total.coerceAtLeast(1))
                _uiState.update {
                    it.copy(
                        remainingSeconds = remaining,
                        progress = progress,
                        endTimeDisplay = calculateEndTime(remaining)
                    )
                }
            }

            override fun onFinish() {
                completeSession()
            }
        }.start()
    }

    fun pauseSession() {
        stopSound()
        countDownTimer?.cancel()
        _uiState.update { it.copy(isPaused = true, endTimeDisplay = null) }
    }

    fun resumeSession() {
        startSession()
    }

    fun resetSession() {
        stopSound()
        countDownTimer?.cancel()
        val state = _uiState.value
        _uiState.update {
            if (state.isCompleted) {
                if (state.isBreak) {
                    // Transition back to work session
                    val focusDuration = if (state.sessionType == SessionType.DEEP_WORK.name) state.deepWorkDuration else state.pomodoroDuration
                    state.copy(
                        isBreak = false,
                        isRunning = false,
                        isPaused = false,
                        isCompleted = false,
                        totalDurationMinutes = focusDuration,
                        remainingSeconds = focusDuration * 60,
                        progress = 0f,
                        endTimeDisplay = null
                    )
                } else {
                    // Transition to break session
                    val breakMin = state.breakDuration
                    state.copy(
                        isBreak = true,
                        isRunning = false,
                        isPaused = false,
                        isCompleted = false,
                        totalDurationMinutes = breakMin,
                        remainingSeconds = breakMin * 60,
                        progress = 0f,
                        endTimeDisplay = null
                    )
                }
            } else {
                // Regular reset of the current running/paused timer duration
                val duration = state.totalDurationMinutes
                state.copy(
                    remainingSeconds = duration * 60,
                    isRunning = false,
                    isPaused = false,
                    isCompleted = false,
                    progress = 0f,
                    endTimeDisplay = null
                )
            }
        }
    }

    fun completeSessionReset() {
        stopSound()
        countDownTimer?.cancel()
        viewModelScope.launch {
            val pomodoro = _uiState.value.pomodoroDuration
            _uiState.update {
                it.copy(
                    sessionType = SessionType.POMODORO.name,
                    isBreak = false,
                    totalDurationMinutes = pomodoro,
                    remainingSeconds = pomodoro * 60,
                    isRunning = false,
                    isPaused = false,
                    isCompleted = false,
                    progress = 0f,
                    endTimeDisplay = null
                )
            }
        }
    }

    fun skipBreak() {
        stopSound()
        val state = _uiState.value
        if (!state.isBreak) return
        countDownTimer?.cancel()

        val focusDuration = if (state.sessionType == SessionType.DEEP_WORK.name) state.deepWorkDuration else state.pomodoroDuration
        _uiState.update {
            it.copy(
                isBreak = false,
                isRunning = false,
                isPaused = false,
                isCompleted = false,
                totalDurationMinutes = focusDuration,
                remainingSeconds = focusDuration * 60,
                progress = 0f,
                endTimeDisplay = null
            )
        }
    }

    fun updateDuration(minutes: Int) {
        val state = _uiState.value
        countDownTimer?.cancel()

        viewModelScope.launch {
            if (state.isBreak) {
                preferencesManager.setBreakDuration(minutes)
                _uiState.update {
                    it.copy(
                        breakDuration = minutes,
                        totalDurationMinutes = minutes,
                        remainingSeconds = minutes * 60,
                        progress = 0f,
                        isRunning = false,
                        isPaused = false,
                        isCompleted = false,
                        endTimeDisplay = null
                    )
                }
            } else if (state.sessionType == SessionType.DEEP_WORK.name) {
                preferencesManager.setDeepWorkDuration(minutes)
                _uiState.update {
                    it.copy(
                        deepWorkDuration = minutes,
                        totalDurationMinutes = minutes,
                        remainingSeconds = minutes * 60,
                        progress = 0f,
                        isRunning = false,
                        isPaused = false,
                        isCompleted = false,
                        endTimeDisplay = null
                    )
                }
            } else {
                preferencesManager.setPomodoroDuration(minutes)
                _uiState.update {
                    it.copy(
                        pomodoroDuration = minutes,
                        totalDurationMinutes = minutes,
                        remainingSeconds = minutes * 60,
                        progress = 0f,
                        isRunning = false,
                        isPaused = false,
                        isCompleted = false,
                        endTimeDisplay = null
                    )
                }
            }
        }
    }

    fun adjustDuration(minutesDelta: Int) {
        val state = _uiState.value
        val newDuration = (state.totalDurationMinutes + minutesDelta).coerceAtLeast(1)
        val deltaSeconds = minutesDelta * 60
        val newRemaining = (state.remainingSeconds + deltaSeconds).coerceAtLeast(0)

        viewModelScope.launch {
            if (state.isBreak) {
                preferencesManager.setBreakDuration(newDuration)
            } else if (state.sessionType == SessionType.DEEP_WORK.name) {
                preferencesManager.setDeepWorkDuration(newDuration)
            } else {
                preferencesManager.setPomodoroDuration(newDuration)
            }
        }

        _uiState.update {
            it.copy(
                totalDurationMinutes = newDuration,
                remainingSeconds = newRemaining,
                progress = if (newDuration > 0) 1f - (newRemaining.toFloat() / (newDuration * 60)) else 0f,
                endTimeDisplay = if (it.isRunning && !it.isPaused) calculateEndTime(newRemaining) else null
            )
        }

        if (state.isRunning && !state.isPaused) {
            countDownTimer?.cancel()
            startSession()
        }
    }

    private fun calculateEndTime(remainingSeconds: Int): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.SECOND, remainingSeconds)
        return java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(calendar.time)
    }

    private fun playCompletionSound() {
        try {
            val resId = context.resources.getIdentifier("focus_finished", "raw", context.packageName)
            if (resId != 0) {
                mediaPlayer?.release()
                mediaPlayer = android.media.MediaPlayer.create(context, resId).apply {
                    setOnCompletionListener { mp ->
                        mp.release()
                        if (mediaPlayer == mp) {
                            mediaPlayer = null
                            _uiState.update { it.copy(isSoundPlaying = false) }
                        }
                    }
                    start()
                }
                _uiState.update { it.copy(isSoundPlaying = true) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun completeSession() {
        val state = _uiState.value
        if (state.isBreak) {
            _uiState.update {
                it.copy(
                    isRunning = false,
                    isPaused = false,
                    isCompleted = true,
                    remainingSeconds = 0,
                    progress = 1f,
                    endTimeDisplay = null
                )
            }
            playCompletionSound()
            return
        }

        _uiState.update {
            it.copy(
                isRunning = false,
                isPaused = false,
                isCompleted = true,
                remainingSeconds = 0,
                progress = 1f,
                endTimeDisplay = null
            )
        }

        playCompletionSound()

        viewModelScope.launch {
            val session = FocusSessionEntity(
                sessionType = state.sessionType,
                durationMinutes = state.totalDurationMinutes,
                isCompleted = true,
                taskId = state.linkedTaskId
            )
            focusRepository.insertSession(session)
            gamificationEngine.awardFocusSessionXp(state.totalDurationMinutes)

            // Complete task automatically if linked and estimated time matches/exceeds focused duration
            val linkedTask = state.linkedTask
            if (linkedTask != null) {
                val estimated = linkedTask.estimatedMinutes
                if (estimated != null && state.totalDurationMinutes >= estimated) {
                    val completedTask = taskRepository.completeTask(linkedTask.id)
                    if (completedTask != null) {
                        gamificationEngine.awardTaskCompletionXp(completedTask)
                        val totalCompleted = taskRepository.getTotalCompletedCount()
                        gamificationEngine.checkFirstTaskAchievement(totalCompleted)
                    }
                }
            }
            updateWidgets()
        }
    }

    private fun updateWidgets() {
        val intent = android.content.Intent("com.example.todo.action.UPDATE_WIDGETS")
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)
    }

    fun stopSound() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _uiState.update { it.copy(isSoundPlaying = false) }
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
        stopSound()
    }

    fun selectTask(taskId: String?) {
        val task = _uiState.value.pendingTasks.find { it.id == taskId }
        _uiState.update { state ->
            val estimated = task?.estimatedMinutes
            val newDuration = estimated ?: if (state.sessionType == SessionType.DEEP_WORK.name) state.deepWorkDuration else state.pomodoroDuration
            state.copy(
                linkedTaskId = taskId,
                linkedTask = task,
                showTaskSelector = false,
                totalDurationMinutes = if (!state.isRunning) newDuration else state.totalDurationMinutes,
                remainingSeconds = if (!state.isRunning) newDuration * 60 else state.remainingSeconds
            )
        }
    }

    fun setShowTaskSelector(show: Boolean) {
        _uiState.update { it.copy(showTaskSelector = show) }
    }
}
