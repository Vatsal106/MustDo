package com.example.todo.core.database.dao

import androidx.room.*
import com.example.todo.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)
    
    @Update
    suspend fun updateTasks(tasks: List<TaskEntity>)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskById(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE status != 'ARCHIVED' ORDER BY dueDateMillis ASC, priority DESC")
    fun observeAllActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status NOT IN ('COMPLETED', 'ARCHIVED') ORDER BY dueDateMillis ASC, priority DESC")
    fun observeActivePendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdDateMillis DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY dueDateMillis ASC")
    fun observeTasksByStatus(status: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId AND status != 'ARCHIVED' ORDER BY dueDateMillis ASC")
    fun observeTasksByCategory(categoryId: String): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE dueDateMillis >= :startOfDay 
        AND dueDateMillis < :endOfDay 
        AND status != 'ARCHIVED'
        ORDER BY priority DESC, dueTimeMillis ASC
    """)
    fun observeTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE dueDateMillis < :todayStart 
        AND status NOT IN ('COMPLETED', 'ARCHIVED')
        ORDER BY dueDateMillis ASC
    """)
    fun observeOverdueTasks(todayStart: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE status = 'COMPLETED' 
        AND completedDateMillis >= :startOfDay 
        AND completedDateMillis < :endOfDay
    """)
    fun observeCompletedTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT DISTINCT t.* FROM tasks t
        LEFT JOIN task_resources tr ON t.id = tr.taskId
        LEFT JOIN task_note_blocks tnb ON t.id = tnb.taskId
        WHERE (
            t.title LIKE '%' || :query || '%' OR 
            t.description LIKE '%' || :query || '%' OR 
            t.tags LIKE '%' || :query || '%' OR
            tr.payloadJson LIKE '%' || :query || '%' OR
            tnb.content LIKE '%' || :query || '%'
        )
        AND t.status != 'ARCHIVED'
        ORDER BY t.dueDateMillis ASC
    """)
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED' AND completedDateMillis >= :startOfDay AND completedDateMillis < :endOfDay")
    fun countCompletedTasksForDate(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE dueDateMillis >= :startOfDay AND dueDateMillis < :endOfDay AND status != 'ARCHIVED'")
    fun countTasksForDate(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED'")
    suspend fun getTotalCompletedCount(): Int

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' AND completedDateMillis >= :startMillis AND completedDateMillis <= :endMillis")
    suspend fun getCompletedTasksBetween(startMillis: Long, endMillis: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE recurrence != 'NONE' AND status = 'COMPLETED' ORDER BY completedDateMillis DESC")
    suspend fun getCompletedRecurringTasks(): List<TaskEntity>

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("""
        SELECT * FROM tasks 
        WHERE (dueDateMillis >= :startOfDay AND dueDateMillis < :endOfDay AND status NOT IN ('COMPLETED', 'ARCHIVED'))
        OR (dueDateMillis < :startOfDay AND status NOT IN ('COMPLETED', 'ARCHIVED'))
        ORDER BY priority DESC, dueTimeMillis ASC
    """)
    suspend fun getPendingAndOverdueTasks(startOfDay: Long, endOfDay: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE dueDateMillis IS NULL AND status != 'ARCHIVED' ORDER BY priority DESC, createdDateMillis DESC")
    fun observeUnscheduledTasks(): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET status = :status WHERE id IN (:taskIds)")
    suspend fun updateStatusForTasks(taskIds: List<String>, status: String)

    @Query("UPDATE tasks SET priority = :priority WHERE id IN (:taskIds)")
    suspend fun updatePriorityForTasks(taskIds: List<String>, priority: String)

    @Query("UPDATE tasks SET categoryId = :categoryId WHERE id IN (:taskIds)")
    suspend fun updateCategoryForTasks(taskIds: List<String>, categoryId: String?)

    @Query("UPDATE tasks SET dueDateMillis = :dueDateMillis, dueTimeMillis = :dueTimeMillis WHERE id IN (:taskIds)")
    suspend fun updateDueDateForTasks(taskIds: List<String>, dueDateMillis: Long?, dueTimeMillis: Long?)

    @Query("UPDATE tasks SET status = 'ARCHIVED' WHERE id IN (:taskIds)")
    suspend fun archiveTasks(taskIds: List<String>)
}
