package com.example.todo.features.dashboard.domain

import com.example.todo.common.util.DateUtils
import com.example.todo.core.database.entity.*
import com.example.todo.features.dashboard.presentation.DashboardUiState
import java.util.Calendar

object DashboardAnalyticsCalculator {
    fun calculate(
        allTasks: List<TaskEntity>,
        focusSessions: List<FocusSessionEntity>,
        streak: Int,
        xp: Int,
        achievements: List<AchievementEntity>,
        now: Long,
        motivationalQuote: String
    ): DashboardUiState {
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

        // 1. Today's Tasks
        val todayTasksList = allTasks.filter { task ->
            task.status != TaskStatus.ARCHIVED.name &&
                    task.dueDateMillis != null &&
                    DateUtils.isSameDay(task.dueDateMillis, now)
        }

        // 2. Overdue Tasks
        val overdueTasksList = allTasks.filter { task ->
            task.dueDateMillis != null &&
                    task.dueDateMillis < todayStart &&
                    task.status != TaskStatus.COMPLETED.name &&
                    task.status != TaskStatus.ARCHIVED.name
        }

        // 3. Task Debt
        val taskDebtCount = overdueTasksList.size

        // 4. Backlog Health
        val backlogHealthStatus = when {
            taskDebtCount == 0 -> "Healthy"
            taskDebtCount in 1..3 -> "Warning"
            taskDebtCount in 4..10 -> "Risk"
            else -> "Critical"
        }

        // 5. Unscheduled & Inbox Health
        val unscheduledTasksList = allTasks.filter { task ->
            task.dueDateMillis == null &&
                    task.status != TaskStatus.COMPLETED.name &&
                    task.status != TaskStatus.ARCHIVED.name
        }
        val unscheduledCountVal = unscheduledTasksList.size
        val inboxHealthStatus = when {
            unscheduledCountVal <= 5 -> "Healthy"
            unscheduledCountVal <= 15 -> "Warning"
            else -> "Needs Planning"
        }

        // 6. Today's Workload
        val activeTodayCount = todayTasksList.filter { it.status != TaskStatus.COMPLETED.name }.size
        val workloadStatus = when {
            activeTodayCount <= 3 -> "Light"
            activeTodayCount <= 8 -> "Moderate"
            else -> "Heavy"
        }

        // 7. Today's Completion percentage (Completed due today / Total due today)
        val totalDueToday = todayTasksList
        val completedDueToday = todayTasksList.filter { it.status == TaskStatus.COMPLETED.name }
        val completionPercent = if (totalDueToday.isNotEmpty()) {
            completedDueToday.size.toFloat() / totalDueToday.size
        } else 0f

        // Helper local function to check if a task was completed on time
        fun isTaskCompletedOnTime(task: TaskEntity): Boolean {
            val completedDate = task.completedDateMillis ?: return false
            val dueDate = task.dueDateMillis ?: return false
            return if (task.dueTimeMillis == null) {
                completedDate <= DateUtils.endOfDay(dueDate)
            } else {
                completedDate <= task.dueTimeMillis
            }
        }

        // 8. On-Time Completion Rate (30 Days)
        val completedIn30Days = allTasks.filter { task ->
            task.status == TaskStatus.COMPLETED.name &&
                    task.completedDateMillis != null &&
                    task.completedDateMillis >= thirtyDaysAgo
        }
        val onTimeCompletedIn30Days = completedIn30Days.filter { isTaskCompletedOnTime(it) }
        val onTimeRate = if (completedIn30Days.isNotEmpty()) {
            (onTimeCompletedIn30Days.size.toFloat() / completedIn30Days.size * 100).toInt()
        } else null

        // 9. Miss Rate (30 Days)
        val tasksDueIn30Days = allTasks.filter { task ->
            task.status != TaskStatus.ARCHIVED.name &&
                    task.dueDateMillis != null &&
                    task.dueDateMillis >= thirtyDaysAgo &&
                    task.dueDateMillis < todayStart
        }
        val missedTasksIn30Days = tasksDueIn30Days.filter { task ->
            task.status != TaskStatus.COMPLETED.name || !isTaskCompletedOnTime(task)
        }
        val missRate = if (tasksDueIn30Days.isNotEmpty()) {
            (missedTasksIn30Days.size.toFloat() / tasksDueIn30Days.size * 100).toInt()
        } else 0

        // 10. Completion Quality
        val completionQualityStatus = when {
            onTimeRate == null -> "Good"
            onTimeRate >= 85 && missRate < 15 -> "Excellent"
            onTimeRate < 60 || missRate > 30 -> "Poor"
            else -> "Good"
        }

        // 11. Recovery Score (Last 7 Days)
        val overdueResolved = allTasks.filter { task ->
            task.status == TaskStatus.COMPLETED.name &&
                    task.completedDateMillis != null &&
                    task.completedDateMillis >= sevenDaysAgo &&
                    task.dueDateMillis != null &&
                    !isTaskCompletedOnTime(task)
        }
        val newOverdue = allTasks.filter { task ->
            task.status != TaskStatus.ARCHIVED.name &&
                    task.dueDateMillis != null &&
                    task.dueDateMillis >= sevenDaysAgo &&
                    task.dueDateMillis < todayStart &&
                    (task.status != TaskStatus.COMPLETED.name || !isTaskCompletedOnTime(task))
        }
        val recoveryScoreVal = when {
            newOverdue.isEmpty() && overdueResolved.isEmpty() -> "Stable"
            newOverdue.isEmpty() && overdueResolved.isNotEmpty() -> "Perfect Recovery"
            newOverdue.isNotEmpty() && overdueResolved.isEmpty() -> "Poor"
            else -> {
                val ratio = overdueResolved.size.toFloat() / newOverdue.size
                when {
                    ratio >= 2.0f -> "Excellent"
                    ratio >= 1.0f -> "Good"
                    ratio > 0.0f -> "Neutral"
                    else -> "Poor"
                }
            }
        }

        // 12. Productivity Health Score & Explanations
        val hasAnyActivityData = totalDueToday.isNotEmpty() || completedIn30Days.isNotEmpty() || overdueTasksList.isNotEmpty()
        val finalHealthScore: Int?
        val explanationList = mutableListOf<String>()

        if (!hasAnyActivityData) {
            finalHealthScore = null
        } else {
            val baseScore = when {
                totalDueToday.isNotEmpty() && completedIn30Days.isNotEmpty() && onTimeRate != null -> {
                    ((completionPercent * 100).toInt() + onTimeRate) / 2
                }
                totalDueToday.isNotEmpty() -> {
                    (completionPercent * 100).toInt()
                }
                completedIn30Days.isNotEmpty() && onTimeRate != null -> {
                    onTimeRate
                }
                else -> 100
            }
            explanationList.add("Base score: $baseScore%")

            val overduePenalty = when {
                taskDebtCount == 0 -> 0
                taskDebtCount in 1..3 -> 10
                taskDebtCount in 4..10 -> 25
                else -> 50
            }
            if (overduePenalty > 0) {
                explanationList.add("• $taskDebtCount Overdue Tasks (-$overduePenalty)")
            }

            val activeIncomplete = allTasks.filter {
                it.status != TaskStatus.COMPLETED.name && it.status != TaskStatus.ARCHIVED.name
            }
            val avgReschedules = if (activeIncomplete.isNotEmpty()) {
                activeIncomplete.map { it.rescheduleCount }.average()
            } else 0.0
            val reschedulePenalty = if (avgReschedules > 2.0) 15 else 0
            if (reschedulePenalty > 0) {
                explanationList.add("• High Reschedules (Avg ${String.format("%.1f", avgReschedules)}) (-15)")
            }

            val missPenalty = if (missRate > 30) 15 else 0
            if (missPenalty > 0) {
                explanationList.add("• Miss Rate $missRate% (-$missPenalty)")
            }

            finalHealthScore = (baseScore - overduePenalty - reschedulePenalty - missPenalty).coerceIn(0, 100)
        }

        // 13. Dynamic Trends (7-Day Task Debt Trend & 30-Day Health Score Trend based on history length)
        val oldestTask = allTasks.filter { it.status != TaskStatus.ARCHIVED.name }.minByOrNull { it.createdDateMillis }
        val daysOfHistory = if (oldestTask != null) {
            ((todayEnd - DateUtils.startOfDay(oldestTask.createdDateMillis)) / (24L * 60 * 60 * 1000)).toInt() + 1
        } else 0

        val taskDebtTrendList: List<Int>
        val healthScoreTrendList: List<Int>

        if (daysOfHistory < 2) {
            taskDebtTrendList = emptyList()
            healthScoreTrendList = emptyList()
        } else {
            val debtHistoryDays = minOf(7, daysOfHistory)
            taskDebtTrendList = ((debtHistoryDays - 1) downTo 0).map { dayOffset ->
                val dayStartOffset = todayStart - (dayOffset * 24 * 60 * 60 * 1000L)
                allTasks.filter { task ->
                    task.dueDateMillis != null &&
                            task.dueDateMillis < dayStartOffset &&
                            task.createdDateMillis <= dayStartOffset &&
                            (task.completedDateMillis == null || task.completedDateMillis > dayStartOffset) &&
                            task.status != TaskStatus.ARCHIVED.name
                }.size
            }

            val healthHistoryDays = minOf(30, daysOfHistory)
            healthScoreTrendList = ((healthHistoryDays - 1) downTo 0).map { dayOffset ->
                val dayStartOffset = todayStart - (dayOffset * 24 * 60 * 60 * 1000L)
                val dayEndOffset = dayStartOffset + (24 * 60 * 60 * 1000L) - 1
                val windowStartOffset = dayStartOffset - (30L * 24 * 60 * 60 * 1000L)

                val dueOnDay = allTasks.filter { task ->
                    task.dueDateMillis != null &&
                            task.dueDateMillis >= dayStartOffset &&
                            task.dueDateMillis <= dayEndOffset &&
                            task.status != TaskStatus.ARCHIVED.name
                }
                val completedOnDay = dueOnDay.filter { task ->
                    task.status == TaskStatus.COMPLETED.name &&
                            task.completedDateMillis != null &&
                            task.completedDateMillis >= dayStartOffset &&
                            task.completedDateMillis <= dayEndOffset
                }
                val dayCompletion = if (dueOnDay.isNotEmpty()) completedOnDay.size.toFloat() / dueOnDay.size else 0f

                val completedInWindowOffset = allTasks.filter { task ->
                    task.status == TaskStatus.COMPLETED.name &&
                            task.completedDateMillis != null &&
                            task.completedDateMillis >= windowStartOffset &&
                            task.completedDateMillis <= dayEndOffset
                }
                val onTimeCompletedInWindow = completedInWindowOffset.filter { task ->
                    val completedDate = task.completedDateMillis ?: return@filter false
                    val due = task.dueDateMillis ?: return@filter false
                    if (task.dueTimeMillis == null) {
                        completedDate <= DateUtils.endOfDay(due)
                    } else {
                        completedDate <= task.dueTimeMillis!!
                    }
                }
                val windowOnTimeRateOffset = if (completedInWindowOffset.isNotEmpty()) {
                    (onTimeCompletedInWindow.size.toFloat() / completedInWindowOffset.size * 100).toInt()
                } else null

                val overdueOnDay = allTasks.filter { task ->
                    task.dueDateMillis != null &&
                            task.dueDateMillis < dayStartOffset &&
                            task.status != TaskStatus.ARCHIVED.name &&
                            (task.completedDateMillis == null || task.completedDateMillis > dayStartOffset)
                }.size

                val dayBaseScore = when {
                    dueOnDay.isNotEmpty() && windowOnTimeRateOffset != null -> {
                        ((dayCompletion * 100).toInt() + windowOnTimeRateOffset) / 2
                    }
                    dueOnDay.isNotEmpty() -> {
                        (dayCompletion * 100).toInt()
                    }
                    windowOnTimeRateOffset != null -> {
                        windowOnTimeRateOffset
                    }
                    else -> 100
                }

                val penaltyVal = when {
                    overdueOnDay == 0 -> 0
                    overdueOnDay in 1..3 -> 10
                    overdueOnDay in 4..10 -> 25
                    else -> 50
                }

                (dayBaseScore - penaltyVal).coerceIn(0, 100)
            }
        }

        // 15. Weekly Review Stats
        val weeklyCompleted = allTasks.filter { task ->
            task.status == TaskStatus.COMPLETED.name &&
                    task.completedDateMillis != null &&
                    task.completedDateMillis >= startOfWeek
        }
        val weeklyMissed = allTasks.filter { task ->
            task.status != TaskStatus.ARCHIVED.name &&
                    task.dueDateMillis != null &&
                    task.dueDateMillis >= startOfWeek &&
                    task.dueDateMillis < todayStart &&
                    (task.status != TaskStatus.COMPLETED.name || !isTaskCompletedOnTime(task))
        }
        val weeklyCompletedOnTime = weeklyCompleted.filter { isTaskCompletedOnTime(it) }
        val weeklyOnTimeRateVal = if (weeklyCompleted.isNotEmpty()) {
            (weeklyCompletedOnTime.size.toFloat() / weeklyCompleted.size * 100).toInt()
        } else null

        val weeklyFocusMinutes = focusSessions.filter { it.startTimeMillis >= startOfWeek }.sumOf { it.durationMinutes }
        val weeklyFocusHrs = weeklyFocusMinutes / 60.0f

        // 16. Focus Consistency (Last 7 Days)
        val focusDays = focusSessions.filter { it.startTimeMillis >= sevenDaysAgo }
            .map { DateUtils.startOfDay(it.startTimeMillis) }
            .distinct()
            .size

        // 17. Most Delayed Task (delay >= 3 days)
        val oldestOverdueTask = overdueTasksList.minByOrNull { it.dueDateMillis ?: Long.MAX_VALUE }
        var mostDelayed: TaskEntity? = null
        var mostDelayedDaysVal = 0
        if (oldestOverdueTask != null && oldestOverdueTask.dueDateMillis != null) {
            val delayDays = ((now - oldestOverdueTask.dueDateMillis) / (24L * 60 * 60 * 1000)).toInt()
            if (delayDays >= 3) {
                mostDelayed = oldestOverdueTask
                mostDelayedDaysVal = delayDays
            }
        }

        // 18. XP Remaining Context
        val xpNeeded = 100 - (xp % 100)

        // 19. Achievements Categorization & Next Badge Progress
        val unlockedList = achievements.filter { it.isUnlocked }
        val lockedList = achievements.filter { !it.isUnlocked }

        var bestNext: AchievementEntity? = null
        var bestNextProgress = 0f
        val totalSessionsCount = focusSessions.size
        val currentLevelVal = xp / 100
        val totalCompletedCount = allTasks.filter { it.status == TaskStatus.COMPLETED.name }.size

        for (ach in lockedList) {
            val progress = when (ach.conditionType) {
                "TASKS_COMPLETED" -> totalCompletedCount.toFloat() / ach.conditionValue
                "STREAK" -> streak.toFloat() / ach.conditionValue
                "LEVEL" -> currentLevelVal.toFloat() / ach.conditionValue
                "FOCUS_SESSIONS" -> totalSessionsCount.toFloat() / ach.conditionValue
                else -> 0f
            }.coerceIn(0f, 1f)

            if (progress > bestNextProgress) {
                bestNextProgress = progress
                bestNext = ach
            }
        }

        return DashboardUiState(
            greeting = DateUtils.getGreeting(),
            todayTasks = todayTasksList,
            overdueTasks = overdueTasksList,
            allTasks = allTasks,
            completedToday = completedDueToday.size,
            totalToday = totalDueToday.size,
            completionPercentage = completionPercent,
            taskDebt = taskDebtCount,
            backlogHealth = backlogHealthStatus,
            unscheduledCount = unscheduledCountVal,
            inboxHealth = inboxHealthStatus,
            todayWorkload = workloadStatus,
            taskDebtTrend = taskDebtTrendList,
            healthScoreTrend = healthScoreTrendList,
            productivityHealthScore = finalHealthScore,
            onTimeCompletionRate30Days = onTimeRate,
            missRate30Days = missRate,
            completionQuality = completionQualityStatus,
            recoveryScore = recoveryScoreVal,
            healthScoreExplanation = explanationList,
            weeklyCompletedCount = weeklyCompleted.size,
            weeklyMissedCount = weeklyMissed.size,
            weeklyOnTimeRate = weeklyOnTimeRateVal,
            weeklyFocusHours = weeklyFocusHrs,
            mostDelayedTask = mostDelayed,
            mostDelayedDays = mostDelayedDaysVal,
            focusDaysInLast7 = focusDays,
            currentStreak = streak,
            totalXp = xp,
            currentLevel = currentLevelVal,
            xpProgress = (xp % 100) / 100f,
            xpNeededToNextLevel = xpNeeded,
            unlockedAchievements = unlockedList,
            lockedAchievements = lockedList.filter { it.id != bestNext?.id },
            nextAchievement = bestNext,
            nextAchievementProgress = bestNextProgress,
            motivationalQuote = motivationalQuote,
            isLoading = false
        )
    }
}
