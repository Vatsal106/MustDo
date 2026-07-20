package com.example.todo.core.notification

import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todo.core.database.dao.TaskDao
import com.example.todo.core.datastore.UserPreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class TaskNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val preferencesManager: UserPreferencesManager,
    private val notificationBuilder: NotificationBuilder
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val masterEnabled = preferencesManager.notificationsEnabled.first()
        if (!masterEnabled) return Result.success()

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
        if (pendingTasks.isEmpty()) {
            return Result.success()
        }

        val notification = notificationBuilder.buildDailyAgenda(pendingTasks)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        return Result.success()
    }

    companion object {
        const val KEY_TIME_OF_DAY = "time_of_day"
        private const val NOTIFICATION_ID = 2005
    }
}
