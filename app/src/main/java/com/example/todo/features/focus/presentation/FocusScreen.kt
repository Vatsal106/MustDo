package com.example.todo.features.focus.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.common.components.CircularProgressRing
import com.example.todo.common.components.StatCard
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme
import androidx.compose.ui.tooling.preview.Preview
import com.example.todo.core.database.entity.SessionType
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FocusScreen(
    initialTaskId: String? = null,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(initialTaskId) {
        if (initialTaskId != null && state.linkedTaskId == null) {
            viewModel.selectTask(initialTaskId)
        }
    }
    val haptic = LocalHapticFeedback.current

    var showDurationPicker by remember { mutableStateOf(false) }
    var sliderValue by remember(state.totalDurationMinutes) { mutableFloatStateOf(state.totalDurationMinutes.toFloat()) }
    var lastHapticValue by remember { mutableIntStateOf(state.totalDurationMinutes) }

    val minutes = state.remainingSeconds / 60
    val seconds = state.remainingSeconds % 60
    val timeDisplay = String.format("%02d:%02d", minutes, seconds)

    // Smoothly transition progress ring color based on state and progress
    val animatedProgressColor by animateColorAsState(
        targetValue = when {
            state.isCompleted -> MustDoColors.Success
            state.isBreak -> MustDoColors.Success
            else -> {
                // lerp from Primary (blue/purple) to Accent (pink/red) as progress increases
                androidx.compose.ui.graphics.lerp(MustDoColors.Primary, MustDoColors.Accent, state.progress)
            }
        },
        animationSpec = tween(500),
        label = "ring_color"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header
            Text(
                text = "Focus",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current Mode / Info display
            val modeLabel = when {
                state.isBreak -> "Break ☕"
                state.sessionType == SessionType.DEEP_WORK.name -> "Deep Work 💻"
                else -> "Pomodoro ⏱️"
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = modeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MustDoColors.Primary
                )
                if (state.isRunning && !state.isPaused && state.endTimeDisplay != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ends at ${state.endTimeDisplay}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Linked Task Selector
            if (!state.isRunning) {
                Surface(
                    onClick = { viewModel.setShowTaskSelector(true) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = MustDoColors.Primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.linkedTask?.title ?: "Link a task (Optional)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (state.linkedTask != null) {
                                Text(
                                    text = "Tap to change",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (state.linkedTask != null) {
                            IconButton(onClick = { viewModel.selectTask(null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else if (state.linkedTask != null) {
                Text(
                    text = "Focusing on: ${state.linkedTask?.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MustDoColors.Primary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Session Type Toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SessionType.entries.forEach { type ->
                    val selected = state.sessionType == type.name && !state.isBreak
                    val label = when (type) {
                        SessionType.POMODORO -> "Pomodoro"
                        SessionType.DEEP_WORK -> "Deep Work"
                    }

                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setSessionType(type.name) },
                        label = {
                            Text(
                                label,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MustDoColors.Primary.copy(alpha = 0.15f),
                            selectedLabelColor = MustDoColors.Primary
                        ),
                        enabled = !state.isRunning && !state.isBreak
                    )
                }
                if (state.isBreak) {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text("Break Active", fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MustDoColors.Success.copy(alpha = 0.15f),
                            selectedLabelColor = MustDoColors.Success
                        ),
                        enabled = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Breathing Animation
            val infiniteTransition = rememberInfiniteTransition(label = "breathing_transition")
            val breathingScale by infiniteTransition.animateFloat(
                initialValue = 0.98f,
                targetValue = 1.02f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "breathing_scale"
            )
            val ringScale = if (state.isRunning && !state.isPaused) breathingScale else 1f

            // Timer Ring
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                        shadowElevation = if (state.isRunning && !state.isPaused) 20f else 0f
                        shape = CircleShape
                        this.clip = false
                        ambientShadowColor = animatedProgressColor
                        spotShadowColor = animatedProgressColor
                    }
                    .clip(CircleShape)
                    .clickable(enabled = !state.isRunning) {
                        showDurationPicker = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
            ) {
                CircularProgressRing(
                    progress = state.progress,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    progressColor = animatedProgressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeDisplay,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 52.sp,
                                letterSpacing = 2.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                state.isCompleted -> "Session Complete! 🎉"
                                state.isPaused -> "Paused"
                                state.isRunning -> "Stay focused..."
                                else -> "${state.totalDurationMinutes} minutes"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!state.isRunning) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap to change",
                                style = MaterialTheme.typography.labelSmall,
                                color = MustDoColors.Primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick adjustment chips directly below the timer
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {
                        viewModel.adjustDuration(-5)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    label = { Text("-5 min") }
                )
                AssistChip(
                    onClick = {
                        viewModel.adjustDuration(5)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    label = { Text("+5 min") }
                )
                AssistChip(
                    onClick = {
                        viewModel.adjustDuration(10)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    label = { Text("+10 min") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset button (Long press for full reset)
                if (state.isRunning || state.isCompleted || state.isBreak) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .combinedClickable(
                                onClick = {
                                    viewModel.resetSession()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onLongClick = {
                                    viewModel.completeSessionReset()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset (Long press for full reset)",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Main button (Start / Pause / Resume)
                FloatingActionButton(
                    onClick = {
                        when {
                            state.isCompleted -> viewModel.resetSession()
                            state.isPaused -> viewModel.resumeSession()
                            state.isRunning -> viewModel.pauseSession()
                            else -> viewModel.startSession()
                        }
                    },
                    containerColor = when {
                        state.isCompleted -> MustDoColors.Success
                        state.isPaused -> MustDoColors.Warning
                        state.isRunning -> MustDoColors.Accent
                        else -> MustDoColors.Primary
                    },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = when {
                            state.isCompleted -> Icons.Default.Refresh
                            state.isPaused -> Icons.Default.PlayArrow
                            state.isRunning -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Skip button (for breaks)
                if (state.isBreak) {
                    FilledTonalIconButton(
                        onClick = {
                            viewModel.skipBreak()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Skip Break")
                    }
                }
            }

            if (state.isSoundPlaying) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.stopSound()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MustDoColors.Accent),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = "Stop sound"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Sound")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Sessions",
                    value = "${state.sessionsCompleted}",
                    icon = Icons.Default.CheckCircle,
                    color = MustDoColors.Primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Today",
                    value = "${state.todayFocusMinutes}m",
                    icon = Icons.Default.Today,
                    color = MustDoColors.Success,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Total",
                    value = "${state.totalFocusMinutes / 60}h",
                    icon = Icons.Default.Timer,
                    color = MustDoColors.Warning,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDurationPicker) {
        ModalBottomSheet(
            onDismissRequest = { showDurationPicker = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Presets
                Text(
                    text = "Presets",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val presets = listOf(15, 25, 30, 45, 60, 90)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = state.totalDurationMinutes == preset
                        InputChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.updateDuration(preset)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showDurationPicker = false
                            },
                            label = { Text("${preset}m") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Custom Slider
                Text(
                    text = "Custom: ${sliderValue.roundToInt()} minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                com.example.todo.common.components.LiquidSlider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        if (sliderValue.roundToInt() != lastHapticValue) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastHapticValue = sliderValue.roundToInt()
                        }
                    },
                    valueRange = 1f..120f
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.updateDuration(sliderValue.roundToInt())
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDurationPicker = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MustDoColors.Primary)
                ) {
                    Text("Apply Custom Duration")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    if (state.showTaskSelector) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowTaskSelector(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Select Task to Focus On",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (state.pendingTasks.isEmpty()) {
                    Text(
                        "No pending tasks available",
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(state.pendingTasks) { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectTask(task.id) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val priorityColor = when(task.priority) {
                                    "URGENT" -> MustDoColors.Accent
                                    "HIGH" -> MustDoColors.Warning
                                    "MEDIUM" -> MustDoColors.Primary
                                    else -> MustDoColors.Success
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(priorityColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (state.linkedTaskId == task.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MustDoColors.Primary
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
fun FocusScreenPreview() {
    MustDoTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Focus",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Deep Work 💻",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MustDoColors.Primary
                )
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressRing(
                    progress = 0.6f,
                    modifier = Modifier.size(240.dp),
                    strokeWidth = 12.dp,
                    progressColor = MustDoColors.Primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "25:00",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}

