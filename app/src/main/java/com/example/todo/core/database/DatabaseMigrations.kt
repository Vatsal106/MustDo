package com.example.todo.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create the task_reminders table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `task_reminders` (
                    `id` TEXT NOT NULL,
                    `taskId` TEXT NOT NULL,
                    `reminderType` TEXT NOT NULL DEFAULT 'BEFORE_DUE_DATE',
                    `triggerTimestamp` INTEGER NOT NULL,
                    `offsetMinutes` INTEGER,
                    `repeatPattern` TEXT NOT NULL DEFAULT 'NONE',
                    `customIntervalMinutes` INTEGER,
                    `repeatDaysOfWeek` TEXT,
                    `label` TEXT NOT NULL DEFAULT '',
                    `isEnabled` INTEGER NOT NULL DEFAULT 1,
                    `lastTriggeredAt` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )

            // Create indices for efficient querying
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_reminders_taskId` ON `task_reminders` (`taskId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_reminders_triggerTimestamp` ON `task_reminders` (`triggerTimestamp`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_reminders_isEnabled` ON `task_reminders` (`isEnabled`)")

            // Migrate existing tasks that have reminderTimeMillis set:
            // Create an EXACT_TIME reminder for each task that has a non-null reminderTimeMillis
            // and is not completed or archived
            db.execSQL(
                """
                INSERT INTO `task_reminders` (`id`, `taskId`, `reminderType`, `triggerTimestamp`, `label`, `isEnabled`, `createdAt`)
                SELECT 
                    lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-' || hex(randomblob(2)) || '-' || hex(randomblob(2)) || '-' || hex(randomblob(6))),
                    `id`,
                    'EXACT_TIME',
                    `reminderTimeMillis`,
                    'Migrated reminder',
                    1,
                    CAST(strftime('%s', 'now') * 1000 AS INTEGER)
                FROM `tasks`
                WHERE `reminderTimeMillis` IS NOT NULL
                AND `status` NOT IN ('COMPLETED', 'ARCHIVED')
                """.trimIndent()
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create task_resources table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `task_resources` (
                    `id` TEXT NOT NULL,
                    `taskId` TEXT NOT NULL,
                    `resourceType` TEXT NOT NULL,
                    `payloadJson` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_resources_taskId` ON `task_resources` (`taskId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_resources_resourceType` ON `task_resources` (`resourceType`)")

            // Create task_note_blocks table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `task_note_blocks` (
                    `id` TEXT NOT NULL,
                    `taskId` TEXT NOT NULL,
                    `blockType` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    `isChecked` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_note_blocks_taskId` ON `task_note_blocks` (`taskId`)")

            // Create task_activities table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `task_activities` (
                    `id` TEXT NOT NULL,
                    `taskId` TEXT NOT NULL,
                    `activityType` TEXT NOT NULL,
                    `details` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_activities_taskId` ON `task_activities` (`taskId`)")
        }
     }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `rescheduleCount` INTEGER NOT NULL DEFAULT 0")
        }
    }
    
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `autoReschedule` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `skippedCount` INTEGER NOT NULL DEFAULT 0")
        }
    }
    
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `orderIndex` INTEGER NOT NULL DEFAULT 0")
        }
    }
}
