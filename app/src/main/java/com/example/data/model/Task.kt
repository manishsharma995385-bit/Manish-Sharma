package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val deadlineMs: Long,
    val project: String,
    val status: String, // "Pending" or "Completed"
    val priority: String, // "High", "Medium", "Low"
    val assignedTo: String,
    val emailSent: Boolean = false, // to track if simulated email notification was triggering
    val dependsOnTaskId: Int? = null // Task dependency link
)
