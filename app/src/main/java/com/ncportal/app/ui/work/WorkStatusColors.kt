package com.ncportal.app.ui.work

import androidx.compose.ui.graphics.Color
import com.ncportal.app.model.TaskStatus

// ─────────────────────────────────────────────────────────────────────────────
// Status colour mapping  (UI layer — overrides the placeholder hex in the model)
//
// Spec:  PENDING = Orange  |  IN_PROGRESS = Blue  |  COMPLETED = Green  |  REVISIT = Red
// Tones: Material You 600-weight equivalents that read against white text.
// ─────────────────────────────────────────────────────────────────────────────

/** Background colour for status chips / cards. Always legible with white text. */
val TaskStatus.chipColor: Color
    get() = when (this) {
        TaskStatus.PENDING     -> Color(0xFFFB8C00)   // Orange 800
        TaskStatus.IN_PROGRESS -> Color(0xFF1E88E5)   // Blue 600
        TaskStatus.COMPLETED   -> Color(0xFF43A047)   // Green 600
        TaskStatus.REVISIT     -> Color(0xFFE53935)   // Red 600
    }

/** Always-white text that pairs with [chipColor]. */
val chipContentColor: Color = Color.White
