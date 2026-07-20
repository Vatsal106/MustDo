package com.example.todo.core.backup

import com.example.todo.core.database.entity.TaskReminderEntity

object BackupMigrationManager {
    const val CURRENT_SCHEMA_VERSION = 3

    fun migrate(payload: BackupPayload): BackupPayload {
        var currentPayload = payload
        if (currentPayload.version > CURRENT_SCHEMA_VERSION) {
            throw IllegalArgumentException("Unsupported backup version: ${currentPayload.version}. Please update the app.")
        }
        
        while (currentPayload.version < CURRENT_SCHEMA_VERSION) {
            currentPayload = when (currentPayload.version) {
                0 -> migrate0To1(currentPayload)
                1 -> migrate1To2(currentPayload)
                2 -> migrate2To3(currentPayload)
                else -> throw IllegalStateException("No migration path found for version ${currentPayload.version}")
            }
        }
        return currentPayload
    }

    private fun migrate0To1(payload: BackupPayload): BackupPayload {
        return payload.copy(
            version = 1,
            settings = payload.settings.sanitize(),
            categories = payload.categories ?: emptyList(),
            tasks = payload.tasks ?: emptyList(),
            subTasks = payload.subTasks ?: emptyList(),
            focusSessions = payload.focusSessions ?: emptyList(),
            achievements = payload.achievements ?: emptyList(),
            reminders = payload.reminders ?: emptyList()
        )
    }

    private fun migrate1To2(payload: BackupPayload): BackupPayload {
        val tasksList = payload.tasks ?: emptyList()
        // Auto-create TaskReminderEntity for any task with reminderTimeMillis
        val migratedReminders = tasksList
            .filter { it.reminderTimeMillis != null && it.status != "COMPLETED" && it.status != "ARCHIVED" }
            .map { task ->
                TaskReminderEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    taskId = task.id,
                    reminderType = "EXACT_TIME",
                    triggerTimestamp = task.reminderTimeMillis!!,
                    label = "Migrated reminder",
                    isEnabled = true,
                    createdAt = System.currentTimeMillis()
                )
            }
        return payload.copy(
            version = 2,
            settings = payload.settings.sanitize(),
            categories = payload.categories ?: emptyList(),
            tasks = tasksList,
            subTasks = payload.subTasks ?: emptyList(),
            focusSessions = payload.focusSessions ?: emptyList(),
            achievements = payload.achievements ?: emptyList(),
            reminders = migratedReminders
        )
    }

    private fun migrate2To3(payload: BackupPayload): BackupPayload {
        return payload.copy(
            version = 3,
            resources = emptyList(),
            noteBlocks = emptyList(),
            activities = emptyList()
        )
    }
}
