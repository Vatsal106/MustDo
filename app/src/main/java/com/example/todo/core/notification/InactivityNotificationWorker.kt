package com.example.todo.core.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.todo.MainActivity
import com.example.todo.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class InactivityNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val motivationalMessages = listOf(
            "Ready to conquer your day? Let's check off some tasks!",
            "Small steps lead to big achievements. So track your small steps with us!",
            "Stay on track! Check your tasks for today.",
            "A productive day starts with a single task. You can do it!",
            "Don't let your streak slip! Open your list and stay ahead."
        )
        
        val randomMessage = motivationalMessages.random()
        
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_DAILY_AGENDA)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Stay Motivated! 💪")
            .setContentText(randomMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(randomMessage))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 5005
        private const val UNIQUE_WORK_NAME = "InactivityNotificationWork"

        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            // Cancel existing work to reset the timer
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            
            // Create a OneTimeWorkRequest with an initial delay of 2 days (48 hours)
            val workRequest = OneTimeWorkRequestBuilder<InactivityNotificationWorker>()
                .setInitialDelay(2, TimeUnit.DAYS)
                .build()
                
            workManager.enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
