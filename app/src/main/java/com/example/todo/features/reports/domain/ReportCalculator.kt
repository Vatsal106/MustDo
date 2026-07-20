package com.example.todo.features.reports.domain

import com.example.todo.core.database.dao.FocusSessionDao
import com.example.todo.core.database.dao.TaskDao
import com.example.todo.core.database.entity.TaskEntity
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class MonthlyReport(
    val monthName: String,
    val tasksCompleted: Int,
    val tasksCompletedDelta: Int, // compared to last month
    val focusHours: Float,
    val focusHoursDelta: Float,
    val onTimeRate: Float,
    val onTimeRateDelta: Float,
    val categoryBreakdown: Map<String, Int> // CategoryName -> TaskCount
)

@Singleton
class ReportCalculator @Inject constructor(
    private val taskDao: TaskDao,
    private val focusSessionDao: FocusSessionDao
) {

    suspend fun generateMonthlyReport(year: Int, month: Int): MonthlyReport {
        val currentMonthStart = getStartOfMonth(year, month)
        val currentMonthEnd = getEndOfMonth(year, month)

        // Previous month calculation
        val prevMonth = if (month == Calendar.JANUARY) Calendar.DECEMBER else month - 1
        val prevYear = if (month == Calendar.JANUARY) year - 1 else year
        val prevMonthStart = getStartOfMonth(prevYear, prevMonth)
        val prevMonthEnd = getEndOfMonth(prevYear, prevMonth)

        val currentTasks = taskDao.getCompletedTasksBetween(currentMonthStart, currentMonthEnd)
        val prevTasks = taskDao.getCompletedTasksBetween(prevMonthStart, prevMonthEnd)

        val currentFocus = focusSessionDao.getSessionsBetween(currentMonthStart, currentMonthEnd)
        val prevFocus = focusSessionDao.getSessionsBetween(prevMonthStart, prevMonthEnd)

        val tasksCompleted = currentTasks.size
        val tasksCompletedDelta = tasksCompleted - prevTasks.size

        val focusMinutes = currentFocus.sumOf { it.durationMinutes }
        val prevFocusMinutes = prevFocus.sumOf { it.durationMinutes }
        val focusHours = focusMinutes / 60f
        val focusHoursDelta = focusHours - (prevFocusMinutes / 60f)

        val onTimeRate = calculateOnTimeRate(currentTasks)
        val prevOnTimeRate = calculateOnTimeRate(prevTasks)
        val onTimeRateDelta = onTimeRate - prevOnTimeRate

        val categoryBreakdown = currentTasks
            .filter { it.categoryId != null }
            .groupingBy { it.categoryId!! }
            .eachCount()
            // In a real app we'd map categoryId to categoryName. For now we just use the ID or generic mapping.
            // Assuming we pass categoryMap from ViewModel or join in Dao.
            // For simplicity in this demo, let's just return the IDs and ViewModel can resolve names.
            
        return MonthlyReport(
            monthName = getMonthName(month),
            tasksCompleted = tasksCompleted,
            tasksCompletedDelta = tasksCompletedDelta,
            focusHours = focusHours,
            focusHoursDelta = focusHoursDelta,
            onTimeRate = onTimeRate,
            onTimeRateDelta = onTimeRateDelta,
            categoryBreakdown = categoryBreakdown
        )
    }

    private fun calculateOnTimeRate(tasks: List<TaskEntity>): Float {
        if (tasks.isEmpty()) return 0f
        val onTime = tasks.count { 
            val due = it.dueDateMillis
            val completed = it.completedDateMillis
            due == null || (completed != null && completed <= due)
        }
        return (onTime.toFloat() / tasks.size.toFloat()) * 100f
    }

    private fun getStartOfMonth(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfMonth(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        return calendar.timeInMillis
    }

    private fun getMonthName(month: Int): String {
        return java.text.DateFormatSymbols().months[month]
    }
}
