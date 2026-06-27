@file:OptIn(ExperimentalMaterial3Api::class)

package com.ncportal.app.ui.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ncportal.app.data.SampleExplorerRepository
import com.ncportal.app.model.Entry
import com.ncportal.app.ui.formatSize
import com.ncportal.app.ui.iconForFile
import com.ncportal.app.ui.theme.NCPortalTheme

@Composable
fun ExplorerScreen(viewModel: ExplorerViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExplorerScreen(
        state = state,
        onOpen = viewModel::open,
        onBreadcrumb = viewModel::navigateTo,
        onUp = { viewModel.navigateUp() },
        onToggleView = viewModel::toggleViewMode,
    )
}

@Composable
private fun ExplorerScreen(
    state: ExplorerUiState,
    onOpen: (Entry) -> Unit,
    onBreadcrumb: (Int) -> Unit,
    onUp: () -> Unit,
    onToggleView: () -> Unit,
) {
    // Hardware/gesture back navigates up the tree until we reach the root.
    BackHandler(enabled = state.canNavigateUp, onBack = onUp)

    Column(modifier = Modifier.fillMaxSize()) {
        ExplorerTopBar(
            path = state.path,
            viewMode = state.viewMode,
            onBreadcrumb = onBreadcrumb,
            onToggleView = onToggleView,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                state.entries.isEmpty() -> EmptyFolder(Modifier.align(Alignment.Center))
                state.viewMode == ViewMode.LIST -> EntryList(state.entries, onOpen)
                else -> EntryGrid(state.entries, onOpen)
            }
        }
    }
}

@Composable
private fun ExplorerTopBar(
    path: List<Entry>,
    viewMode: ViewMode,
    onBreadcrumb: (Int) -> Unit,
    onToggleView: () -> Unit,
) {
    Surface(tonalElevation = 2.dp, shadowElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = path.firstOrNull()?.name ?: "NC Portal",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                IconButton(onClick = onToggleView) {
                    Icon(
                        imageVector = if (viewMode == ViewMode.LIST) Icons.Filled.GridView else Icons.Filled.ViewList,
                        contentDescription = "보기 방식 전환",
                    )
                }
            }
            Breadcrumbs(path = path, onBreadcrumb = onBreadcrumb)
        }
    }
}

@Composable
private fun Breadcrumbs(path: List<Entry>, onBreadcrumb: (Int) -> Unit) {
    val scroll = rememberScrollState()
    // Keep the deepest segment in view as the path grows.
    LaunchedEffect(path.size) { scroll.animateScrollTo(scroll.maxValue) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        path.forEachIndexed { index, entry ->
            if (index > 0) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
            }
            val isLast = index == path.lastIndex
            Text(
                text = entry.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = !isLast) { onBreadcrumb(index) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun EntryList(entries: List<Entry>, onOpen: (Entry) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(entries, key = { it.id }) { entry ->
            EntryRow(entry = entry, onClick = { onOpen(entry) })
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun EntryRow(entry: Entry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryIcon(entry, size = 40.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.subtitle(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (entry.isFolder) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun EntryGrid(entries: List<Entry>, onOpen: (Entry) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 112.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        gridItems(entries, key = { it.id }) { entry ->
            EntryGridItem(entry = entry, onClick = { onOpen(entry) })
        }
    }
}

@Composable
private fun EntryGridItem(entry: Entry, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.aspectRatio(1f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            EntryIcon(entry, size = 48.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EntryIcon(entry: Entry, size: Dp) {
    val tint = if (entry.isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    Icon(
        imageVector = if (entry.isFolder) Icons.Filled.Folder else iconForFile(entry.name),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(size),
    )
}

@Composable
private fun EmptyFolder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "비어 있는 폴더입니다",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Entry.subtitle(): String =
    if (isFolder) "${children.size}개 항목" else formatSize(sizeBytes)

@Preview(showBackground = true)
@Composable
private fun ExplorerScreenPreview() {
    val root = SampleExplorerRepository().root()
    NCPortalTheme {
        ExplorerScreen(
            state = ExplorerUiState(
                path = listOf(root),
                entries = root.children.sortedWith(
                    compareByDescending<Entry> { it.isFolder }.thenBy { it.name.lowercase() },
                ),
                viewMode = ViewMode.LIST,
            ),
            onOpen = {},
            onBreadcrumb = {},
            onUp = {},
            onToggleView = {},
        )
    }
}
