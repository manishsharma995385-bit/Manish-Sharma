package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Task
import com.example.data.model.ChatMessage
import com.example.data.model.DocumentFile
import com.example.data.model.NotificationLog
import com.example.data.model.TimeLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    // Tasks operations
    @Query("SELECT * FROM tasks ORDER BY deadlineMs ASC")
    fun getAllTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE status = 'Pending' AND deadlineMs < :currentTime")
    suspend fun getOverduePendingTasks(currentTime: Long): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)


    // Chat Message operations
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long


    // Document files operations
    @Query("SELECT * FROM document_files ORDER BY uploadTime DESC")
    fun getAllDocumentsFlow(): Flow<List<DocumentFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentFile): Long

    @Delete
    suspend fun deleteDocument(document: DocumentFile)


    // Notification Log operations
    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationLog(log: NotificationLog): Long

    // --- Time Log operations ---
    @Query("SELECT * FROM time_logs ORDER BY timestamp DESC")
    fun getAllTimeLogsFlow(): Flow<List<TimeLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeLog(timeLog: TimeLog): Long

    @Query("DELETE FROM time_logs WHERE id = :id")
    suspend fun deleteTimeLogById(id: Int)
}
