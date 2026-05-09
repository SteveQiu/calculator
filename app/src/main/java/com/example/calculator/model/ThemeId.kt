package com.example.calculator.model

/**
 * Canonical identifiers for every theme in the app.
 * CLASSIC is always free; MIDNIGHT, OCEAN, SUNSET are premium (require ad or purchase to unlock).
 */
enum class ThemeId(
    val themeKey: String,
    val displayName: String,
    val isPremium: Boolean,
    /** Play Store product ID; null for free themes. */
    val skuId: String? = null,
) {
    CLASSIC("classic", "Classic", isPremium = false),
    MIDNIGHT("midnight", "Midnight", isPremium = true, skuId = "theme_midnight"),
    OCEAN("ocean", "Ocean", isPremium = true, skuId = "theme_ocean"),
    SUNSET("sunset", "Sunset", isPremium = true, skuId = "theme_sunset");

    companion object {
        fun fromKey(key: String): ThemeId =
            entries.firstOrNull { it.themeKey == key } ?: CLASSIC
    }
}
