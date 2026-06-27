package com.ncportal.app.ui.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ncportal.app.model.Entry
import com.ncportal.app.ui.components.SectionTopBar
import com.ncportal.app.ui.formatDate
import com.ncportal.app.ui.formatSize
import com.ncportal.app.ui.iconForFile

@Composable
fun HomeScreen(
    onNavigateToFiles: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state = viewModel.uiState
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        SectionTopBar(title = "NC Portal")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StorageCard(
                usedBytes = state.storageUsedBytes,
                totalBytes = state.storageTotalBytes,
                fraction = state.storageFraction,
                fileCount = state.fileCount,
                folderCount = state.folderCount,
            )
            QuickActions(
                onUpload = { context.toast("업로드는 곧 지원됩니다") },
                onNewFolder = { context.toast("새 폴더는 곧 지원됩니다") },
                onSearch = onNavigateToFiles,
            )
            RecentFilesCard(
                files = state.recentFiles,
                onSeeAll = onNavigateToFiles,
                onClickFile = { context.toast("파일 열기는 곧 지원됩니다") },
            )
        }
    }
}

@Composable
private fun StorageCard(
    usedBytes: Long,
    totalBytes: Long,
    fraction: Float,
    fileCount: Int,
    folderCount: Int,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(text = "저장 공간", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${formatSize(usedBytes)} / ${formatSize(totalBytes)} 사용",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "폴더 ${folderCount}개 · 파일 ${fileCount}개 · 샘플 데이터",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickActions(
    onUpload: () -> Unit,
    onNewFolder: () -> Unit,
    onSearch: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickAction(Icons.Filled.CloudUpload, "업로드", onUpload, Modifier.weight(1f))
        QuickAction(Icons.Filled.CreateNewFolder, "새 폴더", onNewFolder, Modifier.weight(1f))
        QuickAction(Icons.Filled.Search, "검색", onSearch, Modifier.weight(1f))
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 14.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun RecentFilesCard(
    files: List<Entry>,
    onSeeAll: () -> Unit,
    onClickFile: (Entry) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "최근 파일",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSeeAll) { Text("모두 보기") }
            }
            if (files.isEmpty()) {
                Text(
                    text = "최근 파일이 없습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            } else {
                for (file in files) {
                    RecentFileRow(file = file, onClick = { onClickFile(file) })
                }
            }
        }
    }
}

@Composable
private fun RecentFileRow(file: Entry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconForFile(file.name),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val date = formatDate(file.modifiedAt)
            Text(
                text = if (date.isEmpty()) formatSize(file.sizeBytes)
                else "$date · ${formatSize(file.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun android.content.Context.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
