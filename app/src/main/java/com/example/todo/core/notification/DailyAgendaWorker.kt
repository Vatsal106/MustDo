package com.example.todo.core.notification

import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
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

import android.util.Log

@HiltWorker
class DailyAgendaWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val preferencesManager: UserPreferencesManager,
    private val notificationBuilder: NotificationBuilder
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val masterEnabled = preferencesManager.notificationsEnabled.first()
            val dailyEnabled = preferencesManager.dailyAgendaEnabled.first()

            if (masterEnabled && dailyEnabled) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val endOfDay = calendar.timeInMillis

                val pendingTasks = taskDao.getPendingAndOverdueTasks(startOfDay, endOfDay)
                if (pendingTasks.isNotEmpty()) {
                    val isUnified = preferencesManager.dailyAgendaUnified.first()
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    
                    if (isUnified) {
                        val notification = notificationBuilder.buildDailyAgenda(pendingTasks)
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    } else {
                        pendingTasks.forEach { task ->
                            val notification = notificationBuilder.buildDailyAgendaTask(task)
                            notificationManager.notify(task.id.hashCode(), notification)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DailyAgendaWorker", "Error executing daily agenda notification worker", e)
        } finally {
            // Reschedule for next day
            scheduleNextRun(context, preferencesManager, forceReplace = true)
        }

        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 5001

        fun scheduleNextRun(
            context: Context,
            preferencesManager: UserPreferencesManager,
            forceReplace: Boolean = false
        ) {
            // Launch a coroutine to read preferences and schedule
            GlobalScope.launch(Dispatchers.IO) {
                val timePrefPrimary = preferencesManager.dailyAgendaTime.first()
                val timePrefSecondary = preferencesManager.dailyAgendaTimeSecondary.first()

                fun getNextOccurrence(timePref: String): Long {
                    val parts = timePref.split(":")
                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                    return calendar.timeInMillis
                }

                val primaryTrigger = getNextOccurrence(timePrefPrimary)
                val secondaryTrigger = getNextOccurrence(timePrefSecondary)

                val nextTrigger = minOf(primaryTrigger, secondaryTrigger)
                val delay = nextTrigger - System.currentTimeMillis()

                val workRequest = OneTimeWorkRequestBuilder<DailyAgendaWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .build()

                val policy = if (forceReplace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "DailyAgendaWork",
                    policy,
                    workRequest
                )
            }
        }
    }
}
