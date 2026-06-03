package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val dueDate: Long, // timestamp in ms
    val isCompleted: Boolean = false,
    val priority: String = "Medium", // High, Medium, Low
    val project: String = "General", // Design, Dev, Marketing, etc.
    val assignedTo: String = "Unassigned" // Sarah, Alex, David, Emma, Unassigned
)
