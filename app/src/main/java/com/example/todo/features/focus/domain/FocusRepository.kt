package com.example.todo.features.focus.domain

import com.example.todo.core.database.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

interface FocusRepository {
    fun observeCompletedSessions(): Flow<List<FocusSessionEntity>>
    fun observeSessionsForDate(dateMillis: Long): Flow<List<FocusSessionEntity>>
    fun observeTotalFocusMinutes(): Flow<Int?>
    fun observeFocusMinutesForTask(taskId: String): Flow<Int?>
    fun observeFocusMinutesForDate(dateMillis: Long): Flow<Int?>
    fun observeTotalSessionCount(): Flow<Int>

    suspend fun insertSession(session: FocusSessionEntity)
    suspend fun updateSession(session: FocusSessionEntity)
    suspend fun getAllSessions(): List<FocusSessionEntity>
    suspend fun deleteAllSessions()
    suspend fun insertAllSessions(sessions: List<FocusSessionEntity>)
}
