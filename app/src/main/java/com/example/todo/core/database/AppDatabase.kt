package com.example.todo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.todo.core.database.dao.*
import com.example.todo.core.database.entity.*

@Database(
    entities = [
        TaskEntity::class,
        SubTaskEntity::class,
        CategoryEntity::class,
        FocusSessionEntity::class,
        AchievementEntity::class,
        TaskReminderEntity::class,
        TaskResourceEntity::class,
        TaskNoteBlockEntity::class,
        TaskActivityEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun subTaskDao(): SubTaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun achievementDao(): AchievementDao
    abstract fun reminderDao(): ReminderDao
    abstract fun taskResourceDao(): TaskResourceDao
    abstract fun taskNoteBlockDao(): TaskNoteBlockDao
    abstract fun taskActivityDao(): TaskActivityDao
}
