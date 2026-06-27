package com.ncportal.app.ui.home

import androidx.lifecycle.ViewModel
import com.ncportal.app.data.ExplorerRepository
import com.ncportal.app.data.SampleExplorerRepository
import com.ncportal.app.model.Entry

/** Dashboard snapshot derived from the explorer tree. */
data class HomeUiState(
    val storageUsedBytes: Long = 0L,
    val storageTotalBytes: Long = 1L,
    val fileCount: Int = 0,
    val folderCount: Int = 0,
    val recentFiles: List<Entry> = emptyList(),
) {
    val storageFraction: Float
        get() = if (storageTotalBytes <= 0L) 0f
        else (storageUsedBytes.toFloat() / storageTotalBytes.toFloat()).coerceIn(0f, 1f)
}

/**
 * Computes the home dashboard from the same [ExplorerRepository] the file tab uses.
 * No-arg constructor so the default `viewModel()` factory can build it.
 */
class HomeViewModel : ViewModel() {

    private val repository: ExplorerRepository = SampleExplorerRepository()

    val uiState: HomeUiState = buildState()

    private fun buildState(): HomeUiState {
        val files = mutableListOf<Entry>()
        var folderCount = 0

        fun walk(entry: Entry) {
            for (child in entry.children) {
                if (child.isFolder) {
                    folderCount++
                    walk(child)
                } else {
                    files += child
                }
            }
        }
        walk(repository.root())

        val totalCapacity = 128L * 1024 * 1024 * 1024 // 128 GB sample capacity
        return HomeUiState(
            storageUsedBytes = files.sumOf { it.sizeBytes },
            storageTotalBytes = totalCapacity,
            fileCount = files.size,
            folderCount = folderCount,
            recentFiles = files.sortedByDescending { it.modifiedAt }.take(5),
        )
    }
}
