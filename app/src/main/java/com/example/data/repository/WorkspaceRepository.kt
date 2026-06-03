package com.example.data.repository

import com.example.data.dao.WorkspaceDao
import com.example.data.model.Task
import com.example.data.model.ChatMessage
import com.example.data.model.DocumentFile
import com.example.data.model.NotificationLog
import com.example.data.model.TimeLog
import kotlinx.coroutines.flow.Flow

class WorkspaceRepository(private val workspaceDao: WorkspaceDao) {

    // Tasks Flows & Operations
    val allTasksFlow: Flow<List<Task>> = workspaceDao.getAllTasksFlow()

    suspend fun getOverduePendingTasks(currentTime: Long): List<Task> {
        return workspaceDao.getOverduePendingTasks(currentTime)
    }

    suspend fun insertTask(task: Task): Long {
        return workspaceDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        workspaceDao.updateTask(task)
    }

    suspend fun deleteTaskById(id: Int) {
        workspaceDao.deleteTaskById(id)
    }


    // Chat Message Flows & Operations
    val allMessagesFlow: Flow<List<ChatMessage>> = workspaceDao.getAllMessagesFlow()

    suspend fun insertMessage(message: ChatMessage): Long {
        return workspaceDao.insertMessage(message)
    }


    // Documents Flows & Operations
    val allDocumentsFlow: Flow<List<DocumentFile>> = workspaceDao.getAllDocumentsFlow()

    suspend fun insertDocument(document: DocumentFile): Long {
        return workspaceDao.insertDocument(document)
    }

    suspend fun deleteDocument(document: DocumentFile) {
        workspaceDao.deleteDocument(document)
    }


    // Notification Log Flows & Operations
    val allNotificationsFlow: Flow<List<NotificationLog>> = workspaceDao.getAllNotificationsFlow()

    suspend fun insertNotificationLog(log: NotificationLog): Long {
        return workspaceDao.insertNotificationLog(log)
    }

    // Time Logs Flows & Operations
    val allTimeLogsFlow: Flow<List<TimeLog>> = workspaceDao.getAllTimeLogsFlow()

    suspend fun insertTimeLog(timeLog: TimeLog): Long {
        return workspaceDao.insertTimeLog(timeLog)
    }

    suspend fun deleteTimeLogById(id: Int) {
        workspaceDao.deleteTimeLogById(id)
    }
}
