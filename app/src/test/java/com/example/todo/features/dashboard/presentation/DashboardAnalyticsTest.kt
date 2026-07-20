package com.example.todo.features.dashboard.presentation

import com.example.todo.common.util.DateUtils
import com.example.todo.core.database.entity.TaskEntity
import com.example.todo.core.database.entity.TaskStatus
import com.example.todo.features.dashboard.domain.DashboardAnalyticsCalculator
import org.junit.Assert.*
import org.junit.Test

class DashboardAnalyticsTest {

    private val now = System.currentTimeMillis()
    private val todayStart = DateUtils.startOfDay(now)
    private val todayEnd = DateUtils.endOfDay(now)

    @Test
    fun testNewUser_NoTasks_InsufficientData() {
        val result = DashboardAnalyticsCalculator.calculate(
            allTasks = emptyList(),
            focusSessions = emptyList(),
            streak = 0,
            xp = 0,
            achievements = emptyList(),
            now = now,
            motivationalQuote = "Test Quote"
        )

        assertNull("Health Score should be null when there is no activity data", result.productivityHealthScore)
        assertNull("On-time rate should be null when no tasks are completed", result.onTimeCompletionRate30Days)
        assertNull("Weekly on-time rate should be null when no tasks are completed", result.weeklyOnTimeRate)
        assertTrue("Task debt trend should be empty if there is not enough history", result.taskDebtTrend.isEmpty())
        assertTrue("Health score trend should be empty if there is not enough history", result.healthScoreTrend.isEmpty())
        assertEquals("Stable", result.recoveryScore)
    }

    @Test
    fun testBacklogUser_OnlyOverdueTasks_NTimeRate() {
        // 10 overdue tasks created 5 days ago, due 2 days ago
        val fiveDaysAgo = todayStart - (5L * 24 * 60 * 60 * 1000)
        val twoDaysAgo = todayStart - (2L * 24 * 60 * 60 * 1000)

        val tasks = (1..10).map { index ->
            TaskEntity(
                id = "task_$index",
                title = "Overdue task $index",
                createdDateMillis = fiveDaysAgo,
                dueDateMillis = twoDaysAgo,
                status = TaskStatus.PENDING.name
            )
        }

        val result = DashboardAnalyticsCalculator.calculate(
            allTasks = tasks,
            focusSessions = emptyList(),
            streak = 0,
            xp = 0,
            achievements = emptyList(),
            now = now,
            motivationalQuote = "Test Quote"
        )

        assertEquals(10, result.taskDebt)
        assertEquals("Risk", result.backlogHealth) // 4..10 is Risk
        assertNull("On-time rate should be null as no tasks have been completed", result.onTimeCompletionRate30Days)
        assertNotNull("Health score should be computed since we have task debt", result.productivityHealthScore)
        // baseScore is 100, penalty for 10 overdue is 25, finalHealthScore should be 75
        assertEquals(75, result.productivityHealthScore)
    }

    @Test
    fun testProductiveUser_OnlyOnTimeCompletedTasks() {
        // 20 tasks completed on time
        val tasks = (1..20).map { index ->
            TaskEntity(
                id = "task_$index",
                title = "Completed Task $index",
                createdDateMillis = todayStart - 10000,
                dueDateMillis = todayStart + 10000,
                completedDateMillis = todayStart,
                status = TaskStatus.COMPLETED.name
            )
        }

        val result = DashboardAnalyticsCalculator.calculate(
            allTasks = tasks,
            focusSessions = emptyList(),
            streak = 5,
            xp = 500,
            achievements = emptyList(),
            now = now,
            motivationalQuote = "Test Quote"
        )

        assertEquals(0, result.taskDebt)
        assertEquals("Healthy", result.backlogHealth)
        assertEquals(Integer.valueOf(100), result.onTimeCompletionRate30Days)
        assertEquals(Integer.valueOf(100), result.productivityHealthScore)
    }

    @Test
    fun testRecoveryUser_OverdueResolved_NoNewOverdue() {
        val sevenDaysAgo = todayStart - (7L * 24 * 60 * 60 * 1000)
        val twoDaysAgo = todayStart - (2L * 24 * 60 * 60 * 1000)

        // 5 tasks resolved late (completed after due date)
        val resolvedTasks = (1..5).map { index ->
            TaskEntity(
                id = "resolved_$index",
                title = "Resolved Late $index",
                createdDateMillis = sevenDaysAgo,
                dueDateMillis = twoDaysAgo,
                completedDateMillis = todayStart,
                status = TaskStatus.COMPLETED.name
            )
        }

        val result = DashboardAnalyticsCalculator.calculate(
            allTasks = resolvedTasks,
            focusSessions = emptyList(),
            streak = 1,
            xp = 100,
            achievements = emptyList(),
            now = now,
            motivationalQuote = "Test Quote"
        )

        // Case B: Resolved > 0, New Overdue = 0 -> Perfect Recovery
        assertEquals("Perfect Recovery", result.recoveryScore)
    }

    @Test
    fun testRecoveryUser_PoorRecovery_OnlyNewOverdue() {
        val twoDaysAgo = todayStart - (2L * 24 * 60 * 60 * 1000)

        // 3 tasks became overdue recently and are not completed
        val newOverdueTasks = (1..3).map { index ->
            TaskEntity(
                id = "new_overdue_$index",
                title = "New Overdue $index",
                createdDateMillis = twoDaysAgo - 10000,
                dueDateMillis = twoDaysAgo,
                status = TaskStatus.PENDING.name
            )
        }

        val result = DashboardAnalyticsCalculator.calculate(
            allTasks = newOverdueTasks,
            focusSessions = emptyList(),
            streak = 0,
            xp = 0,
            achievements = emptyList(),
            now = now,
            motivationalQuote = "Test Quote"
        )

        // Case C: Resolved = 0, New Overdue > 0 -> Poor
        assertEquals("Poor", result.recoveryScore)
    }

    @Test
    fun testOnTimeCompletionRules() {
        val dueDateOnlyTask = TaskEntity(
            id = "due_date_only",
            title = "Due Date Only",
            createdDateMillis = todayStart - (24 * 60 * 60 * 1000L),
            dueDateMillis = todayStart, // Today
            dueTimeMillis = null, // No time
            completedDateMillis = todayStart + (23 * 60 * 60 * 1000L) + (50 * 60 * 1000L), // Completed at 23:50
            status = TaskStatus.COMPLETED.name
        )

        val dueDateOnlyLateTask = TaskEntity(
            id = "due_date_only_late",
            title = "Due Date Only Late",
            createdDateMillis = todayStart - (24 * 60 * 60 * 1000L),
            dueDateMillis = todayStart, // Today
            dueTimeMillis = null, // No time
            completedDateMillis = todayStart + (24 * 60 * 60 * 1000L) + (10 * 60 * 1000L), // Completed next day at 00:10
            status = TaskStatus.COMPLETED.name
        )

        val dueDateTimeTask = TaskEntity(
            id = "due_date_time",
            title = "Due Date & Time",
            createdDateMillis = todayStart,
            dueDateMillis = todayStart,
            dueTimeMillis = todayStart + (12 * 60 * 60 * 1000L), // Due today at 12:00 PM
            completedDateMillis = todayStart + (11 * 60 * 60 * 1000L), // Completed today at 11:00 AM
            status = TaskStatus.COMPLETED.name
        )

        val dueDateTimeLateTask = TaskEntity(
            id = "due_date_time_late",
            title = "Due Date & Time Late",
            createdDateMillis = todayStart,
            dueDateMillis = todayStart,
            dueTimeMillis = todayStart + (12 * 60 * 60 * 1000L), // Due today at 12:00 PM
            completedDateMillis = todayStart + (12 * 60 * 60 * 1000L) + 1000, // Completed today at 12:00:01 PM
            status = TaskStatus.COMPLETED.name
        )

        val tasks = listOf(dueDateOnlyTask, dueDateOnlyLateTask, dueDateTimeTask, dueDateTimeLateTask)

        val result = DashboardAnalyticsCalculator.calculate(
            allTasks = tasks,
            focusSessions = emptyList(),
            streak = 1,
            xp = 100,
            achievements = emptyList(),
            now = now,
            motivationalQuote = "Test Quote"
        )

        // We completed 4 tasks. 2 are on-time (dueDateOnlyTask and dueDateTimeTask), 2 are late.
        // So on-time rate should be 50%
        assertEquals(Integer.valueOf(50), result.onTimeCompletionRate30Days)
    }

    @Test
    fun testSameDayOnTime() {
        val sameDayTask = TaskEntity(
            id = "same_day",
            title = "Same Day Task",
            createdDateMillis = todayStart,
            dueDateMillis = todayStart,
            dueTimeMillis = null,
            completedDateMillis = todayStart + (2 * 60 * 1000L), // Completed 2 mins later
            status = TaskStatus.COMPLETED.name
        )

        val result = DashboardAnalyticsCalculator.calculate(
            allTasks = listOf(sameDayTask),
            focusSessions = emptyList(),
            streak = 1,
            xp = 100,
            achievements = emptyList(),
            now = now,
            motivationalQuote = "Test Quote"
        )

        assertEquals(Integer.valueOf(100), result.onTimeCompletionRate30Days)
    }
}
