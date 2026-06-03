package com.example.repository

import com.example.data.*
import kotlinx.coroutines.flow.Flow

class WorkspaceRepository(private val dao: WorkspaceDao) {

    // --- Tasks API ---
    val allTasks: Flow<List<Task>> = dao.getAllTasksFlow()
    val pendingTasks: Flow<List<Task>> = dao.getPendingTasksFlow()

    suspend fun insertTask(task: Task) {
        dao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        dao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        dao.deleteTask(task)
    }

    suspend fun deleteTaskById(id: Int) {
        dao.deleteTaskById(id)
    }


    // --- Chats API ---
    val allMessages: Flow<List<ChatMessage>> = dao.getAllMessagesFlow()

    suspend fun insertMessage(message: ChatMessage) {
        dao.insertMessage(message)
    }

    suspend fun clearAllMessages() {
        dao.clearAllMessages()
    }


    // --- Documents API ---
    val allDocuments: Flow<List<RepositoryDocument>> = dao.getAllDocumentsFlow()

    suspend fun insertDocument(document: RepositoryDocument) {
        dao.insertDocument(document)
    }

    suspend fun deleteDocument(document: RepositoryDocument) {
        dao.deleteDocument(document)
    }


    // --- Email Logs API ---
    val allEmailLogs: Flow<List<EmailLog>> = dao.getAllEmailLogsFlow()

    suspend fun insertEmailLog(log: EmailLog) {
        dao.insertEmailLog(log)
    }
}
