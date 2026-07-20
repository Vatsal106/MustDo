package com.example.todo.features.calendar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.common.util.DateUtils
import com.example.todo.core.database.entity.*
import com.example.todo.core.datastore.UserPreferencesManager
import com.example.todo.features.focus.domain.FocusRepository
import com.example.todo.features.gamification.domain.GamificationEngine
import com.example.todo.features.settings.domain.CategoryRepository
import com.example.todo.features.tasks.domain.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class CalendarMode {
    MONTH, WEEK, AGENDA, INSIGHTS
}

data class DayTasksSummary(
    val total: Int = 0,
    val completed: Int = 0,
    val urgentCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0,
    val hasOverdue: Boolean = false,
    val categoryColors: Set<String> = emptySet()
)

data class CalendarUiState(
    val selectedDayMillis: Long = DateUtils.startOfDay(),
    val calendarMode: CalendarMode = CalendarMode.MONTH,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH), // 0-indexed
    val searchQuery: String = "",
    val activeFilters: CalendarFilters = CalendarFilters(),
    
    // Loaded data
    val tasks: List<TaskEntity> = emptyList(),
    val filteredTasks: List<TaskEntity> = emptyList(),
    val unscheduledTasks: List<TaskEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val taskResourcesMap: Map<String, List<TaskResourceEntity>> = emptyMap(),
    
    // Aggregated states for calendar modes
    val taskSummaryByDay: Map<Long, DayTasksSummary> = emptyMap(), // dayMillis -> summary
    val overlappingTaskIds: Set<String> = emptySet(),
    val selectedTaskIds: Set<String> = emptySet(),
    
    // Undo Cache
    val lastMovedTaskOriginalState: Pair<String, Long?>? = null, // taskId -> originalDueDateMillis
    
    // Analytics
    val analytics: CalendarAnalyticsData = CalendarAnalyticsData(),
    
    val isLoading: Boolean = true
)

data class CalendarFilters(
    val statuses: Set<String> = emptySet(),
    val priorities: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
    val recurrence: Set<String> = emptySet(),
    val dueStates: Set<String> = emptySet()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val focusRepository: FocusRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: UserPreferencesManager,
    private val analyticsManager: CalendarAnalyticsManager,
    private val gamificationEngine: GamificationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadPreferences()
        observeData()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            combine(
                preferencesManager.calendarMode,
                preferencesManager.calendarFilterStatus,
                preferencesManager.calendarFilterPriority,
                preferencesManager.calendarFilterCategories,
                preferencesManager.calendarFilterRecurrence,
                preferencesManager.calendarFilterDueState
            ) { array ->
                val mode = array[0] as String
                val statuses = array[1] as Set<String>
                val priorities = array[2] as Set<String>
                val categories = array[3] as Set<String>
                val recurrence = array[4] as Set<String>
                val dueStates = array[5] as Set<String>

                val calendarMode = try {
                    CalendarMode.valueOf(mode)
                } catch (e: Exception) {
                    CalendarMode.MONTH
                }
                CalendarFilters(
                    statuses = statuses,
                    priorities = priorities,
                    categories = categories,
                    recurrence = recurrence,
                    dueStates = dueStates
                ) to calendarMode
            }.collect { (filters, mode) ->
                _uiState.update { it.copy(calendarMode = mode, activeFilters = filters) }
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                taskRepository.observeAllTasks(),
                taskRepository.observeUnscheduledTasks(),
                categoryRepository.observeAllCategories(),
                focusRepository.observeCompletedSessions(),
                taskRepository.observeAllTaskResources(),
                _searchQuery,
                preferencesManager.calendarFilterStatus,
                preferencesManager.calendarFilterPriority,
                preferencesManager.calendarFilterCategories,
                preferencesManager.calendarFilterRecurrence,
                preferencesManager.calendarFilterDueState
            ) { array ->
                val allTasks = array[0] as List<TaskEntity>
                val unscheduled = array[1] as List<TaskEntity>
                val categories = array[2] as List<CategoryEntity>
                val sessions = array[3] as List<FocusSessionEntity>
                val resources = array[4] as List<TaskResourceEntity>
                val query = array[5] as String
                val filterStatuses = array[6] as Set<String>
                val filterPriorities = array[7] as Set<String>
                val filterCategories = array[8] as Set<String>
                val filterRecurrence = array[9] as Set<String>
                val filterDueStates = array[10] as Set<String>

                val filters = CalendarFilters(
                    statuses = filterStatuses,
                    priorities = filterPriorities,
                    categories = filterCategories,
                    recurrence = filterRecurrence,
                    dueStates = filterDueStates
                )
                
                // 1. Filter tasks based on Search Query and Calendar Filters
                val filtered = allTasks.filter { task ->
                    // Search query
                    val matchesQuery = query.isBlank() || 
                            task.title.contains(query, ignoreCase = true) || 
                            task.description.contains(query, ignoreCase = true) ||
                            task.notes.contains(query, ignoreCase = true)
                    
                    // Filter Status
                    val matchesStatus = filters.statuses.isEmpty() || task.status in filters.statuses
                    
                    // Filter Priority
                    val matchesPriority = filters.priorities.isEmpty() || task.priority in filters.priorities
                    
                    // Filter Category
                    val matchesCategory = filters.categories.isEmpty() || task.categoryId in filters.categories
                    
                    // Filter Recurrence
                    val matchesRecurrence = filters.recurrence.isEmpty() || task.recurrence in filters.recurrence
                    
                    // Filter Due State
                    val matchesDueState = filters.dueStates.isEmpty() || run {
                        val todayStart = DateUtils.startOfDay()
                        val isOverdue = task.dueDateMillis != null && task.dueDateMillis < todayStart && task.status != TaskStatus.COMPLETED.name
                        val isToday = task.dueDateMillis != null && DateUtils.isSameDay(task.dueDateMillis, todayStart)
                        val isUpcoming = task.dueDateMillis != null && task.dueDateMillis > DateUtils.endOfDay(todayStart)
                        
                        (filters.dueStates.contains("OVERDUE") && isOverdue) ||
                                (filters.dueStates.contains("TODAY") && isToday) ||
                                (filters.dueStates.contains("UPCOMING") && isUpcoming)
                    }

                    matchesQuery && matchesStatus && matchesPriority && matchesCategory && matchesRecurrence && matchesDueState
                }

                // 2. Overlapping tasks detection
                val overlappingIds = mutableSetOf<String>()
                val timedTasks = allTasks.filter { 
                    it.dueDateMillis != null && 
                    it.dueTimeMillis != null && 
                    it.status != TaskStatus.ARCHIVED.name && 
                    it.status != TaskStatus.COMPLETED.name 
                }
                val groupedTasks = timedTasks.groupBy { Pair(DateUtils.startOfDay(it.dueDateMillis!!), it.dueTimeMillis!!) }
                groupedTasks.forEach { (_, list) ->
                    if (list.size > 1) {
                        overlappingIds.addAll(list.map { it.id })
                    }
                }

                // 3. Daily Summary aggregates (total, completed, priority counts, category colors)
                val summaryMap = mutableMapOf<Long, DayTasksSummary>()
                val categoryColorMap = categories.associate { it.id to it.colorHex }
                val todayStart = DateUtils.startOfDay()
                
                allTasks.filter { it.dueDateMillis != null && it.status != TaskStatus.ARCHIVED.name }.forEach { task ->
                    val dayStart = DateUtils.startOfDay(task.dueDateMillis!!)
                    val currentSummary = summaryMap[dayStart] ?: DayTasksSummary()
                    
                    val isCompleted = task.status == TaskStatus.COMPLETED.name
                    val isOverdue = task.dueDateMillis < todayStart && !isCompleted
                    
                    val newColors = if (task.categoryId != null) {
                        val color = categoryColorMap[task.categoryId]
                        if (color != null) currentSummary.categoryColors + color else currentSummary.categoryColors
                    } else currentSummary.categoryColors

                    summaryMap[dayStart] = currentSummary.copy(
                        total = currentSummary.total + 1,
                        completed = currentSummary.completed + (if (isCompleted) 1 else 0),
                        urgentCount = currentSummary.urgentCount + (if (task.priority == Priority.URGENT.name) 1 else 0),
                        highCount = currentSummary.highCount + (if (task.priority == Priority.HIGH.name) 1 else 0),
                        mediumCount = currentSummary.mediumCount + (if (task.priority == Priority.MEDIUM.name) 1 else 0),
                        lowCount = currentSummary.lowCount + (if (task.priority == Priority.LOW.name) 1 else 0),
                        hasOverdue = currentSummary.hasOverdue || isOverdue,
                        categoryColors = newColors
                    )
                }

                // 4. Productivity Analytics
                val analytics = analyticsManager.calculateAnalytics(allTasks, sessions, categories)
                
                val resourcesMap = resources.groupBy { it.taskId }

                _uiState.update {
                    it.copy(
                        tasks = allTasks,
                        filteredTasks = filtered,
                        unscheduledTasks = unscheduled,
                        categories = categories,
                        taskResourcesMap = resourcesMap,
                        overlappingTaskIds = overlappingIds,
                        taskSummaryByDay = summaryMap,
                        activeFilters = filters,
                        analytics = analytics,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun selectDay(dayMillis: Long) {
        val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
        _uiState.update {
            it.copy(
                selectedDayMillis = dayMillis,
                currentYear = cal.get(Calendar.YEAR),
                currentMonth = cal.get(Calendar.MONTH)
            )
        }
    }

    fun navigateMonth(delta: Int) {
        _uiState.update { state ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, state.currentYear)
                set(Calendar.MONTH, state.currentMonth)
                add(Calendar.MONTH, delta)
            }
            state.copy(
                currentYear = cal.get(Calendar.YEAR),
                currentMonth = cal.get(Calendar.MONTH)
            )
        }
    }

    fun selectMode(mode: CalendarMode) {
        _uiState.update { it.copy(calendarMode = mode) }
        viewModelScope.launch {
            preferencesManager.setCalendarMode(mode.name)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    // --- Filter Management ---
    fun toggleStatusFilter(status: String) {
        viewModelScope.launch {
            val current = _uiState.value.activeFilters.statuses
            val updated = if (current.contains(status)) current - status else current + status
            preferencesManager.setCalendarFilterStatus(updated)
        }
    }

    fun togglePriorityFilter(priority: String) {
        viewModelScope.launch {
            val current = _uiState.value.activeFilters.priorities
            val updated = if (current.contains(priority)) current - priority else current + priority
            preferencesManager.setCalendarFilterPriority(updated)
        }
    }

    fun toggleCategoryFilter(categoryId: String) {
        viewModelScope.launch {
            val current = _uiState.value.activeFilters.categories
            val updated = if (current.contains(categoryId)) current - categoryId else current + categoryId
            preferencesManager.setCalendarFilterCategories(updated)
        }
    }

    fun toggleRecurrenceFilter(recurrence: String) {
        viewModelScope.launch {
            val current = _uiState.value.activeFilters.recurrence
            val updated = if (current.contains(recurrence)) current - recurrence else current + recurrence
            preferencesManager.setCalendarFilterRecurrence(updated)
        }
    }

    fun toggleDueStateFilter(dueState: String) {
        viewModelScope.launch {
            val current = _uiState.value.activeFilters.dueStates
            val updated = if (current.contains(dueState)) current - dueState else current + dueState
            preferencesManager.setCalendarFilterDueState(updated)
        }
    }

    fun clearAllFilters() {
        viewModelScope.launch {
            preferencesManager.setCalendarFilterStatus(emptySet())
            preferencesManager.setCalendarFilterPriority(emptySet())
            preferencesManager.setCalendarFilterCategories(emptySet())
            preferencesManager.setCalendarFilterRecurrence(emptySet())
            preferencesManager.setCalendarFilterDueState(emptySet())
        }
    }

    // --- Quick Task Creation ---
    fun quickAddTask(title: String, priority: String, categoryId: String?, dueTimeMillis: Long?) {
        viewModelScope.launch {
            val state = _uiState.value
            val task = TaskEntity(
                title = title,
                priority = priority,
                categoryId = if (categoryId == "CLEAR" || categoryId.isNullOrBlank()) null else categoryId,
                dueDateMillis = state.selectedDayMillis,
                dueTimeMillis = dueTimeMillis,
                status = TaskStatus.PENDING.name,
                createdDateMillis = System.currentTimeMillis()
            )
            taskRepository.insertTask(task)
        }
    }

    // --- Multi-Select Operations ---
    fun toggleTaskSelection(taskId: String) {
        _uiState.update { state ->
            val current = state.selectedTaskIds
            val updated = if (current.contains(taskId)) current - taskId else current + taskId
            state.copy(selectedTaskIds = updated)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedTaskIds = emptySet()) }
    }

    fun bulkCompleteSelected() {
        viewModelScope.launch {
            val selected = _uiState.value.selectedTaskIds.toList()
            taskRepository.bulkCompleteTasks(selected)
            
            // Award XP based on priorities of completed tasks
            selected.forEach { id ->
                val task = taskRepository.getTaskById(id)
                if (task != null) {
                    gamificationEngine.awardTaskCompletionXp(task)
                }
            }
            clearSelection()
        }
    }

    fun bulkArchiveSelected() {
        viewModelScope.launch {
            val selected = _uiState.value.selectedTaskIds.toList()
            taskRepository.bulkArchiveTasks(selected)
            clearSelection()
        }
    }

    fun bulkRescheduleSelected(dateMillis: Long) {
        viewModelScope.launch {
            val selected = _uiState.value.selectedTaskIds.toList()
            taskRepository.bulkUpdateTasks(selected, dateMillis, null, null)
            clearSelection()
        }
    }

    fun bulkUpdateCategoryForSelected(categoryId: String?) {
        viewModelScope.launch {
            val selected = _uiState.value.selectedTaskIds.toList()
            taskRepository.bulkUpdateTasks(selected, null, null, categoryId ?: "CLEAR")
            clearSelection()
        }
    }

    fun bulkUpdatePriorityForSelected(priority: String) {
        viewModelScope.launch {
            val selected = _uiState.value.selectedTaskIds.toList()
            taskRepository.bulkUpdateTasks(selected, null, priority, null)
            clearSelection()
        }
    }

    // --- Single Task Operations & Move / Undo ---
    fun rescheduleTask(taskId: String, newDateMillis: Long) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                val originalDate = task.dueDateMillis
                _uiState.update { it.copy(lastMovedTaskOriginalState = Pair(taskId, originalDate)) }
                taskRepository.updateTask(task.copy(dueDateMillis = newDateMillis))
            }
        }
    }

    fun undoLastMove() {
        viewModelScope.launch {
            val lastMoved = _uiState.value.lastMovedTaskOriginalState
            if (lastMoved != null) {
                val (taskId, originalDate) = lastMoved
                val task = taskRepository.getTaskById(taskId)
                if (task != null) {
                    taskRepository.updateTask(task.copy(dueDateMillis = originalDate))
                }
                _uiState.update { it.copy(lastMovedTaskOriginalState = null) }
            }
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            val task = taskRepository.completeTask(taskId)
            if (task != null) {
                gamificationEngine.awardTaskCompletionXp(task)
            }
        }
    }
}
