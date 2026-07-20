package com.example.todo.features.reports.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todo.core.database.dao.TaskDao
import com.example.todo.core.database.dao.FocusSessionDao
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.todo.MainActivity
import com.example.todo.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class WeeklySummaryWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val focusSessionDao: FocusSessionDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val calendar = Calendar.getInstance()
        val end = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val start = calendar.timeInMillis

        val tasks = taskDao.getCompletedTasksBetween(start, end)
        val focusSessions = focusSessionDao.getSessionsBetween(start, end)

        val taskCount = tasks.size
        val focusHours = focusSessions.sumOf { it.durationMinutes } / 60f

        val message = "You completed $taskCount tasks and focused for ${String.format(java.util.Locale.US, "%.1f", focusHours)} hours this week!"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1005,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "weekly_review")
            .setSmallIcon(R.drawable.ic_check_circle_outline)
            .setContentTitle("Weekly Summary")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1005, builder.build())

        return Result.success()
    }
}
