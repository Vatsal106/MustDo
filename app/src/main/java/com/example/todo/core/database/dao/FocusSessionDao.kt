package com.example.todo.core.database.dao

import androidx.room.*
import com.example.todo.core.database.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity)

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Delete
    suspend fun deleteSession(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE isCompleted = 1 ORDER BY startTimeMillis DESC")
    fun observeCompletedSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE isCompleted = 1 AND startTimeMillis >= :startOfDay AND startTimeMillis < :endOfDay")
    fun observeSessionsForDate(startOfDay: Long, endOfDay: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE isCompleted = 1 AND startTimeMillis >= :startMillis AND startTimeMillis <= :endMillis")
    suspend fun getSessionsBetween(startMillis: Long, endMillis: Long): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions WHERE isCompleted = 1 AND startTimeMillis >= :startMillis")
    suspend fun getSessionsSince(startMillis: Long): List<FocusSessionEntity>

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE isCompleted = 1")
    fun observeTotalFocusMinutes(): Flow<Int?>

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE isCompleted = 1 AND taskId = :taskId")
    fun observeFocusMinutesForTask(taskId: String): Flow<Int?>

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE isCompleted = 1 AND startTimeMillis >= :startOfDay AND startTimeMillis < :endOfDay")
    fun observeFocusMinutesForDate(startOfDay: Long, endOfDay: Long): Flow<Int?>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE isCompleted = 1")
    fun observeTotalSessionCount(): Flow<Int>

    @Query("SELECT * FROM focus_sessions ORDER BY startTimeMillis DESC")
    suspend fun getAllSessions(): List<FocusSessionEntity>

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<FocusSessionEntity>)
}
