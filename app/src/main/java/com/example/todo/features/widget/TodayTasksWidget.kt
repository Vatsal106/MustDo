package com.example.todo.features.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.ColorFilter
import androidx.glance.color.ColorProvider
import com.example.todo.MainActivity
import com.example.todo.R
import com.example.todo.common.util.DateUtils
import com.example.todo.core.database.entity.TaskEntity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class TodayTasksWidget : GlanceAppWidget() {
    companion object {
        val taskIdKey = ActionParameters.Key<String>("taskId")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val taskDao = WidgetDependencies.getTaskDao(context)

        // Only fetch tasks due today or overdue (not completed, not archived)
        val now = System.currentTimeMillis()
        val todayStart = DateUtils.startOfDay(now)
        val todayEnd = DateUtils.endOfDay(now)
        val tasks = taskDao.getPendingAndOverdueTasks(todayStart, todayEnd)

        val overdueTasks = tasks.filter { it.dueDateMillis != null && it.dueDateMillis < todayStart }
        val todayTasks = tasks.filter { it.dueDateMillis != null && it.dueDateMillis >= todayStart }
        val totalCount = tasks.size
        val overdueCount = overdueTasks.size

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.surface)
                        .padding(16.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    // ── Header ──
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "Today's Tasks",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                            if (overdueCount > 0) {
                                Text(
                                    text = "$overdueCount overdue",
                                    style = TextStyle(
                                        color = ColorProvider(
                                            day = Color(0xFFEF4444),
                                            night = Color(0xFFF87171)
                                        ),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        // Task count badge
                        Box(
                            modifier = GlanceModifier
                                .cornerRadius(12.dp)
                                .background(
                                    ColorProvider(
                                        day = Color(0xFF6C63FF),
                                        night = Color(0xFF7C74FF)
                                    )
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$totalCount",
                                style = TextStyle(
                                    color = ColorProvider(
                                        day = Color.White,
                                        night = Color.White
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    // ── Divider ──
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(GlanceTheme.colors.outline)
                    ) {}

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // ── Task list or empty state ──
                    if (tasks.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🎉",
                                    style = TextStyle(fontSize = 32.sp)
                                )
                                Spacer(modifier = GlanceModifier.height(8.dp))
                                Text(
                                    text = "All done for today!",
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(
                                    text = "Enjoy your free time",
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            // Overdue section
                            if (overdueTasks.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "OVERDUE",
                                        style = TextStyle(
                                            color = ColorProvider(
                                                day = Color(0xFFEF4444),
                                                night = Color(0xFFF87171)
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        modifier = GlanceModifier.padding(top = 4.dp, bottom = 4.dp)
                                    )
                                }
                                items(overdueTasks) { task ->
                                    TaskWidgetRow(task = task, isOverdue = true)
                                }
                            }

                            // Today section
                            if (todayTasks.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "TODAY",
                                        style = TextStyle(
                                            color = GlanceTheme.colors.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        modifier = GlanceModifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }
                                items(todayTasks) { task ->
                                    TaskWidgetRow(task = task, isOverdue = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskWidgetRow(task: TaskEntity, isOverdue: Boolean) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(10.dp)
            .background(
                if (isOverdue) ColorProvider(
                    day = Color(0xFFFEF2F2),
                    night = Color(0xFF2D1B1B)
                ) else ColorProvider(
                    day = Color(0xFFF8F7FF),
                    night = Color(0xFF1E1B2E)
                )
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        androidx.glance.Image(
            provider = ImageProvider(R.drawable.ic_circle_outline),
            contentDescription = "Complete",
            modifier = GlanceModifier
                .size(22.dp)
                .clickable(
                    onClick = actionRunCallback<CompleteTaskAction>(
                        actionParametersOf(
                            TodayTasksWidget.taskIdKey to task.id
                        )
                    )
                ),
            colorFilter = ColorFilter.tint(
                when (task.priority) {
                    "URGENT" -> ColorProvider(
                        day = Color(0xFFDC2626),
                        night = Color(0xFFEF4444)
                    )
                    "HIGH" -> ColorProvider(
                        day = Color(0xFFEA580C),
                        night = Color(0xFFFB923C)
                    )
                    "MEDIUM" -> ColorProvider(
                        day = Color(0xFF6C63FF),
                        night = Color(0xFF7C74FF)
                    )
                    else -> ColorProvider(
                        day = Color(0xFF6B7280),
                        night = Color(0xFF9CA3AF)
                    )
                }
            )
        )

        Spacer(modifier = GlanceModifier.width(10.dp))

        // Task title
        Text(
            text = task.title,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )

        // Priority indicator
        if (task.priority == "URGENT" || task.priority == "HIGH") {
            Spacer(modifier = GlanceModifier.width(6.dp))
            androidx.glance.Image(
                provider = ImageProvider(R.drawable.ic_flag_small),
                contentDescription = task.priority,
                modifier = GlanceModifier.size(14.dp),
                colorFilter = ColorFilter.tint(
                    if (task.priority == "URGENT") ColorProvider(
                        day = Color(0xFFDC2626),
                        night = Color(0xFFEF4444)
                    ) else ColorProvider(
                        day = Color(0xFFEA580C),
                        night = Color(0xFFFB923C)
                    )
                )
            )
        }
    }

    Spacer(modifier = GlanceModifier.height(4.dp))
}

class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TodayTasksWidget.taskIdKey] ?: return
        android.util.Log.d("TodayTasksWidget", "CompleteTaskAction.onAction: taskId = $taskId")
        val repository = WidgetDependencies.getTaskRepository(context)
        val gamificationEngine = WidgetDependencies.getGamificationEngine(context)
        
        val completedTask = repository.completeTask(taskId)
        if (completedTask != null) {
            android.util.Log.d("TodayTasksWidget", "CompleteTaskAction.onAction: Task completed successfully via repository")
            gamificationEngine.awardTaskCompletionXp(completedTask)
            val totalCompleted = repository.getTotalCompletedCount()
            gamificationEngine.checkFirstTaskAchievement(totalCompleted)
        } else {
            android.util.Log.e("TodayTasksWidget", "CompleteTaskAction.onAction: Repository returned null for completeTask")
        }

        // Directly update all widgets to reflect the change immediately
        TodayTasksWidget().updateAll(context)
        StreakXpWidget().updateAll(context)
        FocusQuickWidget().updateAll(context)
        android.util.Log.d("TodayTasksWidget", "CompleteTaskAction.onAction: Called updateAll on all widgets")

        // Broadcast to notify any other components
        val intent = android.content.Intent("com.example.todo.action.UPDATE_WIDGETS")
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)
    }
}
