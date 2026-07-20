package com.example.todo.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    @SerializedName("id", alternate = ["a"])
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("name", alternate = ["b"])
    val name: String,
    @SerializedName("colorHex", alternate = ["c"])
    val colorHex: String = "#6C63FF",
    @SerializedName("iconName", alternate = ["d"])
    val iconName: String = "Label",
    @SerializedName("isSystem", alternate = ["e"])
    val isSystem: Boolean = false,
    @SerializedName("orderIndex", alternate = ["f"])
    val orderIndex: Int = 0
)
