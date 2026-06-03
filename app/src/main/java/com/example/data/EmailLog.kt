package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "email_logs")
data class EmailLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskTitle: String,
    val recipientName: String,
    val recipientEmail: String,
    val sentTimestamp: Long = System.currentTimeMillis(),
    val status: String = "SENT" // SENT, PENDING, FAILED
)
