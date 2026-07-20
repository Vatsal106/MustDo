package com.example.todo.features.widget

import android.content.Context
import com.example.todo.core.database.dao.TaskDao
import com.example.todo.core.database.dao.FocusSessionDao
import com.example.todo.core.datastore.UserPreferencesManager
import com.example.todo.features.tasks.domain.TaskRepository
import com.example.todo.features.gamification.domain.GamificationEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskDao(): TaskDao
    fun focusSessionDao(): FocusSessionDao
    fun userPreferencesManager(): UserPreferencesManager
    fun taskRepository(): TaskRepository
    fun gamificationEngine(): GamificationEngine
}

object WidgetDependencies {
    fun getGamificationEngine(context: Context): GamificationEngine {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        return entryPoint.gamificationEngine()
    }

    fun getTaskRepository(context: Context): TaskRepository {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        return entryPoint.taskRepository()
    }

    fun getTaskDao(context: Context): TaskDao {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        return entryPoint.taskDao()
    }
    
    fun getFocusDao(context: Context): FocusSessionDao {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        return entryPoint.focusSessionDao()
    }
    
    fun getUserPreferencesManager(context: Context): UserPreferencesManager {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        return entryPoint.userPreferencesManager()
    }
}
