package com.example.todo.core.database.dao

import androidx.room.*
import com.example.todo.core.database.entity.TaskActivityEntity
import com.example.todo.core.database.entity.TaskNoteBlockEntity
import com.example.todo.core.database.entity.TaskResourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskResourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resource: TaskResourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(resources: List<TaskResourceEntity>)

    @Delete
    suspend fun delete(resource: TaskResourceEntity)

    @Query("DELETE FROM task_resources WHERE id = :resourceId")
    suspend fun deleteById(resourceId: String)

    @Query("DELETE FROM task_resources WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    @Query("DELETE FROM task_resources")
    suspend fun deleteAll()

    @Query("SELECT * FROM task_resources WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun observeByTaskId(taskId: String): Flow<List<TaskResourceEntity>>

    @Query("SELECT * FROM task_resources WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getByTaskId(taskId: String): List<TaskResourceEntity>

    @Query("SELECT * FROM task_resources")
    suspend fun getAll(): List<TaskResourceEntity>

    @Query("SELECT * FROM task_resources")
    fun observeAll(): Flow<List<TaskResourceEntity>>
}

@Dao
interface TaskNoteBlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(block: TaskNoteBlockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(blocks: List<TaskNoteBlockEntity>)

    @Update
    suspend fun update(block: TaskNoteBlockEntity)

    @Delete
    suspend fun delete(block: TaskNoteBlockEntity)

    @Query("DELETE FROM task_note_blocks WHERE id = :blockId")
    suspend fun deleteById(blockId: String)

    @Query("DELETE FROM task_note_blocks WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    @Query("DELETE FROM task_note_blocks")
    suspend fun deleteAll()

    @Query("SELECT * FROM task_note_blocks WHERE taskId = :taskId ORDER BY position ASC")
    fun observeByTaskId(taskId: String): Flow<List<TaskNoteBlockEntity>>

    @Query("SELECT * FROM task_note_blocks WHERE taskId = :taskId ORDER BY position ASC")
    suspend fun getByTaskId(taskId: String): List<TaskNoteBlockEntity>

    @Query("SELECT * FROM task_note_blocks")
    suspend fun getAll(): List<TaskNoteBlockEntity>

    @Query("SELECT * FROM task_note_blocks")
    fun observeAll(): Flow<List<TaskNoteBlockEntity>>
}

@Dao
interface TaskActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: TaskActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<TaskActivityEntity>)

    @Query("DELETE FROM task_activities WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    @Query("DELETE FROM task_activities")
    suspend fun deleteAll()

    @Query("SELECT * FROM task_activities WHERE taskId = :taskId ORDER BY createdAt DESC")
    fun observeByTaskId(taskId: String): Flow<List<TaskActivityEntity>>

    @Query("SELECT * FROM task_activities WHERE taskId = :taskId ORDER BY createdAt DESC")
    suspend fun getByTaskId(taskId: String): List<TaskActivityEntity>

    @Query("SELECT * FROM task_activities")
    suspend fun getAll(): List<TaskActivityEntity>
}
