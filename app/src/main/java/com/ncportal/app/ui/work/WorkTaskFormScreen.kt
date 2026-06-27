@file:OptIn(ExperimentalMaterial3Api::class)

package com.ncportal.app.ui.work

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ncportal.app.model.Organization
import com.ncportal.app.model.WorkTask
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// Form result  (passed to WorkScreen on submit)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Immutable snapshot of the form fields on submit.
 * [scheduledDate] is epoch-ms; **0L** means the user did not pick a date.
 */
data class TaskFormData(
    val title: String,
    val organizationId: String,
    val requester: String,
    val contact: String,
    val scheduledDate: Long,
    val workDescription: String,
    val notes: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

private val DATE_DISPLAY_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")

/**
 * Create / edit form for [WorkTask].
 *
 * Pass [editingTask] = null for create mode; a non-null task for edit mode.
 * Initial field values come from [editingTask], so [rememberSaveable] then
 * preserves them across configuration changes without an extra LaunchedEffect.
 *
 * Fields:
 * - 제목 (title) — required
 * - 기관 (organization) — ExposedDropdownMenuBox
 * - 작업예정일 — DatePickerDialog (Material3 ExperimentalMaterial3Api)
 * - 요청자 / 연락처 — singleLine OutlinedTextField
 * - 작업내용 — multiline OutlinedTextField (minLines = 4)
 * - 비고 — multiline OutlinedTextField (minLines = 2)
 */
@Composable
fun WorkTaskFormScreen(
    editingTask: WorkTask?,
    organizations: List<Organization>,
    onSubmit: (TaskFormData) -> Unit,
    onCancel: () -> Unit,
) {
    val isEditMode = editingTask != null

    // ── Form state ────────────────────────────────────────────────────────────
    var title       by rememberSaveable { mutableStateOf(editingTask?.title ?: "") }
    var orgId       by rememberSaveable { mutableStateOf(editingTask?.organizationId ?: "") }
    var requester   by rememberSaveable { mutableStateOf(editingTask?.requester ?: "") }
    var contact     by rememberSaveable { mutableStateOf(editingTask?.contact ?: "") }
    var workDesc    by rememberSaveable { mutableStateOf(editingTask?.workDescription ?: "") }
    var notes       by rememberSaveable { mutableStateOf(editingTask?.notes ?: "") }
    // Long is java.io.Serializable so rememberSaveable handles it automatically.
    var scheduledMs by rememberSaveable { mutableStateOf(editingTask?.scheduledDate ?: 0L) }

    // Validation
    var titleError by rememberSaveable { mutableStateOf(false) }

    val selectedOrg = organizations.firstOrNull { it.id == orgId }

    // ── DatePicker ────────────────────────────────────────────────────────────
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = scheduledMs.takeIf { it != 0L },
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        scheduledMs = datePickerState.selectedDateMillis ?: 0L
                        showDatePicker = false
                    },
                ) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Org dropdown ──────────────────────────────────────────────────────────
    var orgDropdownExpanded by remember { mutableStateOf(false) }

    // ── Scaffold ──────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "업무 수정" else "새 업무 등록") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "닫기")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier        = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── 제목 ──────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value          = title,
                    onValueChange  = { title = it; titleError = false },
                    label          = { Text("제목 *") },
                    singleLine     = true,
                    isError        = titleError,
                    supportingText = if (titleError) {
                        { Text("제목을 입력하세요") }
                    } else null,
                    modifier       = Modifier.fillMaxWidth(),
                )
            }

            // ── 기관 선택  (ExposedDropdownMenuBox) ───────────────────────
            item {
                ExposedDropdownMenuBox(
                    expanded        = orgDropdownExpanded,
                    onExpandedChange = { orgDropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value         = selectedOrg?.name ?: "",
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("기관") },
                        placeholder   = { Text("기관을 선택하세요") },
                        trailingIcon  = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = orgDropdownExpanded)
                        },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded        = orgDropdownExpanded,
                        onDismissRequest = { orgDropdownExpanded = false },
                    ) {
                        // "Clear" option
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "— 선택 안 함 —",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            onClick = { orgId = ""; orgDropdownExpanded = false },
                        )
                        // Organisation entries — M3 DropdownMenuItem has no supportingText;
                        // show contact as a second line inside the text lambda instead.
                        organizations.forEach { org ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(org.name)
                                        if (org.contact.isNotBlank()) {
                                            Text(
                                                text  = org.contact,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                },
                                onClick = { orgId = org.id; orgDropdownExpanded = false },
                            )
                        }
                    }
                }
            }

            // ── 작업예정일  (DatePickerDialog trigger) ────────────────────
            item {
                val dateLabel = remember(scheduledMs) {
                    if (scheduledMs == 0L) ""
                    else Instant.ofEpochMilli(scheduledMs)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(DATE_DISPLAY_FMT)
                }
                OutlinedTextField(
                    value         = dateLabel,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("작업예정일") },
                    placeholder   = { Text("날짜를 선택하세요") },
                    leadingIcon   = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "날짜 선택")
                        }
                    },
                    trailingIcon  = if (scheduledMs != 0L) {
                        {
                            IconButton(onClick = { scheduledMs = 0L }) {
                                Icon(Icons.Filled.Clear, contentDescription = "날짜 지우기")
                            }
                        }
                    } else null,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }

            // ── 요청자 ────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value         = requester,
                    onValueChange = { requester = it },
                    label         = { Text("요청자") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }

            // ── 연락처 ────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value           = contact,
                    onValueChange   = { contact = it },
                    label           = { Text("연락처") },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier        = Modifier.fillMaxWidth(),
                )
            }

            // ── 작업내용 (multiline) ───────────────────────────────────────
            item {
                OutlinedTextField(
                    value         = workDesc,
                    onValueChange = { workDesc = it },
                    label         = { Text("작업내용") },
                    minLines      = 4,
                    maxLines      = 10,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }

            // ── 비고 (multiline) ──────────────────────────────────────────
            item {
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("비고") },
                    minLines      = 2,
                    maxLines      = 6,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }

            // ── Submit / Cancel ───────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick  = onCancel,
                        modifier = Modifier.weight(1f),
                    ) { Text("취소") }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@Button
                            }
                            onSubmit(
                                TaskFormData(
                                    title           = title.trim(),
                                    organizationId  = orgId,
                                    requester       = requester.trim(),
                                    contact         = contact.trim(),
                                    scheduledDate   = scheduledMs,
                                    workDescription = workDesc.trim(),
                                    notes           = notes.trim(),
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(if (isEditMode) "저장" else "등록") }
                }
            }
        }
    }
}
