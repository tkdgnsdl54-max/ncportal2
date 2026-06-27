package com.ncportal.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Human-readable byte size, e.g. 1536 -> "1.5 KB". Shared across screens. */
fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return "%.1f %s".format(value, units[index])
}

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.systemDefault())

/** Epoch millis -> "2026.06.27". Returns "" for non-positive (unknown) timestamps. */
fun formatDate(epochMillis: Long): String =
    if (epochMillis <= 0L) "" else DATE_FORMATTER.format(Instant.ofEpochMilli(epochMillis))

/** Picks a Material icon for a file based on its extension. */
fun iconForFile(name: String): ImageVector =
    when (name.substringAfterLast('.', "").lowercase()) {
        "png", "jpg", "jpeg", "gif", "webp", "bmp" -> Icons.Filled.Image
        "mp4", "mkv", "mov", "avi", "webm" -> Icons.Filled.Movie
        "mp3", "wav", "flac", "aac", "ogg" -> Icons.Filled.MusicNote
        "pdf" -> Icons.Filled.PictureAsPdf
        "xls", "xlsx", "csv" -> Icons.Filled.TableChart
        "zip", "tar", "gz", "rar", "7z" -> Icons.Filled.FolderZip
        "md" -> Icons.Filled.Article
        else -> Icons.Filled.Description
    }
