package com.example.todo.core.backup

data class BackupInfo(
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val createdAt: Long,
    val backupVersion: Int,
    val appVersion: String,
    val taskCount: Int,
    val categoryCount: Int,
    val subTaskCount: Int,
    val focusSessionCount: Int,
    val achievementCount: Int,
    val reminderCount: Int = 0,
    val isValid: Boolean = true,
    val validationMessage: String? = null
)
