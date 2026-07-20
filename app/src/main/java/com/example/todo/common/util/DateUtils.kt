package com.example.todo.common.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    fun startOfDay(dateMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDay(dateMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun startOfTomorrow(dateMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun formatDate(millis: Long?): String {
        if (millis == null) return ""
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(millis))
    }

    fun formatTime(millis: Long?): String {
        if (millis == null) return ""
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(millis))
    }

    fun formatDateTime(millis: Long?): String {
        if (millis == null) return ""
        return SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(millis))
    }

    fun formatRelativeDate(millis: Long?): String {
        if (millis == null) return "No date"
        val today = startOfDay()
        val tomorrow = startOfTomorrow()
        val dateStart = startOfDay(millis)

        return when {
            dateStart == today -> "Today"
            dateStart == tomorrow -> "Tomorrow"
            dateStart < today -> {
                val daysAgo = ((today - dateStart) / (24 * 60 * 60 * 1000)).toInt()
                if (daysAgo == 1) "Yesterday" else "$daysAgo days ago"
            }
            else -> {
                val daysUntil = ((dateStart - today) / (24 * 60 * 60 * 1000)).toInt()
                if (daysUntil <= 7) "In $daysUntil days" else formatDate(millis)
            }
        }
    }

    fun advanceDate(dateMillis: Long, recurrence: String): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        when (recurrence) {
            "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    fun isSameDay(millis1: Long, millis2: Long): Boolean {
        return startOfDay(millis1) == startOfDay(millis2)
    }

    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            hour < 21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    fun formatTime24To12(time24: String): String {
        return try {
            val parts = time24.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
        } catch (e: Exception) {
            time24
        }
    }
}
