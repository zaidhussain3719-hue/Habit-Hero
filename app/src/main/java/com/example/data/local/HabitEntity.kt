package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Health",
    val frequency: String = "Daily",
    val targetCount: Int = 1,
    val unit: String = "times",
    val reminderTime: String = "08:00",
    val iconName: String = "CheckCircle",
    val colorHex: String = "#10B981",
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)

@Entity(tableName = "habit_logs")
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val date: String, // "YYYY-MM-DD"
    val completedCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)
