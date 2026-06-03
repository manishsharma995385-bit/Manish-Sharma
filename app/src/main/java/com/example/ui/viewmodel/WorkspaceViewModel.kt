package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.DocumentFile
import com.example.data.model.NotificationLog
import com.example.data.model.Task
import com.example.data.model.TimeLog
import com.example.data.repository.WorkspaceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = WorkspaceRepository(db.workspaceDao())

    // UI flows backed by standard Room reactive streams
    val tasksState: StateFlow<List<Task>> = repository.allTasksFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val messagesState: StateFlow<List<ChatMessage>> = repository.allMessagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val documentsState: StateFlow<List<DocumentFile>> = repository.allDocumentsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val notificationsState: StateFlow<List<NotificationLog>> = repository.allNotificationsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val timeLogsState: StateFlow<List<TimeLog>> = repository.allTimeLogsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeTimerTaskId = MutableStateFlow<Int?>(null)
    val activeTimerTaskId: StateFlow<Int?> = _activeTimerTaskId.asStateFlow()

    private val _activeTimerSeconds = MutableStateFlow(0L)
    val activeTimerSeconds: StateFlow<Long> = _activeTimerSeconds.asStateFlow()

    // Local file generation feedback state
    private val _pdfExportState = MutableStateFlow<String?>(null)
    val pdfExportState: StateFlow<String?> = _pdfExportState.asStateFlow()

    // Role state management
    private val _userRoleState = MutableStateFlow("Admin") // "Admin", "Project Manager", "Team Member"
    val userRoleState: StateFlow<String> = _userRoleState.asStateFlow()

    fun setUserRole(role: String) {
        _userRoleState.value = role
    }

    fun getCurrentUserName(): String {
        return when (_userRoleState.value) {
            "Admin" -> "CEO Admin Office"
            "Project Manager" -> "Sarah (Project Manager)"
            else -> "Alex (Lead Dev)"
        }
    }

    init {
        // Pre-populate data if the database is initially empty
        seedDataIfNeeded()
        // Start proactive monitoring check for overdue items to trigger email notifications
        startDeadlineTracker()
        // Start live task timer ticker
        startTimerTicker()
    }

    private fun seedDataIfNeeded() {
        viewModelScope.launch {
            // Check tasks
            delay(500) // Small safety delay to allow DB sync
            repository.allTasksFlow.collect { list ->
                if (list.isEmpty()) {
                    populateInitialWorkspaceData()
                }
            }
        }
    }

    private suspend fun populateInitialWorkspaceData() {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L

        // Tasks
        val parentId = repository.insertTask(
            Task(
                title = "Submit Q2 Financial Draft",
                description = "Outline the operational and R&D budgets for review by executive committee.",
                deadlineMs = now - oneDay, // Overdue task!
                project = "Finance Operations",
                status = "Pending",
                priority = "High",
                assignedTo = "Sarah (Project Manager)",
                emailSent = false
            )
        ).toInt()

        repository.insertTask(
            Task(
                title = "Backend Service Refactoring",
                description = "Complete auth token migration and endpoint optimizations.",
                deadlineMs = now + (3 * oneDay),
                project = "Backend Refit V2",
                status = "Pending",
                priority = "High",
                assignedTo = "Alex (Lead Dev)",
                emailSent = false,
                dependsOnTaskId = parentId
            )
        )
        repository.insertTask(
            Task(
                title = "Social Banner Mockups",
                description = "Draft three distinct color themes for project deployment banners.",
                deadlineMs = now + oneDay,
                project = "Branding Launch",
                status = "Completed",
                priority = "Medium",
                assignedTo = "Lisa (UI/UX Designer)",
                emailSent = false
            )
        )

        // Collaboration Chat
        repository.insertMessage(
            ChatMessage(
                sender = "Sarah (Project Manager)",
                content = "Good morning! Let's check our project dashboard. Some items are approaching their real-time deadlines.",
                timestamp = now - (2 * 60 * 60 * 1000),
                isSystem = false
            )
        )
        repository.insertMessage(
            ChatMessage(
                sender = "Alex (Lead Dev)",
                content = "Morning Sarah! I've uploaded the schema specification as a draft. Working on final security patches next.",
                timestamp = now - (60 * 60 * 1000),
                isSystem = false
            )
        )

        // Seed Documents
        repository.insertDocument(
            DocumentFile(
                name = "Database_Schema_Spec.pdf",
                fileType = "pdf",
                sizeText = "450 KB",
                localUri = null,
                uploadTime = now - (60 * 60 * 1000),
                uploadedBy = "Alex (Lead Dev)"
            )
        )
        repository.insertDocument(
            DocumentFile(
                name = "Q1_Compliance_Audit.xlsx",
                fileType = "xlsx",
                sizeText = "1.2 MB",
                localUri = null,
                uploadTime = now - (4 * 60 * 60 * 1000),
                uploadedBy = "Sarah (Project Manager)"
            )
        )

        // Seed some initial Time Logs
        val oneHour = 3600L
        repository.insertTimeLog(
            TimeLog(
                taskId = 1,
                taskTitle = "Submit Q2 Financial Draft",
                project = "Finance Operations",
                user = "Sarah (Project Manager)",
                timeSpentSeconds = oneHour * 2 + 15 * 60, // 2h 15m
                timestamp = now - (6 * 60 * 60 * 1000)
            )
        )
        repository.insertTimeLog(
            TimeLog(
                taskId = 2,
                taskTitle = "Backend Service Refactoring",
                project = "Backend Refit V2",
                user = "Alex (Lead Dev)",
                timeSpentSeconds = oneHour * 4 + 45 * 60, // 4h 45m
                timestamp = now - (3 * 60 * 60 * 1000)
            )
        )
        repository.insertTimeLog(
            TimeLog(
                taskId = 3,
                taskTitle = "Social Banner Mockups",
                project = "Branding Launch",
                user = "Lisa (UI/UX)",
                timeSpentSeconds = oneHour * 1 + 30 * 60, // 1h 30m
                timestamp = now - (1 * 60 * 60 * 1000)
            )
        )
    }

    private fun startDeadlineTracker() {
        viewModelScope.launch {
            while (true) {
                // Monitor overdue items once every 10 seconds in background
                checkAndTriggerOverdueEmails()
                delay(10000)
            }
        }
    }

    private suspend fun checkAndTriggerOverdueEmails() {
        val now = System.currentTimeMillis()
        val overdueTasks = repository.getOverduePendingTasks(now)
        for (task in overdueTasks) {
            if (!task.emailSent) {
                // Trigger Simulated Automated "Email notification" for high compliance & visibility
                val emailRecipient = when (task.assignedTo) {
                    "Sarah (Project Manager)" -> "sarah.pm@workspace.com"
                    "Alex (Lead Dev)" -> "alex.dev@workspace.com"
                    "Lisa (UI/UX Designer)" -> "lisa.branding@workspace.com"
                    else -> "team.alert@workspace.com"
                }

                val log = NotificationLog(
                    taskTitle = task.title,
                    recipientEmail = emailRecipient,
                    timestamp = now,
                    isOverdueAlert = true,
                    statusMessage = "Automated Email alert successfully dispatched to $emailRecipient"
                )

                // Save notification log
                repository.insertNotificationLog(log)

                // Update task's sent status to avoid double triggering
                repository.updateTask(task.copy(emailSent = true))

                // Insert System log alert in chat for high visibility
                repository.insertMessage(
                    ChatMessage(
                        sender = "Automated Alert Bot",
                        content = "⚠️ DEADLINE BREACH: Team email alert dispatched to $emailRecipient regarding overdue item: '${task.title}'!",
                        timestamp = now,
                        isSystem = true
                    )
                )
            }
        }
    }

    // Interactive operations
    fun addTask(
        title: String,
        description: String,
        deadlineMs: Long,
        project: String,
        priority: String,
        assignedTo: String,
        dependsOnTaskId: Int? = null
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title.ifEmpty { "New Task" },
                description = description.ifEmpty { "No details provided." },
                deadlineMs = if (deadlineMs <= 0) System.currentTimeMillis() + (24 * 60 * 60 * 1000) else deadlineMs,
                project = project.ifEmpty { "General Workspace" },
                status = "Pending",
                priority = priority,
                assignedTo = assignedTo.ifEmpty { "Me" },
                emailSent = false,
                dependsOnTaskId = dependsOnTaskId
            )
            repository.insertTask(task)
            // Trigger check immediately in case new task was created in past as overdue
            checkAndTriggerOverdueEmails()
        }
    }

    fun toggleTaskStatus(task: Task, context: Context) {
        viewModelScope.launch {
            // Role Permission Checking
            if (_userRoleState.value == "Team Member") {
                val currentName = getCurrentUserName()
                // Check if assigned to the active user (partial check for flexibility)
                val matchesAssignee = task.assignedTo.startsWith("Alex", ignoreCase = true) || task.assignedTo.startsWith("Lisa", ignoreCase = true)
                if (task.assignedTo != currentName && !matchesAssignee) {
                    Toast.makeText(
                        context,
                        "🔒 Permission Denied: Team Members can only complete items assigned to them ($currentName).",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
            }

            val isCompleting = task.status != "Completed"
            if (isCompleting && task.dependsOnTaskId != null) {
                val parentTask = tasksState.value.find { it.id == task.dependsOnTaskId }
                if (parentTask != null && parentTask.status != "Completed") {
                    Toast.makeText(
                        context,
                        "⚠️ Blocked! Pre-requisite job '${parentTask.title}' must be Completed first.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
            }

            val newStatus = if (task.status == "Completed") "Pending" else "Completed"
            // Reset email trigger tracker if reopening task as Pending
            val resetEmailTracker = if (newStatus == "Pending") false else task.emailSent
            repository.updateTask(task.copy(status = newStatus, emailSent = resetEmailTracker))
        }
    }

    fun editTask(
        id: Int,
        title: String,
        description: String,
        deadlineMs: Long,
        project: String,
        priority: String,
        assignedTo: String,
        dependsOnTaskId: Int? = null
    ) {
        viewModelScope.launch {
            val existing = tasksState.value.find { it.id == id } ?: return@launch
            val updated = existing.copy(
                title = title.ifEmpty { existing.title },
                description = description.ifEmpty { existing.description },
                deadlineMs = deadlineMs,
                project = project.ifEmpty { existing.project },
                priority = priority,
                assignedTo = assignedTo,
                dependsOnTaskId = dependsOnTaskId
            )
            repository.updateTask(updated)
            // Re-trigger alert evaluations
            checkAndTriggerOverdueEmails()
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            repository.deleteTaskById(id)
        }
    }

    fun submitChatMessage(sender: String, messageText: String) {
        if (messageText.trim().isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.insertMessage(
                ChatMessage(
                    sender = sender.ifEmpty { "You" },
                    content = messageText,
                    timestamp = now,
                    isSystem = false
                )
            )

            // Trigger collaborative simulation bot reply
            delay(1200)
            val replyUser: String
            val replyText: String

            when {
                messageText.lowercase().contains("pdf") || messageText.lowercase().contains("report") -> {
                    replyUser = "Sarah (Project Manager)"
                    replyText = "I see. I've compiled the status report checklist. You can export a certified compliance PDF report straight from the Dashboard!"
                }
                messageText.lowercase().contains("deadline") || messageText.lowercase().contains("overdue") || messageText.lowercase().contains("late") -> {
                    replyUser = "Alex (Lead Dev)"
                    replyText = "Ah, let me check. The automated email tracker monitors task compliance, and will warn the assigned owners instantly."
                }
                messageText.lowercase().contains("document") || messageText.lowercase().contains("file") || messageText.lowercase().contains("upload") -> {
                    replyUser = "Lisa (UI/UX Designer)"
                    replyText = "Awesome, use the 'Upload Document' panel to add any project attachment. It references securely into the Room cache repository."
                }
                else -> {
                    val pool = listOf(
                        Pair("Sarah (Project Manager)", "Sounds good! I'll update the project schedule."),
                        Pair("Alex (Lead Dev)", "Solid copy. Proceeding with the tasks as planned."),
                        Pair("Lisa (UI/UX Designer)", "Looks perfect. Let me know if you need layout adjustments.")
                    )
                    val picked = pool.random()
                    replyUser = picked.first
                    replyText = picked.second
                }
            }

            repository.insertMessage(
                ChatMessage(
                    sender = replyUser,
                    content = replyText,
                    timestamp = System.currentTimeMillis(),
                    isSystem = false
                )
            )
        }
    }

    // Handles document uploading and storing details
    fun addUploadedDocument(name: String, fileType: String, sizeText: String, localUri: String?, user: String) {
        viewModelScope.launch {
            val doc = DocumentFile(
                name = name,
                fileType = fileType,
                sizeText = sizeText,
                localUri = localUri,
                uploadTime = System.currentTimeMillis(),
                uploadedBy = user.ifEmpty { "You" }
            )
            repository.insertDocument(doc)
        }
    }

    fun removeDocument(doc: DocumentFile) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
        }
    }

    // Read details from selected device file URI
    fun handleFileUriSelected(context: Context, uri: Uri, user: String) {
        try {
            var fileName = "imported_file"
            var sizeString = "Unknown Size"
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        fileName = it.getString(nameIdx)
                    }
                    val sizeIdx = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIdx != -1) {
                        val sizeBytes = it.getLong(sizeIdx)
                        sizeString = when {
                            sizeBytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f MB", sizeBytes.toDouble() / (1024 * 1024))
                            sizeBytes >= 1024 -> String.format(Locale.getDefault(), "%.2f KB", sizeBytes.toDouble() / 1024)
                            else -> "$sizeBytes B"
                        }
                    }
                }
            }

            // Extract real extension
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val extension = when {
                fileName.contains(".") -> fileName.substringAfterLast(".").lowercase()
                mimeType.contains("/") -> mimeType.substringAfter("/").lowercase()
                else -> "bin"
            }

            // Save reference to local database securely
            addUploadedDocument(
                name = fileName,
                fileType = extension,
                sizeText = sizeString,
                localUri = uri.toString(),
                user = user
            )

            Toast.makeText(context, "Successfully uploaded: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error uploading document: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Generate real PDF complying with standard graphics.pdf requirements
    fun triggerExportPdfReport(context: Context) {
        _pdfExportState.value = "Generating Report..."
        viewModelScope.launch {
            try {
                val currentTasks = tasksState.value
                val docs = documentsState.value
                val chats = messagesState.value

                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 width x height in postscript points
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Formatting brushes
                val paintHeader = Paint().apply {
                    color = Color.rgb(33, 33, 33)
                    textSize = 22f
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                val paintSecHeader = Paint().apply {
                    color = Color.rgb(63, 81, 181)
                    textSize = 14f
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                val paintBody = Paint().apply {
                    color = Color.rgb(50, 50, 50)
                    textSize = 10f
                    isAntiAlias = true
                }

                val paintMuted = Paint().apply {
                    color = Color.rgb(120, 120, 120)
                    textSize = 10f
                    isAntiAlias = true
                }

                val paintGridLine = Paint().apply {
                    color = Color.rgb(220, 220, 220)
                    strokeWidth = 1f
                }

                val paintStatusBoxCompleted = Paint().apply {
                    color = Color.rgb(200, 230, 201) // light green
                }

                val paintStatusBoxPending = Paint().apply {
                    color = Color.rgb(255, 236, 179) // light yellow
                }

                var y = 50f

                // Draw header block
                canvas.drawText("Project Workspace Compliance Report", 50f, y, paintHeader)
                y += 20f
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                canvas.drawText("System Audit Trail: ${sdf.format(Date())}", 50f, y, paintMuted)
                y += 15f
                canvas.drawLine(50f, y, 545f, y, paintGridLine)
                y += 25f

                // Summary Statistics section
                canvas.drawText("I. OFFICE PERFORMANCE SUMMARY", 50f, y, paintSecHeader)
                y += 20f

                val total = currentTasks.size
                val completed = currentTasks.count { it.status == "Completed" }
                val pending = currentTasks.count { it.status == "Pending" }
                val overdue = currentTasks.count { it.status == "Pending" && it.deadlineMs < System.currentTimeMillis() }

                canvas.drawText("Active Task Inventory: $total Items", 60f, y, paintBody)
                y += 15f
                canvas.drawText("Completed Deliverables: $completed", 60f, y, paintBody)
                canvas.drawText("Open/Pending Pipeline: $pending", 300f, y, paintBody)
                y += 15f
                canvas.drawText("Breached Milestones (Overdue Tracker Alerts): $overdue Tasks", 60f, y, paintBody)
                y += 20f
                canvas.drawLine(50f, y, 545f, y, paintGridLine)
                y += 25f

                // Task grid section
                canvas.drawText("II. REAL-TIME DEADLINE & COMPLIANCE MATRIX", 50f, y, paintSecHeader)
                y += 20f

                // Table Header row
                canvas.drawText("Deliverable Name", 50f, y, paintMuted)
                canvas.drawText("Assigned Member", 220f, y, paintMuted)
                canvas.drawText("Target Due Date", 380f, y, paintMuted)
                canvas.drawText("Status", 480f, y, paintMuted)
                y += 5f
                canvas.drawLine(50f, y, 545f, y, paintGridLine)
                y += 18f

                val sdfDeadline = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                for (task in currentTasks.take(20)) { // Fit up to 20 lines elegantly on page 1
                    if (y > 760f) break

                    val shortTitle = if (task.title.length > 25) task.title.take(22) + "..." else task.title
                    canvas.drawText(shortTitle, 50f, y, paintBody)

                    val shortAssignee = if (task.assignedTo.length > 20) task.assignedTo.take(17) + "..." else task.assignedTo
                    canvas.drawText(shortAssignee, 220f, y, paintBody)

                    val deadlineStr = sdfDeadline.format(Date(task.deadlineMs))
                    canvas.drawText(deadlineStr, 380f, y, paintBody)

                    // Draw a subtle badge color backing
                    if (task.status == "Completed") {
                        canvas.drawRect(478f, y - 9f, 535f, y + 3f, paintStatusBoxCompleted)
                        canvas.drawText("COMPLETED", 482f, y, paintBody)
                    } else {
                        canvas.drawRect(478f, y - 9f, 535f, y + 3f, paintStatusBoxPending)
                        val isOverdue = task.deadlineMs < System.currentTimeMillis()
                        if (isOverdue) {
                            val paintAlert = Paint().apply {
                                color = Color.RED
                                textSize = 9f
                                isFakeBoldText = true
                            }
                            canvas.drawText("BREACHED", 482f, y, paintAlert)
                        } else {
                            canvas.drawText("PENDING", 482f, y, paintBody)
                        }
                    }

                    y += 18f
                }

                // Document and systems section
                if (y < 700f) {
                    y += 10f
                    canvas.drawLine(50f, y, 545f, y, paintGridLine)
                    y += 15f
                    canvas.drawText("III. STORAGE & COLLABORATION STATUS", 50f, y, paintSecHeader)
                    y += 12f
                    canvas.drawText("Documents Registered: ${docs.size} securely indexed project resources.", 60f, y, paintBody)
                    y += 12f
                    canvas.drawText("Latest System Broadcast: \"Slack/Teams integration mock dispatch live.\"", 60f, y, paintBody)
                }

                // Timesheet summaries section
                val timeLogEntries = timeLogsState.value
                val userLogs = timeLogEntries.groupBy { it.user }.mapValues { entry ->
                    entry.value.sumOf { it.timeSpentSeconds }
                }
                if (y < 670f && userLogs.isNotEmpty()) {
                    y += 15f
                    canvas.drawLine(50f, y, 545f, y, paintGridLine)
                    y += 15f
                    canvas.drawText("IV. WORKSPACE TIMESHEET ANALYTICS BY MEMBER", 50f, y, paintSecHeader)
                    y += 14f
                    userLogs.forEach { (user, seconds) ->
                        if (y < 765f) {
                            val hrs = seconds / 3600
                            val mins = (seconds % 3600) / 60
                            canvas.drawText("• Team Member [$user]: ${hrs}h ${mins}m logged across assignments.", 60f, y, paintBody)
                            y += 13f
                        }
                    }
                }

                y += 20f
                if (y > 800f) y = 800f
                canvas.drawLine(50f, y, 545f, y, paintGridLine)
                y += 15f
                canvas.drawText("CONFIDENTIAL STATUS COMPLIANCE REPORT - GENERATED SECURELY VIA ROOM PERSISTENCE ENGINE.", 60f, y, paintMuted)

                pdfDocument.finishPage(page)

                // Write file to application cache directory
                val rootDir = context.cacheDir
                val file = File(rootDir, "Workspace_Compliance_Report.pdf")
                val fos = FileOutputStream(file)
                pdfDocument.writeTo(fos)
                pdfDocument.close()
                fos.close()

                _pdfExportState.value = file.absolutePath

                // Raise success toast and launch direct system view intent
                Toast.makeText(context, "PDF Report Exported Successfully!", Toast.LENGTH_SHORT).show()
                openExportedPdf(context, file)
            } catch (e: Exception) {
                _pdfExportState.value = null
                Toast.makeText(context, "PDF compilation failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openExportedPdf(context: Context, file: File) {
        try {
            // Expose file safely via FileProvider
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Create selector chooser
            val chooser = Intent.createChooser(intent, "Open Compliance PDF Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch PDF viewer. File saved under Cache directory.", Toast.LENGTH_LONG).show()
        }
    }

    fun clearPdfState() {
        _pdfExportState.value = null
    }

    private fun startTimerTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_activeTimerTaskId.value != null) {
                    _activeTimerSeconds.value += 1
                }
            }
        }
    }

    fun startTimerForTask(taskId: Int) {
        _activeTimerTaskId.value = taskId
        _activeTimerSeconds.value = 0L
    }

    fun pauseTimer() {
        _activeTimerTaskId.value = null
    }

    fun resumeTimerForTask(taskId: Int) {
        _activeTimerTaskId.value = taskId
    }

    fun clearTimer() {
        _activeTimerTaskId.value = null
        _activeTimerSeconds.value = 0L
    }

    fun submitTimeLog(taskId: Int, seconds: Long, userName: String, taskTitle: String, project: String) {
        viewModelScope.launch {
            if (seconds <= 0) return@launch
            val log = TimeLog(
                taskId = taskId,
                taskTitle = taskTitle,
                project = project,
                user = userName,
                timeSpentSeconds = seconds,
                timestamp = System.currentTimeMillis()
            )
            repository.insertTimeLog(log)

            // Post system message in collaboration chat for visual realism and status reports
            val hourMinStr = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
            repository.insertMessage(
                ChatMessage(
                    sender = "System Logger",
                    content = "⏱️ Time Log Recorded: $userName spent $hourMinStr on [${project}] Task '${taskTitle}'.",
                    timestamp = System.currentTimeMillis(),
                    isSystem = true
                )
            )

            if (_activeTimerTaskId.value == taskId) {
                clearTimer()
            }
        }
    }

    fun deleteTimeLog(logId: Int) {
        viewModelScope.launch {
            repository.deleteTimeLogById(logId)
        }
    }
}
