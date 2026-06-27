package com.ncportal.app.ui.explorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncportal.app.data.ExplorerRepository
import com.ncportal.app.data.SampleExplorerRepository
import com.ncportal.app.model.Entry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Holds the explorer's navigation state as a stack of folders and exposes a
 * single [uiState] for the screen to render. Swap the repository for a real
 * data source when one exists.
 */
class ExplorerViewModel : ViewModel() {

    private val repository: ExplorerRepository = SampleExplorerRepository()

    // index 0 == root, last element == the currently open folder.
    private val pathStack = MutableStateFlow(listOf(repository.root()))
    private val viewMode = MutableStateFlow(ViewMode.LIST)

    val uiState: StateFlow<ExplorerUiState> =
        combine(pathStack, viewMode) { stack, mode -> buildState(stack, mode) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = buildState(pathStack.value, viewMode.value),
            )

    /** Open a folder (descend into it). Files are a no-op for now — hook actions here later. */
    fun open(entry: Entry) {
        if (entry.isFolder) {
            pathStack.value = pathStack.value + entry
        }
    }

    /** Jump to a breadcrumb segment by its index in the current path. */
    fun navigateTo(index: Int) {
        val stack = pathStack.value
        if (index in stack.indices && index != stack.lastIndex) {
            pathStack.value = stack.subList(0, index + 1)
        }
    }

    /** Go up one level. Returns false if already at the root. */
    fun navigateUp(): Boolean {
        val stack = pathStack.value
        if (stack.size <= 1) return false
        pathStack.value = stack.dropLast(1)
        return true
    }

    fun toggleViewMode() {
        viewMode.value = if (viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
    }

    private fun buildState(stack: List<Entry>, mode: ViewMode) = ExplorerUiState(
        path = stack,
        entries = stack.last().children.sortedWith(EntryOrder),
        viewMode = mode,
    )

    private companion object {
        // Folders first, then files; case-insensitive alphabetical within each group.
        val EntryOrder: Comparator<Entry> =
            compareByDescending<Entry> { it.isFolder }.thenBy { it.name.lowercase() }
    }
}
