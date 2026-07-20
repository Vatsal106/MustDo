package com.example.todo.features.calendar.presentation

import com.example.todo.common.util.DateUtils
import com.example.todo.core.database.entity.CategoryEntity
import com.example.todo.core.database.entity.FocusSessionEntity
import com.example.todo.core.database.entity.TaskEntity
import com.example.todo.core.database.entity.TaskStatus
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class CategoryShare(
    val categoryId: String?,
    val categoryName: String,
    val colorHex: String,
    val count: Int,
    val percentage: Float
)

data class CalendarAnalyticsData(
    val todayFocusHours: Float = 0f,
    val weekFocusHours: Float = 0f,
    val monthFocusHours: Float = 0f,
    val tasksCompletedPerDay: List<Pair<String, Float>> = emptyList(), // Day label -> Count
    val tasksCompletedPerWeek: List<Pair<String, Float>> = emptyList(), // Week label -> Count
    val focusMinutesPerDay: List<Pair<String, Float>> = emptyList(), // Day label -> Minutes
    val mostProductiveDay: String = "None",
    val mostProductiveDayRate: Int = 0,
    val categoryDistribution: List<CategoryShare> = emptyList()
)

@Singleton
class CalendarAnalyticsManager @Inject constructor() {

    fun calculateAnalytics(
        allTasks: List<TaskEntity>,
        allSessions: List<FocusSessionEntity>,
        categories: List<CategoryEntity>
    ): CalendarAnalyticsData {
        val now = System.currentTimeMillis()
        val todayStart = DateUtils.startOfDay(now)
        
        // 1. Focus Hours
        val oneWeekAgo = todayStart - 6 * 24 * 60 * 60 * 1000L
        val oneMonthAgo = todayStart - 29 * 24 * 60 * 60 * 1000L
        
        val todaySessions = allSessions.filter { it.isCompleted && it.startTimeMillis >= todayStart }
        val weekSessions = allSessions.filter { it.isCompleted && it.startTimeMillis >= oneWeekAgo }
        val monthSessions = allSessions.filter { it.isCompleted && it.startTimeMillis >= oneMonthAgo }
        
        val todayFocusHours = todaySessions.sumOf { it.durationMinutes } / 60f
        val weekFocusHours = weekSessions.sumOf { it.durationMinutes } / 60f
        val monthFocusHours = monthSessions.sumOf { it.durationMinutes } / 60f

        // 2. Completed Tasks and Focus per Day (Last 7 Days)
        val dayFormatter = SimpleDateFormat("EEE", Locale.getDefault())
        val tasksCompletedPerDay = mutableListOf<Pair<String, Float>>()
        val focusMinutesPerDay = mutableListOf<Pair<String, Float>>()
        
        val dayCompletionMap = mutableMapOf<Int, Int>() // day of week -> completed task count
        val dayTotalScheduledMap = mutableMapOf<Int, Int>() // day of week -> total scheduled task count
        
        for (i in 6 downTo 0) {
            val dayStart = todayStart - i * 24 * 60 * 60 * 1000L
            val dayEnd = dayStart + 24 * 60 * 60 * 1000L - 1
            val dateLabel = dayFormatter.format(Date(dayStart))
            
            // Completed tasks on this day
            val completedCount = allTasks.count { 
                it.status == TaskStatus.COMPLETED.name && 
                it.completedDateMillis != null && 
                it.completedDateMillis in dayStart..dayEnd
            }
            tasksCompletedPerDay.add(dateLabel to completedCount.toFloat())
            
            // Focus minutes on this day
            val focusMins = allSessions.filter { 
                it.isCompleted && 
                it.startTimeMillis in dayStart..dayEnd 
            }.sumOf { it.durationMinutes }
            focusMinutesPerDay.add(dateLabel to focusMins.toFloat())
        }

        // 3. Completed Tasks per Week (Last 4 Weeks)
        val tasksCompletedPerWeek = mutableListOf<Pair<String, Float>>()
        for (i in 3 downTo 0) {
            val weekStart = todayStart - (i * 7 + 6) * 24 * 60 * 60 * 1000L
            val weekEnd = todayStart - (i * 7) * 24 * 60 * 60 * 1000L + 24 * 60 * 60 * 1000L - 1
            val label = if (i == 0) "This Week" else "$i wk ago"
            
            val count = allTasks.count { 
                it.status == TaskStatus.COMPLETED.name && 
                it.completedDateMillis != null && 
                it.completedDateMillis in weekStart..weekEnd
            }
            tasksCompletedPerWeek.add(label to count.toFloat())
        }

        // 4. Most Productive Day
        // Let's count completion percentage or total completions by Day of Week
        val calendar = Calendar.getInstance()
        val dayOfWeekCounts = mutableMapOf<Int, Int>() // Calendar.SUNDAY to count
        val dayOfWeekTotals = mutableMapOf<Int, Int>() 
        
        allTasks.forEach { task ->
            if (task.dueDateMillis != null) {
                calendar.timeInMillis = task.dueDateMillis
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                dayOfWeekTotals[dayOfWeek] = (dayOfWeekTotals[dayOfWeek] ?: 0) + 1
                if (task.status == TaskStatus.COMPLETED.name) {
                    dayOfWeekCounts[dayOfWeek] = (dayOfWeekCounts[dayOfWeek] ?: 0) + 1
                }
            }
        }
        
        var bestDayOfWeek = -1
        var bestCompletionRate = 0f
        var bestTotalCompleted = 0
        
        val daysOfWeekList = listOf(
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, 
            Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
        )
        
        daysOfWeekList.forEach { day ->
            val completed = dayOfWeekCounts[day] ?: 0
            val total = dayOfWeekTotals[day] ?: 0
            if (total > 0) {
                val rate = completed.toFloat() / total
                if (rate > bestCompletionRate || (rate == bestCompletionRate && completed > bestTotalCompleted)) {
                    bestCompletionRate = rate
                    bestDayOfWeek = day
                    bestTotalCompleted = completed
                }
            }
        }
        
        val dayNames = mapOf(
            Calendar.SUNDAY to "Sunday",
            Calendar.MONDAY to "Monday",
            Calendar.TUESDAY to "Tuesday",
            Calendar.WEDNESDAY to "Wednesday",
            Calendar.THURSDAY to "Thursday",
            Calendar.FRIDAY to "Friday",
            Calendar.SATURDAY to "Saturday"
        )
        val mostProductiveDay = dayNames[bestDayOfWeek] ?: "None"
        val mostProductiveDayRate = (bestCompletionRate * 100).toInt()

        // 5. Category Distribution (completed tasks)
        val completedTasks = allTasks.filter { it.status == TaskStatus.COMPLETED.name }
        val totalCompletedCount = completedTasks.size
        
        val categoryDistribution = if (totalCompletedCount > 0) {
            val counts = completedTasks.groupBy { it.categoryId }
            categories.map { category ->
                val count = counts[category.id]?.size ?: 0
                CategoryShare(
                    categoryId = category.id,
                    categoryName = category.name,
                    colorHex = category.colorHex,
                    count = count,
                    percentage = (count.toFloat() / totalCompletedCount) * 100f
                )
            }.filter { it.count > 0 }.sortedByDescending { it.count }
        } else {
            emptyList()
        }

        return CalendarAnalyticsData(
            todayFocusHours = todayFocusHours,
            weekFocusHours = weekFocusHours,
            monthFocusHours = monthFocusHours,
            tasksCompletedPerDay = tasksCompletedPerDay,
            tasksCompletedPerWeek = tasksCompletedPerWeek,
            focusMinutesPerDay = focusMinutesPerDay,
            mostProductiveDay = mostProductiveDay,
            mostProductiveDayRate = mostProductiveDayRate,
            categoryDistribution = categoryDistribution
        )
    }
}
