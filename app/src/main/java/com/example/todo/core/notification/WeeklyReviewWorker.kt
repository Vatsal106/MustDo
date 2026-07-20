package com.example.todo.core.notification

import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.todo.core.database.dao.FocusSessionDao
import com.example.todo.core.database.dao.TaskDao
import com.example.todo.core.datastore.UserPreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class WeeklyReviewWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val focusSessionDao: FocusSessionDao,
    private val preferencesManager: UserPreferencesManager,
    private val notificationBuilder: NotificationBuilder
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val masterEnabled = preferencesManager.notificationsEnabled.first()
        val weeklyEnabled = preferencesManager.weeklyReviewEnabled.first()

        if (masterEnabled && weeklyEnabled) {
            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)

            val allTasks = taskDao.getAllTasks()
            val completedCount = allTasks.count { 
                it.status == "COMPLETED" && (it.completedDateMillis ?: 0L) >= sevenDaysAgo 
            }
            val remainingCount = allTasks.count { 
                it.status != "COMPLETED" && it.status != "ARCHIVED" 
            }

            val allSessions = focusSessionDao.getAllSessions()
            val focusMinutes = allSessions
                .filter { it.isCompleted && it.startTimeMillis >= sevenDaysAgo }
                .sumOf { it.durationMinutes }
                .toLong()

            val currentStreak = preferencesManager.currentStreak.first()

            val stats = WeeklyStats(
                completedCount = completedCount,
                remainingCount = remainingCount,
                focusMinutes = focusMinutes,
                currentStreak = currentStreak
            )

            val notification = notificationBuilder.buildWeeklyReview(stats)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        // Reschedule for next week
        scheduleNextRun(context, preferencesManager, forceReplace = true)

        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 5002

        fun scheduleNextRun(
            context: Context,
            preferencesManager: UserPreferencesManager,
            forceReplace: Boolean = false
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                val dayOfWeek = preferencesManager.weeklyReviewDay.first() // 1 = Sunday, 2 = Monday...
                val timePref = preferencesManager.weeklyReviewTime.first() // e.g. "19:00"

                val parts = timePref.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 19
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, dayOfWeek)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }
                val delay = calendar.timeInMillis - System.currentTimeMillis()

                val workRequest = OneTimeWorkRequestBuilder<WeeklyReviewWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .build()

                val policy = if (forceReplace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "WeeklyReviewWork",
                    policy,
                    workRequest
                )
            }
        }
    }
}
