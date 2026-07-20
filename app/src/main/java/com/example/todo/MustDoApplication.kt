package com.example.todo

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.example.todo.core.worker.BackupWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

@HiltAndroidApp
class MustDoApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var channelManager: com.example.todo.core.notification.NotificationChannelManager

    @Inject
    lateinit var reminderScheduler: com.example.todo.core.notification.ReminderScheduler

    @Inject
    lateinit var preferencesManager: com.example.todo.core.datastore.UserPreferencesManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // Setup channels
        channelManager.createAllChannels()

        // Reschedule reminders
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            reminderScheduler.rescheduleAll()
        }

        // Initialize scheduled notifications (using KEEP policy to avoid resetting current schedule)
        com.example.todo.core.notification.DailyAgendaWorker.scheduleNextRun(this, preferencesManager, forceReplace = false)
        com.example.todo.core.notification.WeeklyReviewWorker.scheduleNextRun(this, preferencesManager, forceReplace = false)
        com.example.todo.core.notification.OverdueCheckWorker.scheduleNextRun(this, preferencesManager, forceReplace = false)

        // Dynamically reschedule when settings are changed by the user
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            combine(
                preferencesManager.dailyAgendaTime,
                preferencesManager.dailyAgendaTimeSecondary,
                preferencesManager.dailyAgendaEnabled
            ) { time, secTime, enabled -> Triple(time, secTime, enabled) }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    com.example.todo.core.notification.DailyAgendaWorker.scheduleNextRun(
                        this@MustDoApplication,
                        preferencesManager,
                        forceReplace = true
                    )
                }
        }

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            combine(
                preferencesManager.weeklyReviewDay,
                preferencesManager.weeklyReviewTime,
                preferencesManager.weeklyReviewEnabled
            ) { day, time, enabled -> Triple(day, time, enabled) }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    com.example.todo.core.notification.WeeklyReviewWorker.scheduleNextRun(
                        this@MustDoApplication,
                        preferencesManager,
                        forceReplace = true
                    )
                }
        }

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            combine(
                preferencesManager.overdueReminderFrequency,
                preferencesManager.overdueReminderEnabled
            ) { freq, enabled -> Pair(freq, enabled) }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    com.example.todo.core.notification.OverdueCheckWorker.scheduleNextRun(
                        this@MustDoApplication,
                        preferencesManager,
                        forceReplace = true
                    )
                }
        }

        schedulePeriodicBackup()
    }

    private fun schedulePeriodicBackup() {
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PeriodicBackupWork",
            ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )
    }
}
