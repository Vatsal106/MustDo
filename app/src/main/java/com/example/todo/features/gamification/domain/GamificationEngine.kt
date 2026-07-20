package com.example.todo.features.gamification.domain

import com.example.todo.core.database.dao.AchievementDao
import com.example.todo.core.database.dao.FocusSessionDao
import com.example.todo.core.database.entity.AchievementEntity
import com.example.todo.core.database.entity.Priority
import com.example.todo.core.database.entity.TaskEntity
import com.example.todo.core.datastore.UserPreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationEngine @Inject constructor(
    private val preferencesManager: UserPreferencesManager,
    private val achievementDao: AchievementDao,
    private val focusSessionDao: FocusSessionDao
) {

    val totalXp: Flow<Int> = preferencesManager.totalXp
    val currentStreak: Flow<Int> = preferencesManager.currentStreak
    val longestStreak: Flow<Int> = preferencesManager.longestStreak

    fun getLevel(xp: Int): Int = xp / 100

    fun getXpProgress(xp: Int): Float {
        val currentLevelXp = xp % 100
        return currentLevelXp / 100f
    }

    fun getXpForPriority(priority: String): Int {
        return when (priority) {
            Priority.LOW.name -> 10
            Priority.MEDIUM.name -> 15
            Priority.HIGH.name -> 20
            Priority.URGENT.name -> 25
            else -> 10
        }
    }

    suspend fun awardTaskCompletionXp(task: TaskEntity): Int {
        var xp = getXpForPriority(task.priority)
        val completedDate = task.completedDateMillis ?: System.currentTimeMillis()
        
        // Completed on time: +5 XP
        val isOnTime = if (task.dueDateMillis != null) {
            if (task.dueTimeMillis == null) {
                completedDate <= com.example.todo.common.util.DateUtils.endOfDay(task.dueDateMillis)
            } else {
                completedDate <= task.dueTimeMillis
            }
        } else {
            false
        }
        if (isOnTime) {
            xp += 5
        }
        
        // Never rescheduled bonus: +3 XP (only if task age > 24 hours)
        if (task.rescheduleCount == 0 && (completedDate - task.createdDateMillis) > 24 * 60 * 60 * 1000L) {
            xp += 3
        }
        
        preferencesManager.addXp(xp)
        recordProductiveAction()
        checkAchievements()
        return xp
    }

    suspend fun awardFocusSessionXp(durationMinutes: Int) {
        val xp = durationMinutes // 1 XP per minute
        preferencesManager.addXp(xp)
        recordProductiveAction()
        checkAchievements()
    }

    suspend fun recordProductiveAction() {
        val now = System.currentTimeMillis()
        val todayStart = com.example.todo.common.util.DateUtils.startOfDay(now)
        val lastProductive = preferencesManager.lastProductiveActionDate.first()
        val currentStreak = preferencesManager.currentStreak.first()
        
        if (lastProductive > 0) {
            val lastProductiveStart = com.example.todo.common.util.DateUtils.startOfDay(lastProductive)
            val dayDiff = ((todayStart - lastProductiveStart) / (24 * 60 * 60 * 1000)).toInt()
            
            when {
                dayDiff == 1 -> {
                    // Consecutive day
                    val newStreak = currentStreak + 1
                    preferencesManager.setCurrentStreak(newStreak)
                    val longest = preferencesManager.longestStreak.first()
                    if (newStreak > longest) {
                        preferencesManager.setLongestStreak(newStreak)
                    }
                }
                dayDiff > 1 -> {
                    // Streak was broken, start new streak of 1
                    preferencesManager.setCurrentStreak(1)
                }
                dayDiff == 0 -> {
                    // Already did a productive action today, keep streak as is
                }
            }
        } else {
            // First time productive action
            preferencesManager.setCurrentStreak(1)
            preferencesManager.setLongestStreak(1)
        }
        
        preferencesManager.setLastProductiveActionDate(now)
    }

    suspend fun updateStreak() {
        val now = System.currentTimeMillis()
        val lastProductive = preferencesManager.lastProductiveActionDate.first()
        val todayStart = com.example.todo.common.util.DateUtils.startOfDay(now)
        
        if (lastProductive > 0) {
            val lastProductiveStart = com.example.todo.common.util.DateUtils.startOfDay(lastProductive)
            val dayDiff = ((todayStart - lastProductiveStart) / (24 * 60 * 60 * 1000)).toInt()
            
            if (dayDiff > 1) {
                // Streak broken because the last productive action was more than 1 day ago
                preferencesManager.setCurrentStreak(0)
            }
        } else {
            preferencesManager.setCurrentStreak(0)
        }
        
        preferencesManager.setLastActiveDate(now)
        checkAchievements()
    }

    suspend fun checkAchievements(): List<AchievementEntity> {
        val locked = achievementDao.getLockedAchievements()
        val newlyUnlocked = mutableListOf<AchievementEntity>()

        val totalXp = preferencesManager.totalXp.first()
        val level = getLevel(totalXp)
        val streak = preferencesManager.currentStreak.first()
        val totalSessions = focusSessionDao.observeTotalSessionCount().first()

        for (achievement in locked) {
            val unlocked = when (achievement.conditionType) {
                "TASKS_COMPLETED" -> {
                    // We check this separately when completing tasks
                    false // Handled via direct task count check
                }
                "STREAK" -> streak >= achievement.conditionValue
                "LEVEL" -> level >= achievement.conditionValue
                "FOCUS_SESSIONS" -> totalSessions >= achievement.conditionValue
                else -> false
            }

            if (unlocked) {
                val unlockedAchievement = achievement.copy(
                    isUnlocked = true,
                    unlockedTimeMillis = System.currentTimeMillis()
                )
                achievementDao.updateAchievement(unlockedAchievement)
                newlyUnlocked.add(unlockedAchievement)
            }
        }

        return newlyUnlocked
    }

    suspend fun checkFirstTaskAchievement(totalCompleted: Int): AchievementEntity? {
        val locked = achievementDao.getLockedAchievements()
        val firstTask = locked.find { it.id == "ach_first_task" }
        if (firstTask != null && totalCompleted >= 1) {
            val unlocked = firstTask.copy(
                isUnlocked = true,
                unlockedTimeMillis = System.currentTimeMillis()
            )
            achievementDao.updateAchievement(unlocked)
            return unlocked
        }
        return null
    }

    fun observeAchievements(): Flow<List<AchievementEntity>> =
        achievementDao.observeAllAchievements()

    suspend fun getAllAchievements(): List<AchievementEntity> =
        achievementDao.getAllAchievements()

    suspend fun deleteAllAchievements() =
        achievementDao.deleteAll()

    suspend fun insertAllAchievements(achievements: List<AchievementEntity>) =
        achievementDao.insertAll(achievements)

    suspend fun resetGamification() {
        preferencesManager.setTotalXp(0)
        preferencesManager.setCurrentStreak(0)
        preferencesManager.setLongestStreak(0)
        preferencesManager.setLastActiveDate(0L)
    }
}
