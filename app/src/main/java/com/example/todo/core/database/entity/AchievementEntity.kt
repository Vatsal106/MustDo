package com.example.todo.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    @SerializedName("id", alternate = ["a"])
    val id: String,
    @SerializedName("title", alternate = ["b"])
    val title: String,
    @SerializedName("description", alternate = ["c"])
    val description: String,
    @SerializedName("iconName", alternate = ["d"])
    val iconName: String = "EmojiEvents",
    @SerializedName("conditionType", alternate = ["e"])
    val conditionType: String,
    @SerializedName("conditionValue", alternate = ["f"])
    val conditionValue: Int,
    @SerializedName("isUnlocked", alternate = ["g"])
    val isUnlocked: Boolean = false,
    @SerializedName("unlockedTimeMillis", alternate = ["h"])
    val unlockedTimeMillis: Long? = null
)
