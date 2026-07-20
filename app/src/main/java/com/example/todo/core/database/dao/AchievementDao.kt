package com.example.todo.core.database.dao

import androidx.room.*
import com.example.todo.core.database.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, conditionValue ASC")
    fun observeAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 0")
    suspend fun getLockedAchievements(): List<AchievementEntity>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievements(): List<AchievementEntity>

    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    fun observeUnlockedCount(): Flow<Int>

    @Query("DELETE FROM achievements")
    suspend fun deleteAll()
}
