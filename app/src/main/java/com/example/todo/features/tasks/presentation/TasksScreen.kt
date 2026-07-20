package com.example.todo.features.tasks.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import com.example.todo.core.database.entity.TaskEntity
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.common.components.*
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme
import androidx.compose.ui.tooling.preview.Preview
import com.example.todo.common.util.capitalizeFirstLetter
import com.example.todo.core.database.entity.AchievementEntity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.ui.text.style.TextDecoration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onTaskClick: (String) -> Unit,
    onAddTask: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    var previousIndex by remember { mutableStateOf(0) }
    var previousScrollOffset by remember { mutableStateOf(0) }
    var isFabVisible by remember { mutableStateOf(true) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset
        
        if (currentIndex > previousIndex) {
            isFabVisible = false
        } else if (currentIndex < previousIndex) {
            isFabVisible = true
        } else {
            if (currentOffset > previousScrollOffset + 15) {
                isFabVisible = false
            } else if (currentOffset < previousScrollOffset - 15) {
                isFabVisible = true
            }
        }
        
        previousIndex = currentIndex
        previousScrollOffset = currentOffset
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var selectedLongPressedTask by remember { mutableStateOf<com.example.todo.core.database.entity.TaskEntity?>(null) }
    var showMoveCategoryDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showPermissionPrompt by remember { mutableStateOf(false) }

    var showConfetti by remember { mutableStateOf(false) }
    var prevTodayPendingCount by remember { mutableStateOf<Int?>(null) }

    // ── Completion animation overlay state ───────────────────────────────
    var showXpChip by remember { mutableStateOf(false) }
    var xpAmount by remember { mutableIntStateOf(0) }
    var showStreakBanner by remember { mutableStateOf(false) }
    var streakCount by remember { mutableIntStateOf(0) }
    var unlockedAchievement by remember { mutableStateOf<AchievementEntity?>(null) }

    // Collect one-shot completion events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.completionEvents.collect { event ->
            // XP chip
            if (event.xpAwarded > 0) {
                xpAmount = event.xpAwarded
                showXpChip = true
            }
            // Streak banner
            if (event.newStreak != null) {
                streakCount = event.newStreak
                showStreakBanner = true
            }
            // Achievement unlock
            if (event.unlockedAchievement != null) {
                unlockedAchievement = event.unlockedAchievement
            }
        }
    }

    val todayStart = remember { com.example.todo.common.util.DateUtils.startOfDay() }
    val todayPendingCount = remember(state.tasks) {
        state.tasks.count {
            it.dueDateMillis != null &&
            com.example.todo.common.util.DateUtils.startOfDay(it.dueDateMillis) == todayStart &&
            it.status != "COMPLETED"
        }
    }

    LaunchedEffect(todayPendingCount) {
        if (prevTodayPendingCount != null && prevTodayPendingCount!! > 0 && todayPendingCount == 0) {
            showConfetti = true
        }
        prevTodayPendingCount = todayPendingCount
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.checkForBackup()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missingPermissions = permissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            viewModel.checkForBackup()
        }
    }

    LaunchedEffect(state.tasks, state.isLoading) {
        if (!state.isLoading && state.tasks.isEmpty()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                if (!android.os.Environment.isExternalStorageManager()) {
                    showPermissionPrompt = true
                }
            }
        }
    }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (hasPermission && !state.isLoading && state.tasks.isEmpty()) {
            viewModel.checkForBackup()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                GradientFab(
                    onClick = onAddTask,
                    icon = Icons.Default.Add
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Sort button
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option.label,
                                        fontWeight = if (state.sortOption == option) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (state.sortOption == option) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MustDoColors.Primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        shadowElevation = if (state.searchQuery.isNotBlank()) 4f else 0f
                        shape = RoundedCornerShape(18.dp)
                        clip = false
                        ambientShadowColor = MustDoColors.Primary
                        spotShadowColor = MustDoColors.Primary
                    },
                placeholder = { Text("Search tasks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MustDoColors.Primary,
                    unfocusedBorderColor = MustDoColors.DarkBorder
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.selectedFilter == filter && state.selectedCategoryId == null,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.label, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MustDoColors.Primary.copy(alpha = 0.15f),
                            selectedLabelColor = MustDoColors.Primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.selectedFilter == filter && state.selectedCategoryId == null,
                            borderColor = MustDoColors.DarkBorder,
                            selectedBorderColor = Color.Transparent
                        )
                    )
                }

                // Category filters
                state.categories.forEach { category ->
                    val catColor = try {
                        androidx.compose.ui.graphics.Color(
                            android.graphics.Color.parseColor(category.colorHex)
                        )
                    } catch (e: Exception) {
                        MustDoColors.Primary
                    }

                    FilterChip(
                        selected = state.selectedCategoryId == category.id,
                        onClick = {
                            viewModel.setCategoryFilter(
                                if (state.selectedCategoryId == category.id) null else category.id
                            )
                        },
                        label = { Text(category.name, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = catColor.copy(alpha = 0.15f),
                            selectedLabelColor = catColor,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.selectedCategoryId == category.id,
                            borderColor = MustDoColors.DarkBorder,
                            selectedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Box replacing PullToRefreshBox
            Box(
                modifier = Modifier.weight(1f)
            ) {
                if (state.tasks.isEmpty() && !state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Outlined.Inbox,
                            title = if (state.searchQuery.isNotBlank()) "No results" else "No tasks yet",
                            subtitle = if (state.searchQuery.isNotBlank())
                                "Try a different search term"
                            else
                                "Tap the button below to create your first task",
                            modifier = Modifier.fillMaxSize(),
                            action = {
                                if (state.searchQuery.isBlank()) {
                                    FilledTonalButton(onClick = onAddTask) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Create Task")
                                    }
                                }
                            }
                        )
                    }
                } else {
                    val groupedTasks = remember(state.tasks, state.sortOption) {
                    if (state.sortOption == SortOption.CUSTOM) {
                        mapOf<Long?, List<TaskEntity>>(null to state.tasks)
                    } else {
                        state.tasks.groupBy { task ->
                            task.dueDateMillis?.let { com.example.todo.common.util.DateUtils.startOfDay(it) }
                        }.toSortedMap(compareBy { it ?: Long.MAX_VALUE })
                    }
                }

                val todayStart = remember { com.example.todo.common.util.DateUtils.startOfDay() }
                val scrollToTodayIndex = remember(groupedTasks, todayStart, state.sortOption) {
                    if (state.sortOption == SortOption.CUSTOM) return@remember null
                    
                    var index = 0
                    var foundIndex: Int? = null
                    for ((dateStart, tasksForDate) in groupedTasks) {
                        if (dateStart != null && dateStart >= todayStart) {
                            foundIndex = index
                            break
                        }
                        index += 1 // header item
                        index += tasksForDate.size // task items
                    }
                    foundIndex
                }

                var hasScrolledToToday by remember { mutableStateOf(false) }
                LaunchedEffect(state.isLoading, scrollToTodayIndex) {
                    if (!state.isLoading && scrollToTodayIndex != null && !hasScrolledToToday) {
                        listState.scrollToItem(scrollToTodayIndex)
                        hasScrolledToToday = true
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedTasks.forEach { (dateStart, tasksForDate) ->
                        if (state.sortOption != SortOption.CUSTOM) {
                            item(key = "header_${dateStart ?: 0}") {
                                val headerText = if (dateStart == null) {
                                    "No Due Date"
                                } else {
                                    java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault()).format(java.util.Date(dateStart))
                                }
                                Text(
                                    text = headerText,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MustDoColors.Primary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                                )
                            }
                        }

                        items(
                            items = tasksForDate,
                            key = { it.id }
                        ) { task ->
                            val category = state.categories.find { it.id == task.categoryId }
                            
                            Box(modifier = Modifier.animateItem()) {
                            SwipeableTaskItem(
                                onComplete = {
                                    viewModel.completeTask(task.id)
                                },
                                onArchive = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.archiveTask(task.id)
                                    coroutineScope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Task archived!",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.unarchiveTask(task.id)
                                        }
                                    }
                                }
                            ) {
                                val hasResources = (state.taskResourcesMap[task.id] ?: emptyList()).isNotEmpty()
                                val noteBlocks = state.taskNoteBlocksMap[task.id] ?: emptyList()
                                val hasNoteBlocks = noteBlocks.isNotEmpty()
                                val checklistBlocks = noteBlocks.filter { it.blockType == "CHECKLIST" }
                                val checklistTotal = checklistBlocks.size
                                val checklistChecked = checklistBlocks.count { it.content.startsWith("[x] ") }
                                
                                TaskCard(
                                    task = task,
                                    onTaskClick = onTaskClick,
                                    onCompleteClick = { 
                                        viewModel.completeTask(it)
                                    },
                                    categoryName = category?.name,
                                    categoryColor = category?.colorHex,
                                    onLongClick = { selectedLongPressedTask = task },
                                    hasResources = hasResources,
                                    hasNoteBlocks = hasNoteBlocks,
                                    onInfoClick = { viewModel.showTaskDetails(task) },
                                    checklistChecked = checklistChecked,
                                    checklistTotal = checklistTotal,
                                    completionEventFlow = viewModel.completionEvents
                                )
                        }
                    }
                    }
                }
                // Bottom spacer for FAB
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
            }
        }
        ConfettiAnimation(trigger = showConfetti, onAnimationEnd = { showConfetti = false })

        // ── Completion animation overlays ────────────────────────────────


    }
    }
    }

    if (selectedLongPressedTask != null) {
        val task = selectedLongPressedTask!!
        ModalBottomSheet(
            onDismissRequest = { selectedLongPressedTask = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val isCompleted = task.status == com.example.todo.core.database.entity.TaskStatus.COMPLETED.name
                val isArchived = task.status == com.example.todo.core.database.entity.TaskStatus.ARCHIVED.name

                // 1. Mark as Undone / Completed
                ListItem(
                    headlineContent = { Text(if (isCompleted) "Mark as Undone" else "Mark as Completed") },
                    leadingContent = {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.Undo else Icons.Default.CheckCircle,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        if (isCompleted) {
                            viewModel.markTaskAsUndone(task.id)
                        } else {
                            viewModel.completeTask(task.id)
                        }
                        selectedLongPressedTask = null
                    }
                )

                // 2. Move Category
                ListItem(
                    headlineContent = { Text("Move Category") },
                    leadingContent = { Icon(Icons.Default.Label, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showMoveCategoryDialog = true
                    }
                )

                // 3. Remove from Archive / Archive Task
                ListItem(
                    headlineContent = { Text(if (isArchived) "Remove from Archive" else "Archive Task") },
                    leadingContent = {
                        Icon(
                            imageVector = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        if (isArchived) {
                            viewModel.unarchiveTask(task.id)
                        } else {
                            viewModel.archiveTask(task.id)
                        }
                        selectedLongPressedTask = null
                    }
                )

                // 4. Add/Show Notes
                ListItem(
                    headlineContent = { Text("Add/Show Notes") },
                    leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showNotesDialog = true
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 5. Delete Task
                ListItem(
                    headlineContent = { Text("Delete Task", color = MaterialTheme.colorScheme.error) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable {
                        viewModel.deleteTask(task.id)
                        selectedLongPressedTask = null
                    }
                )
            }
        }
    }

    if (showMoveCategoryDialog && selectedLongPressedTask != null) {
        val task = selectedLongPressedTask!!
        AlertDialog(
            onDismissRequest = { showMoveCategoryDialog = false },
            title = { Text("Move Category") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    ListItem(
                        headlineContent = { Text("No Category") },
                        leadingContent = {
                            RadioButton(
                                selected = task.categoryId == null,
                                onClick = {
                                    viewModel.changeTaskCategory(task.id, null)
                                    showMoveCategoryDialog = false
                                    selectedLongPressedTask = null
                                }
                            )
                        },
                        modifier = Modifier.clickable {
                            viewModel.changeTaskCategory(task.id, null)
                            showMoveCategoryDialog = false
                            selectedLongPressedTask = null
                        }
                    )

                    state.categories.forEach { category ->
                        ListItem(
                            headlineContent = { Text(category.name) },
                            leadingContent = {
                                RadioButton(
                                    selected = task.categoryId == category.id,
                                    onClick = {
                                        viewModel.changeTaskCategory(task.id, category.id)
                                        showMoveCategoryDialog = false
                                        selectedLongPressedTask = null
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.changeTaskCategory(task.id, category.id)
                                showMoveCategoryDialog = false
                                selectedLongPressedTask = null
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNotesDialog && selectedLongPressedTask != null) {
        val task = selectedLongPressedTask!!
        var notesText by remember(task.id) { mutableStateOf(task.notes) }
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            title = { Text("Task Notes") },
            text = {
                Column {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it.capitalizeFirstLetter() },
                        modifier = Modifier.fillMaxWidth().graphicsLayer {
                            shadowElevation = if (notesText.isNotBlank()) 4f else 0f
                            shape = RoundedCornerShape(18.dp)
                            clip = false
                            ambientShadowColor = MustDoColors.Primary
                            spotShadowColor = MustDoColors.Primary
                        },
                        placeholder = { Text("Add notes to this task...") },
                        minLines = 3,
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MustDoColors.Primary,
                            unfocusedBorderColor = MustDoColors.DarkBorder
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateTaskNotes(task.id, notesText.trim())
                    showNotesDialog = false
                    selectedLongPressedTask = null
                }) {
                    Text("Save", color = MustDoColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotesDialog = false
                    selectedLongPressedTask = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (state.showRestorePrompt && state.backupTimestamp != null) {
        val backupDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(state.backupTimestamp!!))

        AlertDialog(
            onDismissRequest = { viewModel.dismissRestorePrompt() },
            title = { Text("Restore Data?") },
            text = {
                Text("We automatically detected a local backup from $backupDate. Would you like to restore your tasks and settings?")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.restoreBackup() }) {
                    Text("Restore", color = MustDoColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestorePrompt() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (state.restoreErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearRestoreErrorMessage() },
            title = { Text("Restore Failed", fontWeight = FontWeight.Bold) },
            text = { Text(state.restoreErrorMessage!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearRestoreErrorMessage() }) {
                    Text("OK", color = MustDoColors.Primary)
                }
            }
        )
    }

    if (showPermissionPrompt) {
        AlertDialog(
            onDismissRequest = { showPermissionPrompt = false },
            title = { Text("Permission Required") },
            text = {
                Text("To automatically detect backups from your previous installations (stored in Downloads/MustDo/Backups), this app requires the \"All Files Access\" permission.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionPrompt = false
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        context.startActivity(intent)
                    }
                }) {
                    Text("Grant Permission", color = MustDoColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (state.selectedTaskDetails != null) {
        val task = state.selectedTaskDetails!!
        AlertDialog(
            onDismissRequest = { viewModel.hideTaskDetails() },
            title = {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (task.description.isNotBlank()) {
                        Column {
                            Text("Description", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(task.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (task.notes.isNotBlank()) {
                        Column {
                            Text("Notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(task.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Note Blocks
                    if (state.selectedTaskNoteBlocks.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Smart Notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            state.selectedTaskNoteBlocks.forEach { block ->
                                when (block.blockType) {
                                    "TEXT" -> {
                                        Text(block.content, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    "CHECKLIST" -> {
                                        val isChecked = block.content.startsWith("[x] ")
                                        val textVal = if (isChecked) block.content.removePrefix("[x] ") else block.content.removePrefix("[ ] ")
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { viewModel.toggleDetailsChecklistBlock(block.id, block.content) },
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = textVal,
                                                style = MaterialTheme.typography.bodyMedium,
                                                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    "LINK" -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                try {
                                                    val formattedUrl = if (!block.content.startsWith("http://") && !block.content.startsWith("https://")) {
                                                        "https://" + block.content
                                                    } else block.content
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Link, contentDescription = null, tint = MustDoColors.Primary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(block.content, color = MustDoColors.Primary, style = MaterialTheme.typography.bodyMedium, textDecoration = TextDecoration.Underline)
                                        }
                                    }
                                    "QUOTE" -> {
                                        Row {
                                            Box(modifier = Modifier.width(3.dp).height(24.dp).background(MustDoColors.Primary))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(block.content, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal)
                                        }
                                    }
                                    "DIVIDER" -> {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Contacts
                    val contacts = state.selectedTaskResources.filter { it.resourceType == "CONTACT" }
                    if (contacts.isNotEmpty()) {
                        val gson = com.google.gson.Gson()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Contacts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            contacts.forEach { resource ->
                                val contact = gson.fromJson(resource.payloadJson, com.example.todo.core.database.entity.ContactPayload::class.java)
                                Card(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}")))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No dialer app found", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = contact.phone.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(contact.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            if (contact.phone.isNotEmpty()) Text(contact.phone, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row {
                                            if (contact.phone.isNotEmpty()) {
                                                IconButton(onClick = {
                                                    try {
                                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}")))
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "No dialer app found", Toast.LENGTH_SHORT).show()
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = MustDoColors.Primary)
                                                }
                                            }
                                            if (contact.email.isNotEmpty()) {
                                                IconButton(onClick = {
                                                    try {
                                                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}")))
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Email, contentDescription = "Email", tint = MustDoColors.Primary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Attachments
                    val attachments = state.selectedTaskResources.filter { it.resourceType == "ATTACHMENT" }
                    if (attachments.isNotEmpty()) {
                        val gson = com.google.gson.Gson()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Attachments", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            attachments.forEach { resource ->
                                val attachment = gson.fromJson(resource.payloadJson, com.example.todo.core.database.entity.AttachmentPayload::class.java)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(attachment.fileName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                        Row {
                                            IconButton(onClick = {
                                                try {
                                                    val file = java.io.File(attachment.filePath)
                                                    if (!file.exists()) {
                                                        Toast.makeText(context, "File not found on device", Toast.LENGTH_SHORT).show()
                                                        return@IconButton
                                                    }
                                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, attachment.mimeType)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(Icons.Default.OpenInNew, contentDescription = "Open", tint = MustDoColors.Primary)
                                            }
                                            IconButton(onClick = {
                                                try {
                                                    val file = java.io.File(attachment.filePath)
                                                    if (!file.exists()) {
                                                        Toast.makeText(context, "File not found on device", Toast.LENGTH_SHORT).show()
                                                        return@IconButton
                                                    }
                                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                                        type = attachment.mimeType
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(intent, "Share File"))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Cannot share file", Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(Icons.Default.Share, contentDescription = "Share", tint = MustDoColors.Primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Voice notes
                    val voiceNotes = state.selectedTaskResources.filter { it.resourceType == "VOICE_NOTE" }
                    if (voiceNotes.isNotEmpty()) {
                        val gson = com.google.gson.Gson()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Voice Notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            voiceNotes.forEach { resource ->
                                val voice = gson.fromJson(resource.payloadJson, com.example.todo.core.database.entity.VoiceNotePayload::class.java)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val isPlaying = state.currentlyPlayingFilePath == voice.filePath && state.isPlaying
                                                IconButton(onClick = { viewModel.playVoiceNote(voice.filePath) }) {
                                                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = MustDoColors.Primary)
                                                }
                                                Text("Voice Memo", style = MaterialTheme.typography.bodyMedium)
                                            }
                                            Text(
                                                text = "${(voice.duration / 1000) / 60}:${String.format("%02d", (voice.duration / 1000) % 60)}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        if (state.currentlyPlayingFilePath == voice.filePath) {
                                            com.example.todo.common.components.LiquidSlider(
                                                value = state.playbackPositionMillis.toFloat(),
                                                onValueChange = { viewModel.seekVoiceNote(it.toLong()) },
                                                valueRange = 0f..state.playbackDurationMillis.toFloat()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Locations
                    val locations = state.selectedTaskResources.filter { it.resourceType == "LOCATION" }
                    if (locations.isNotEmpty()) {
                        val gson = com.google.gson.Gson()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Locations", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            locations.forEach { resource ->
                                val location = gson.fromJson(resource.payloadJson, com.example.todo.core.database.entity.LocationPayload::class.java)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(location.locationName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            if (location.address.isNotEmpty()) Text(location.address, style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (location.locationLink.isNotEmpty()) {
                                            IconButton(onClick = {
                                                try {
                                                    val formattedUrl = if (!location.locationLink.startsWith("http://") && !location.locationLink.startsWith("https://")) {
                                                        "https://" + location.locationLink
                                                    } else location.locationLink
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Cannot open location link", Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(Icons.Default.Link, contentDescription = "Open Link", tint = MustDoColors.Primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.hideTaskDetails() }) {
                    Text("Close", color = MustDoColors.Primary)
                }
            }
        )
    }

    // Achievement unlock bottom sheet
    //AchievementUnlockSheet(
     //   achievement = unlockedAchievement,
     //   onDismiss = { unlockedAchievement = null }
   // )
}

@Composable
private fun SwipeableTaskItem(
    onComplete: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var itemWidth by remember { mutableIntStateOf(0) }
    val swipeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var hapticTriggered by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { itemWidth = it.width }
            .clip(RoundedCornerShape(16.dp))
    ) {
        val widthPx = itemWidth.toFloat()
        val threshold = widthPx * 0.6f

        if (itemWidth > 0 && swipeOffset.value != 0f) {
            val isSwipingRight = swipeOffset.value > 0
            val progress = (kotlin.math.abs(swipeOffset.value) / widthPx).coerceIn(0f, 1f)
            val isPastThreshold = kotlin.math.abs(swipeOffset.value) >= threshold

            LaunchedEffect(isPastThreshold) {
                if (isPastThreshold && !hapticTriggered) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    hapticTriggered = true
                } else if (!isPastThreshold) {
                    hapticTriggered = false
                }
            }

            val backgroundColor = if (isSwipingRight) {
                if (isPastThreshold) MustDoColors.Success else MustDoColors.Success.copy(alpha = progress.coerceIn(0.1f, 0.6f))
            } else {
                if (isPastThreshold) MustDoColors.Warning else MustDoColors.Warning.copy(alpha = progress.coerceIn(0.1f, 0.6f))
            }

            val icon = if (isSwipingRight) Icons.Default.CheckCircle else Icons.Default.Archive
            val iconAlignment = if (isSwipingRight) Alignment.CenterStart else Alignment.CenterEnd
            val iconScale = if (isPastThreshold) 1.3f else (0.6f + progress * 0.7f).coerceIn(0.6f, 1.3f)
            val iconPadding = 24.dp

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(backgroundColor, shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = iconPadding)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineX = if (isSwipingRight) threshold else size.width - threshold
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(lineX, 0f),
                        end = Offset(lineX, size.height),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(iconAlignment)
                        .graphicsLayer(
                            scaleX = iconScale,
                            scaleY = iconScale
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
                .pointerInput(itemWidth) {
                    if (itemWidth <= 0) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (kotlin.math.abs(swipeOffset.value) >= threshold) {
                                    val targetOffset = if (swipeOffset.value > 0) widthPx else -widthPx
                                    swipeOffset.animateTo(targetOffset)
                                    if (swipeOffset.value > 0) onComplete() else onArchive()
                                    swipeOffset.snapTo(0f)
                                } else {
                                    swipeOffset.animateTo(0f)
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                swipeOffset.animateTo(0f)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val newOffset = swipeOffset.value + dragAmount
                                swipeOffset.snapTo(newOffset.coerceIn(-widthPx, widthPx))
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}

// ─── Confetti Animation ──────────────────────────────────────────────────────

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    val color: Color,
    val size: Float,
    val speedX: Float,
    var speedY: Float,
    val rotationSpeed: Float,
    var rotation: Float = 0f
)

@Composable
fun ConfettiAnimation(trigger: Boolean, onAnimationEnd: () -> Unit) {
    if (!trigger) return

    val transition = rememberInfiniteTransition(label = "confetti")
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val particles = remember {
        List(80) {
            ConfettiParticle(
                x = 0f,
                y = 0f,
                color = listOf(
                    Color(0xFFFFC107), Color(0xFFFF5722), Color(0xFFE91E63),
                    Color(0xFF9C27B0), Color(0xFF3F51B5), Color(0xFF00BCD4),
                    Color(0xFF4CAF50), Color(0xFF8BC34A)
                ).random(),
                size = kotlin.random.Random.nextFloat() * 12f + 8f,
                speedX = (kotlin.random.Random.nextFloat() - 0.5f) * 15f,
                speedY = -(kotlin.random.Random.nextFloat() * 15f + 15f),
                rotationSpeed = kotlin.random.Random.nextFloat() * 360f - 180f
            )
        }
    }

    var timeStep by remember { mutableStateOf(0) }

    LaunchedEffect(animProgress) {
        timeStep++
        if (timeStep > 60) {
            onAnimationEnd()
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            if (p.x == 0f && p.y == 0f) {
                p.x = width / 2
                p.y = height + 50f
            }

            p.x += p.speedX
            p.y += p.speedY
            p.speedY += 0.5f // gravity
            p.rotation += p.rotationSpeed * 0.016f

            rotate(p.rotation, Offset(p.x, p.y)) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(p.x - p.size / 2, p.y - p.size / 2),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
fun TasksScreenPreview() {
    MustDoTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tasks",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }

                // Filter Chips Mockup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MustDoColors.Primary.copy(alpha = 0.15f),
                            selectedLabelColor = MustDoColors.Primary
                        )
                    )
                    FilterChip(
                        selected = false,
                        onClick = {},
                        label = { Text("Pending") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {},
                        label = { Text("Completed") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tasks List Mockup
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "12/07/26",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    item {
                        TaskCard(
                            task = TaskEntity(
                                id = "1",
                                title = "Day-3 • Session 1 - Python Collections",
                                description = "AI & Machine Learning Engineer Bootcamp...",
                                status = "PENDING",
                                priority = "High",
                                categoryId = "1"
                            ),
                            onTaskClick = {},
                            onCompleteClick = {},
                            categoryName = "Learning",
                            categoryColor = "#FFD43B",
                            onLongClick = {},
                            hasResources = true,
                            hasNoteBlocks = true,
                            onInfoClick = {},
                            checklistChecked = 0,
                            checklistTotal = 9,
                            completionEventFlow = kotlinx.coroutines.flow.MutableSharedFlow()
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            // Mock FAB
            FloatingActionButton(
                onClick = {},
                containerColor = MustDoColors.Primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    }
}


