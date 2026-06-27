@file:OptIn(ExperimentalMaterial3Api::class)

package com.ncportal.app.ui.work

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ncportal.app.model.LogAction
import com.ncportal.app.model.Organization
import com.ncportal.app.model.TaskStatus
import com.ncportal.app.model.WorkLogEntry
import com.ncportal.app.model.WorkTask
import com.ncportal.app.model.nextStates
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
private val DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Read-only detail view for a single [WorkTask].
 *
 * Sections (top → bottom):
 * 1. **Header card** — title, status chip, all field values
 * 2. **Status action row** — buttons driven by [TaskStatus.nextStates]
 * 3. **Work log timeline** — chronological [WorkLogEntry] list with icons
 * 4. **Add note** — TextButton that opens an [AlertDialog]
 *
 * Pass [task] = null while the ViewModel is loading; a spinner is shown.
 */
@Composable
fun WorkTaskDetailScreen(
    task: WorkTask?,
    organizations: List<Organization>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onAddNote: (String) -> Unit,
) {
    var showNoteDialog by rememberSaveable { mutableStateOf(false) }
    var noteText by rememberSaveable { mutableStateOf("") }

    if (showNoteDialog) {
        AddNoteDialog(
            text         = noteText,
            onTextChange = { noteText = it },
            onConfirm    = {
                onAddNote(noteText)
                noteText = ""
                showNoteDialog = false
            },
            onDismiss    = {
                noteText = ""
                showNoteDialog = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("업무 상세") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit, enabled = task != null) {
                        Icon(Icons.Filled.Edit, contentDescription = "수정")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (task == null) {
            Box(
                modifier          = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment  = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val orgName = organizations.firstOrNull { it.id == task.organizationId }?.name

        LazyColumn(
            modifier        = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding  = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Header card ─────────────────────────────────────────────
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Title + status chip
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.Top,
                        ) {
                            Text(
                                text     = task.title,
                                style    = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            StatusChip(status = task.status)
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(12.dp))

                        // Fields
                        DetailRow(Icons.Filled.Business, "기관", orgName ?: "—")
                        DetailRow(Icons.Filled.Person, "요청자", task.requester.ifBlank { "—" })
                        DetailRow(Icons.Filled.Phone, "연락처", task.contact.ifBlank { "—" })
                        DetailRow(
                            icon  = Icons.Filled.CalendarMonth,
                            label = "작업예정일",
                            value = if (task.scheduledDate == 0L) "—"
                            else Instant.ofEpochMilli(task.scheduledDate)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(DATE_FMT),
                        )

                        if (task.workDescription.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "작업내용",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(task.workDescription, style = MaterialTheme.typography.bodyMedium)
                        }

                        if (task.notes.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "비고",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(task.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // ── Status action row ────────────────────────────────────────
            item {
                StatusActionRow(
                    currentStatus = task.status,
                    onTransition  = onStatusChange,
                )
            }

            // ── Work log header + add-note button ────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("작업 이력", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showNoteDialog = true }) {
                        Icon(
                            imageVector        = Icons.Filled.AddComment,
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("메모 추가")
                    }
                }
            }

            // ── Work log entries (oldest → newest) ───────────────────────
            val sortedLog = task.workLogs.sortedBy { it.timestamp }
            if (sortedLog.isEmpty()) {
                item {
                    Text(
                        text     = "작업 이력이 없습니다",
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            } else {
                items(sortedLog, key = { it.id }) { entry ->
                    WorkLogRow(entry = entry)
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status action row
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders only the buttons that are valid for [currentStatus] by calling
 * [TaskStatus.nextStates]. This means the set of visible buttons changes
 * automatically as the task progresses.
 *
 * Transition → button mapping:
 * - → IN_PROGRESS : 작업시작  (FilledTonalButton, blue accent)
 * - → COMPLETED   : 완료처리  (Button, primary)
 * - → REVISIT     : 재방문    (OutlinedButton, red content colour)
 * - → PENDING     : 대기로    (OutlinedButton, neutral)
 */
@Composable
private fun StatusActionRow(
    currentStatus: TaskStatus,
    onTransition: (TaskStatus) -> Unit,
) {
    val nextStates = currentStatus.nextStates()
    if (nextStates.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "상태 변경",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                nextStates.forEach { target ->
                    when (target) {
                        TaskStatus.IN_PROGRESS -> FilledTonalButton(
                            onClick  = { onTransition(target) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("작업시작")
                        }

                        TaskStatus.COMPLETED -> Button(
                            onClick  = { onTransition(target) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("완료처리")
                        }

                        TaskStatus.REVISIT -> OutlinedButton(
                            onClick  = { onTransition(target) },
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = TaskStatus.REVISIT.chipColor,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Replay, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("재방문")
                        }

                        TaskStatus.PENDING -> OutlinedButton(
                            onClick  = { onTransition(target) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("대기로")
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Work log timeline row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WorkLogRow(entry: WorkLogEntry) {
    val (icon, tint) = when (entry.action) {
        LogAction.CREATED            -> Icons.Filled.FiberNew to MaterialTheme.colorScheme.primary
        LogAction.STARTED            -> Icons.Filled.PlayArrow to TaskStatus.IN_PROGRESS.chipColor
        LogAction.COMPLETED          -> Icons.Filled.CheckCircle to TaskStatus.COMPLETED.chipColor
        LogAction.REVISIT_SCHEDULED  -> Icons.Filled.Replay to TaskStatus.REVISIT.chipColor
        LogAction.NOTE_ADDED         -> Icons.Filled.Notes to MaterialTheme.colorScheme.tertiary
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = entry.action.label,
            tint               = tint,
            modifier           = Modifier
                .size(20.dp)
                .padding(top = 2.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(entry.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                text  = Instant.ofEpochMilli(entry.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(DATETIME_FMT),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add note dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AddNoteDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon             = { Icon(Icons.Filled.AddComment, contentDescription = null) },
        title            = { Text("메모 추가") },
        text             = {
            OutlinedTextField(
                value         = text,
                onValueChange = onTextChange,
                label         = { Text("내용을 입력하세요") },
                minLines      = 3,
                maxLines      = 6,
                modifier      = Modifier.fillMaxWidth(),
            )
        },
        confirmButton    = {
            TextButton(
                onClick  = onConfirm,
                enabled  = text.isNotBlank(),
            ) { Text("추가") }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Detail row helper
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(68.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
