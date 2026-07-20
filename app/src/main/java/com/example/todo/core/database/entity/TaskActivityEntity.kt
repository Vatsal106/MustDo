package com.example.todo.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "task_activities",
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
data class TaskActivityEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val activityType: String, // e.g. "CREATED", "COMPLETED", "ADD_RESOURCE", "DELETE_RESOURCE", "ADD_NOTE_BLOCK"
    val details: String,
    val createdAt: Long = System.currentTimeMillis()
)
