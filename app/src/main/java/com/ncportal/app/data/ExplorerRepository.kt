package com.ncportal.app.data

import com.ncportal.app.model.Entry

/**
 * Supplies the explorer tree to the UI layer.
 *
 * Swap [SampleExplorerRepository] for a real implementation later (SSH/SFTP,
 * cloud storage, on-device files, a backend portal API, …). The UI only depends
 * on this interface, so the rest of the app does not change.
 */
interface ExplorerRepository {
    /** The top-level node the explorer opens at. */
    fun root(): Entry
}
