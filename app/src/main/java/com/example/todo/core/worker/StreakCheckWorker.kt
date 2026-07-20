package com.example.todo.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todo.core.datastore.UserPreferencesManager
import com.example.todo.features.gamification.domain.GamificationEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class StreakCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val gamificationEngine: GamificationEngine,
    private val preferencesManager: UserPreferencesManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            gamificationEngine.updateStreak()
            gamificationEngine.checkAchievements()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
