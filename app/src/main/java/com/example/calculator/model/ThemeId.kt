package com.example.calculator.model

enum class ThemeId(val displayName: String, val isPremium: Boolean, val skuId: String?) {
    CLASSIC("Classic", false, null),
    MIDNIGHT("Midnight", true, "theme_midnight"),
    OCEAN("Ocean", true, "theme_ocean"),
    SUNSET("Sunset", true, "theme_sunset");

    companion object {
        fun fromKey(key: String): ThemeId =
            entries.firstOrNull { it.name.lowercase() == key.lowercase() } ?: CLASSIC
    }
}