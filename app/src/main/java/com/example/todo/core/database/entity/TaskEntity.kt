package com.example.todo.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["dueDateMillis"]),
        Index(value = ["status"]),
        Index(value = ["priority"]),
        Index(value = ["createdDateMillis"]),
        Index(value = ["title"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    @SerializedName("id", alternate = ["a"])
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("title", alternate = ["b"])
    val title: String,
    @SerializedName("description", alternate = ["c"])
    val description: String = "",
    @SerializedName("priority", alternate = ["d"])
    val priority: String = Priority.MEDIUM.name,
    @ColumnInfo(name = "categoryId")
    @SerializedName("categoryId", alternate = ["e"])
    val categoryId: String? = null,
    @SerializedName("dueDateMillis", alternate = ["f"])
    val dueDateMillis: Long? = null,
    @SerializedName("dueTimeMillis", alternate = ["g"])
    val dueTimeMillis: Long? = null,
    @SerializedName("tags", alternate = ["h"])
    val tags: String = "",  // comma-separated
    @SerializedName("notes", alternate = ["i"])
    val notes: String = "",
    @SerializedName("reminderTimeMillis", alternate = ["j"])
    val reminderTimeMillis: Long? = null,
    @SerializedName("estimatedMinutes", alternate = ["k"])
    val estimatedMinutes: Int? = null,
    @SerializedName("recurrence", alternate = ["l"])
    val recurrence: String = Recurrence.NONE.name,
    @SerializedName("status", alternate = ["m"])
    val status: String = TaskStatus.PENDING.name,
    @SerializedName("dependencyTaskId", alternate = ["n"])
    val dependencyTaskId: String? = null,
    @SerializedName("createdDateMillis", alternate = ["o"])
    val createdDateMillis: Long = System.currentTimeMillis(),
    @SerializedName("completedDateMillis", alternate = ["p"])
    val completedDateMillis: Long? = null,
    @SerializedName("rescheduleCount", alternate = ["q"])
    val rescheduleCount: Int = 0,
    @SerializedName("autoReschedule", alternate = ["r"])
    val autoReschedule: Boolean = false,
    @SerializedName("skippedCount", alternate = ["s"])
    val skippedCount: Int = 0,
    @SerializedName("orderIndex", alternate = ["t"])
    val orderIndex: Int = 0
)

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, ARCHIVED
}

enum class Priority {
    LOW, MEDIUM, HIGH, URGENT
}

enum class Recurrence {
    NONE, DAILY, WEEKLY, MONTHLY
}
