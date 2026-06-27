package com.ncportal.app.ui.explorer

import com.ncportal.app.model.Entry

enum class ViewMode { LIST, GRID }

/** Immutable snapshot of everything the explorer screen needs to render. */
data class ExplorerUiState(
    /** Breadcrumb trail: index 0 is the root, the last element is the open folder. */
    val path: List<Entry> = emptyList(),
    /** Children of the open folder, already sorted for display. */
    val entries: List<Entry> = emptyList(),
    val viewMode: ViewMode = ViewMode.LIST,
    /** Non-null when the post reader is shown over the board list. */
    val selectedPost: Entry? = null,
) {
    val currentFolder: Entry? get() = path.lastOrNull()
    val canNavigateUp: Boolean get() = path.size > 1
    val isPostOpen: Boolean get() = selectedPost != null
}
