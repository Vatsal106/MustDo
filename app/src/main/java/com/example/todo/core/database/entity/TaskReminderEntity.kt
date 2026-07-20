package com.example.todo.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(
    tableName = "task_reminders",
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
        Index(value = ["triggerTimestamp"]),
        Index(value = ["isEnabled"])
    ]
)
data class TaskReminderEntity(
    @PrimaryKey
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("taskId")
    val taskId: String,

    @SerializedName("reminderType")
    val reminderType: String = ReminderType.BEFORE_DUE_DATE.name,

    @SerializedName("triggerTimestamp")
    val triggerTimestamp: Long,

    /** For BEFORE_DUE_DATE: minutes before due date to fire */
    @SerializedName("offsetMinutes")
    val offsetMinutes: Int? = null,

    @SerializedName("repeatPattern")
    val repeatPattern: String = RepeatPattern.NONE.name,

    /** For CUSTOM_INTERVAL repeat: fire every N minutes */
    @SerializedName("customIntervalMinutes")
    val customIntervalMinutes: Int? = null,

    /** For WEEKLY repeat: comma-separated day-of-week indices, e.g. "1,4" = Mon,Thu (Calendar.MONDAY=2 based, but we use 1=Sun..7=Sat) */
    @SerializedName("repeatDaysOfWeek")
    val repeatDaysOfWeek: String? = null,

    /** User-facing label, e.g. "1 day before", "At due time" */
    @SerializedName("label")
    val label: String = "",

    @SerializedName("isEnabled")
    val isEnabled: Boolean = true,

    @SerializedName("lastTriggeredAt")
    val lastTriggeredAt: Long? = null,

    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReminderType {
    /** Fire at exact triggerTimestamp */
    EXACT_TIME,
    /** Fire offsetMinutes before task's dueDate */
    BEFORE_DUE_DATE,
    /** Fire on repeatPattern schedule (daily, weekly with specific days, monthly, yearly) */
    RECURRING,
    /** Fire every customIntervalMinutes until task is completed (max 24h cap) */
    CUSTOM_INTERVAL
}

enum class RepeatPattern {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}
