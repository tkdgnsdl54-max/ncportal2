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

class ExplorerViewModel : ViewModel() {

    private val repository: ExplorerRepository = SampleExplorerRepository()

    private val pathStack = MutableStateFlow(listOf(repository.root()))
    private val viewMode = MutableStateFlow(ViewMode.LIST)
    private val selectedPost = MutableStateFlow<Entry?>(null)

    val uiState: StateFlow<ExplorerUiState> =
        combine(pathStack, viewMode, selectedPost) { stack, mode, post ->
            buildState(stack, mode, post)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = buildState(pathStack.value, viewMode.value, selectedPost.value),
        )

    /** Folder => descend. .md post => open reader. Other files => no-op. */
    fun open(entry: Entry) {
        when {
            entry.isFolder -> pathStack.value = pathStack.value + entry
            entry.isPost   -> selectedPost.value = entry
        }
    }

    /** Close the reader and return to the board list. */
    fun closePost() { selectedPost.value = null }

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

    private fun buildState(stack: List<Entry>, mode: ViewMode, post: Entry?) = ExplorerUiState(
        path = stack,
        entries = stack.last().children.sortedWith(EntryOrder),
        viewMode = mode,
        selectedPost = post,
    )

    private companion object {
        val EntryOrder: Comparator<Entry> =
            compareByDescending<Entry> { it.isFolder }.thenBy { it.name.lowercase() }
    }
}
