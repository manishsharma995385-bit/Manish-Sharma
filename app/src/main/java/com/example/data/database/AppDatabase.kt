package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.WorkspaceDao
import com.example.data.model.Task
import com.example.data.model.ChatMessage
import com.example.data.model.DocumentFile
import com.example.data.model.NotificationLog
import com.example.data.model.TimeLog

@Database(
    entities = [
        Task::class,
        ChatMessage::class,
        DocumentFile::class,
        NotificationLog::class,
        TimeLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workspace_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
