package com.ncportal.app.ui.work

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ncportal.app.data.OrganizationRepository
import com.ncportal.app.data.WorkTaskRepository
import com.ncportal.app.model.LogAction
import com.ncportal.app.model.Organization
import com.ncportal.app.model.TaskStatus
import com.ncportal.app.model.WorkLogEntry
import com.ncportal.app.model.WorkNavState
import com.ncportal.app.model.WorkSeedData
import com.ncportal.app.model.WorkTask
import com.ncportal.app.model.WorkUiState
import com.ncportal.app.model.generateId
import com.ncportal.app.model.nextStates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single ViewModel for the entire Work module.
 *
 * Extends [AndroidViewModel] to access [Application] context for file I/O
 * without leaking an Activity. The framework's default
 * [androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory] constructs
 * this class automatically — no custom factory is required.
 *
 * On first launch (no persisted files): seeds from [WorkSeedData] and
 * immediately persists the seed data. On subsequent launches: loads from disk.
 *
 * Mutations are optimistic: [uiState] updates instantly; disk persistence
 * happens in a background coroutine. Disk errors are logged but not surfaced.
 */
class WorkViewModel(application: Application) : AndroidViewModel(application) {

    private val taskRepo = WorkTaskRepository(application)
    private val orgRepo  = OrganizationRepository(application)

    private val _uiState = MutableStateFlow(WorkUiState())
    val uiState: StateFlow<WorkUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val tasks = taskRepo.loadAll()
            val orgs  = orgRepo.loadAll()
            if (tasks.isEmpty() && orgs.isEmpty()) {
                _uiState.update {
                    it.copy(
                        tasks         = WorkSeedData.tasks,
                        organizations = WorkSeedData.organizations,
                    )
                }
                runCatching {
                    taskRepo.saveAll(WorkSeedData.tasks)
                    orgRepo.saveAll(WorkSeedData.organizations)
                }.onFailure { Log.e(TAG, "Failed to persist seed data", it) }
            } else {
                _uiState.update { it.copy(tasks = tasks, organizations = orgs) }
            }
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    fun navigateTo(state: WorkNavState) {
        _uiState.update { it.copy(navState = state) }
    }

    fun setStatusFilter(filter: TaskStatus?) {
        _uiState.update { it.copy(statusFilter = filter) }
    }

    // ── Task queries ───────────────────────────────────────────────────────────

    fun getTask(taskId: String): WorkTask? =
        _uiState.value.tasks.firstOrNull { it.id == taskId }

    // ── Task mutations ─────────────────────────────────────────────────────────

    fun createTask(
        title: String,
        organizationId: String,
        requester: String,
        contact: String,
        scheduledDate: Long,
        workDescription: String,
        notes: String,
    ) {
        val now = System.currentTimeMillis()
        val task = WorkTask(
            id              = generateId(),
            title           = title,
            organizationId  = organizationId,
            requester       = requester,
            contact         = contact,
            scheduledDate   = scheduledDate,
            status          = TaskStatus.PENDING,
            workDescription = workDescription,
            notes           = notes,
            workLogs        = listOf(
                WorkLogEntry(
                    id          = generateId(),
                    timestamp   = now,
                    action      = LogAction.CREATED,
                    description = "업무가 등록됨",
                ),
            ),
            createdAt = now,
            updatedAt = now,
        )
        _uiState.update { it.copy(tasks = it.tasks + task) }
        persistTasks()
    }

    fun updateTask(
        taskId: String,
        title: String,
        organizationId: String,
        requester: String,
        contact: String,
        scheduledDate: Long,
        workDescription: String,
        notes: String,
    ) {
        _uiState.update { state ->
            state.copy(
                tasks = state.tasks.map { t ->
                    if (t.id != taskId) t
                    else t.copy(
                        title           = title,
                        organizationId  = organizationId,
                        requester       = requester,
                        contact         = contact,
                        scheduledDate   = scheduledDate,
                        workDescription = workDescription,
                        notes           = notes,
                        updatedAt       = System.currentTimeMillis(),
                    )
                },
            )
        }
        persistTasks()
    }

    fun deleteTask(taskId: String) {
        _uiState.update { it.copy(tasks = it.tasks.filter { t -> t.id != taskId }) }
        persistTasks()
    }

    fun updateStatus(taskId: String, newStatus: TaskStatus) {
        val current = getTask(taskId) ?: return
        if (newStatus !in current.status.nextStates()) return

        val now = System.currentTimeMillis()
        val action = when (newStatus) {
            TaskStatus.IN_PROGRESS -> LogAction.STARTED
            TaskStatus.COMPLETED   -> LogAction.COMPLETED
            TaskStatus.REVISIT     -> LogAction.REVISIT_SCHEDULED
            TaskStatus.PENDING     -> LogAction.CREATED
        }
        val entry = WorkLogEntry(
            id          = generateId(),
            timestamp   = now,
            action      = action,
            description = "상태가 '${newStatus.label}'(으)로 변경됨",
        )
        _uiState.update { state ->
            state.copy(
                tasks = state.tasks.map { t ->
                    if (t.id != taskId) t
                    else t.copy(
                        status    = newStatus,
                        workLogs  = t.workLogs + entry,
                        updatedAt = now,
                    )
                },
            )
        }
        persistTasks()
    }

    fun addNote(taskId: String, note: String) {
        if (note.isBlank()) return
        val now = System.currentTimeMillis()
        val entry = WorkLogEntry(
            id          = generateId(),
            timestamp   = now,
            action      = LogAction.NOTE_ADDED,
            description = note.trim(),
        )
        _uiState.update { state ->
            state.copy(
                tasks = state.tasks.map { t ->
                    if (t.id != taskId) t
                    else t.copy(
                        workLogs  = t.workLogs + entry,
                        updatedAt = now,
                    )
                },
            )
        }
        persistTasks()
    }

    // ── Organisation mutations ─────────────────────────────────────────────────

    fun addOrganization(name: String, contact: String, memo: String = "") {
        val org = Organization(
            id      = generateId(),
            name    = name.trim(),
            contact = contact.trim(),
            memo    = memo.trim(),
        )
        _uiState.update { it.copy(organizations = it.organizations + org) }
        persistOrgs()
    }

    fun updateOrganization(id: String, name: String, contact: String, memo: String = "") {
        _uiState.update { state ->
            state.copy(
                organizations = state.organizations.map { org ->
                    if (org.id != id) org
                    else org.copy(name = name.trim(), contact = contact.trim(), memo = memo.trim())
                },
            )
        }
        persistOrgs()
    }

    fun deleteOrganization(orgId: String) {
        _uiState.update { state ->
            state.copy(organizations = state.organizations.filter { it.id != orgId })
        }
        persistOrgs()
    }

    // ── Persistence helpers ────────────────────────────────────────────────────

    private fun persistTasks() {
        val snapshot = _uiState.value.tasks
        viewModelScope.launch {
            runCatching { taskRepo.saveAll(snapshot) }
                .onFailure { Log.e(TAG, "persistTasks failed", it) }
        }
    }

    private fun persistOrgs() {
        val snapshot = _uiState.value.organizations
        viewModelScope.launch {
            runCatching { orgRepo.saveAll(snapshot) }
                .onFailure { Log.e(TAG, "persistOrgs failed", it) }
        }
    }

    private companion object {
        const val TAG = "WorkViewModel"
    }
}
