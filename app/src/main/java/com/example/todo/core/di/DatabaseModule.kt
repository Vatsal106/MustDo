package com.example.todo.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.todo.core.database.AppDatabase
import com.example.todo.core.database.dao.*
import com.example.todo.core.database.entity.AchievementEntity
import com.example.todo.core.database.entity.CategoryEntity
import com.example.todo.core.database.entity.TaskResourceEntity
import com.example.todo.core.database.entity.TaskNoteBlockEntity
import com.example.todo.core.database.entity.TaskActivityEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.example.todo.core.database.DatabaseMigrations
import com.example.todo.core.database.dao.ReminderDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        categoryDaoProvider: Provider<CategoryDao>,
        achievementDaoProvider: Provider<AchievementDao>
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mustdo_database"
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Seed default categories and achievements
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        categoryDaoProvider.get().insertAll(defaultCategories())
                        achievementDaoProvider.get().insertAll(defaultAchievements())
                    }
                }
            })
            .addMigrations(
                DatabaseMigrations.MIGRATION_1_2,
                DatabaseMigrations.MIGRATION_2_3,
                DatabaseMigrations.MIGRATION_3_4,
                DatabaseMigrations.MIGRATION_4_5,
                DatabaseMigrations.MIGRATION_5_6
            )
            .build()
     }

     @Provides
     fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

     @Provides
     fun provideSubTaskDao(database: AppDatabase): SubTaskDao = database.subTaskDao()

     @Provides
     fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

     @Provides
     fun provideFocusSessionDao(database: AppDatabase): FocusSessionDao = database.focusSessionDao()

     @Provides
     fun provideAchievementDao(database: AppDatabase): AchievementDao = database.achievementDao()

     @Provides
     fun provideReminderDao(database: AppDatabase): ReminderDao = database.reminderDao()

     @Provides
     @Singleton
     fun provideTaskResourceDao(database: AppDatabase): TaskResourceDao = database.taskResourceDao()

     @Provides
     @Singleton
     fun provideTaskNoteBlockDao(database: AppDatabase): TaskNoteBlockDao = database.taskNoteBlockDao()

     @Provides
     @Singleton
     fun provideTaskActivityDao(database: AppDatabase): TaskActivityDao = database.taskActivityDao()
}

private fun defaultCategories(): List<CategoryEntity> = listOf(
    CategoryEntity(id = "cat_work", name = "Work", colorHex = "#6C63FF", iconName = "Work", isSystem = true, orderIndex = 0),
    CategoryEntity(id = "cat_personal", name = "Personal", colorHex = "#FF6B6B", iconName = "Person", isSystem = true, orderIndex = 1),
    CategoryEntity(id = "cat_health", name = "Health", colorHex = "#51CF66", iconName = "FavoriteBorder", isSystem = true, orderIndex = 2),
    CategoryEntity(id = "cat_learning", name = "Learning", colorHex = "#FFD43B", iconName = "MenuBook", isSystem = true, orderIndex = 3),
    CategoryEntity(id = "cat_finance", name = "Finance", colorHex = "#20C997", iconName = "AccountBalance", isSystem = true, orderIndex = 4),
)

private fun defaultAchievements(): List<AchievementEntity> = listOf(
    AchievementEntity(
        id = "ach_first_task", title = "First Step", description = "Complete your first task",
        iconName = "Star", conditionType = "TASKS_COMPLETED", conditionValue = 1
    ),
    AchievementEntity(
        id = "ach_streak_7", title = "Week Warrior", description = "Maintain a 7-day streak",
        iconName = "LocalFire", conditionType = "STREAK", conditionValue = 7
    ),
    AchievementEntity(
        id = "ach_streak_30", title = "Monthly Master", description = "Maintain a 30-day streak",
        iconName = "Whatshot", conditionType = "STREAK", conditionValue = 30
    ),
    AchievementEntity(
        id = "ach_level_5", title = "Rising Star", description = "Reach Level 5",
        iconName = "TrendingUp", conditionType = "LEVEL", conditionValue = 5
    ),
    AchievementEntity(
        id = "ach_level_10", title = "Productivity Pro", description = "Reach Level 10",
        iconName = "EmojiEvents", conditionType = "LEVEL", conditionValue = 10
    ),
    AchievementEntity(
        id = "ach_focus_master", title = "Focus Master", description = "Complete 50 focus sessions",
        iconName = "Psychology", conditionType = "FOCUS_SESSIONS", conditionValue = 50
    ),
)
