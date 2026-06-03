package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    // --- Tasks Queries ---
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    fun getPendingTasksFlow(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)


    // --- Chat Messages Queries ---
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()


    // --- Repository Documents Queries ---
    @Query("SELECT * FROM repository_documents ORDER BY uploadDate DESC")
    fun getAllDocumentsFlow(): Flow<List<RepositoryDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: RepositoryDocument)

    @Delete
    suspend fun deleteDocument(document: RepositoryDocument)


    // --- Email Logs Queries ---
    @Query("SELECT * FROM email_logs ORDER BY sentTimestamp DESC")
    fun getAllEmailLogsFlow(): Flow<List<EmailLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmailLog(log: EmailLog)
}
