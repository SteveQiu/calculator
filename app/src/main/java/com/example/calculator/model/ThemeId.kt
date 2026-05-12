package com.example.calculator.model

enum class ThemeId(val displayName: String, val isPremium: Boolean, val skuId: String?) {
    CLASSIC("Classic", false, null),
    MIDNIGHT("Midnight", true, "theme_midnight"),
    OCEAN("Ocean", true, "theme_ocean"),
    SUNSET("Sunset", true, "theme_sunset"),
    RABBIT("Rabbit 🐰🥕", true, "theme_rabbit"),
    PANDA("Panda 🐼", true, "theme_panda"),
    GLASS_ICE("Glass Ice", true, "theme_glass_ice"),
    CHERRY_BLOSSOM("Cherry Blossom", true, "theme_cherry_blossom");

    companion object {
        fun fromKey(key: String): ThemeId =
            entries.firstOrNull { it.name.lowercase() == key.lowercase() } ?: CLASSIC
    }
}