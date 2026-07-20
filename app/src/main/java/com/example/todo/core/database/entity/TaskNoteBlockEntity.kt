package com.example.todo.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "task_note_blocks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"])
    ]
)
data class TaskNoteBlockEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val blockType: String, // "TEXT", "CHECKLIST", "LINK", "QUOTE", "DIVIDER"
    val content: String,
    val position: Int,
    val isChecked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class NoteBlockType {
    TEXT, CHECKLIST, LINK, QUOTE, DIVIDER
}
