package com.example.calculator.model

/**
 * Canonical identifiers for every theme in the app.
 * CLASSIC is always free; MIDNIGHT, OCEAN, SUNSET are premium.
 */
enum class ThemeId(val themeKey: String) {
    CLASSIC("classic"),
    MIDNIGHT("midnight"),
    OCEAN("ocean"),
    SUNSET("sunset");

    companion object {
        fun fromKey(key: String): ThemeId =
            entries.firstOrNull { it.themeKey == key } ?: CLASSIC
    }
}
