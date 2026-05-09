package com.example.calculator.model

import androidx.annotation.StyleRes

/**
 * Represents a single calculator theme.
 *
 * @param id         Canonical enum identifier.
 * @param name       Human-readable display name shown in ThemePickerActivity.
 * @param isPremium  True if the theme requires an unlock (ad or purchase).
 * @param styleResId Android style resource applied via activity.setTheme() before setContentView.
 */
data class Theme(
    val id: ThemeId,
    val name: String,
    val isPremium: Boolean,
    @StyleRes val styleResId: Int,
) {
    /** Classic is always unlocked; premium themes require an explicit unlock. */
    val requiresUnlock: Boolean get() = isPremium
}
