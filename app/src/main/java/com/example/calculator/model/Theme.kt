package com.example.calculator.model

import android.content.Context
import com.example.calculator.R

/**
 * Self-describing theme definition. Every piece of theme metadata lives here.
 * To add a new theme: (1) add to ThemeId enum, (2) register in ThemeRegistry.all — nothing else.
 *
 * [colors] is a function so color resolution is deferred until a [Context] is available
 * (Android color resources require Context). Call as `theme.colors(context)`.
 */
data class Theme(
    val id: ThemeId,
    val displayName: String,
    val isPremium: Boolean,
    val colors: (Context) -> ThemeColors,
    val iconRes: Int? = null,
    val iconEmoji: String? = null,
    val skuId: String? = null,
    /** Font resource ID for the calculator display and number buttons (e.g. R.font.fredoka_one).
     *  null = use the system default typeface. */
    val fontResId: Int? = null
)

/**
 * Single registry for all themes. Add new themes here and only here.
 * Delegates color resolution to the existing [toColors] extension so colors.xml remains
 * the canonical color store.
 */
object ThemeRegistry {

    val all: List<Theme> = listOf(
        Theme(
            id          = ThemeId.CLASSIC,
            displayName = "Classic",
            isPremium   = false,
            colors      = { ctx -> ThemeId.CLASSIC.toColors(ctx) },
            iconEmoji   = "🔢",
            skuId       = null
        ),
        Theme(
            id          = ThemeId.MIDNIGHT,
            displayName = "Midnight",
            isPremium   = true,
            colors      = { ctx -> ThemeId.MIDNIGHT.toColors(ctx) },
            iconEmoji   = "🌙",
            skuId       = "theme_midnight"
        ),
        Theme(
            id          = ThemeId.OCEAN,
            displayName = "Ocean",
            isPremium   = true,
            colors      = { ctx -> ThemeId.OCEAN.toColors(ctx) },
            iconEmoji   = "🌊",
            skuId       = "theme_ocean"
        ),
        Theme(
            id          = ThemeId.SUNSET,
            displayName = "Sunset",
            isPremium   = true,
            colors      = { ctx -> ThemeId.SUNSET.toColors(ctx) },
            iconEmoji   = "🌇",
            skuId       = "theme_sunset"
        ),
        Theme(
            id          = ThemeId.RABBIT,
            displayName = "Rabbit 🐰🥕",
            isPremium   = true,
            colors      = { ctx -> ThemeId.RABBIT.toColors(ctx) },
            iconEmoji   = "🐰",
            skuId       = "theme_rabbit",
            fontResId   = null  // Rusty will set once res/font/ file is added
        ),
        Theme(
            id          = ThemeId.PANDA,
            displayName = "Panda 🐼",
            isPremium   = true,
            colors      = { ctx -> ThemeId.PANDA.toColors(ctx) },
            iconEmoji   = "🐼",
            skuId       = "theme_panda",
            fontResId   = null  // Rusty will set once res/font/ file is added
        ),
        Theme(
            id          = ThemeId.GLASS_ICE,
            displayName = "Glass Ice",
            isPremium   = true,
            colors      = { ctx -> ThemeId.GLASS_ICE.toColors(ctx) },
            iconRes     = R.drawable.ic_theme_glass_ice,
            iconEmoji   = "❄️",
            skuId       = "theme_glass_ice"
        )
    )

    private val byId: Map<ThemeId, Theme> = all.associateBy { it.id }

    /** Returns the Theme for [id], falling back to CLASSIC if somehow not found. */
    fun forId(id: ThemeId): Theme = byId[id] ?: all.first()
}