package com.example.ui.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.EmailLog
import com.example.data.RepositoryDocument
import com.example.data.Task
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun generateWorkspaceReport(
        context: Context,
        tasks: List<Task>,
        documents: List<RepositoryDocument>,
        emailLogs: List<EmailLog>
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val totalPages = 2 // Let's make it a detailed 2-page report!
            val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val dateStr = formatter.format(Date())

            for (pageNumber in 1..totalPages) {
                // A4 Size: 595 x 842 points
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Setup Paints
                val headerPaint = Paint().apply {
                    color = Color.rgb(26, 35, 126) // Deep Indigo #1A237E
                    isAntiAlias = true
                }
                val borderPaint = Paint().apply {
                    color = Color.rgb(224, 224, 224)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                val accentPaint = Paint().apply {
                    color = Color.rgb(21, 101, 192) // Indigo Blue
                    isAntiAlias = true
                }
                val textPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 10f
                    isAntiAlias = true
                }
                val textBoldPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 10f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val titlePaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 16f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val subtitlePaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 10f
                    isAntiAlias = true
                }
                val sectionHeaderPaint = Paint().apply {
                    color = Color.rgb(26, 35, 126)
                    textSize = 12f
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                if (pageNumber == 1) {
                    // PAGE 1: TITLE & STATS & KEY TASKS

                    // Top header background banner
                    canvas.drawRect(30f, 30f, 565f, 110f, headerPaint)
                    canvas.drawText("PROJECT WORKSPACE STATUS REPORT", 50f, 65f, titlePaint)
                    canvas.drawText("Report Generated: $dateStr | Safe Repository Copy", 50f, 90f, subtitlePaint)

                    // Draw Page Boundary Box
                    canvas.drawRect(30f, 30f, 565f, 812f, borderPaint)

                    // METRICS OVERVIEW
                    canvas.drawText("1. EXECUTIVE METRICS SUMMARY", 50f, 140f, sectionHeaderPaint)
                    canvas.drawLine(50f, 145f, 545f, 145f, borderPaint)

                    val completedCount = tasks.count { it.isCompleted }
                    val totalCount = tasks.size
                    val overdueCount = tasks.count { !it.isCompleted && it.dueDate < System.currentTimeMillis() }
                    val progressPercent = if (totalCount > 0) (completedCount * 100) / totalCount else 0

                    // Score Cards Grid
                    val colWidth = 150f
                    val rowY = 175f

                    // Card 1: Tasks Complete
                    canvas.drawRect(50f, rowY - 15, 50f + colWidth, rowY + 35, borderPaint)
                    canvas.drawText("Tasks Progress", 55f, rowY, textBoldPaint)
                    canvas.drawText("$completedCount / $totalCount Completed ($progressPercent%)", 55f, rowY + 20, textPaint)

                    // Card 2: Overdue items
                    canvas.drawRect(50f + colWidth + 10, rowY - 15, 50f + (colWidth * 2) + 10, rowY + 35, borderPaint)
                    canvas.drawText("Overdue Items", 50f + colWidth + 15, rowY, textBoldPaint)
                    canvas.drawText("$overdueCount Warning Alerts Active", 50f + colWidth + 15, rowY + 20, textPaint)

                    // Card 3: Files & Logs
                    canvas.drawRect(50f + (colWidth * 2) + 20, rowY - 15, 545f, rowY + 35, borderPaint)
                    canvas.drawText("Repository & Logs", 50f + (colWidth * 2) + 25, rowY, textBoldPaint)
                    canvas.drawText("${documents.size} Files | ${emailLogs.size} Overdue Alerts", 50f + (colWidth * 2) + 25, rowY + 20, textPaint)

                    // TASKS LISTING (Top 10)
                    canvas.drawText("2. CURRENT DEADLINES & RESPONSIBILITIES", 50f, 250f, sectionHeaderPaint)
                    canvas.drawLine(50f, 255f, 545f, 255f, borderPaint)

                    var taskY = 280f
                    // Table Header
                    canvas.drawText("Task Title", 50f, taskY, textBoldPaint)
                    canvas.drawText("Project", 200f, taskY, textBoldPaint)
                    canvas.drawText("Priority", 290f, taskY, textBoldPaint)
                    canvas.drawText("Assignee", 360f, taskY, textBoldPaint)
                    canvas.drawText("Status", 450f, taskY, textBoldPaint)
                    canvas.drawText("Due Date", 500f, taskY, textBoldPaint)
                    canvas.drawLine(50f, taskY + 5, 545f, taskY + 5, borderPaint)

                    taskY += 20f

                    val itemsToPrint = tasks.take(15) // fit first 15 items
                    for (task in itemsToPrint) {
                        if (taskY > 770f) break

                        val titleCut = if (task.title.length > 22) task.title.substring(0, 20) + ".." else task.title
                        val dateFormatted = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(task.dueDate))

                        canvas.drawText(titleCut, 50f, taskY, textPaint)
                        canvas.drawText(task.project, 200f, taskY, textPaint)
                        canvas.drawText(task.priority, 290f, taskY, textPaint)
                        canvas.drawText(task.assignedTo, 360f, taskY, textPaint)
                        canvas.drawText(if (task.isCompleted) "Done" else "Pending", 450f, taskY, textPaint)
                        canvas.drawText(dateFormatted, 500f, taskY, textPaint)

                        taskY += 18f
                    }

                    if (tasks.size > 15) {
                        canvas.drawText("... and ${tasks.size - 15} more secondary tasks on Page 2.", 50f, 790f, textPaint)
                    }

                    // Page indicator
                    canvas.drawText("Page 1 of $totalPages", 500f, 795f, textPaint)

                } else {
                    // PAGE 2: MAIN COMPONENT DOCUMENT FILE INVENTORY & EMAIL LOGS

                    // Draw Page Boundary Box
                    canvas.drawRect(30f, 30f, 565f, 812f, borderPaint)

                    // Small Header
                    canvas.drawRect(30f, 30f, 565f, 65f, accentPaint)
                    canvas.drawText("PROJECT WORKSPACE REPORTS (CONTINUED)", 50f, 50f, titlePaint)

                    // DOCUMENTS SECTION
                    canvas.drawText("3. SECURE REPOSITORY INVENTORY", 50f, 100f, sectionHeaderPaint)
                    canvas.drawLine(50f, 105f, 545f, 105f, borderPaint)

                    var docY = 125f
                    // Table Header
                    canvas.drawText("File Name", 50f, docY, textBoldPaint)
                    canvas.drawText("Type", 250f, docY, textBoldPaint)
                    canvas.drawText("Size", 320f, docY, textBoldPaint)
                    canvas.drawText("Uploaded By", 390f, docY, textBoldPaint)
                    canvas.drawText("Registry Date", 470f, docY, textBoldPaint)
                    canvas.drawLine(50f, docY + 5, 545f, docY + 5, borderPaint)

                    docY += 20f
                    val docsToPrint = documents.take(15)
                    if (docsToPrint.isEmpty()) {
                        canvas.drawText("[No repository files registered. Workspace ledger empty]", 50f, docY, textPaint)
                        docY += 20f
                    } else {
                        for (doc in docsToPrint) {
                            if (docY > 380f) break
                            val nameCut = if (doc.fileName.length > 25) doc.fileName.substring(0, 23) + ".." else doc.fileName
                            val docDate = SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(doc.uploadDate))

                            canvas.drawText(nameCut, 50f, docY, textPaint)
                            canvas.drawText(doc.fileType.uppercase(Locale.ROOT), 250f, docY, textPaint)
                            canvas.drawText(doc.fileSize, 320f, docY, textPaint)
                            canvas.drawText(doc.uploadedBy, 390f, docY, textPaint)
                            canvas.drawText(docDate, 470f, docY, textPaint)

                            docY += 18f
                        }
                    }

                    // OVERDUE NOTIFICATIONS OUTBOUND LOG
                    canvas.drawText("4. AUTOMATED EMAIL OVERDUE LOGS", 50f, 430f, sectionHeaderPaint)
                    canvas.drawLine(50f, 435f, 545f, 435f, borderPaint)

                    var emailY = 455f
                    // Table Header
                    canvas.drawText("Related Task Title", 50f, emailY, textBoldPaint)
                    canvas.drawText("Recipient Name", 230f, emailY, textBoldPaint)
                    canvas.drawText("Outbound Email", 340f, emailY, textBoldPaint)
                    canvas.drawText("Date Fired", 470f, emailY, textBoldPaint)
                    canvas.drawLine(50f, emailY + 5, 545f, emailY + 5, borderPaint)

                    emailY += 20f
                    val logsToPrint = emailLogs.take(15)
                    if (logsToPrint.isEmpty()) {
                        canvas.drawText("[No overdue email logs recorded in database]", 50f, emailY, textPaint)
                        emailY += 20f
                    } else {
                        for (log in logsToPrint) {
                            if (emailY > 750f) break
                            val taskTitleCut = if (log.taskTitle.length > 24) log.taskTitle.substring(0, 22) + ".." else log.taskTitle
                            val firedDate = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(log.sentTimestamp))

                            canvas.drawText(taskTitleCut, 50f, emailY, textPaint)
                            canvas.drawText(log.recipientName, 230f, emailY, textPaint)
                            canvas.drawText(log.recipientEmail, 340f, emailY, textPaint)
                            canvas.drawText(firedDate, 470f, emailY, textPaint)

                            emailY += 18f
                        }
                    }

                    // Disclaimer/Footer
                    canvas.drawText("This status document is generated locally and persistently using Room and Sandbox APIs.", 50f, 785f, textPaint)
                    canvas.drawText("Page 2 of $totalPages", 500f, 795f, textPaint)
                }

                pdfDocument.finishPage(page)
            }

            // Save PDF to App Files Directory
            val pdfDir = File(context.getExternalFilesDir(null), "Reports")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            val pdfFile = File(pdfDir, "Workspace_Audit_Report_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
