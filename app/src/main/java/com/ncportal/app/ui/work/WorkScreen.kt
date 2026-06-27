@file:OptIn(ExperimentalMaterial3Api::class)

package com.ncportal.app.ui.work

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ncportal.app.model.WorkNavState
import com.ncportal.app.model.WorkUiState

/**
 * Root composable for the **Work** tab.
 *
 * Owns the [WorkViewModel] and wires the [WorkNavState] machine:
 *
 * ```
 * TaskList ←→ TaskForm (create / edit)
 *          ←→ TaskDetail
 *          ←→ OrgList
 *          ←→ Schedule → TaskDetail
 * ```
 *
 * The system back button always pops one level toward [WorkNavState.TaskList].
 * From [TaskList], back is handled by the parent (NCPortalApp tab back).
 */
@Composable
fun WorkScreen(viewModel: WorkViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Single-level back: any sub-screen → TaskList.
    // Deeper stack (e.g. Detail → back to Schedule) is intentionally not modelled:
    // the spec says "detail/form → list → tab-level back".
    BackHandler(enabled = uiState.navState !is WorkNavState.TaskList) {
        viewModel.navigateTo(WorkNavState.TaskList)
    }

    when (val nav = uiState.navState) {
        // ── Task list ───────────────────────────────────────────────────────
        is WorkNavState.TaskList -> WorkTaskListScreen(
            uiState        = uiState,
            onCreateTask   = { viewModel.navigateTo(WorkNavState.TaskForm()) },
            onTaskClick    = { id -> viewModel.navigateTo(WorkNavState.TaskDetail(id)) },
            onOrgList      = { viewModel.navigateTo(WorkNavState.OrgList) },
            onSchedule     = { viewModel.navigateTo(WorkNavState.Schedule) },
            onFilterChange = viewModel::setStatusFilter,
        )

        // ── Create / Edit form ──────────────────────────────────────────────
        is WorkNavState.TaskForm -> {
            val editingTask = nav.taskId?.let { id -> uiState.tasks.firstOrNull { it.id == id } }
            WorkTaskFormScreen(
                editingTask   = editingTask,
                organizations = uiState.organizations,
                onSubmit      = { data ->
                    if (nav.taskId == null) {
                        viewModel.createTask(
                            title           = data.title,
                            organizationId  = data.organizationId,
                            requester       = data.requester,
                            contact         = data.contact,
                            scheduledDate   = data.scheduledDate,
                            workDescription = data.workDescription,
                            notes           = data.notes,
                        )
                        viewModel.navigateTo(WorkNavState.TaskList)
                    } else {
                        viewModel.updateTask(
                            taskId          = nav.taskId,
                            title           = data.title,
                            organizationId  = data.organizationId,
                            requester       = data.requester,
                            contact         = data.contact,
                            scheduledDate   = data.scheduledDate,
                            workDescription = data.workDescription,
                            notes           = data.notes,
                        )
                        viewModel.navigateTo(WorkNavState.TaskDetail(nav.taskId))
                    }
                },
                onCancel = {
                    viewModel.navigateTo(
                        if (nav.taskId == null) WorkNavState.TaskList
                        else WorkNavState.TaskDetail(nav.taskId),
                    )
                },
            )
        }

        // ── Task detail ─────────────────────────────────────────────────────
        is WorkNavState.TaskDetail -> {
            val task = uiState.tasks.firstOrNull { it.id == nav.taskId }
            WorkTaskDetailScreen(
                task           = task,
                organizations  = uiState.organizations,
                onBack         = { viewModel.navigateTo(WorkNavState.TaskList) },
                onEdit         = { viewModel.navigateTo(WorkNavState.TaskForm(nav.taskId)) },
                onStatusChange = { newStatus -> viewModel.updateStatus(nav.taskId, newStatus) },
                onAddNote      = { note -> viewModel.addNote(nav.taskId, note) },
            )
        }

        // ── Organisation list ───────────────────────────────────────────────
        is WorkNavState.OrgList -> OrganizationListScreen(
            uiState     = uiState,
            onBack      = { viewModel.navigateTo(WorkNavState.TaskList) },
            onAddOrg    = { name, contact, memo -> viewModel.addOrganization(name, contact, memo) },
            onUpdateOrg = { id, name, contact, memo -> viewModel.updateOrganization(id, name, contact, memo) },
            onDeleteOrg = viewModel::deleteOrganization,
        )

        // ── Schedule ────────────────────────────────────────────────────────
        is WorkNavState.Schedule -> ScheduleScreen(
            uiState     = uiState,
            onTaskClick = { id -> viewModel.navigateTo(WorkNavState.TaskDetail(id)) },
            onBack      = { viewModel.navigateTo(WorkNavState.TaskList) },
        )
    }
}
