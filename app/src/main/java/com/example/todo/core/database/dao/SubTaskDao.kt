package com.example.todo.core.database.dao

import androidx.room.*
import com.example.todo.core.database.entity.SubTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subTasks: List<SubTaskEntity>)

    @Update
    suspend fun updateSubTask(subTask: SubTaskEntity)

    @Delete
    suspend fun deleteSubTask(subTask: SubTaskEntity)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubTasksByTaskId(taskId: String)

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY orderIndex ASC")
    fun observeSubTasksByTaskId(taskId: String): Flow<List<SubTaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY orderIndex ASC")
    suspend fun getSubTasksByTaskId(taskId: String): List<SubTaskEntity>

    @Query("SELECT * FROM subtasks")
    suspend fun getAllSubTasks(): List<SubTaskEntity>

    @Query("DELETE FROM subtasks")
    suspend fun deleteAll()
}
