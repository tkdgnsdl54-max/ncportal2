package com.ncportal.app.ui.transfers

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ncportal.app.ui.components.SectionTopBar
import com.ncportal.app.ui.formatSize

private enum class TransferStatus { UPLOADING, DOWNLOADING, COMPLETED, QUEUED }

private data class TransferItem(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val progress: Float,
    val status: TransferStatus,
)

@Composable
fun TransfersScreen() {
    val items = sampleTransfers()
    Column(modifier = Modifier.fillMaxSize()) {
        SectionTopBar(title = "전송")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { item ->
                TransferRow(item)
            }
        }
    }
}

@Composable
private fun TransferRow(item: TransferItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = item.status.icon(),
                contentDescription = null,
                tint = if (item.status == TransferStatus.COMPLETED) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.status.statusLabel(item.sizeBytes, item.progress),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.status == TransferStatus.UPLOADING || item.status == TransferStatus.DOWNLOADING) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                    )
                }
            }
        }
    }
}

private fun TransferStatus.icon(): ImageVector = when (this) {
    TransferStatus.UPLOADING -> Icons.Filled.CloudUpload
    TransferStatus.DOWNLOADING -> Icons.Filled.CloudDownload
    TransferStatus.COMPLETED -> Icons.Filled.CheckCircle
    TransferStatus.QUEUED -> Icons.Filled.Schedule
}

private fun TransferStatus.statusLabel(sizeBytes: Long, progress: Float): String {
    val size = formatSize(sizeBytes)
    val pct = (progress * 100).toInt()
    return when (this) {
        TransferStatus.UPLOADING -> "업로드 중 · $pct% · $size"
        TransferStatus.DOWNLOADING -> "다운로드 중 · $pct% · $size"
        TransferStatus.COMPLETED -> "완료 · $size"
        TransferStatus.QUEUED -> "대기 중 · $size"
    }
}

// Design-only sample data; replaced when a real transfer queue is wired up.
private fun sampleTransfers(): List<TransferItem> = listOf(
    TransferItem("t1", "소개영상.mp4", 24_800_000, 0.62f, TransferStatus.UPLOADING),
    TransferItem("t2", "백업.zip", 58_900_000, 0.30f, TransferStatus.DOWNLOADING),
    TransferItem("t3", "사양서.pdf", 482_133, 1f, TransferStatus.COMPLETED),
    TransferItem("t4", "2026-2분기.xlsx", 88_400, 0f, TransferStatus.QUEUED),
)
