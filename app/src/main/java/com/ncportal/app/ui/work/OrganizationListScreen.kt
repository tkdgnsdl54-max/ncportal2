@file:OptIn(ExperimentalMaterial3Api::class)

package com.ncportal.app.ui.work

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ncportal.app.model.Organization
import com.ncportal.app.model.WorkUiState

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Organisation master list.
 *
 * - FAB opens [OrgEditDialog] for creating a new organisation
 * - Tap the edit icon on a [ListItem] opens [OrgEditDialog] pre-filled for that org
 * - Swipe left ([SwipeToDismissBox]) deletes an org
 */
@Composable
fun OrganizationListScreen(
    uiState: WorkUiState,
    onBack: () -> Unit,
    onAddOrg: (name: String, contact: String, memo: String) -> Unit,
    onUpdateOrg: (id: String, name: String, contact: String, memo: String) -> Unit,
    onDeleteOrg: (id: String) -> Unit,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    // null = no dialog; non-null = editing that org
    var editingOrg by remember { mutableStateOf<Organization?>(null) }

    if (showAddDialog) {
        OrgEditDialog(
            initial   = null,
            onConfirm = { name, contact, memo ->
                onAddOrg(name, contact, memo)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editingOrg?.let { org ->
        OrgEditDialog(
            initial   = org,
            onConfirm = { name, contact, memo ->
                onUpdateOrg(org.id, name, contact, memo)
                editingOrg = null
            },
            onDismiss = { editingOrg = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("기관 관리") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "기관 추가")
            }
        },
    ) { innerPadding ->
        if (uiState.organizations.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Business, null,
                        tint     = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "등록된 기관이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier       = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(uiState.organizations, key = { it.id }) { org ->
                    OrgSwipeItem(
                        org      = org,
                        onEdit   = { editingOrg = org },
                        onDelete = { onDeleteOrg(org.id) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color    = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Swipe-to-delete org item
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Wraps a [ListItem] inside [SwipeToDismissBox].
 *
 * Only **right-to-left** swipe (EndToStart) triggers deletion.
 * The red background with a [Icons.Filled.Delete] icon is revealed as the
 * foreground content slides away.
 */
@Composable
private fun OrgSwipeItem(
    org: Organization,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.40f },
    )

    SwipeToDismissBox(
        state                   = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent       = {
            // Red delete background (only visible when swiping EndToStart)
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector        = Icons.Filled.Delete,
                    contentDescription = "삭제",
                    tint               = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        // Foreground content
        ListItem(
            headlineContent   = {
                Text(
                    org.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            supportingContent = when {
                org.contact.isNotBlank() && org.memo.isNotBlank() ->
                    { { Text("${org.contact}  ·  ${org.memo}") } }
                org.contact.isNotBlank() -> { { Text(org.contact) } }
                org.memo.isNotBlank()    -> { { Text(org.memo) } }
                else                     -> null
            },
            leadingContent    = {
                Icon(
                    Icons.Filled.Business,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                )
            },
            trailingContent   = {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "수정",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            colors            = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add / Edit dialog
// ─────────────────────────────────────────────────────────────────────────────

/**
 * [AlertDialog] for adding or editing an [Organization].
 *
 * Fields: 기관명 (required), 연락처, 메모
 * [initial] = null → "기관 추가" mode; non-null → "기관 수정" mode
 */
@Composable
private fun OrgEditDialog(
    initial: Organization?,
    onConfirm: (name: String, contact: String, memo: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name      by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var contact   by rememberSaveable { mutableStateOf(initial?.contact ?: "") }
    var memo      by rememberSaveable { mutableStateOf(initial?.memo ?: "") }
    var nameError by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon             = { Icon(Icons.Filled.Business, contentDescription = null) },
        title            = { Text(if (initial == null) "기관 추가" else "기관 수정") },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value          = name,
                    onValueChange  = { name = it; nameError = false },
                    label          = { Text("기관명 *") },
                    singleLine     = true,
                    isError        = nameError,
                    supportingText = if (nameError) {
                        { Text("기관명을 입력하세요") }
                    } else null,
                    modifier       = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value           = contact,
                    onValueChange   = { contact = it },
                    label           = { Text("연락처") },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier        = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = memo,
                    onValueChange = { memo = it },
                    label         = { Text("메모") },
                    minLines      = 2,
                    maxLines      = 4,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton    = {
            TextButton(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@TextButton }
                    onConfirm(name.trim(), contact.trim(), memo.trim())
                },
            ) { Text(if (initial == null) "추가" else "저장") }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}
