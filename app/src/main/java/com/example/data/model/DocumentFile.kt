package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "document_files")
data class DocumentFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val fileType: String,
    val sizeText: String,
    val localUri: String?, // Points to selected local file URI (if loaded from device)
    val uploadTime: Long,
    val uploadedBy: String,
    val isSecure: Boolean = true
)
