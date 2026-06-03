package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.data.model.DocumentFile
import com.example.data.model.NotificationLog
import com.example.data.model.Task
import com.example.data.model.TimeLog
import com.example.ui.viewmodel.WorkspaceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // State Collection
    val tasks by viewModel.tasksState.collectAsStateWithLifecycle()
    val messages by viewModel.messagesState.collectAsStateWithLifecycle()
    val documents by viewModel.documentsState.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationsState.collectAsStateWithLifecycle()
    val activeRole by viewModel.userRoleState.collectAsStateWithLifecycle()
    val pdfExportPath by viewModel.pdfExportState.collectAsStateWithLifecycle()
    val timeLogs by viewModel.timeLogsState.collectAsStateWithLifecycle()
    val activeTimerTaskId by viewModel.activeTimerTaskId.collectAsStateWithLifecycle()
    val activeTimerSeconds by viewModel.activeTimerSeconds.collectAsStateWithLifecycle()

    // Dialog state
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var selectedTaskToEdit by remember { mutableStateOf<Task?>(null) }
    var showEditTaskDialog by remember { mutableStateOf(false) }

    // Calendar state
    var calendarYear by remember { mutableStateOf(2026) }
    var calendarMonth by remember { mutableStateOf(5) } // June (0-indexed Calendar in Java: 5 = June)
    var selectedDayInMonth by remember { mutableStateOf(3) } // Default to current model date June 3

    // Tab tracking: "dashboard" vs "logs"
    var selectedTabState by remember { mutableStateOf("dashboard") }

    // Global Search & Filtering States
    var searchQuery by remember { mutableStateOf("") }
    var filterProject by remember { mutableStateOf("All") }
    var filterAssignee by remember { mutableStateOf("All") }
    var filterPriority by remember { mutableStateOf("All") }
    var filterDateRange by remember { mutableStateOf("All") } // "All", "Selected Day", "Overdue", "Within 7 Days"
    var showAdvancedFilters by remember { mutableStateOf(false) }

    val isFilterActive = searchQuery.isNotEmpty() ||
                         filterProject != "All" ||
                         filterAssignee != "All" ||
                         filterPriority != "All" ||
                         filterDateRange != "All"

    val filteredTasks = remember(
        tasks,
        searchQuery,
        filterProject,
        filterAssignee,
        filterPriority,
        filterDateRange,
        calendarYear,
        calendarMonth,
        selectedDayInMonth
    ) {
        tasks.filter { task ->
            val matchesQuery = if (searchQuery.isBlank()) true else {
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true) ||
                task.project.contains(searchQuery, ignoreCase = true) ||
                task.assignedTo.contains(searchQuery, ignoreCase = true) ||
                task.priority.contains(searchQuery, ignoreCase = true)
            }

            val matchesProject = if (filterProject == "All") true else {
                task.project.equals(filterProject, ignoreCase = true)
            }

            val matchesAssignee = if (filterAssignee == "All") true else {
                task.assignedTo.contains(filterAssignee, ignoreCase = true) ||
                (filterAssignee.contains("Alex") && task.assignedTo.contains("Alex", ignoreCase = true)) ||
                (filterAssignee.contains("Lisa") && task.assignedTo.contains("Lisa", ignoreCase = true))
            }

            val matchesPriority = if (filterPriority == "All") true else {
                task.priority.equals(filterPriority, ignoreCase = true)
            }

            val matchesDate = when (filterDateRange) {
                "Selected Day" -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = task.deadlineMs }
                    cal.get(Calendar.YEAR) == calendarYear &&
                    cal.get(Calendar.MONTH) == calendarMonth &&
                    cal.get(Calendar.DAY_OF_MONTH) == selectedDayInMonth
                }
                "Overdue" -> {
                    task.status != "Completed" && task.deadlineMs < System.currentTimeMillis()
                }
                "Within 7 Days" -> {
                    val msDiff = task.deadlineMs - System.currentTimeMillis()
                    msDiff in 0..(7L * 24 * 60 * 60 * 1000)
                }
                else -> true
            }

            matchesQuery && matchesProject && matchesAssignee && matchesPriority && matchesDate
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupervisorAccount,
                            contentDescription = "Workspace Header Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Project Workspace",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Collab Hub • Persistent Ledger Engine",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Export Button
                    Button(
                        onClick = { viewModel.triggerExportPdfReport(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .testTag("export_pdf_button")
                            .padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "PDF PDF",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF Report", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            // Role Quick Switching and Navigation Summary indicators
            Surface(
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Current Permission Shell:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Admin", "Project Manager", "Team Member").forEach { role ->
                            val isSelected = activeRole == role
                            Button(
                                onClick = {
                                    viewModel.setUserRole(role)
                                    Toast.makeText(context, "Permissions shifted to $role", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("role_pill_$role")
                                    .height(38.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                val icon = when (role) {
                                    "Admin" -> Icons.Default.AdminPanelSettings
                                    "Project Manager" -> Icons.Default.AssignmentInd
                                    else -> Icons.Default.WorkOutline
                                }
                                Icon(icon, contentDescription = role, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(role, fontSize = 10.sp, overflow = TextOverflow.Ellipsis, maxLines = 1)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Helpful notice explaining privileges based on role selection
                    Text(
                        text = when (activeRole) {
                            "Admin" -> "🔓 Full Administration. All screens, creation/deletion, emails & audits unlocked."
                            "Project Manager" -> "📋 Management Authority. Can create tasks and attach files; cannot delete records."
                            else -> "🔒 Team Workspace. Can participate in chat & toggle only tasks assigned to Lisa or Alex."
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // View tabs: Dashboard vs Time Tracker vs Systems Audit Logs
            TabRow(
                selectedTabIndex = when (selectedTabState) {
                    "dashboard" -> 0
                    "timetracker" -> 1
                    else -> 2
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabState == "dashboard",
                    onClick = { selectedTabState = "dashboard" },
                    text = { Text("Workspace Central", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") }
                )
                Tab(
                    selected = selectedTabState == "timetracker",
                    onClick = { selectedTabState = "timetracker" },
                    text = { Text("Time Tracker", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Timer Component") }
                )
                Tab(
                    selected = selectedTabState == "logs",
                    onClick = { selectedTabState = "logs" },
                    text = { Text("Audit Trail Logs (${notifications.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Email, contentDescription = "Logs") }
                )
            }

            AnimatedContent(
                targetState = selectedTabState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                modifier = Modifier.weight(1f)
            ) { currentTab ->
                if (currentTab == "dashboard") {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(10.dp)) }

                        // PDF Status Bar if generated
                        if (pdfExportPath != null) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Description,
                                            contentDescription = "PDF Icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Report Export Ready!",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                "Saved securely to local workspace memory.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.8f)
                                            )
                                        }
                                        IconButton(onClick = { viewModel.clearPdfState() }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close PDF prompt")
                                        }
                                    }
                                }
                            }
                        }

                        // Global Search and Filtering Bar
                        item {
                            GlobalSearchFilterBar(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                filterProject = filterProject,
                                onFilterProjectChange = { filterProject = it },
                                filterAssignee = filterAssignee,
                                onFilterAssigneeChange = { filterAssignee = it },
                                filterPriority = filterPriority,
                                onFilterPriorityChange = { filterPriority = it },
                                filterDateRange = filterDateRange,
                                onFilterDateRangeChange = { filterDateRange = it },
                                showAdvancedFilters = showAdvancedFilters,
                                onToggleAdvancedFilters = { showAdvancedFilters = !showAdvancedFilters },
                                tasks = tasks
                            )
                        }

                        // Executive Performance Scorecards
                        item {
                            ExecutiveMetricsSection(tasks = tasks)
                        }

                        // Calendar graphical grid view (Requested Feature 1)
                        item {
                            CalendarGridView(
                                year = calendarYear,
                                month = calendarMonth,
                                selectedDay = selectedDayInMonth,
                                tasks = tasks,
                                onDayClick = { day ->
                                    selectedDayInMonth = day
                                },
                                onPrevMonth = {
                                    if (calendarMonth == 0) {
                                        calendarMonth = 11
                                        calendarYear -= 1
                                    } else {
                                        calendarMonth -= 1
                                    }
                                    selectedDayInMonth = 1
                                },
                                onNextMonth = {
                                    if (calendarMonth == 11) {
                                        calendarMonth = 0
                                        calendarYear += 1
                                    } else {
                                        calendarMonth += 1
                                    }
                                    selectedDayInMonth = 1
                                }
                            )
                        }

                        // Selected Day Tasks list & Quick actions block
                        item {
                            TasksScheduledSection(
                                year = calendarYear,
                                month = calendarMonth,
                                day = selectedDayInMonth,
                                tasks = tasks,
                                activeRole = activeRole,
                                onToggleStatus = { task ->
                                    viewModel.toggleTaskStatus(task, context)
                                },
                                onDeleteTask = { taskId ->
                                    if (activeRole == "Team Member") {
                                        Toast.makeText(context, "⚠️ Action Blocked: Only Admin & Managers can delete tasks.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.deleteTask(taskId)
                                    }
                                },
                                onEditTask = { task ->
                                    if (activeRole == "Team Member") {
                                        Toast.makeText(context, "⚠️ Action Blocked: Team members cannot edit tasks.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        selectedTaskToEdit = task
                                        showEditTaskDialog = true
                                    }
                                },
                                onAddNewTaskClick = {
                                    if (activeRole == "Team Member") {
                                        Toast.makeText(context, "⚠️ Perms Guard: Team members cannot append deliverables.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showAddTaskDialog = true
                                    }
                                },
                                isFilterActive = isFilterActive,
                                filteredTasks = filteredTasks,
                                activeTimerTaskId = activeTimerTaskId,
                                onStartTimer = { taskId -> viewModel.startTimerForTask(taskId) },
                                onPauseTimer = { viewModel.pauseTimer() },
                                onClearFilters = {
                                    searchQuery = ""
                                    filterProject = "All"
                                    filterAssignee = "All"
                                    filterPriority = "All"
                                    filterDateRange = "All"
                                }
                            )
                        }

                        // Secure Document Repository with select mechanisms
                        item {
                            DocumentRepositorySection(
                                documents = documents,
                                currentRole = activeRole,
                                onUploadSelected = { uri ->
                                    viewModel.handleFileUriSelected(context, uri, viewModel.getCurrentUserName())
                                },
                                onDeleteDocument = { doc ->
                                    if (activeRole == "Team Member") {
                                        Toast.makeText(context, "⚠️ Access Denied: Team Members cannot drop records.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.removeDocument(doc)
                                    }
                                }
                            )
                        }

                        // Collaborative live team chat module section (Requested Feature 2 component)
                        item {
                            LiveTeamChatSection(
                                messages = messages,
                                activeRole = activeRole,
                                onSubmitMessage = { content ->
                                    viewModel.submitChatMessage(viewModel.getCurrentUserName(), content)
                                }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(20.dp)) }
                    }
                } else if (currentTab == "timetracker") {
                    TimeTrackerSection(
                        timeLogs = timeLogs,
                        tasks = tasks,
                        viewModel = viewModel,
                        currentUserName = viewModel.getCurrentUserName()
                    )
                } else {
                    // AUDIT & EMAIL LOG TRAIL TAB
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Audit Details",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Automated Compliance Ledger: Email notification alerts trigger automatically when any task passes its real-time deadline.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                "Systems Outbound Email Queue Alerts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        if (notifications.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.MailOutline,
                                            contentDescription = "Empty log",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "No automated overdue emails logs emitted yet.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(notifications) { log ->
                                NotificationLogCard(log = log)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Task Dialog (Includes dependencies lookup!)
    if (showAddTaskDialog) {
        AddTaskDialog(
            tasksList = tasks,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, dueMs, proj, priority, assignee, parentId ->
                viewModel.addTask(title, desc, dueMs, proj, priority, assignee, parentId)
                showAddTaskDialog = false
            }
        )
    }

    // Edit Task Dialog
    if (showEditTaskDialog && selectedTaskToEdit != null) {
        EditTaskDialog(
            task = selectedTaskToEdit!!,
            tasksList = tasks,
            onDismiss = {
                showEditTaskDialog = false
                selectedTaskToEdit = null
            },
            onConfirm = { title, desc, dueMs, proj, priority, assignee, parentId ->
                viewModel.editTask(
                    id = selectedTaskToEdit!!.id,
                    title = title,
                    description = desc,
                    deadlineMs = dueMs,
                    project = proj,
                    priority = priority,
                    assignedTo = assignee,
                    dependsOnTaskId = parentId
                )
                showEditTaskDialog = false
                selectedTaskToEdit = null
            }
        )
    }
}

// ==================== METRICS COMPONENT ====================
@Composable
fun ExecutiveMetricsSection(tasks: List<Task>) {
    val totalTasks = tasks.size
    val completedCount = tasks.count { it.status == "Completed" }
    val progressPercent = if (totalTasks > 0) (completedCount * 100) / totalTasks else 0
    val overdueCount = tasks.count {
        it.status == "Pending" && it.deadlineMs < System.currentTimeMillis()
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "OFFICE METRICS DASHBOARD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Milestones Done", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$completedCount/$totalTasks",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Card 2
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (overdueCount > 0) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.tertiaryContainer
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Overdue Pending", fontSize = 10.sp, color = if (overdueCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "$overdueCount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (overdueCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            if (overdueCount > 0) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Breached",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Card 3
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Task Completion Rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$progressPercent% Completed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ==================== CALENDAR GRAPHICAL COMPONENT ====================
@Composable
fun CalendarGridView(
    year: Int,
    month: Int,
    selectedDay: Int,
    tasks: List<Task>,
    onDayClick: (Int) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Compute basic month limits using Java Calendar API
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 2 = Mon ... 7 = Sat
    val maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val weeksLabel = listOf("S", "M", "T", "W", "T", "F", "S")

    // Compile maps of day in month -> task flags
    // Compare times using raw day calculations to highlight dates
    val dayWithTasksMap = remember(tasks, year, month) {
        val map = mutableMapOf<Int, MutableList<Task>>()
        val currentSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        tasks.forEach { t ->
            val tCal = Calendar.getInstance().apply { timeInMillis = t.deadlineMs }
            if (tCal.get(Calendar.YEAR) == year && tCal.get(Calendar.MONTH) == month) {
                val d = tCal.get(Calendar.DAY_OF_MONTH)
                if (map[d] == null) map[d] = mutableListOf()
                map[d]?.add(t)
            }
        }
        map
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workspace_calendar_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Calendar Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "CALENDAR DEADLINE MATRIX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${monthNames[month]} $year",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPrevMonth,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Prev month", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next month", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Week headlines
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weeksLabel.forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic grid construction
            // Determine days grids layout
            val daysCountInMatrix = 42 // 6 weeks maximum to ensure square aesthetics
            val blankDaysBefore = startDayOfWeek - 1

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (weekIndex in 0 until 6) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (dayIndex in 0 until 7) {
                            val absoluteIndex = (weekIndex * 7) + dayIndex
                            val calculatedDay = absoluteIndex - blankDaysBefore + 1

                            if (calculatedDay in 1..maxDaysInMonth) {
                                val dayTasks = dayWithTasksMap[calculatedDay] ?: emptyList()
                                val isSelected = selectedDay == calculatedDay

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                dayTasks.any { it.status == "Completed" } -> MaterialTheme.colorScheme.surfaceVariant
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onDayClick(calculatedDay) }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$calculatedDay",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )

                                        // Deadline Dots
                                        if (dayTasks.isNotEmpty() && !isSelected) {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 1.dp)
                                            ) {
                                                val hasHigh = dayTasks.any { it.priority == "High" && it.status == "Pending" }
                                                val hasMed = dayTasks.any { it.priority == "Medium" && it.status == "Pending" }
                                                val hasAllCompleted = dayTasks.all { it.status == "Completed" }

                                                val dotColor = when {
                                                    hasAllCompleted -> Color.Green
                                                    hasHigh -> Color.Red
                                                    hasMed -> Color.Yellow
                                                    else -> Color.Blue
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Spacer for out-of-month dates
                                Spacer(modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calendar legend indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Legend: ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red))
                    Text("Overdue/High Alert", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Yellow))
                    Text("Medium Deadline", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Green))
                    Text("Done", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ==================== SELECTION LIST COMPONENT ====================
@Composable
fun TasksScheduledSection(
    year: Int,
    month: Int,
    day: Int,
    tasks: List<Task>,
    activeRole: String,
    onToggleStatus: (Task) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onEditTask: (Task) -> Unit,
    onAddNewTaskClick: () -> Unit,
    isFilterActive: Boolean = false,
    filteredTasks: List<Task> = emptyList(),
    onClearFilters: () -> Unit = {},
    activeTimerTaskId: Int? = null,
    onStartTimer: (Int) -> Unit = {},
    onPauseTimer: () -> Unit = {}
) {
    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    // Filter tasks whose target deadline fits target selected day (or use filtered list if searching)
    val targetedTasks = remember(tasks, year, month, day, isFilterActive, filteredTasks) {
        if (isFilterActive) {
            filteredTasks
        } else {
            tasks.filter { t ->
                val cal = Calendar.getInstance().apply { timeInMillis = t.deadlineMs }
                cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month && cal.get(Calendar.DAY_OF_MONTH) == day
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isFilterActive) Icons.Default.Search else Icons.Default.DateRange,
                    contentDescription = "Date Tasks",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = if (isFilterActive) "🔍 Search Results (${targetedTasks.size} Items)"
                               else "Deadlines on ${monthNames[month]} $day, $year (${targetedTasks.size} Items)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (isFilterActive) {
                        Text(
                            text = "Global search matches across all dates",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isFilterActive) {
                    TextButton(
                        onClick = onClearFilters,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("reset_search_section_btn")
                    ) {
                        Text("Reset", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }

                // Quick add button
                ElevatedButton(
                    onClick = onAddNewTaskClick,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .testTag("add_task_trigger")
                        .height(30.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task Icon", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (targetedTasks.isEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFilterActive) "No tasks found matching your search. Click 'Reset' to view day-based deliverables."
                               else "No tasks scheduled on this day. Press 'Add Task' to register project items.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            targetedTasks.forEach { task ->
                TaskDeadlineCard(
                    task = task,
                    allTasks = tasks,
                    activeRole = activeRole,
                    onToggleStatus = { onToggleStatus(task) },
                    onDelete = { onDeleteTask(task.id) },
                    onEdit = { onEditTask(task) },
                    activeTimerTaskId = activeTimerTaskId,
                    onStartTimer = { onStartTimer(task.id) },
                    onPauseTimer = onPauseTimer
                )
            }
        }
    }
}

// Single task list item supporting dependency check description!
@Composable
fun TaskDeadlineCard(
    task: Task,
    allTasks: List<Task>,
    activeRole: String,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    activeTimerTaskId: Int? = null,
    onStartTimer: () -> Unit = {},
    onPauseTimer: () -> Unit = {}
) {
    // Look up prerequisite description if any
    val dependentOnTaskName = remember(task.dependsOnTaskId, allTasks) {
        if (task.dependsOnTaskId != null) {
            allTasks.find { it.id == task.dependsOnTaskId }?.title
        } else null
    }

    val isCompleted = task.status == "Completed"
    val isOverdue = !isCompleted && task.deadlineMs < System.currentTimeMillis()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                             else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isCompleted -> MaterialTheme.colorScheme.outlineVariant.copy(0.5f)
                task.priority == "High" -> Color.Red.copy(0.4f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Interactive Status Checkbox
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onToggleStatus() },
                    modifier = Modifier
                        .size(20.dp)
                        .testTag("task_complete_check_${task.id}")
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(0.5f) else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Project & Assignee Badges
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(task.project, fontSize = 9.sp) },
                            modifier = Modifier.height(24.dp)
                        )
                        SuggestionChip(
                            onClick = {},
                            label = { Text("🧑 ${task.assignedTo}", fontSize = 9.sp) },
                            modifier = Modifier.height(24.dp)
                        )
                        val isTimerActiveForThis = activeTimerTaskId == task.id
                        if (isTimerActiveForThis) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Timer running indicator",
                                        tint = Color.Red,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Tracking Work...",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red
                                    )
                                }
                            }
                        }
                        if (task.priority == "High") {
                            Surface(
                                color = Color.Red.copy(0.1f),
                                contentColor = Color.Red,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "High Alert",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Pre-requisites Labeling components (Requested Feature 2)
                    if (dependentOnTaskName != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(0.3f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked dependency symbol",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Depends on: $dependentOnTaskName",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Task Timer Play/Pause
                    val isTimerActive = activeTimerTaskId == task.id
                    IconButton(
                        onClick = {
                            if (isTimerActive) onPauseTimer() else onStartTimer()
                        },
                        modifier = Modifier.size(32.dp).testTag("task_timer_btn_${task.id}")
                    ) {
                        Icon(
                            imageVector = if (isTimerActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isTimerActive) "Pause Work Timer" else "Start Work Timer",
                            tint = if (isTimerActive) Color.Red else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Edit handle (accessible to Admin & Project Managers)
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp).testTag("task_edit_btn_${task.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit task Details",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete handle
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("task_delete_btn_${task.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Drop task",
                            tint = MaterialTheme.colorScheme.error.copy(0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==================== DOCUMENT REPOSITORY COMPONENT ====================
@Composable
fun DocumentRepositorySection(
    documents: List<DocumentFile>,
    currentRole: String,
    onUploadSelected: (android.net.Uri) -> Unit,
    onDeleteDocument: (DocumentFile) -> Unit
) {
    // Device system file uploader intent launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onUploadSelected(uri)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("repository_documents_block"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = "Doc Repo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "COLLABORATIVE FILES REPOSITORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }

                // Upload trigger button
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("upload_file_button").height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Cloud upload", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload File", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Files listing
            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No documents registered in the workspace registry.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                documents.forEach { doc ->
                    DocumentItemRow(
                        doc = doc,
                        onDeleteClick = { onDeleteDocument(doc) }
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentItemRow(
    doc: DocumentFile,
    onDeleteClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val uploadDateText = formatter.format(Date(doc.uploadTime))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.4f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (doc.fileType.lowercase()) {
                "pdf" -> Icons.Default.PictureAsPdf
                "xlsx", "csv" -> Icons.Default.GridOn
                else -> Icons.Default.Description
            },
            contentDescription = "File indicator icon",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillPadding()
            ) {
                Text(text = "Size: ${doc.sizeText}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "By: ${doc.uploadedBy}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Doc", tint = MaterialTheme.colorScheme.error.copy(0.7f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun Modifier.fillPadding(): Modifier = this.padding(top = 1.dp)

// ==================== COLLABORATIVE TEAM CHAT WORKPLACE ====================
@Composable
fun LiveTeamChatSection(
    messages: List<ChatMessage>,
    activeRole: String,
    onSubmitMessage: (String) -> Unit
) {
    var textInputState by remember { mutableStateOf("") }
    val chatScrollState = rememberLazyListState()

    // Scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            chatScrollState.animateScrollToItem(messages.size - 1)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("live_team_chat_matrix"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Chat, contentDescription = "Workspace Chat", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "CONCURRENT TEAM CHATROOM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chat Scroll window
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(
                    state = chatScrollState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages) { msg ->
                        ChatBubbleCard(message = msg)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Input dispatch Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textInputState,
                    onValueChange = { textInputState = it },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    placeholder = { Text("Ask Sarah or Alex about deadlines...", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                IconButton(
                    onClick = {
                        if (textInputState.trim().isNotEmpty()) {
                            onSubmitMessage(textInputState)
                            textInputState = ""
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("send_chat_msg_btn")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun ChatBubbleCard(message: ChatMessage) {
    val isSystem = message.isSystem

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        contentAlignment = if (isSystem) Alignment.Center else Alignment.TopStart
    ) {
        if (isSystem) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(0.3f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = message.content,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (message.sender.contains("You") || message.sender.contains("CEO"))
                            MaterialTheme.colorScheme.primaryContainer.copy(0.4f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                    )
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = message.sender,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                    Text(
                        text = timeString,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = message.content, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// ==================== SYSTEM AUDIT logs CARD ====================
@Composable
fun NotificationLogCard(log: NotificationLog) {
    val dateText = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth().testTag("system_log_${log.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = "Alert",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    "AUDIT DETECTED DEADLINE DELAY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = dateText,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Target Deliverable: '${log.taskTitle}'",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Status: ${log.statusMessage}",
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==================== CREATE TASK DIALOG WITH DEPENDENCY SPINNER ====================
@Composable
fun AddTaskDialog(
    tasksList: List<Task>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        dueDateMs: Long,
        project: String,
        priority: String,
        assignee: String,
        dependsOnId: Int?
    ) -> Unit
) {
    var titleState by remember { mutableStateOf("") }
    var descState by remember { mutableStateOf("") }
    var selectedProject by remember { mutableStateOf("Operations") }
    var selectedPriority by remember { mutableStateOf("Medium") }
    var selectedAssignee by remember { mutableStateOf("Alex (Lead Dev)") }

    var chosenDaysFromNow by remember { mutableStateOf("3") } // Offset picker representation

    // Task dependency properties
    var selectedDependencyTaskId by remember { mutableStateOf<Int?>(null) }
    var dropDownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .testTag("add_task_dialog"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "REGISTER WORKSPACE DELIVERABLE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                // Input fields
                OutlinedTextField(
                    value = titleState,
                    onValueChange = { titleState = it },
                    label = { Text("Task Headline", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("add_task_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = descState,
                    onValueChange = { descState = it },
                    label = { Text("Task Description / Specifications", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("add_task_desc_input"),
                    maxLines = 2
                )

                // Split metrics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = chosenDaysFromNow,
                        onValueChange = { chosenDaysFromNow = it },
                        label = { Text("Days from Now", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_task_days_input"),
                        singleLine = true
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Task Alert Priority", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("High", "Medium").forEach { p ->
                                val active = selectedPriority == p
                                Button(
                                    onClick = { selectedPriority = p },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(p, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }

                // Project and Assignee Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Category Group", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("Operations", "Branding").forEach { proj ->
                                val active = selectedProject == proj
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedProject = proj }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(proj, fontSize = 9.sp, color = if (active) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Assignee Target", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("Alex (Lead Dev)", "Lisa (UI/UX)").forEach { target ->
                                val active = selectedAssignee == target
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedAssignee = target }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(target.take(9), fontSize = 9.sp, color = if (active) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // TASK DEPENDENCY linking dropdown selection spinner (Requested Feature 2 configuration!)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "LINK PRE-REQUISITE DEPENDENCY (Optional)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { dropDownExpanded = !dropDownExpanded }
                            .padding(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val activeDependencyName = if (selectedDependencyTaskId != null) {
                                tasksList.find { it.id == selectedDependencyTaskId }?.title ?: "Select Task"
                            } else {
                                "None (No Preceding Dependencies)"
                            }

                            Text(
                                text = activeDependencyName,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (dropDownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown icon",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = dropDownExpanded,
                            onDismissRequest = { dropDownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("(No Parent Task Dependency)", fontSize = 11.sp) },
                                onClick = {
                                    selectedDependencyTaskId = null
                                    dropDownExpanded = false
                                }
                            )
                            tasksList.filter { it.status != "Completed" }.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text("ID ${t.id}: ${t.title} [Pending]", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        selectedDependencyTaskId = t.id
                                        dropDownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Confirm Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val days = chosenDaysFromNow.toIntOrNull() ?: 1
                            val deadlineMs = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)
                            onConfirm(
                                titleState,
                                descState,
                                deadlineMs,
                                selectedProject,
                                selectedPriority,
                                selectedAssignee,
                                selectedDependencyTaskId
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_confirm_add_task_btn")
                    ) {
                        Text("Add Chores")
                    }
                }
            }
        }
    }
}

@Composable
fun EditTaskDialog(
    task: Task,
    tasksList: List<Task>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        dueDateMs: Long,
        project: String,
        priority: String,
        assignee: String,
        dependsOnId: Int?
    ) -> Unit
) {
    var titleState by remember { mutableStateOf(task.title) }
    var descState by remember { mutableStateOf(task.description) }
    var selectedProject by remember { mutableStateOf(task.project) }
    var selectedPriority by remember { mutableStateOf(task.priority) }
    var selectedAssignee by remember { mutableStateOf(task.assignedTo) }

    val daysDiff = remember(task.deadlineMs) {
        val calculated = ((task.deadlineMs - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L))
        if (calculated < 1) "1" else "$calculated"
    }
    var chosenDaysFromNow by remember { mutableStateOf(daysDiff) }

    var selectedDependencyTaskId by remember { mutableStateOf<Int?>(task.dependsOnTaskId) }
    var dropDownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .testTag("edit_task_dialog"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "RECONFIGURE WORKSPACE DELIVERABLE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = titleState,
                    onValueChange = { titleState = it },
                    label = { Text("Task Headline", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_task_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = descState,
                    onValueChange = { descState = it },
                    label = { Text("Task Description / Specifications", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_task_desc_input"),
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = chosenDaysFromNow,
                        onValueChange = { chosenDaysFromNow = it },
                        label = { Text("Days from Now", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_task_days_input"),
                        singleLine = true
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Task Alert Priority", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("High", "Medium").forEach { p ->
                                val active = selectedPriority == p
                                Button(
                                    onClick = { selectedPriority = p },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(p, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Category Group", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("Operations", "Branding").forEach { proj ->
                                val active = selectedProject == proj
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedProject = proj }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(proj, fontSize = 9.sp, color = if (active) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Assignee Target", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("Alex (Lead Dev)", "Lisa (UI/UX)").forEach { target ->
                                val active = selectedAssignee == target
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedAssignee = target }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(target.take(9), fontSize = 9.sp, color = if (active) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "LINK PRE-REQUISITE DEPENDENCY (Optional)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { dropDownExpanded = !dropDownExpanded }
                            .padding(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val activeDependencyName = if (selectedDependencyTaskId != null) {
                                tasksList.find { it.id == selectedDependencyTaskId }?.title ?: "Select Task"
                            } else {
                                "None (No Preceding Dependencies)"
                            }

                            Text(
                                text = activeDependencyName,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (dropDownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown icon",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = dropDownExpanded,
                            onDismissRequest = { dropDownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("(No Parent Task Dependency)", fontSize = 11.sp) },
                                onClick = {
                                    selectedDependencyTaskId = null
                                    dropDownExpanded = false
                                }
                            )
                            tasksList.filter { it.id != task.id && it.status != "Completed" }.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text("ID ${t.id}: ${t.title} [Pending]", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        selectedDependencyTaskId = t.id
                                        dropDownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val days = chosenDaysFromNow.toIntOrNull() ?: 1
                            val deadlineMs = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)
                            onConfirm(
                                titleState,
                                descState,
                                deadlineMs,
                                selectedProject,
                                selectedPriority,
                                selectedAssignee,
                                selectedDependencyTaskId
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_confirm_edit_task_btn")
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

// ==================== GLOBAL SEARCH AND FILTERING BAR ====================
@Composable
fun GlobalSearchFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filterProject: String,
    onFilterProjectChange: (String) -> Unit,
    filterAssignee: String,
    onFilterAssigneeChange: (String) -> Unit,
    filterPriority: String,
    onFilterPriorityChange: (String) -> Unit,
    filterDateRange: String,
    onFilterDateRangeChange: (String) -> Unit,
    showAdvancedFilters: Boolean,
    onToggleAdvancedFilters: () -> Unit,
    tasks: List<Task>
) {
    // Collect dynamic projects and assignees from the current tasks list to guarantee exact matching options
    val projectsList = remember(tasks) {
        val list = tasks.map { it.project }.distinct().filter { it.isNotBlank() }.toMutableList()
        if (!list.contains("Operations")) list.add("Operations")
        if (!list.contains("Branding")) list.add("Branding")
        list.sorted()
    }

    val assigneesList = remember(tasks) {
        val list = tasks.map { it.assignedTo }.distinct().filter { it.isNotBlank() }.toMutableList()
        if (list.none { it.contains("Alex", ignoreCase = true) }) list.add("Alex (Lead Dev)")
        if (list.none { it.contains("Lisa", ignoreCase = true) }) list.add("Lisa (UI/UX)")
        list.sorted()
    }

    val isAnyFilterSelected = searchQuery.isNotEmpty() || filterProject != "All" || filterAssignee != "All" || filterPriority != "All" || filterDateRange != "All"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("global_search_filter_bar_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            // Top search text bar Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search title, specs, project, assignee...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Search",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("global_search_text_input")
                )

                // Advanced tune toggle button
                IconButton(
                    onClick = onToggleAdvancedFilters,
                    modifier = Modifier
                        .size(44.dp)
                        .border(
                            1.dp,
                            if (showAdvancedFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(10.dp)
                        )
                        .background(
                            if (showAdvancedFilters) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(10.dp)
                        )
                        .testTag("toggle_filters_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Tune Filters",
                        tint = if (showAdvancedFilters) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isAnyFilterSelected) {
                    IconButton(
                        onClick = {
                            onSearchQueryChange("")
                            onFilterProjectChange("All")
                            onFilterAssigneeChange("All")
                            onFilterPriorityChange("All")
                            onFilterDateRangeChange("All")
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .testTag("reset_all_filters_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Reset Filters",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Advanced Filters Panel
            AnimatedVisibility(
                visible = showAdvancedFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))

                    // 1. Project Selector
                    Column {
                        Text(
                            text = "PROJECT CATEGORY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            FilterGroupChip(
                                text = "All",
                                selected = filterProject == "All",
                                onClick = { onFilterProjectChange("All") }
                            )
                            projectsList.forEach { proj ->
                                FilterGroupChip(
                                    text = proj,
                                    selected = filterProject == proj,
                                    onClick = { onFilterProjectChange(proj) }
                                )
                            }
                        }
                    }

                    // 2. Assignee Selector
                    Column {
                        Text(
                            text = "ASSIGNEE TARGET",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            FilterGroupChip(
                                text = "All",
                                selected = filterAssignee == "All",
                                onClick = { onFilterAssigneeChange("All") }
                            )
                            assigneesList.forEach { target ->
                                val label = when {
                                    target.contains("Alex", ignoreCase = true) -> "Alex"
                                    target.contains("Lisa", ignoreCase = true) -> "Lisa"
                                    else -> target.take(8)
                                }
                                FilterGroupChip(
                                    text = label,
                                    selected = filterAssignee == target || (filterAssignee == "Alex" && target.contains("Alex")) || (filterAssignee == "Lisa" && target.contains("Lisa")),
                                    onClick = { onFilterAssigneeChange(target) }
                                )
                            }
                        }
                    }

                    // 3. Priority + Deadline Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = "PRIORITY ALERT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                listOf("All", "High", "Medium").forEach { priority ->
                                    FilterGroupChip(
                                        text = priority,
                                        selected = filterPriority == priority,
                                        onClick = { onFilterPriorityChange(priority) }
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = "DEADLINE TIME-FRAME",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                mapOf(
                                    "All" to "All",
                                    "Selected Day" to "Selected",
                                    "Overdue" to "Overdue",
                                    "Within 7 Days" to "Next 7d"
                                ).forEach { (key, display) ->
                                    FilterGroupChip(
                                        text = display,
                                        selected = filterDateRange == key,
                                        onClick = { onFilterDateRangeChange(key) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterGroupChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(end = 4.dp, bottom = 4.dp)
            .testTag("filter_chip_$text")
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            ),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

// ==================== TIME TRACKER COMPONENT ====================
@Composable
fun TimeTrackerSection(
    timeLogs: List<TimeLog>,
    tasks: List<Task>,
    viewModel: WorkspaceViewModel,
    currentUserName: String
) {
    val context = LocalContext.current
    val activeTimerTaskId by viewModel.activeTimerTaskId.collectAsStateWithLifecycle()
    val activeTimerSeconds by viewModel.activeTimerSeconds.collectAsStateWithLifecycle()

    var selectedTaskForManualLog by remember { mutableStateOf<Task?>(null) }
    var manualHoursText by remember { mutableStateOf("") }
    var manualMinutesText by remember { mutableStateOf("") }
    var expandedTaskDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Unified Analytics Intro Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⏱️ Unified Time Tracker & Analytics",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Track real-time productivity per project, record specific tasks, and generate total timesheet compliance analytics.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.triggerExportPdfReport(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("export_time_pdf_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF icon", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Compliance PDF Status Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live Timer Session Section
        item {
            if (activeTimerTaskId != null) {
                val activeTask = tasks.find { it.id == activeTimerTaskId }
                if (activeTask != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(0.6f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Stopwatch active",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "LIVE TIMER RUNNING",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = activeTask.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Project: ${activeTask.project} | Assignee: ${activeTask.assignedTo}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.7f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(0.4f))
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formatMMSS(activeTimerSeconds),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.submitTimeLog(
                                            taskId = activeTask.id,
                                            seconds = activeTimerSeconds,
                                            userName = currentUserName,
                                            taskTitle = activeTask.title,
                                            project = activeTask.project
                                        )
                                        Toast.makeText(context, "Logged ${activeTimerSeconds / 60}m to database!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.weight(1f).height(38.dp).testTag("save_active_timer_btn")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Submit Log", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.clearTimer()
                                        Toast.makeText(context, "Timer reset.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier.weight(1f).height(38.dp).testTag("cancel_active_timer_btn")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Discard", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "info",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "💡 Pro Tip: To track live work, press the Play icon next to any task on your workspace calendar list!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // User report dashboard summaries (Satisfies Reports per user constraint)
        item {
            Text(
                text = "📊 Executive Team Timesheet Reports",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        item {
            val userLogs = remember(timeLogs) {
                timeLogs.groupBy { it.user }.mapValues { entry ->
                    entry.value.sumOf { it.timeSpentSeconds }
                }
            }

            if (userLogs.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No timesheet records logged yet.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    userLogs.forEach { (user, seconds) ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("timesheet_user_card_$user"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.firstOrNull()?.toString() ?: "?",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = user,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatSecondsToHMS(seconds),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Total Logged",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Manual log entry submission drawer
        item {
            Text(
                text = "📝 Log Completed Task Manually",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Register manual timesheet log into compliance matrix",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedTaskDropdown = true },
                            modifier = Modifier.fillMaxWidth().testTag("manual_log_task_selector"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedTaskForManualLog?.title ?: "Select Task to log time...",
                                    fontSize = 12.sp,
                                    color = if (selectedTaskForManualLog != null) MaterialTheme.colorScheme.onSurface 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown symbol")
                            }
                        }

                        DropdownMenu(
                            expanded = expandedTaskDropdown,
                            onDismissRequest = { expandedTaskDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            tasks.forEach { task ->
                                DropdownMenuItem(
                                    text = { Text("${task.title} [${task.project}]", fontSize = 12.sp) },
                                    onClick = {
                                        selectedTaskForManualLog = task
                                        expandedTaskDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualHoursText,
                            onValueChange = { manualHoursText = it.filter { char -> char.isDigit() } },
                            label = { Text("Hours", fontSize = 11.sp) },
                            placeholder = { Text("0") },
                            modifier = Modifier.weight(1f).testTag("manual_hours_input"),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )

                        OutlinedTextField(
                            value = manualMinutesText,
                            onValueChange = { manualMinutesText = it.filter { char -> char.isDigit() } },
                            label = { Text("Minutes", fontSize = 11.sp) },
                            placeholder = { Text("0") },
                            modifier = Modifier.weight(1f).testTag("manual_minutes_input"),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val targetTask = selectedTaskForManualLog
                            if (targetTask == null) {
                                Toast.makeText(context, "Please select a task first!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val hoursVal = manualHoursText.toLongOrNull() ?: 0L
                            val minutesVal = manualMinutesText.toLongOrNull() ?: 0L
                            val totalSeconds = (hoursVal * 3600) + (minutesVal * 60)

                            if (totalSeconds <= 0) {
                                Toast.makeText(context, "Duration must be greater than zero!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.submitTimeLog(
                                taskId = targetTask.id,
                                seconds = totalSeconds,
                                userName = currentUserName,
                                taskTitle = targetTask.title,
                                project = targetTask.project
                            )

                            Toast.makeText(context, "Recorded manual log successfully!", Toast.LENGTH_SHORT).show()
                            selectedTaskForManualLog = null
                            manualHoursText = ""
                            manualMinutesText = ""
                        },
                        modifier = Modifier.fillMaxWidth().testTag("submit_manual_log_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add symbol")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record Time Spent")
                    }
                }
            }
        }

        // Detailed session ledger logs
        item {
            Text(
                text = "🔎 Recent Registered Sessions Logs",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        if (timeLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.2f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No timesheet logs currently recorded in the database.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(timeLogs) { log ->
                TimeLogEntryCard(log = log, onDelete = { viewModel.deleteTimeLog(log.id) })
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun TimeLogEntryCard(log: TimeLog, onDelete: () -> Unit) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(log.timestamp) { sdf.format(Date(log.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🧑 ${log.user}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.taskTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(log.project, fontSize = 9.sp) },
                        modifier = Modifier.height(20.dp)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Duration: ${formatSecondsToHMS(log.timeSpentSeconds)}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Log entry",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun formatSecondsToHMS(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) {
        String.format(Locale.getDefault(), "%dh %dm", h, m)
    } else if (m > 0) {
        String.format(Locale.getDefault(), "%dm %ds", m, s)
    } else {
        String.format(Locale.getDefault(), "%ds", s)
    }
}

fun formatMMSS(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}
