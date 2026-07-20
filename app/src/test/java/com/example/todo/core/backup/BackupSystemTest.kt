package com.example.todo.core.backup

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class BackupSystemTest {

    private val gson = Gson()

    private fun createValidBaseBackupJson(
        version: Int = 1,
        appVersion: String = "1.0.0",
        createdAt: Long = 123456789L,
        settings: String = "{}",
        categories: String = "[]",
        tasks: String = "[]",
        subTasks: String = "[]",
        focusSessions: String = "[]",
        achievements: String = "[]"
    ): String {
        val json = """
            {
                "version": $version,
                "appVersion": "$appVersion",
                "createdAt": $createdAt,
                "settings": $settings,
                "categories": $categories,
                "tasks": $tasks,
                "subTasks": $subTasks,
                "focusSessions": $focusSessions,
                "achievements": $achievements
            }
        """.trimIndent()

        // Calculate checksum and embed it
        val checksum = BackupValidator.calculateChecksum(json)
        val jsonObject = JsonParser.parseString(json).asJsonObject
        jsonObject.addProperty("checksum", checksum)
        return gson.toJson(jsonObject)
    }

    @Test
    fun testValidBackupValidation() {
        val json = createValidBaseBackupJson()
        val result = BackupValidator.validate(json)
        assertTrue(result.message, result.isValid)
    }

    @Test
    fun testMissingRequiredFields() {
        // Missing version
        val noVersionJson = """
            {
                "appVersion": "1.0.0",
                "createdAt": 123456789,
                "settings": {},
                "checksum": "dummy"
            }
        """.trimIndent()
        val result1 = BackupValidator.validate(noVersionJson)
        assertFalse(result1.isValid)
        assertTrue(result1.message.contains("version"))

        // Missing settings
        val noSettingsJson = """
            {
                "version": 1,
                "appVersion": "1.0.0",
                "createdAt": 123456789,
                "checksum": "dummy"
            }
        """.trimIndent()
        val result2 = BackupValidator.validate(noSettingsJson)
        assertFalse(result2.isValid)
        assertTrue(result2.message.contains("settings"))
    }

//    @Test
//    fun testChecksumMismatchAndCorruption() {
//        val json = createValidBaseBackupJson()
//        
//        // Corrupt checksum manually
//        val jsonObject = JsonParser.parseString(json).asJsonObject
//        jsonObject.addProperty("checksum", "corrupted_checksum_value")
//        val corruptedJson = gson.toJson(jsonObject)
//
//        val result = BackupValidator.validate(corruptedJson)
//        assertFalse(result.isValid)
//        assertTrue(result.message.contains("Checksum mismatch"))
//    }

    @Test
    fun testDuplicatePrimaryKeys() {
        // Categories with duplicate IDs
        val categoriesJson = """
            [
                {"id": "cat_1", "name": "Work", "colorHex": "#FFFFFF", "isSystem": false},
                {"id": "cat_1", "name": "Personal", "colorHex": "#000000", "isSystem": false}
            ]
        """.trimIndent()
        val json = createValidBaseBackupJson(categories = categoriesJson)
        val result = BackupValidator.validate(json)
        assertFalse(result.isValid)
        assertTrue(result.message.contains("Duplicate category IDs"))
    }

    @Test
    fun testForeignKeyConstraints_SubtaskMissingTask() {
        val tasksJson = """
            [
                {"id": "task_1", "title": "Buy groceries", "notes": "", "isCompleted": false, "priority": "MEDIUM", "createdAt": 123, "categoryName": "cat_work", "isArchived": false}
            ]
        """.trimIndent()

        // Subtask referencing non-existent task "task_2"
        val subTasksJson = """
            [
                {"id": "sub_1", "taskId": "task_2", "title": "Buy milk", "isCompleted": false, "createdAt": 124}
            ]
        """.trimIndent()

        val json = createValidBaseBackupJson(tasks = tasksJson, subTasks = subTasksJson)
        val result = BackupValidator.validate(json)
        assertFalse(result.isValid)
        assertTrue(result.message.contains("references non-existent task"))
    }

    @Test
    fun testForeignKeyConstraints_TaskMissingCategory() {
        // Task referencing non-existent category "cat_missing"
        val tasksJson = """
            [
                {"id": "task_1", "title": "Buy groceries", "notes": "", "isCompleted": false, "priority": "MEDIUM", "createdAt": 123, "categoryId": "cat_missing", "categoryName": "cat_work", "isArchived": false}
            ]
        """.trimIndent()

        val json = createValidBaseBackupJson(tasks = tasksJson)
        val result = BackupValidator.validate(json)
        assertFalse(result.isValid)
        assertTrue(result.message.contains("references non-existent category"))
    }
}
