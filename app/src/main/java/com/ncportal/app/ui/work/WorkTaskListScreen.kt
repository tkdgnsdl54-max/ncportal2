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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ncportal.app.model.TaskStatus
import com.ncportal.app.model.WorkTask
import com.ncportal.app.model.WorkUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// Filter model
// ─────────────────────────────────────────────────────────────────────────────

/** Status filter chips shown at the top of the task list. */
enum class StatusFilter(val label: String, val status: TaskStatus?) {
    ALL("전체", null),
    PENDING("대기중", TaskStatus.PENDING),
    IN_PROGRESS("진행중", TaskStatus.IN_PROGRESS),
    COMPLETED("완료", TaskStatus.COMPLETED),
    REVISIT("재방문", TaskStatus.REVISIT),
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Filterable task list — the "home" screen of the Work module.
 *
 * Layout:
 * ```
 * TopAppBar  [일정] [기관 관리]
 * ─────────────────────────────
 * FilterChip row  (전체 / 대기중 / 진행중 / 완료 / 재방문)
 * ─────────────────────────────
 * LazyColumn of ElevatedCard task items
 * FAB  (+)
 * ```
 */
@Composable
fun WorkTaskListScreen(
    uiState: WorkUiState,
    onCreateTask: () -> Unit,
    onTaskClick: (taskId: String) -> Unit,
    onOrgList: () -> Unit,
    onSchedule: () -> Unit,
    onFilterChange: (TaskStatus?) -> Unit,
) {
    val activeFilter = StatusFilter.entries.firstOrNull { it.status == uiState.statusFilter }
        ?: StatusFilter.ALL

    val visibleTasks = remember(uiState.tasks, uiState.statusFilter) {
        if (uiState.statusFilter == null) uiState.tasks
        else uiState.tasks.filter { it.status == uiState.statusFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("업무 관리") },
                actions = {
                    IconButton(onClick = onSchedule) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "일정 보기")
                    }
                    IconButton(onClick = onOrgList) {
                        Icon(Icons.Filled.Business, contentDescription = "기관 관리")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTask) {
                Icon(Icons.Filled.Add, contentDescription = "새 업무 등록")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Status filter chips ────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(StatusFilter.entries) { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick  = { onFilterChange(filter.status) },
                        label    = { Text(filter.label) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = filter.status?.chipColor
                                ?: MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor     = if (filter.status != null) chipContentColor
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Task list / empty state ────────────────────────────────────
            if (visibleTasks.isEmpty()) {
                EmptyTasksPlaceholder(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(visibleTasks, key = { it.id }) { task ->
                        WorkTaskCard(
                            task           = task,
                            organizationName = uiState.organizations
                                .firstOrNull { it.id == task.organizationId }?.name,
                            onClick        = { onTaskClick(task.id) },
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Task card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WorkTaskCard(
    task: WorkTask,
    organizationName: String?,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title row + status chip
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text     = task.title,
                    style    = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(status = task.status)
            }

            Spacer(Modifier.height(6.dp))

            // Metadata row: org name + scheduled date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Filled.Business,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text  = organizationName ?: "기관 미지정",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (task.scheduledDate != 0L) {
                    val dateLabel = remember(task.scheduledDate) {
                        Instant.ofEpochMilli(task.scheduledDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(DateTimeFormatter.ofPattern("MM/dd"))
                    }
                    Icon(
                        imageVector        = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier
                            .padding(start = 10.dp)
                            .size(14.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text  = dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared status chip  (used in this screen + detail screen + schedule screen)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Non-interactive status chip with a solid colour background derived from
 * [TaskStatus.chipColor].
 */
@Composable
fun StatusChip(status: TaskStatus, modifier: Modifier = Modifier) {
    SuggestionChip(
        onClick  = {},
        label    = {
            Text(
                text  = status.label,
                style = MaterialTheme.typography.labelSmall,
                color = chipContentColor,
            )
        },
        colors   = SuggestionChipDefaults.suggestionChipColors(
            containerColor = status.chipColor,
        ),
        border   = null,
        modifier = modifier,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyTasksPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier              = modifier.padding(32.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector        = Icons.Filled.AssignmentTurnedIn,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.outline,
            modifier           = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text  = "등록된 업무가 없습니다",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
