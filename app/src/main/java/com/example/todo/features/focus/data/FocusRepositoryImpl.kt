package com.example.todo.features.focus.data

import com.example.todo.common.util.DateUtils
import com.example.todo.core.database.dao.FocusSessionDao
import com.example.todo.core.database.entity.FocusSessionEntity
import com.example.todo.features.focus.domain.FocusRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusRepositoryImpl @Inject constructor(
    private val focusSessionDao: FocusSessionDao
) : FocusRepository {

    override fun observeCompletedSessions(): Flow<List<FocusSessionEntity>> =
        focusSessionDao.observeCompletedSessions()

    override fun observeSessionsForDate(dateMillis: Long): Flow<List<FocusSessionEntity>> =
        focusSessionDao.observeSessionsForDate(
            DateUtils.startOfDay(dateMillis),
            DateUtils.endOfDay(dateMillis)
        )

    override fun observeTotalFocusMinutes(): Flow<Int?> =
        focusSessionDao.observeTotalFocusMinutes()

    override fun observeFocusMinutesForTask(taskId: String): Flow<Int?> =
        focusSessionDao.observeFocusMinutesForTask(taskId)

    override fun observeFocusMinutesForDate(dateMillis: Long): Flow<Int?> =
        focusSessionDao.observeFocusMinutesForDate(
            DateUtils.startOfDay(dateMillis),
            DateUtils.endOfDay(dateMillis)
        )

    override fun observeTotalSessionCount(): Flow<Int> =
        focusSessionDao.observeTotalSessionCount()

    override suspend fun insertSession(session: FocusSessionEntity) =
        focusSessionDao.insertSession(session)

    override suspend fun updateSession(session: FocusSessionEntity) =
        focusSessionDao.updateSession(session)

    override suspend fun getAllSessions(): List<FocusSessionEntity> =
        focusSessionDao.getAllSessions()

    override suspend fun deleteAllSessions() =
        focusSessionDao.deleteAll()

    override suspend fun insertAllSessions(sessions: List<FocusSessionEntity>) =
        focusSessionDao.insertAll(sessions)
}
