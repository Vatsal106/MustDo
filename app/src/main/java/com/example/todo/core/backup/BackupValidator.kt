package com.example.todo.core.backup

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.security.MessageDigest

object BackupValidator {
    
    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )

    private val gson = Gson()

    fun validate(json: String): ValidationResult {
        return try {
            val jsonElement = JsonParser.parseString(json)
            if (!jsonElement.isJsonObject) {
                return ValidationResult(false, "Invalid JSON structure: Root must be a JSON object")
            }

            val jsonObject = jsonElement.asJsonObject

            // 1. Check required fields
            if (!jsonObject.has("version")) {
                return ValidationResult(false, "Missing required field: 'version'")
            }
            if (!jsonObject.has("appVersion")) {
                return ValidationResult(false, "Missing required field: 'appVersion'")
            }
            if (!jsonObject.has("createdAt")) {
                return ValidationResult(false, "Missing required field: 'createdAt'")
            }
            if (!jsonObject.has("settings")) {
                return ValidationResult(false, "Missing required field: 'settings'")
            }

            // 2. Checksum verification (Disabled for now while importing)
            /*
            val version = if (jsonObject.has("version")) jsonObject.get("version").asInt else 0
            if (jsonObject.has("checksum")) {
                val embeddedChecksum = jsonObject.get("checksum").asString
                val computedChecksum = calculateChecksum(json)
                if (embeddedChecksum != computedChecksum) {
                    return ValidationResult(false, "Checksum mismatch: Backup file is corrupted or modified")
                }
            } else if (version >= 2) {
                return ValidationResult(false, "Missing checksum for integrity verification")
            }
            */

            // Deserialize to run logical validations
            val payload = gson.fromJson(jsonObject, BackupPayload::class.java)

            if (payload.version <= 0) {
                return ValidationResult(false, "Invalid backup version: ${payload.version}")
            }

            val tasksList = payload.tasks ?: emptyList()
            val categoriesList = payload.categories ?: emptyList()
            val subTasksList = payload.subTasks ?: emptyList()
            val focusSessionsList = payload.focusSessions ?: emptyList()
            val achievementsList = payload.achievements ?: emptyList()
            val remindersList = payload.reminders ?: emptyList()
            val resourcesList = payload.resources ?: emptyList()
            val noteBlocksList = payload.noteBlocks ?: emptyList()
            val activitiesList = payload.activities ?: emptyList()

            // 3. Unique Primary Keys Validation
            val taskIds = tasksList.map { it.id }.toSet()
            if (taskIds.size != tasksList.size) {
                return ValidationResult(false, "Data integrity failure: Duplicate task IDs detected")
            }

            val categoryIds = categoriesList.map { it.id }.toSet()
            if (categoryIds.size != categoriesList.size) {
                return ValidationResult(false, "Data integrity failure: Duplicate category IDs detected")
            }

            val subTaskIds = subTasksList.map { it.id }.toSet()
            if (subTaskIds.size != subTasksList.size) {
                return ValidationResult(false, "Data integrity failure: Duplicate subtask IDs detected")
            }

            val sessionIds = focusSessionsList.map { it.id }.toSet()
            if (sessionIds.size != focusSessionsList.size) {
                return ValidationResult(false, "Data integrity failure: Duplicate focus session IDs detected")
            }

            val achievementIds = achievementsList.map { it.id }.toSet()
            if (achievementIds.size != achievementsList.size) {
                return ValidationResult(false, "Data integrity failure: Duplicate achievement IDs detected")
            }

            val reminderIds = remindersList.map { it.id }.toSet()
            if (reminderIds.size != remindersList.size) {
                return ValidationResult(false, "Data integrity failure: Duplicate reminder IDs detected")
            }

            val resourceIds = resourcesList.map { it.id }.toSet()
            if (resourceIds.size != resourcesList.size) {
                return ValidationResult(false, "Data integrity failure: Duplicate resource IDs detected")
            }

            val noteBlockIds = noteBlocksList.map { it.id }.toSet()
            if (noteBlockIds.size != noteBlocksList.size) {
                return ValidationResult(false, "Data integrity failure: Duplicate note block IDs detected")
            }

            val activityIds = activitiesList.map { it.id }.toSet()
            if (activityIds.size != activitiesList.size) {
                return ValidationResult(false, "Data integrity failure: Duplicate activity IDs detected")
            }

            // 4. Foreign Key Constraints Validation
            // Subtasks must point to existing Tasks
            subTasksList.forEach { subTask ->
                if (subTask.taskId.isBlank()) {
                    return ValidationResult(false, "Data integrity failure: Subtask '${subTask.title}' has no associated taskId")
                }
                if (!taskIds.contains(subTask.taskId)) {
                    return ValidationResult(false, "Data integrity failure: Subtask '${subTask.title}' references non-existent task '${subTask.taskId}'")
                }
            }

            // Reminders must point to existing Tasks
            remindersList.forEach { reminder ->
                if (reminder.taskId.isBlank()) {
                    return ValidationResult(false, "Data integrity failure: Reminder '${reminder.id}' has no associated taskId")
                }
                if (!taskIds.contains(reminder.taskId)) {
                    return ValidationResult(false, "Data integrity failure: Reminder '${reminder.id}' references non-existent task '${reminder.taskId}'")
                }
            }

            // Resources must point to existing Tasks
            resourcesList.forEach { resource ->
                if (resource.taskId.isBlank()) {
                    return ValidationResult(false, "Data integrity failure: Resource '${resource.id}' has no associated taskId")
                }
                if (!taskIds.contains(resource.taskId)) {
                    return ValidationResult(false, "Data integrity failure: Resource '${resource.id}' references non-existent task '${resource.taskId}'")
                }
            }

            // Note blocks must point to existing Tasks
            noteBlocksList.forEach { block ->
                if (block.taskId.isBlank()) {
                    return ValidationResult(false, "Data integrity failure: Note block '${block.id}' has no associated taskId")
                }
                if (!taskIds.contains(block.taskId)) {
                    return ValidationResult(false, "Data integrity failure: Note block '${block.id}' references non-existent task '${block.taskId}'")
                }
            }

            // Activities must point to existing Tasks
            activitiesList.forEach { activity ->
                if (activity.taskId.isBlank()) {
                    return ValidationResult(false, "Data integrity failure: Activity '${activity.id}' has no associated taskId")
                }
                if (!taskIds.contains(activity.taskId)) {
                    return ValidationResult(false, "Data integrity failure: Activity '${activity.id}' references non-existent task '${activity.taskId}'")
                }
            }

            // Tasks must reference existing Categories (if categoryId is not null)
            tasksList.forEach { task ->
                if (task.categoryId != null && !categoryIds.contains(task.categoryId)) {
                    return ValidationResult(false, "Data integrity failure: Task '${task.title}' references non-existent category '${task.categoryId}'")
                }
            }

            ValidationResult(true, "Backup is valid")
        } catch (e: Exception) {
            ValidationResult(false, "Failed to parse JSON backup: ${e.message}")
        }
    }

    fun calculateChecksum(payloadJson: String): String {
        val jsonElement = JsonParser.parseString(payloadJson)
        val jsonObject = jsonElement.asJsonObject
        jsonObject.addProperty("checksum", "")
        
        // Serialize with standard Gson to get canonical ordering
        val standardizedJson = gson.toJson(jsonObject)
        return sha256(standardizedJson)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { String.format("%02x", it) }
    }
}
