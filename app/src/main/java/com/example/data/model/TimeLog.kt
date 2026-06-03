package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_logs")
data class TimeLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val taskTitle: String,
    val project: String, // e.g. "Operations", "Branding", "Dev", "Design", etc.
    val user: String, // The developer/team member logging the time
    val timeSpentSeconds: Long, // Duration of log in seconds
    val timestamp: Long = System.currentTimeMillis() // When the entry was recorded
)
