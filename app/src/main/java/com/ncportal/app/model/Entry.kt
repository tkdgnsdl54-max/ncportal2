package com.ncportal.app.model

/**
 * A single node in the explorer tree — either a folder (which may have [children])
 * or a leaf item. The UI is intentionally agnostic about what a "file" really is,
 * so this same model can later be backed by SSH/SFTP, a cloud API, local storage,
 * or any other source.
 */
data class Entry(
    val id: String,
    val name: String,
    val type: EntryType,
    val sizeBytes: Long = 0L,
    val modifiedAt: Long = 0L,
    val children: List<Entry> = emptyList(),
    val content: String? = null,
) {
    val isFolder: Boolean get() = type == EntryType.FOLDER

    val isPost: Boolean
        get() = type == EntryType.FILE &&
            name.substringAfterLast('.', "").equals("md", ignoreCase = true)
}

enum class EntryType { FOLDER, FILE }
