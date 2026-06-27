package com.ncportal.app.model

import java.util.concurrent.atomic.AtomicLong

// ─────────────────────────────────────────────────────────────────────────────
// ID generation  (no UUID / external dependency)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Process-lifetime monotonic counter used by [generateId].
 * AtomicLong is bundled with the JDK — zero new dependencies.
 */
private val _idSeq = AtomicLong(0L)

/**
 * Generates a locally unique string ID without the UUID library.
 *
 * Format: `<epoch_ms>-<4-digit-sequence>`
 *
 * The epoch component guarantees rough time-ordering; the counter prevents
 * collisions when multiple IDs are generated in the same millisecond.
 * IDs are NOT globally unique across devices — use only for local storage.
 */
fun generateId(): String {
    val ts  = System.currentTimeMillis()
    val seq = _idSeq.incrementAndGet()
    return "$ts-${seq.toString().padStart(4, '0')}"
}

// ─────────────────────────────────────────────────────────────────────────────
// TaskStatus
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Lifecycle state of a [WorkTask].
 *
 * @param label Korean UI label shown in chips / badges
 * @param color Hex color string (ARGB, Material 500-weight palette)
 */
enum class TaskStatus(val label: String, val color: String) {
    PENDING    ("대기중", "#9E9E9E"),   // Grey 500
    IN_PROGRESS("진행중", "#2196F3"),   // Blue 500
    COMPLETED  ("완료",   "#4CAF50"),   // Green 500
    REVISIT    ("재방문", "#FF9800"),   // Orange 500
}

/**
 * State-machine extension — returns the valid next states reachable from [this].
 *
 * Transition table:
 * ```
 * PENDING     → IN_PROGRESS, REVISIT
 * IN_PROGRESS → COMPLETED, REVISIT, PENDING
 * COMPLETED   → REVISIT
 * REVISIT     → IN_PROGRESS, PENDING
 * ```
 *
 * Usage in the UI: show only the buttons returned by this function when the
 * user tries to change a task's status.
 */
fun TaskStatus.nextStates(): List<TaskStatus> = when (this) {
    TaskStatus.PENDING     -> listOf(TaskStatus.IN_PROGRESS, TaskStatus.REVISIT)
    TaskStatus.IN_PROGRESS -> listOf(TaskStatus.COMPLETED, TaskStatus.REVISIT, TaskStatus.PENDING)
    TaskStatus.COMPLETED   -> listOf(TaskStatus.REVISIT)
    TaskStatus.REVISIT     -> listOf(TaskStatus.IN_PROGRESS, TaskStatus.PENDING)
}

// ─────────────────────────────────────────────────────────────────────────────
// LogAction
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Describes the kind of event recorded in a [WorkLogEntry].
 *
 * @param label Korean display name shown in the audit-log timeline
 */
enum class LogAction(val label: String) {
    CREATED           ("생성됨"),
    STARTED           ("시작됨"),
    COMPLETED         ("완료됨"),
    REVISIT_SCHEDULED ("재방문 예정"),
    NOTE_ADDED        ("메모 추가"),
}

// ─────────────────────────────────────────────────────────────────────────────
// WorkLogEntry
// ─────────────────────────────────────────────────────────────────────────────

/**
 * An immutable audit-log record that captures a single event on a [WorkTask].
 *
 * Log entries are append-only: never mutate or delete them, only prepend/append
 * new ones to [WorkTask.workLogs].
 *
 * @param id          Generated via [generateId]
 * @param timestamp   Epoch-ms when the action occurred
 * @param action      Type of event
 * @param description Human-readable detail (may be empty for auto-generated entries)
 */
data class WorkLogEntry(
    val id: String,
    val timestamp: Long,
    val action: LogAction,
    val description: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// Organization
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A client organisation / site.  Multiple [WorkTask]s can reference the same
 * organisation via [WorkTask.organizationId].
 *
 * @param id      Stable identifier generated via [generateId]
 * @param contact Main phone number for the organisation (not the requester)
 * @param memo    Free-form notes about the organisation itself
 */
data class Organization(
    val id: String,
    val name: String,
    val contact: String,
    val memo: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// WorkTask
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Core domain object representing one unit of field / support work.
 *
 * @param id              Stable identifier; generated via [generateId]
 * @param organizationId  References [Organization.id]; **empty string** means
 *                        the task is not linked to any organisation
 * @param requester       Name of the person who requested the work
 * @param contact         Direct phone number for the requester
 * @param scheduledDate   Epoch-ms of the planned visit date; **0L** means not set
 * @param workDescription Detailed description of the work to be done / done
 * @param workLogs        Ordered audit trail; the first entry is the oldest
 * @param notes           General free-form memo unrelated to the audit trail
 * @param createdAt       Epoch-ms when this task was first created
 * @param updatedAt       Epoch-ms of the last mutation (status change, note edit, …)
 */
data class WorkTask(
    val id: String,
    val title: String,
    val organizationId: String,
    val requester: String,
    val contact: String,
    val scheduledDate: Long,
    val status: TaskStatus,
    val workDescription: String = "",
    val workLogs: List<WorkLogEntry>,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
)

// ─────────────────────────────────────────────────────────────────────────────
// Navigation state
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Models the navigation stack for the Work module as a sealed class so the
 * compiler enforces exhaustive `when` expressions in the UI.
 *
 * - [TaskList]   — default landing screen showing all tasks
 * - [TaskDetail] — read-only detail view for one task
 * - [TaskForm]   — create form when [taskId] is null; edit form otherwise
 * - [OrgList]    — organisation master list
 * - [Schedule]   — tasks grouped by scheduled date
 */
sealed class WorkNavState {
    object TaskList : WorkNavState()
    data class TaskDetail(val taskId: String) : WorkNavState()
    data class TaskForm(val taskId: String? = null) : WorkNavState()
    object OrgList : WorkNavState()
    object Schedule : WorkNavState()
}

// ─────────────────────────────────────────────────────────────────────────────
// UI state  (ViewModel → Screen / Composable)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Single source of truth emitted by the Work ViewModel as a [kotlinx.coroutines.flow.StateFlow].
 *
 * Immutable; every update produces a new copy via `copy(…)`.
 *
 * @param statusFilter Active filter chip value; **null** means "show all statuses"
 * @param navState     Current navigation destination inside the Work module
 */
data class WorkUiState(
    val tasks: List<WorkTask> = emptyList(),
    val organizations: List<Organization> = emptyList(),
    val statusFilter: TaskStatus? = null,
    val navState: WorkNavState = WorkNavState.TaskList,
)

// ─────────────────────────────────────────────────────────────────────────────
// Seed / sample data
// ─────────────────────────────────────────────────────────────────────────────

/** Convenience: epoch-ms offset from now by [days]. Negative = past, positive = future. */
private fun epochOffset(days: Int = 0): Long =
    System.currentTimeMillis() + days.toLong() * 86_400_000L

/**
 * Hardcoded seed data for first-launch defaults and development previews.
 *
 * Includes three organisations (A기관, B기관, C기관) and four sample tasks
 * that together cover every [TaskStatus] value.
 *
 * IDs are stable string literals (not [generateId]) so seed data is
 * idempotent across restarts.
 */
object WorkSeedData {

    // ── Organisations ──────────────────────────────────────────────────────

    val organizations: List<Organization> = listOf(
        Organization(
            id      = "org-001",
            name    = "A기관",
            contact = "031-100-0001",
            memo    = "서울 본사 담당 기관. 보안 구역 출입증 사전 신청 필요.",
        ),
        Organization(
            id      = "org-002",
            name    = "B기관",
            contact = "031-200-0002",
            memo    = "경기 북부 지역 기관. 주차 공간 협소 — 대중교통 권장.",
        ),
        Organization(
            id      = "org-003",
            name    = "C기관",
            contact = "031-300-0003",
            memo    = "인천 지역 기관. 오전 9시~17시 방문 가능.",
        ),
    )

    // ── Tasks ──────────────────────────────────────────────────────────────

    val tasks: List<WorkTask> = listOf(

        // ── 1. PENDING — waiting to be started ──
        WorkTask(
            id             = "task-001",
            title          = "A기관 네트워크 장비 점검 요청",
            organizationId = "org-001",
            requester      = "홍길동",
            contact        = "010-1234-5678",
            scheduledDate  = epochOffset(+3),
            status         = TaskStatus.PENDING,
            workLogs       = listOf(
                WorkLogEntry(
                    id          = "log-001-a",
                    timestamp   = epochOffset(-1),
                    action      = LogAction.CREATED,
                    description = "A기관 담당자 요청으로 작업 등록",
                ),
            ),
            notes          = "방문 전 담당자(홍길동)에게 사전 연락 필요. 출입증 신청: 2일 전.",
            createdAt      = epochOffset(-1),
            updatedAt      = epochOffset(-1),
        ),

        // ── 2. IN_PROGRESS — currently being worked on ──
        WorkTask(
            id             = "task-002",
            title          = "B기관 업무용 소프트웨어 설치 지원",
            organizationId = "org-002",
            requester      = "김철수",
            contact        = "010-9876-5432",
            scheduledDate  = epochOffset(0),
            status         = TaskStatus.IN_PROGRESS,
            workLogs       = listOf(
                WorkLogEntry(
                    id          = "log-002-a",
                    timestamp   = epochOffset(-2),
                    action      = LogAction.CREATED,
                    description = "B기관 소프트웨어 설치 요청 접수",
                ),
                WorkLogEntry(
                    id          = "log-002-b",
                    timestamp   = epochOffset(0),
                    action      = LogAction.STARTED,
                    description = "현장 방문 시작 — 설치 진행 중",
                ),
            ),
            notes          = "설치 파일 USB 지참. 라이선스 키: 별도 이메일 확인.",
            createdAt      = epochOffset(-2),
            updatedAt      = epochOffset(0),
        ),

        // ── 3. COMPLETED — fully done ──
        WorkTask(
            id             = "task-003",
            title          = "C기관 사무용 프린터 교체",
            organizationId = "org-003",
            requester      = "이영희",
            contact        = "010-5555-1234",
            scheduledDate  = epochOffset(-5),
            status         = TaskStatus.COMPLETED,
            workLogs       = listOf(
                WorkLogEntry(
                    id          = "log-003-a",
                    timestamp   = epochOffset(-7),
                    action      = LogAction.CREATED,
                    description = "C기관 프린터 교체 요청 등록",
                ),
                WorkLogEntry(
                    id          = "log-003-b",
                    timestamp   = epochOffset(-5),
                    action      = LogAction.STARTED,
                    description = "현장 방문 시작 — 기존 프린터 분리",
                ),
                WorkLogEntry(
                    id          = "log-003-c",
                    timestamp   = epochOffset(-5),
                    action      = LogAction.COMPLETED,
                    description = "신규 프린터 설치 및 인쇄 테스트 정상 완료",
                ),
            ),
            notes          = "교체된 구형 프린터 수거 완료. 폐기 처리 요망.",
            createdAt      = epochOffset(-7),
            updatedAt      = epochOffset(-5),
        ),

        // ── 4. REVISIT — needs a follow-up visit ──
        WorkTask(
            id             = "task-004",
            title          = "A기관 서버실 스토리지 교체 재방문",
            organizationId = "org-001",
            requester      = "박민준",
            contact        = "010-3333-7777",
            scheduledDate  = epochOffset(+7),
            status         = TaskStatus.REVISIT,
            workLogs       = listOf(
                WorkLogEntry(
                    id          = "log-004-a",
                    timestamp   = epochOffset(-10),
                    action      = LogAction.CREATED,
                    description = "서버실 SSD 교체 요청 등록",
                ),
                WorkLogEntry(
                    id          = "log-004-b",
                    timestamp   = epochOffset(-8),
                    action      = LogAction.STARTED,
                    description = "1차 방문 — 현장 실사 및 규격 확인",
                ),
                WorkLogEntry(
                    id          = "log-004-c",
                    timestamp   = epochOffset(-8),
                    action      = LogAction.REVISIT_SCHEDULED,
                    description = "부품(SSD) 조달 필요 — 7일 후 재방문 예정으로 상태 변경",
                ),
                WorkLogEntry(
                    id          = "log-004-d",
                    timestamp   = epochOffset(-7),
                    action      = LogAction.NOTE_ADDED,
                    description = "담당자 확인: 부품 발주 완료, 납기 약 6일 예정",
                ),
            ),
            notes          = "SSD 부품 입고 확인 후 방문 일정 최종 확정. 담당자 연락 선행.",
            createdAt      = epochOffset(-10),
            updatedAt      = epochOffset(-7),
        ),
    )

    /** Pre-built [WorkUiState] for preview Composables and first-launch. */
    val initialUiState: WorkUiState = WorkUiState(
        tasks         = tasks,
        organizations = organizations,
    )
}
