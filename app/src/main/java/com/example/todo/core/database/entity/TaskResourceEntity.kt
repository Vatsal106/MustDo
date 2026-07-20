package com.example.todo.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "task_resources",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["resourceType"])
    ]
)
data class TaskResourceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val resourceType: String, // "CONTACT", "ATTACHMENT", "VOICE_NOTE", "LOCATION"
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ResourceType {
    CONTACT, ATTACHMENT, VOICE_NOTE, LOCATION
}

data class ContactPayload(
    val name: String,
    val phone: String = "",
    val email: String = "",
    val whatsappNumber: String = "",
    val company: String = "",
    val notes: String = ""
)

data class AttachmentPayload(
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val fileSize: Long
)

data class VoiceNotePayload(
    val filePath: String,
    val duration: Long, // milliseconds
    val transcription: String = ""
)

data class LocationPayload(
    val locationName: String,
    val locationLink: String = "",
    val address: String = ""
)
