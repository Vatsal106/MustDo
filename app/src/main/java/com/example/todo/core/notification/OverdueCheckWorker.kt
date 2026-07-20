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

@HiltWorker
class OverdueCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val preferencesManager: UserPreferencesManager,
    private val notificationBuilder: NotificationBuilder
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val masterEnabled = preferencesManager.notificationsEnabled.first()
        val overdueEnabled = preferencesManager.overdueReminderEnabled.first()

        if (masterEnabled && overdueEnabled) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = calendar.timeInMillis
            val overdueTasks = taskDao.getAllTasks().filter {
                it.status != "COMPLETED" && it.status != "ARCHIVED" && (it.dueDateMillis ?: Long.MAX_VALUE) < todayStart
            }

            if (overdueTasks.isNotEmpty()) {
                val notification = notificationBuilder.buildOverdueReminder(overdueTasks)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }

        // Reschedule based on frequency
        scheduleNextRun(context, preferencesManager, forceReplace = true)

        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 5003

        fun scheduleNextRun(
            context: Context,
            preferencesManager: UserPreferencesManager,
            forceReplace: Boolean = false
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                val frequency = preferencesManager.overdueReminderFrequency.first()
                val delayHours = when (frequency) {
                    "HOURLY" -> 1L
                    "TWICE_DAILY" -> 12L
                    "DAILY" -> 24L
                    else -> 24L
                }

                val delayMillis = delayHours * 60 * 60 * 1000L

                val workRequest = OneTimeWorkRequestBuilder<OverdueCheckWorker>()
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .build()

                val policy = if (forceReplace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "OverdueCheckWork",
                    policy,
                    workRequest
                )
            }
        }
    }
}
