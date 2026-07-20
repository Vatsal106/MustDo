package com.example.todo.features.dashboard.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.common.components.*
import com.example.todo.common.theme.MustDoColors
import com.example.todo.common.theme.MustDoTheme
import androidx.compose.ui.tooling.preview.Preview
import com.example.todo.common.util.DateUtils
import com.example.todo.core.database.entity.TaskEntity
import com.example.todo.core.database.entity.TaskStatus
import java.util.Calendar
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext

@Composable
fun Sparkline(
    points: List<Int>,
    modifier: Modifier = Modifier,
    lineColor: Color = MustDoColors.Primary,
    lineWidth: Float = 4f
) {
    if (points.isEmpty()) return
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val maxVal = (points.maxOrNull() ?: 1).toFloat()
        val minVal = (points.minOrNull() ?: 0).toFloat()
        val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

        val path = Path()
        val segmentWidth = width / (points.size - 1).coerceAtLeast(1)

        points.forEachIndexed { index, value ->
            val x = index * segmentWidth
            val y = height - ((value - minVal) / range) * (height - 8f) - 4f
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = lineWidth)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onTaskClick: (String) -> Unit,
    onAddTask: () -> Unit,
    onNavigateToReports: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val now = System.currentTimeMillis()
    val todayStart = DateUtils.startOfDay(now)
    val tomorrowStart = DateUtils.startOfTomorrow(now)
    val tomorrowEnd = DateUtils.endOfDay(tomorrowStart)
    
    val calendar = Calendar.getInstance().apply {
        timeInMillis = todayStart
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        add(Calendar.WEEK_OF_YEAR, 1)
    }
    val endOfWeek = calendar.timeInMillis

    val listState = rememberLazyListState()
    var previousIndex by remember { mutableStateOf(0) }
    var previousScrollOffset by remember { mutableStateOf(0) }
    var isFabVisible by remember { mutableStateOf(true) }
    var isStatsExpanded by remember { mutableStateOf(false) }

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MustDoColors.Primary)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Greeting + Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = state.greeting,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Streak: ${state.currentStreak}🔥 • Level ${state.currentLevel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MustDoColors.Primary
                            )
                        }
                        IconButton(onClick = onNavigateToReports) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Reports",
                                tint = MustDoColors.Primary
                            )
                        }
                    }
                }

                // Global Search Bar
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth().graphicsLayer {
                                shadowElevation = if (state.searchQuery.isNotBlank()) 4f else 0f
                                shape = RoundedCornerShape(18.dp)
                                this.clip = false
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
                        
                        if (state.searchQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.searchResults.isEmpty()) {
                                Text(
                                    "No results found",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.searchResults.forEach { task ->
                                        TaskCard(
                                            task = task,
                                            onTaskClick = onTaskClick,
                                            onCompleteClick = { viewModel.completeTask(it) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val healthScore = state.productivityHealthScore
                    val healthColor = when {
                        healthScore == null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        healthScore >= 80 -> MustDoColors.Success
                        healthScore >= 50 -> MustDoColors.Warning
                        else -> MustDoColors.Accent
                    }
                    val qualityColor = when (state.completionQuality) {
                        "Excellent" -> MustDoColors.Success
                        "Good" -> MustDoColors.Warning
                        else -> MustDoColors.Accent
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = healthColor.copy(alpha = 0.08f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressRing(
                                    progress = (healthScore ?: 0) / 100f,
                                    modifier = Modifier.size(84.dp),
                                    strokeWidth = 8.dp,
                                    progressColor = healthColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (healthScore != null) "$healthScore%" else "N/A",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Productivity Health",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = qualityColor.copy(alpha = 0.15f),
                                            contentColor = qualityColor
                                        ) {
                                            Text(
                                                text = if (healthScore != null) state.completionQuality else "Insufficient Data",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "On-Time: ${state.onTimeCompletionRate30Days?.let { "$it%" } ?: "N/A"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            // Health Score Explanations
                            if (state.healthScoreExplanation.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Health Impact:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    state.healthScoreExplanation.forEach { item ->
                                        Text(
                                            text = item,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (item.startsWith("•")) MustDoColors.Accent else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            // 30-Day Sparkline Trend Chart
                            Spacer(modifier = Modifier.height(16.dp))
                            Column {
                                Text(
                                    text = "30-Day Accountability Trend",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (state.healthScoreTrend.size > 1) {
                                    Sparkline(
                                        points = state.healthScoreTrend,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp),
                                        lineColor = healthColor
                                    )
                                } else {
                                    Text(
                                        text = "Not Enough History",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Today's Progress & Workload (Rank 2)
                item {
                    val workloadColor = when (state.todayWorkload) {
                        "Light" -> MustDoColors.Success
                        "Moderate" -> MustDoColors.Warning
                        else -> MustDoColors.Accent
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressRing(
                                progress = state.completionPercentage,
                                modifier = Modifier.size(68.dp),
                                strokeWidth = 6.dp,
                                progressColor = MustDoColors.Primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${(state.completionPercentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Today's Workload",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = workloadColor.copy(alpha = 0.15f),
                                        contentColor = workloadColor
                                    ) {
                                        Text(
                                            text = state.todayWorkload,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Always resolve your Task Debt first before starting newer tasks.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Analytics Toggle Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isStatsExpanded = !isStatsExpanded }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Analytics & Performance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Icon(
                            imageVector = if (isStatsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Stats",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isStatsExpanded) {
                    // 3. Weekly Review Card (Hero Analytics, Rank 3)
                    item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Weekly Performance Review",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MustDoColors.Primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MustDoColors.Success)
                                    Text(text = "${state.weeklyCompletedCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "Completed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, tint = MustDoColors.Accent)
                                    Text(text = "${state.weeklyMissedCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "Missed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = MustDoColors.Info)
                                    Text(text = state.weeklyOnTimeRate?.let { "$it%" } ?: "N/A", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "On-Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MustDoColors.Warning)
                                    Text(text = "${"%.1f".format(state.weeklyFocusHours)}h", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "Focus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // 4. Accountability Status Bar (Task Debt + Inbox Tasks, Rank 4)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val backlogColor = when (state.backlogHealth) {
                            "Healthy" -> MustDoColors.Success
                            "Warning" -> MustDoColors.Warning
                            else -> MustDoColors.Accent
                        }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(12.dp)
                            ) {
                                Icon(Icons.Default.ReportProblem, contentDescription = null, tint = backlogColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "${state.taskDebt}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = "Task Debt", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = backlogColor.copy(alpha = 0.15f),
                                    contentColor = backlogColor,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = state.backlogHealth,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                
                                // 7-Day Task Debt Trend Sparkline
                                if (state.taskDebtTrend.size > 1) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Sparkline(
                                        points = state.taskDebtTrend,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(20.dp),
                                        lineColor = backlogColor
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Not Enough History",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        val inboxColor = when (state.inboxHealth) {
                            "Healthy" -> MustDoColors.Success
                            "Warning" -> MustDoColors.Warning
                            else -> MustDoColors.Accent
                        }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(12.dp)
                            ) {
                                Icon(Icons.Default.Inbox, contentDescription = null, tint = inboxColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "${state.unscheduledCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = "Inbox Tasks", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = inboxColor.copy(alpha = 0.15f),
                                    contentColor = inboxColor,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = state.inboxHealth,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Recovery Score & Focus Consistency (Rank 5)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val recoveryColor = when (state.recoveryScore) {
                            "Perfect" -> MustDoColors.Success
                            "Excellent" -> MustDoColors.Success
                            "Good" -> MustDoColors.Info
                            "Stable" -> MaterialTheme.colorScheme.onSurfaceVariant
                            "Neutral" -> MustDoColors.Warning
                            else -> MustDoColors.Accent
                        }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = recoveryColor)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Recovery", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = state.recoveryScore, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = recoveryColor)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MustDoColors.Primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Focus Ratio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "${state.focusDaysInLast7}/7 Days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 6. Overdue Warnings List (Rank 6)
                if (state.overdueTasks.isNotEmpty()) {
                    item {
                        SectionHeader(title = "⚠️ Overdue Warnings (${state.overdueTasks.size})")
                    }
                    items(
                        items = state.overdueTasks.take(5),
                        key = { "overdue_${it.id}" }
                    ) { task ->
                        val delayDays = if (task.dueDateMillis != null) {
                            ((now - task.dueDateMillis) / (24L * 60 * 60 * 1000)).toInt()
                        } else 0
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val category = state.categories.find { it.id == task.categoryId }
                                val hasResources = (state.taskResourcesMap[task.id] ?: emptyList()).isNotEmpty()
                                val noteBlocks = state.taskNoteBlocksMap[task.id] ?: emptyList()
                                val hasNoteBlocks = noteBlocks.isNotEmpty()
                                val checklistBlocks = noteBlocks.filter { it.blockType == "CHECKLIST" }
                                val checklistTotal = checklistBlocks.size
                                val checklistChecked = checklistBlocks.count { it.content.startsWith("[x] ") }
                                TaskCard(
                                    task = task,
                                    onTaskClick = onTaskClick,
                                    onCompleteClick = { viewModel.completeTask(it) },
                                    categoryName = category?.name,
                                    categoryColor = category?.colorHex,
                                    hasResources = hasResources,
                                    hasNoteBlocks = hasNoteBlocks,
                                    onInfoClick = { viewModel.showTaskDetails(task) },
                                    checklistChecked = checklistChecked,
                                    checklistTotal = checklistTotal
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MustDoColors.Accent.copy(alpha = 0.15f),
                                    contentColor = MustDoColors.Accent,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        text = "$delayDays Days Overdue",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 7. Most Delayed Task Alert
                if (state.mostDelayedTask != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MustDoColors.Accent.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, MustDoColors.Accent.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Report, contentDescription = null, tint = MustDoColors.Accent)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Critical Procrastination Warning!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MustDoColors.Accent
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "\"${state.mostDelayedTask?.title}\" is currently ${state.mostDelayedDays} days overdue. Resolve this bottleneck immediately to recover your health score.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // 8. Upcoming Deadlines (Rank 8)
                item {
                    val activeToday = state.todayTasks.filter { it.status != TaskStatus.COMPLETED.name }.size
                    val activeTomorrow = state.allTasks.filter { task ->
                        task.dueDateMillis != null &&
                                task.dueDateMillis >= tomorrowStart &&
                                task.dueDateMillis <= tomorrowEnd &&
                                task.status != TaskStatus.COMPLETED.name &&
                                task.status != TaskStatus.ARCHIVED.name
                    }.size
                    val activeThisWeek = state.allTasks.filter { task ->
                        task.dueDateMillis != null &&
                                task.dueDateMillis >= todayStart &&
                                task.dueDateMillis <= endOfWeek &&
                                task.status != TaskStatus.COMPLETED.name &&
                                task.status != TaskStatus.ARCHIVED.name
                    }.size

                    Column {
                        SectionHeader(title = "📅 Upcoming Deadlines")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "$activeToday", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Tomorrow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "$activeTomorrow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "This Week", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "$activeThisWeek", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 9. XP Progress Context (Rank 9)
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Level ${state.currentLevel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${state.xpNeededToNextLevel} XP to Level ${state.currentLevel + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MustDoColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { state.xpProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = MustDoColors.Primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }

                // 10. Achievements Visibility & Categorization (Rank 10)
                item {
                    Column {
                        SectionHeader(title = "🏆 Achievements")
                        
                        // Next Achievement
                        if (state.nextAchievement != null) {
                            Text(
                                text = "Next Achievement",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MustDoColors.Primary.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, MustDoColors.Primary.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.StarBorder, contentDescription = null, tint = MustDoColors.Primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(text = state.nextAchievement?.title ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(text = state.nextAchievement?.description ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { state.nextAchievementProgress },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = MustDoColors.Primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${(state.nextAchievementProgress * 100).toInt()}% Completed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Unlocked Badges Row/Grid
                        if (state.unlockedAchievements.isNotEmpty()) {
                            Text(
                                text = "Unlocked Badges (${state.unlockedAchievements.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.unlockedAchievements.forEach { ach ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MustDoColors.Success.copy(alpha = 0.05f)),
                                        border = BorderStroke(1.dp, MustDoColors.Success.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MustDoColors.Success, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = ach.title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Locked Badges List
                        if (state.lockedAchievements.isNotEmpty()) {
                            Text(
                                text = "Locked Badges",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.lockedAchievements.forEach { ach ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = ach.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                } // End if (isStatsExpanded)

                // Today's tasks list header
                item {
                    SectionHeader(title = "📋 Today's Tasks")
                }

                if (state.todayTasks.isEmpty() && state.overdueTasks.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.TaskAlt,
                            title = "All clear!",
                            subtitle = "No tasks due today. Enjoy your day or add a new task.",
                            action = {
                                FilledTonalButton(onClick = onAddTask) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add Task")
                                }
                            }
                        )
                    }
                } else {
                    items(
                        items = state.todayTasks,
                        key = { "today_${it.id}" }
                    ) { task ->
                        val category = state.categories.find { it.id == task.categoryId }
                        val hasResources = (state.taskResourcesMap[task.id] ?: emptyList()).isNotEmpty()
                        val hasNoteBlocks = (state.taskNoteBlocksMap[task.id] ?: emptyList()).isNotEmpty()
                        TaskCard(
                            task = task,
                            onTaskClick = onTaskClick,
                            onCompleteClick = { viewModel.completeTask(it) },
                            categoryName = category?.name,
                            categoryColor = category?.colorHex,
                            hasResources = hasResources,
                            hasNoteBlocks = hasNoteBlocks,
                            onInfoClick = { viewModel.showTaskDetails(task) }
                        )
                    }
                }

                // Bottom spacer for FAB
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (state.selectedTaskDetails != null) {
        val task = state.selectedTaskDetails!!
        val context = LocalContext.current
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
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
fun DashboardScreenPreview() {
    MustDoTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Greeting
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Good Evening!",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Streak: 5🔥 • Level 10",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MustDoColors.Primary
                            )
                        }
                    }
                }

                // Stats Card mockup
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MustDoColors.Success.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressRing(
                                progress = 0.85f,
                                modifier = Modifier.size(84.dp),
                                strokeWidth = 8.dp,
                                progressColor = MustDoColors.Success,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "85%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(
                                    text = "Productivity Health",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Excellent Quality",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MustDoColors.Success
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

