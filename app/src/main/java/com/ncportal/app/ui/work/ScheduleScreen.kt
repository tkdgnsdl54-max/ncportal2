@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.ncportal.app.ui.work

import androidx.compose.foundation.ExperimentalFoundationApi

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ncportal.app.model.WorkTask
import com.ncportal.app.model.WorkUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Schedule view — tasks grouped by [WorkTask.scheduledDate] (epoch-ms), shown
 * with the nearest upcoming date first.
 *
 * Layout per date bucket:
 * ```
 * [sticky header]  ── date chip (오늘 / 내일 / 지남 / absolute date)
 *   OutlinedCard   ── status colour bar  |  title · org name  |  StatusChip
 *   OutlinedCard
 *   …
 * ```
 *
 * Tasks with [WorkTask.scheduledDate] == 0L (no date) are grouped under a
 * "날짜 미지정" bucket appended at the end.
 */
@Composable
fun ScheduleScreen(
    uiState: WorkUiState,
    onTaskClick: (taskId: String) -> Unit,
    onBack: () -> Unit,
) {
    // Group tasks: dated buckets sorted ascending, undated bucket last.
    val grouped: Map<LocalDate?, List<WorkTask>> = remember(uiState.tasks) {
        val dated   = uiState.tasks
            .filter { it.scheduledDate != 0L }
            .groupBy { task ->
                Instant.ofEpochMilli(task.scheduledDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .entries
            .sortedBy { (date, _) -> date }
            .associate { (k, v) -> (k as LocalDate?) to v }

        val undated = uiState.tasks.filter { it.scheduledDate == 0L }

        if (undated.isEmpty()) dated
        else dated + (null to undated)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("일정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (grouped.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.outline,
                        modifier           = Modifier
                            .padding(bottom = 12.dp)
                            .size(64.dp),
                    )
                    Text(
                        "일정이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            grouped.forEach { (date, dateTasks) ->
                // ── Sticky date header ────────────────────────────────────
                stickyHeader(key = date?.toString() ?: "undated") {
                    DateHeader(date = date)
                }
                // ── Task cards for this date ──────────────────────────────
                items(dateTasks, key = { it.id }) { task ->
                    ScheduleTaskCard(
                        task             = task,
                        organizationName = uiState.organizations
                            .firstOrNull { it.id == task.organizationId }?.name,
                        onClick          = { onTaskClick(task.id) },
                        modifier         = Modifier.padding(
                            horizontal = 16.dp,
                            vertical   = 4.dp,
                        ),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Date header chip  (sticky)
// ─────────────────────────────────────────────────────────────────────────────

private val DATE_FULL_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
private val DATE_SHORT_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM월 dd일")

@Composable
private fun DateHeader(date: LocalDate?) {
    val today    = LocalDate.now()
    val tomorrow = today.plusDays(1)

    val label = when {
        date == null      -> "날짜 미지정"
        date == today     -> "오늘  ·  ${date.format(DATE_SHORT_FMT)}"
        date == tomorrow  -> "내일  ·  ${date.format(DATE_SHORT_FMT)}"
        date.isBefore(today) -> "지남  ·  ${date.format(DATE_FULL_FMT)}"
        else              -> "${date.format(DATE_FULL_FMT)} (${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)})"
    }

    val containerColor = when {
        date == null          -> MaterialTheme.colorScheme.surfaceVariant
        date.isBefore(today)  -> MaterialTheme.colorScheme.errorContainer
        date == today         -> MaterialTheme.colorScheme.primaryContainer
        date == tomorrow      -> MaterialTheme.colorScheme.secondaryContainer
        else                  -> MaterialTheme.colorScheme.tertiaryContainer
    }

    // Full-width surface so the chip doesn't flicker when scrolling sticky
    Surface(
        color    = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = containerColor,
            ) {
                Text(
                    text     = label,
                    style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Schedule task card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScheduleTaskCard(
    task: WorkTask,
    organizationName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick  = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Coloured status bar
            Surface(
                shape    = MaterialTheme.shapes.extraSmall,
                color    = task.status.chipColor,
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp),
            ) {}

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = task.title,
                    style    = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!organizationName.isNullOrBlank()) {
                    Text(
                        text  = organizationName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
            StatusChip(status = task.status)
        }
    }
}
