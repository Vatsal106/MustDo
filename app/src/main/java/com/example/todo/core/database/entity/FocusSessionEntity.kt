package com.example.todo.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["startTimeMillis"])
    ]
)
data class FocusSessionEntity(
    @PrimaryKey
    @SerializedName("id", alternate = ["a"])
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("taskId", alternate = ["b"])
    val taskId: String? = null,
    @SerializedName("sessionType", alternate = ["c"])
    val sessionType: String = SessionType.POMODORO.name,
    @SerializedName("startTimeMillis", alternate = ["d"])
    val startTimeMillis: Long = System.currentTimeMillis(),
    @SerializedName("durationMinutes", alternate = ["e"])
    val durationMinutes: Int = 25,
    @SerializedName("isCompleted", alternate = ["f"])
    val isCompleted: Boolean = false
)

enum class SessionType {
    POMODORO, DEEP_WORK
}
