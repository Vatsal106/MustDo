package com.example.todo.features.calendar.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.common.components.*
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme
import androidx.compose.ui.tooling.preview.Preview
import com.example.todo.common.util.DateUtils
import com.example.todo.core.database.entity.CategoryEntity
import com.example.todo.core.database.entity.Priority
import com.example.todo.core.database.entity.TaskEntity
import com.example.todo.core.database.entity.TaskStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onTaskClick: (String) -> Unit,
    onAddTask: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showFiltersSheet by remember { mutableStateOf(false) }
    var showMoveDialogForTaskId by remember { mutableStateOf<String?>(null) }
    var showUnscheduledDrawer by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val filterCount = remember(state.activeFilters) {
        state.activeFilters.statuses.size +
                state.activeFilters.priorities.size +
                state.activeFilters.categories.size +
                state.activeFilters.recurrence.size +
                state.activeFilters.dueStates.size
    }

    LaunchedEffect(state.lastMovedTaskOriginalState) {
        if (state.lastMovedTaskOriginalState != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Task moved",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastMove()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Unified Search Bar & Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search calendar...", fontSize = 14.sp) },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Search, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(20.dp)
                                ) 
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.updateSearchQuery("") }, 
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close, 
                                            contentDescription = "Clear", 
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .graphicsLayer {
                                    shadowElevation = if (searchQuery.isNotBlank()) 4f else 0f
                                    shape = RoundedCornerShape(18.dp)
                                    this.clip = false
                                    ambientShadowColor = MustDoColors.Primary
                                    spotShadowColor = MustDoColors.Primary
                                },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MustDoColors.Primary,
                                unfocusedBorderColor = MustDoColors.DarkBorder
                            ),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showFiltersSheet = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (filterCount > 0) {
                                        Badge(containerColor = MustDoColors.Primary) {
                                            Text(filterCount.toString(), color = Color.White, fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filters",
                                    tint = if (filterCount > 0) MustDoColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Material 3 TabRow for View Selection
                    val modes = listOf(
                        CalendarMode.MONTH to "Month",
                        CalendarMode.WEEK to "Week",
                        CalendarMode.AGENDA to "Agenda",
                        CalendarMode.INSIGHTS to "Insights"
                    )
                    val selectedIndex = modes.indexOfFirst { it.first == state.calendarMode }

                    TabRow(
                        selectedTabIndex = selectedIndex,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = { HorizontalDivider() }
                    ) {
                        modes.forEachIndexed { index, (mode, label) ->
                            Tab(
                                selected = selectedIndex == index,
                                onClick = { viewModel.selectMode(mode) },
                                text = { 
                                    Text(
                                        text = label, 
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedIndex == index) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) 
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (state.selectedTaskIds.isEmpty() && state.calendarMode != CalendarMode.INSIGHTS) {
                GradientFab(
                    onClick = onAddTask,
                    icon = Icons.Default.Add
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main Views based on mode
                when (state.calendarMode) {
                    CalendarMode.MONTH -> {
                        MonthPlannerView(
                            state = state,
                            viewModel = viewModel,
                            onTaskClick = onTaskClick,
                            onRescheduleClick = { showMoveDialogForTaskId = it }
                        )
                    }
                    CalendarMode.WEEK -> {
                        WeekPlannerView(
                            state = state,
                            viewModel = viewModel,
                            onTaskClick = onTaskClick,
                            onRescheduleClick = { showMoveDialogForTaskId = it }
                        )
                    }
                    CalendarMode.AGENDA -> {
                        AgendaPlannerView(
                            state = state,
                            viewModel = viewModel,
                            onTaskClick = onTaskClick,
                            onRescheduleClick = { showMoveDialogForTaskId = it }
                        )
                    }
                    CalendarMode.INSIGHTS -> {
                        InsightsDashboardView(state = state)
                    }
                }
            }

            // Dialogue & Drawers
            if (showUnscheduledDrawer) {
                UnscheduledTasksDrawer(
                    tasks = state.unscheduledTasks,
                    categories = state.categories,
                    onDismiss = { showUnscheduledDrawer = false },
                    onTaskClick = onTaskClick,
                    onScheduleClick = { taskId ->
                        showMoveDialogForTaskId = taskId
                        showUnscheduledDrawer = false
                    }
                )
            }

            if (showMoveDialogForTaskId != null) {
                MoveTaskDialog(
                    taskId = showMoveDialogForTaskId!!,
                    onDismiss = { showMoveDialogForTaskId = null },
                    onConfirm = { taskId, newDateMillis ->
                        viewModel.rescheduleTask(taskId, newDateMillis)
                        showMoveDialogForTaskId = null
                    }
                )
            }

            if (showFiltersSheet) {
                FilterBottomSheet(
                    filters = state.activeFilters,
                    categories = state.categories,
                    onDismiss = { showFiltersSheet = false },
                    onToggleStatus = { viewModel.toggleStatusFilter(it) },
                    onTogglePriority = { viewModel.togglePriorityFilter(it) },
                    onToggleCategory = { viewModel.toggleCategoryFilter(it) },
                    onToggleRecurrence = { viewModel.toggleRecurrenceFilter(it) },
                    onToggleDueState = { viewModel.toggleDueStateFilter(it) },
                    onClearAll = { viewModel.clearAllFilters() }
                )
            }

            if (state.selectedTaskIds.isNotEmpty()) {
                MultiSelectBar(
                    selectedCount = state.selectedTaskIds.size,
                    categories = state.categories,
                    onClear = { viewModel.clearSelection() },
                    onComplete = { viewModel.bulkCompleteSelected() },
                    onArchive = { viewModel.bulkArchiveSelected() },
                    onMove = { dateMillis -> viewModel.bulkRescheduleSelected(dateMillis) },
                    onCategory = { catId -> viewModel.bulkUpdateCategoryForSelected(catId) },
                    onPriority = { priority -> viewModel.bulkUpdatePriorityForSelected(priority) }
                )
            }
        }
    }
}

// ─── MONTH PLANNER VIEW (COLLAPSIBLE GRID) ──────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MonthPlannerView(
    state: CalendarUiState,
    viewModel: CalendarViewModel,
    onTaskClick: (String) -> Unit,
    onRescheduleClick: (String) -> Unit
) {
    var isCalendarCollapsed by rememberSaveable { mutableStateOf(false) }
    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
    val isCollapsed = isCalendarCollapsed
    var isNoDueDateExpanded by rememberSaveable { mutableStateOf(false) }

    val monthName = remember(state.currentYear, state.currentMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, state.currentYear)
            set(Calendar.MONTH, state.currentMonth)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    val selectedDayTasks = remember(state.filteredTasks, state.selectedDayMillis) {
        state.filteredTasks.filter { it.dueDateMillis != null && DateUtils.isSameDay(it.dueDateMillis, state.selectedDayMillis) }
    }

    val completedCount = remember(selectedDayTasks) { selectedDayTasks.count { it.status == TaskStatus.COMPLETED.name } }
    val completionPercent = remember(selectedDayTasks, completedCount) {
        if (selectedDayTasks.isNotEmpty()) (completedCount * 100) / selectedDayTasks.size else 0
    }
    val dateText = remember(state.selectedDayMillis) {
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(state.selectedDayMillis))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateMonth(-1) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = { viewModel.navigateMonth(1) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { viewModel.selectDay(DateUtils.startOfDay()) },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Today", fontSize = 11.sp)
                }

                IconButton(
                    onClick = { isCalendarCollapsed = !isCollapsed },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = "Toggle Collapse",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Days of week row header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = "Wk",
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold
            )
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Collapsible month grid
        AnimatedVisibility(
            visible = !isCollapsed,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            MonthGrid(state = state, viewModel = viewModel)
        }

        AnimatedVisibility(
            visible = isCollapsed,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            // Render only the selected day's week row
            CollapsedWeekRow(state = state, viewModel = viewModel)
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            stickyHeader {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$dateText  •  ${selectedDayTasks.size} Tasks  •  $completionPercent% Complete",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (selectedDayTasks.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.EventAvailable,
                        title = "No tasks planned",
                        subtitle = "Day is clear",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                }
            } else {
                items(
                    items = selectedDayTasks,
                    key = { it.id }
                ) { task ->
                    val isSelected = state.selectedTaskIds.contains(task.id)
                    val hasConflict = state.overlappingTaskIds.contains(task.id)
                    
                    val resources = state.taskResourcesMap[task.id] ?: emptyList()
                    val hasContact = resources.any { it.resourceType == "CONTACT" }
                    val hasLocation = resources.any { it.resourceType == "LOCATION" }
                    
                    TaskPlannerCard(
                        task = task,
                        categories = state.categories,
                        isSelected = isSelected,
                        hasConflict = hasConflict,
                        hasContact = hasContact,
                        hasLocation = hasLocation,
                        onTaskClick = onTaskClick,
                        onLongClick = { viewModel.toggleTaskSelection(it) },
                        onCompleteClick = { viewModel.completeTask(it) },
                        onRescheduleClick = onRescheduleClick
                    )
                }
            }

            // Collapse/Expand "No Due Date" section
            if (state.unscheduledTasks.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    CollapsibleSectionHeader(
                        title = "📥 No Due Date (${state.unscheduledTasks.size})",
                        isExpanded = isNoDueDateExpanded,
                        onToggle = { isNoDueDateExpanded = !isNoDueDateExpanded }
                    )
                }
                if (isNoDueDateExpanded) {
                    items(
                        items = state.unscheduledTasks,
                        key = { "unscheduled_${it.id}" }
                    ) { task ->
                        val isSelected = state.selectedTaskIds.contains(task.id)
                        val hasConflict = state.overlappingTaskIds.contains(task.id)
                        
                        val resources = state.taskResourcesMap[task.id] ?: emptyList()
                        val hasContact = resources.any { it.resourceType == "CONTACT" }
                        val hasLocation = resources.any { it.resourceType == "LOCATION" }
                        
                        TaskPlannerCard(
                            task = task,
                            categories = state.categories,
                            isSelected = isSelected,
                            hasConflict = hasConflict,
                            hasContact = hasContact,
                            hasLocation = hasLocation,
                            onTaskClick = onTaskClick,
                            onLongClick = { viewModel.toggleTaskSelection(it) },
                            onCompleteClick = { viewModel.completeTask(it) },
                            onRescheduleClick = onRescheduleClick
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(88.dp)) }
        }
    }
}

@Composable
fun CollapsedWeekRow(
    state: CalendarUiState,
    viewModel: CalendarViewModel
) {
    val weekStart = remember(state.selectedDayMillis) {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            timeInMillis = state.selectedDayMillis
        }
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        cal.timeInMillis
    }

    val daysOfWeek = remember(weekStart) {
        val list = mutableListOf<Long>()
        val cal = Calendar.getInstance().apply { timeInMillis = weekStart }
        for (i in 0 until 7) {
            list.add(DateUtils.startOfDay(cal.timeInMillis))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val weekNum = Calendar.getInstance().apply { timeInMillis = state.selectedDayMillis }.get(Calendar.WEEK_OF_YEAR)
        Text(
            text = weekNum.toString(),
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold
        )

        daysOfWeek.forEach { dayMillis ->
            val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val isSelected = DateUtils.isSameDay(state.selectedDayMillis, dayMillis)
            val isToday = DateUtils.isSameDay(System.currentTimeMillis(), dayMillis)
            val summary = state.taskSummaryByDay[dayMillis] ?: DayTasksSummary()

            MonthDayCell(
                day = day,
                isSelected = isSelected,
                isToday = isToday,
                summary = summary,
                onClick = { viewModel.selectDay(dayMillis) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── WEEK PLANNER VIEW (STATIC 7-DAY OVERVIEW STRIP) ─────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeekPlannerView(
    state: CalendarUiState,
    viewModel: CalendarViewModel,
    onTaskClick: (String) -> Unit,
    onRescheduleClick: (String) -> Unit
) {
    val weekStart = remember(state.selectedDayMillis) {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            timeInMillis = state.selectedDayMillis
        }
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        cal.timeInMillis
    }

    val daysOfWeek = remember(weekStart) {
        val list = mutableListOf<Long>()
        val cal = Calendar.getInstance().apply { timeInMillis = weekStart }
        for (i in 0 until 7) {
            list.add(DateUtils.startOfDay(cal.timeInMillis))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val weekNum = remember(state.selectedDayMillis) {
        Calendar.getInstance().apply { timeInMillis = state.selectedDayMillis }.get(Calendar.WEEK_OF_YEAR)
    }

    var isNoDueDateExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Week Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.selectDay(state.selectedDayMillis - 7 * 24 * 60 * 60 * 1000L) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(
                text = "Week $weekNum",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.selectDay(state.selectedDayMillis + 7 * 24 * 60 * 60 * 1000L) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }

        // Static 7-Day Overview Strip Spanning Full Width (Mon-Sun layout with zero horizontal scrolling)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            daysOfWeek.forEach { dayMillis ->
                val isSelected = DateUtils.isSameDay(state.selectedDayMillis, dayMillis)
                val dayTasks = state.filteredTasks.filter { it.dueDateMillis != null && DateUtils.isSameDay(it.dueDateMillis, dayMillis) }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MustDoColors.Primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) MustDoColors.Primary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.selectDay(dayMillis) }
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
                    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
                    val dateNum = cal.get(Calendar.DAY_OF_MONTH).toString()

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(dayName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = dateNum,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MustDoColors.Primary else MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (dayTasks.isNotEmpty()) {
                            val completed = dayTasks.count { it.status == TaskStatus.COMPLETED.name }
                            Text(
                                text = "$completed/${dayTasks.size}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MustDoColors.Primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            // Up to 3 tiny priority dots
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                dayTasks.take(3).forEach { task ->
                                    val dotColor = when (task.priority) {
                                        Priority.LOW.name -> MustDoColors.PriorityLow
                                        Priority.MEDIUM.name -> MustDoColors.PriorityMedium
                                        Priority.HIGH.name -> MustDoColors.PriorityHigh
                                        Priority.URGENT.name -> MustDoColors.PriorityUrgent
                                        else -> MustDoColors.PriorityMedium
                                    }
                                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(dotColor))
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Selected Day Tasks
        val selectedDayTasks = remember(state.filteredTasks, state.selectedDayMillis) {
            state.filteredTasks.filter { it.dueDateMillis != null && DateUtils.isSameDay(it.dueDateMillis, state.selectedDayMillis) }
        }

        val completedCount = remember(selectedDayTasks) { selectedDayTasks.count { it.status == TaskStatus.COMPLETED.name } }
        val completionPercent = remember(selectedDayTasks, completedCount) {
            if (selectedDayTasks.isNotEmpty()) (completedCount * 100) / selectedDayTasks.size else 0
        }
        val dateText = remember(state.selectedDayMillis) {
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(state.selectedDayMillis))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            stickyHeader {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$dateText  •  ${selectedDayTasks.size} Tasks  •  $completionPercent% Complete",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (selectedDayTasks.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.EventAvailable,
                        title = "All clear",
                        subtitle = "No tasks planned",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                }
            } else {
                items(
                    items = selectedDayTasks,
                    key = { it.id }
                ) { task ->
                    val isTaskSelected = state.selectedTaskIds.contains(task.id)
                    val hasConflict = state.overlappingTaskIds.contains(task.id)

                    val resources = state.taskResourcesMap[task.id] ?: emptyList()
                    val hasContact = resources.any { it.resourceType == "CONTACT" }
                    val hasLocation = resources.any { it.resourceType == "LOCATION" }

                    TaskPlannerCard(
                        task = task,
                        categories = state.categories,
                        isSelected = isTaskSelected,
                        hasConflict = hasConflict,
                        hasContact = hasContact,
                        hasLocation = hasLocation,
                        onTaskClick = onTaskClick,
                        onLongClick = { viewModel.toggleTaskSelection(it) },
                        onCompleteClick = { viewModel.completeTask(it) },
                        onRescheduleClick = onRescheduleClick
                    )
                }
            }

            // Collapse/Expand "No Due Date" section
            if (state.unscheduledTasks.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    CollapsibleSectionHeader(
                        title = "📥 No Due Date (${state.unscheduledTasks.size})",
                        isExpanded = isNoDueDateExpanded,
                        onToggle = { isNoDueDateExpanded = !isNoDueDateExpanded }
                    )
                }
                if (isNoDueDateExpanded) {
                    items(
                        items = state.unscheduledTasks,
                        key = { "unscheduled_${it.id}" }
                    ) { task ->
                        val isSelected = state.selectedTaskIds.contains(task.id)
                        val hasConflict = state.overlappingTaskIds.contains(task.id)
                        
                        val resources = state.taskResourcesMap[task.id] ?: emptyList()
                        val hasContact = resources.any { it.resourceType == "CONTACT" }
                        val hasLocation = resources.any { it.resourceType == "LOCATION" }
                        
                        TaskPlannerCard(
                            task = task,
                            categories = state.categories,
                            isSelected = isSelected,
                            hasConflict = hasConflict,
                            hasContact = hasContact,
                            hasLocation = hasLocation,
                            onTaskClick = onTaskClick,
                            onLongClick = { viewModel.toggleTaskSelection(it) },
                            onCompleteClick = { viewModel.completeTask(it) },
                            onRescheduleClick = onRescheduleClick
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(88.dp)) }
        }
    }
}

// ─── AGENDA PLANNER VIEW (COLLAPSIBLE TIMELINE SECTIONS) ─────────────────

@Composable
fun AgendaPlannerView(
    state: CalendarUiState,
    viewModel: CalendarViewModel,
    onTaskClick: (String) -> Unit,
    onRescheduleClick: (String) -> Unit
) {
    val todayStart = DateUtils.startOfDay()
    val tomorrowStart = DateUtils.startOfTomorrow()
    val weekEnd = todayStart + 7 * 24 * 60 * 60 * 1000L
    val nextWeekEnd = todayStart + 14 * 24 * 60 * 60 * 1000L

    val overdueTasks = remember(state.filteredTasks) {
        state.filteredTasks.filter { it.dueDateMillis != null && it.dueDateMillis < todayStart && it.status != TaskStatus.COMPLETED.name }
    }
    val todayTasks = remember(state.filteredTasks) {
        state.filteredTasks.filter { it.dueDateMillis != null && DateUtils.isSameDay(it.dueDateMillis, todayStart) }
    }
    val tomorrowTasks = remember(state.filteredTasks) {
        state.filteredTasks.filter { it.dueDateMillis != null && DateUtils.isSameDay(it.dueDateMillis, tomorrowStart) }
    }
    val thisWeekTasks = remember(state.filteredTasks) {
        state.filteredTasks.filter { it.dueDateMillis != null && it.dueDateMillis in (tomorrowStart + 24*60*60*1000L) until weekEnd }
    }
    val nextWeekTasks = remember(state.filteredTasks) {
        state.filteredTasks.filter { it.dueDateMillis != null && it.dueDateMillis in weekEnd until nextWeekEnd }
    }
    val upcomingTasks = remember(state.filteredTasks) {
        state.filteredTasks.filter { it.dueDateMillis != null && it.dueDateMillis >= nextWeekEnd }
    }

    val expandedStates = rememberSaveable(
        saver = mapSaver(
            save = { it.toMap() },
            restore = { 
                val restored = mutableStateMapOf<String, Boolean>()
                restored.putAll(it as Map<String, Boolean>)
                restored
            }
        )
    ) {
        mutableStateMapOf<String, Boolean>().apply {
            put("Overdue", true)
            put("Today", true)
            put("Tomorrow", true)
            put("Later This Week", true)
            put("Next Week", true)
            put("Upcoming", true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (overdueTasks.isNotEmpty()) {
            item {
                CollapsibleSectionHeader(
                    title = "⚠️ Overdue (${overdueTasks.size})",
                    isExpanded = expandedStates["Overdue"] ?: true,
                    onToggle = { expandedStates["Overdue"] = !(expandedStates["Overdue"] ?: true) }
                )
            }
            if (expandedStates["Overdue"] ?: true) {
                items(overdueTasks, key = { "overdue_${it.id}" }) { task ->
                    AgendaItemRow(state, task, onTaskClick, viewModel, onRescheduleClick)
                }
            }
        }

        item {
            CollapsibleSectionHeader(
                title = "📅 Today (${todayTasks.size})",
                isExpanded = expandedStates["Today"] ?: true,
                onToggle = { expandedStates["Today"] = !(expandedStates["Today"] ?: true) }
            )
        }
        if (expandedStates["Today"] ?: true) {
            if (todayTasks.isEmpty()) {
                item { EmptyAgendaItem(text = "No tasks for today") }
            } else {
                items(todayTasks, key = { "today_${it.id}" }) { task ->
                    AgendaItemRow(state, task, onTaskClick, viewModel, onRescheduleClick)
                }
            }
        }

        item {
            CollapsibleSectionHeader(
                title = "🌅 Tomorrow (${tomorrowTasks.size})",
                isExpanded = expandedStates["Tomorrow"] ?: true,
                onToggle = { expandedStates["Tomorrow"] = !(expandedStates["Tomorrow"] ?: true) }
            )
        }
        if (expandedStates["Tomorrow"] ?: true) {
            if (tomorrowTasks.isEmpty()) {
                item { EmptyAgendaItem(text = "No tasks for tomorrow") }
            } else {
                items(tomorrowTasks, key = { "tomorrow_${it.id}" }) { task ->
                    AgendaItemRow(state, task, onTaskClick, viewModel, onRescheduleClick)
                }
            }
        }

        if (thisWeekTasks.isNotEmpty()) {
            item {
                CollapsibleSectionHeader(
                    title = "📅 Later This Week (${thisWeekTasks.size})",
                    isExpanded = expandedStates["Later This Week"] ?: true,
                    onToggle = { expandedStates["Later This Week"] = !(expandedStates["Later This Week"] ?: true) }
                )
            }
            if (expandedStates["Later This Week"] ?: true) {
                items(thisWeekTasks, key = { "thisweek_${it.id}" }) { task ->
                    AgendaItemRow(state, task, onTaskClick, viewModel, onRescheduleClick)
                }
            }
        }

        if (nextWeekTasks.isNotEmpty()) {
            item {
                CollapsibleSectionHeader(
                    title = "📅 Next Week (${nextWeekTasks.size})",
                    isExpanded = expandedStates["Next Week"] ?: true,
                    onToggle = { expandedStates["Next Week"] = !(expandedStates["Next Week"] ?: true) }
                )
            }
            if (expandedStates["Next Week"] ?: true) {
                items(nextWeekTasks, key = { "nextweek_${it.id}" }) { task ->
                    AgendaItemRow(state, task, onTaskClick, viewModel, onRescheduleClick)
                }
            }
        }

        if (upcomingTasks.isNotEmpty()) {
            item {
                CollapsibleSectionHeader(
                    title = "🔮 Upcoming (${upcomingTasks.size})",
                    isExpanded = expandedStates["Upcoming"] ?: true,
                    onToggle = { expandedStates["Upcoming"] = !(expandedStates["Upcoming"] ?: true) }
                )
            }
            if (expandedStates["Upcoming"] ?: true) {
                items(upcomingTasks, key = { "upcoming_${it.id}" }) { task ->
                    AgendaItemRow(state, task, onTaskClick, viewModel, onRescheduleClick)
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
fun AgendaItemRow(
    state: CalendarUiState,
    task: TaskEntity,
    onTaskClick: (String) -> Unit,
    viewModel: CalendarViewModel,
    onRescheduleClick: (String) -> Unit
) {
    val isSelected = state.selectedTaskIds.contains(task.id)
    val hasConflict = state.overlappingTaskIds.contains(task.id)
    
    TaskPlannerCard(
        task = task,
        categories = state.categories,
        isSelected = isSelected,
        hasConflict = hasConflict,
        onTaskClick = onTaskClick,
        onLongClick = { viewModel.toggleTaskSelection(it) },
        onCompleteClick = { viewModel.completeTask(it) },
        onRescheduleClick = onRescheduleClick
    )
}

@Composable
fun EmptyAgendaItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
fun CollapsibleSectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── HIGH-DENSITY COMPACT TASK CARD (56dp-64dp) ─────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskPlannerCard(
    task: TaskEntity,
    categories: List<CategoryEntity>,
    isSelected: Boolean,
    hasConflict: Boolean,
    hasContact: Boolean = false,
    hasLocation: Boolean = false,
    onTaskClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
    onCompleteClick: (String) -> Unit,
    onRescheduleClick: (String) -> Unit
) {
    val category = categories.find { it.id == task.categoryId }
    val isCompleted = task.status == TaskStatus.COMPLETED.name
    
    val borderStroke = when {
        isSelected -> BorderStroke(1.5.dp, MustDoColors.Primary)
        hasConflict -> BorderStroke(1.dp, MustDoColors.Warning.copy(alpha = 0.5f))
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp) // Restricting height strictly within 56dp-64dp for density
            .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(12.dp)) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onTaskClick(task.id) },
                onLongClick = { onLongClick(task.id) }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            CompletionCheckbox(
                isCompleted = isCompleted,
                priority = task.priority,
                onToggle = { onCompleteClick(task.id) }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Main Text details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    )

                    if (task.recurrence != "NONE") {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (category != null) {
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(category.colorHex))
                        } catch (e: Exception) {
                            MustDoColors.Primary
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = catColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = catColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                    if (hasContact) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "👤 Contact Attached",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                    if (hasLocation) {
                        Text(
                            text = "📍",
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Quick actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val priColor = when (task.priority) {
                    Priority.LOW.name -> MustDoColors.PriorityLow
                    Priority.MEDIUM.name -> MustDoColors.PriorityMedium
                    Priority.HIGH.name -> MustDoColors.PriorityHigh
                    Priority.URGENT.name -> MustDoColors.PriorityUrgent
                    else -> MustDoColors.PriorityMedium
                }
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(priColor))

                if (hasConflict) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Conflict",
                        tint = MustDoColors.Warning,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onRescheduleClick(task.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Reschedule",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─── PREMIUM INSIGHTS DASHBOARD (KPI-FIRST REDESIGN) ────────────────────

@Composable
fun InsightsDashboardView(
    state: CalendarUiState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // KPI Summary Cards placed at the top (Grid layout)
        Text(
            text = "📈 Performance KPIs",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val totalCompleted = remember(state.tasks) { state.tasks.count { it.status == TaskStatus.COMPLETED.name } }
            
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KPICard(
                    label = "Completed",
                    value = totalCompleted.toString(),
                    icon = Icons.Default.AssignmentTurnedIn,
                    color = MustDoColors.Primary
                )
                KPICard(
                    label = "Focus Hours",
                    value = String.format(Locale.getDefault(), "%.1fh", state.analytics.weekFocusHours),
                    icon = Icons.Default.Timer,
                    color = MustDoColors.Info
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val rate = if (state.tasks.isNotEmpty()) (totalCompleted * 100) / state.tasks.size else 0
                KPICard(
                    label = "Completion Rate",
                    value = "$rate%",
                    icon = Icons.Default.Star,
                    color = MustDoColors.Success
                )
                KPICard(
                    label = "Streaks",
                    value = "${state.analytics.mostProductiveDayRate}%",
                    icon = Icons.Default.LocalFireDepartment,
                    color = MustDoColors.Warning
                )
            }
        }

        // Productive day details
        if (state.analytics.mostProductiveDay != "None") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MustDoColors.Warning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Most productive weekday: ${state.analytics.mostProductiveDay}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Canvas charts placed below KPIs
        if (state.analytics.tasksCompletedPerDay.isNotEmpty()) {
            Text(
                text = "📊 Daily Completion Trends",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            CustomBarChart(data = state.analytics.tasksCompletedPerDay, color = MustDoColors.Primary)
        }

        if (state.analytics.focusMinutesPerDay.isNotEmpty()) {
            Text(
                text = "⏱️ Focus Minutes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            CustomLineChart(data = state.analytics.focusMinutesPerDay, color = MustDoColors.Info)
        }

        // Category breakdown progress bars
        Text(
            text = "📁 Category Distributions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        if (state.analytics.categoryDistribution.isEmpty()) {
            Text(
                text = "No category data available.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.analytics.categoryDistribution.forEach { share ->
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(share.colorHex))
                        } catch (e: Exception) {
                            MustDoColors.Primary
                        }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(share.categoryName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${share.count} tasks (${share.percentage.toInt()}%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            LinearProgressIndicator(
                                progress = { share.percentage / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = catColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
fun KPICard(
    label: String,
    value: String,
    icon: ImageVector? = null,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─── MONTH GRID ──────────────────────────────────────────────────────────

@Composable
fun MonthGrid(
    state: CalendarUiState,
    viewModel: CalendarViewModel
) {
    val calendar = remember(state.currentYear, state.currentMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, state.currentYear)
            set(Calendar.MONTH, state.currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday

    val todayCal = Calendar.getInstance()
    val isCurrentMonth = todayCal.get(Calendar.YEAR) == state.currentYear && todayCal.get(Calendar.MONTH) == state.currentMonth
    val today = if (isCurrentMonth) todayCal.get(Calendar.DAY_OF_MONTH) else -1

    val selectedCal = Calendar.getInstance().apply { timeInMillis = state.selectedDayMillis }
    val selectedDay = if (selectedCal.get(Calendar.YEAR) == state.currentYear && selectedCal.get(Calendar.MONTH) == state.currentMonth)
        selectedCal.get(Calendar.DAY_OF_MONTH) else -1

    val totalCells = firstDayOfWeek + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Row week number
                val rowFirstCellIndex = row * 7
                val rowFirstDay = rowFirstCellIndex - firstDayOfWeek + 1
                val weekNumCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, state.currentYear)
                    set(Calendar.MONTH, state.currentMonth)
                    set(Calendar.DAY_OF_MONTH, rowFirstDay.coerceIn(1, daysInMonth))
                }
                
                Text(
                    text = weekNumCal.get(Calendar.WEEK_OF_YEAR).toString(),
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )

                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstDayOfWeek + 1

                    if (day in 1..daysInMonth) {
                        val isSelected = day == selectedDay
                        val isToday = day == today
                        
                        val dayCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, state.currentYear)
                            set(Calendar.MONTH, state.currentMonth)
                            set(Calendar.DAY_OF_MONTH, day)
                        }
                        val dayStartMillis = DateUtils.startOfDay(dayCal.timeInMillis)
                        val summary = state.taskSummaryByDay[dayStartMillis] ?: DayTasksSummary()

                        MonthDayCell(
                            day = day,
                            isSelected = isSelected,
                            isToday = isToday,
                            summary = summary,
                            onClick = { viewModel.selectDay(dayStartMillis) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun MonthDayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    summary: DayTasksSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val densityBg = when {
        summary.total == 0 -> Color.Transparent
        summary.total <= 3 -> MustDoColors.Primary.copy(alpha = 0.05f)
        summary.total <= 8 -> MustDoColors.Primary.copy(alpha = 0.12f)
        else -> MustDoColors.Primary.copy(alpha = 0.22f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1.5f)
            .padding(1.dp)
            .graphicsLayer {
                if (isToday) {
                    shadowElevation = 6f
                    ambientShadowColor = MustDoColors.Primary
                    spotShadowColor = MustDoColors.Primary
                    shape = RoundedCornerShape(12.dp)
                    this.clip = false
                }
            }
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) Modifier.background(
                    Brush.linearGradient(listOf(MustDoColors.Primary, MustDoColors.PrimaryLight))
                )
                else Modifier.background(densityBg)
            )
            .border(
                width = if (isSelected || isToday) 1.5.dp else 0.dp,
                color = if (isSelected) Color.Transparent else if (isToday) MustDoColors.Primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = day.toString(),
                fontSize = 12.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else if (isToday) MustDoColors.Primary else MaterialTheme.colorScheme.onSurface
            )

            // Simple priority dots (max 3 dots)
            if (summary.total > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                    var dotCount = 0
                    if (summary.urgentCount > 0 && dotCount < 3) {
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MustDoColors.PriorityUrgent))
                        dotCount++
                    }
                    if (summary.highCount > 0 && dotCount < 3) {
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MustDoColors.PriorityHigh))
                        dotCount++
                    }
                    if (summary.mediumCount > 0 && dotCount < 3) {
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MustDoColors.PriorityMedium))
                        dotCount++
                    }
                    if (summary.lowCount > 0 && dotCount < 3) {
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MustDoColors.PriorityLow))
                        dotCount++
                    }
                }
            }
        }
    }
}

// ─── UNSCHEDULED DRAWER ──────────────────────────────────────────────────

@Composable
fun UnscheduledTasksDrawer(
    tasks: List<TaskEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onTaskClick: (String) -> Unit,
    onScheduleClick: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📥 Unscheduled Tasks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (tasks.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All tasks scheduled!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            val category = categories.find { it.id == task.categoryId }
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTaskClick(task.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (category != null) {
                                            Text(
                                                text = category.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(android.graphics.Color.parseColor(category.colorHex))
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { onScheduleClick(task.id) },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Schedule", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── QUICK ADD TASK DIALOG ───────────────────────────────────────────────

@Composable
fun QuickAddTaskDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM.name) }
    var selectedCategoryIndex by remember { mutableStateOf(-1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Quick Add",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Task title...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Priority", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Priority.values().forEach { pri ->
                        FilterChip(
                            selected = selectedPriority == pri.name,
                            onClick = { selectedPriority = pri.name },
                            label = { Text(pri.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories.size) { index ->
                        val cat = categories[index]
                        FilterChip(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = if (selectedCategoryIndex == index) -1 else index },
                            label = { Text(cat.name, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MustDoColors.Primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val catId = if (selectedCategoryIndex != -1) categories[selectedCategoryIndex].id else null
                                onConfirm(title, selectedPriority, catId, null)
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MustDoColors.Primary)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ─── MOVE TASK DIALOG ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveTaskDialog(
    taskId: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = calendar.timeInMillis)
    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Reschedule Task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onConfirm(taskId, DateUtils.startOfDay()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Today")
                }
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { onConfirm(taskId, DateUtils.startOfTomorrow()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Tomorrow")
                }
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        val nextMondayCal = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            if (timeInMillis <= System.currentTimeMillis()) {
                                add(Calendar.WEEK_OF_YEAR, 1)
                            }
                        }
                        onConfirm(taskId, DateUtils.startOfDay(nextMondayCal.timeInMillis))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Next Monday")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose Date")
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel", color = MustDoColors.Primary)
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onConfirm(taskId, it)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ─── FILTER BOTTOM SHEET ─────────────────────────────────────────────────

@Composable
fun FilterBottomSheet(
    filters: CalendarFilters,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onToggleStatus: (String) -> Unit,
    onTogglePriority: (String) -> Unit,
    onToggleCategory: (String) -> Unit,
    onToggleRecurrence: (String) -> Unit,
    onToggleDueState: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onClearAll) {
                        Text("Clear All", color = MustDoColors.Accent)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Due State
                Text("Due State", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("OVERDUE", "TODAY", "UPCOMING").forEach { state ->
                        FilterChip(
                            selected = filters.dueStates.contains(state),
                            onClick = { onToggleDueState(state) },
                            label = { Text(state.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status
                Text("Status", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskStatus.values().filter { it != TaskStatus.ARCHIVED }.forEach { stat ->
                        FilterChip(
                            selected = filters.statuses.contains(stat.name),
                            onClick = { onToggleStatus(stat.name) },
                            label = { Text(stat.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Priority
                Text("Priority", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Priority.values().forEach { pri ->
                        FilterChip(
                            selected = filters.priorities.contains(pri.name),
                            onClick = { onTogglePriority(pri.name) },
                            label = { Text(pri.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Categories
                Text("Categories", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories.size) { index ->
                        val cat = categories[index]
                        FilterChip(
                            selected = filters.categories.contains(cat.id),
                            onClick = { onToggleCategory(cat.id) },
                            label = { Text(cat.name, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MustDoColors.Primary)
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

// ─── MULTI-SELECT FLOATING BAR ───────────────────────────────────────────

@Composable
fun MultiSelectBar(
    selectedCount: Int,
    categories: List<CategoryEntity>,
    onClear: () -> Unit,
    onComplete: () -> Unit,
    onArchive: () -> Unit,
    onMove: (Long) -> Unit,
    onCategory: (String?) -> Unit,
    onPriority: (String) -> Unit
) {
    var showMoveDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = MaterialTheme.colorScheme.inverseOnSurface,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                    }
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onComplete) {
                        Icon(Icons.Default.Check, contentDescription = "Complete All")
                    }
                    IconButton(onClick = onArchive) {
                        Icon(Icons.Default.Archive, contentDescription = "Archive All")
                    }
                    IconButton(onClick = { showMoveDialog = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Move All")
                    }
                }
            }
        }
    }

    if (showMoveDialog) {
        MoveTaskDialog(
            taskId = "",
            onDismiss = { showMoveDialog = false },
            onConfirm = { _, newDate ->
                onMove(newDate)
                showMoveDialog = false
            }
        )
    }
}

// ─── CHARTS ──────────────────────────────────────────────────────────────

@Composable
fun CustomBarChart(
    data: List<Pair<String, Float>>,
    color: Color
) {
    val maxVal = remember(data) { (data.maxOfOrNull { it.second } ?: 0f).coerceAtLeast(1f) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { (label, value) ->
                val ratio = value / maxVal
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = value.toInt().toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .fillMaxHeight(ratio.coerceAtLeast(0.05f))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(color)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CustomLineChart(
    data: List<Pair<String, Float>>,
    color: Color
) {
    val maxVal = remember(data) { (data.maxOfOrNull { it.second } ?: 0f).coerceAtLeast(1f) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val pointsCount = data.size
                if (pointsCount < 2) return@Canvas
                
                val dx = width / (pointsCount - 1)
                val path = androidx.compose.ui.graphics.Path()
                val fillPath = androidx.compose.ui.graphics.Path()
                
                data.forEachIndexed { index, (_, value) ->
                    val ratio = value / maxVal
                    val x = index * dx
                    val y = height - (ratio * height)
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                    if (index == pointsCount - 1) {
                        fillPath.lineTo(x, height)
                        fillPath.close()
                    }
                }
                
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
                
                drawPath(
                    path = path,
                    color = color,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
                
                data.forEachIndexed { index, (_, value) ->
                    val ratio = value / maxVal
                    val x = index * dx
                    val y = height - (ratio * height)
                    drawCircle(
                        color = color,
                        radius = 3.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach { (label, _) ->
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
fun CalendarScreenPreview() {
    MustDoTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Mock search bar and filter row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Search calendar...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filters", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    }
                }

                // Tab selection
                val modes = listOf("Month", "Week", "Agenda", "Insights")
                TabRow(
                    selectedTabIndex = 0,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { HorizontalDivider() }
                ) {
                    modes.forEachIndexed { index, label ->
                        Tab(
                            selected = index == 0,
                            onClick = {},
                            text = { 
                                Text(
                                    text = label, 
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (index == 0) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            }
                        )
                    }
                }

                // Mock month grid view
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "July 2026",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}

