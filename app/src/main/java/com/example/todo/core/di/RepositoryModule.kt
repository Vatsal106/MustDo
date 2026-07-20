package com.example.todo.core.di

import com.example.todo.features.focus.data.FocusRepositoryImpl
import com.example.todo.features.focus.domain.FocusRepository
import com.example.todo.features.settings.data.CategoryRepositoryImpl
import com.example.todo.features.settings.domain.CategoryRepository
import com.example.todo.features.tasks.data.TaskRepositoryImpl
import com.example.todo.features.tasks.domain.TaskRepository
import com.example.todo.features.tasks.domain.ReminderRepository
import com.example.todo.features.tasks.data.ReminderRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindFocusRepository(impl: FocusRepositoryImpl): FocusRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository
}
