package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repository_documents")
data class RepositoryDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val fileSize: String,
    val uploadedBy: String,
    val uploadDate: Long = System.currentTimeMillis(),
    val fileType: String, // pdf, docx, xlsx, pptx, png, zip, csv
    val localPath: String = "" // For optional reference files or local cache
)
