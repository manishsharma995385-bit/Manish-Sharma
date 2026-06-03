package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_logs")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskTitle: String,
    val recipientEmail: String,
    val timestamp: Long,
    val isOverdueAlert: Boolean = true,
    val statusMessage: String // e.g. "Sent automatically to team@work.com"
)
