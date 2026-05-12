package com.example.calculator.model

import android.content.Context
import androidx.annotation.DrawableRes
import com.example.calculator.R

/**
 * Self-describing theme definition. Every piece of theme metadata lives here.
 *
 * To add a new theme:
 *   1. Add enum constant to [ThemeId]
 *   2. Add color resources to colors.xml (pattern: {theme_name}_{element})
 *   3. Add [ThemeColors] branch to [ThemeId.toColors] in ThemeColors.kt
 *   4. Optionally create a background drawable in res/drawable/ and pass its ID as [backgroundImageRes]
 *   5. Optionally create a custom font XML and pass its ID as [fontResId]
 *   6. Add a [Theme] entry to [ThemeRegistry.all] — done!
 *
 * [colors] is a function so color resolution is deferred until a [Context] is available.
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
    val fontResId: Int? = null,
    /** Optional background image drawable resource ID. When set, replaces the solid background color
     *  with this drawable (e.g. a gradient or pattern). null = use [ThemeColors.background] as solid color. */
    @DrawableRes val backgroundImageRes: Int? = null
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
            fontResId   = R.font.fredoka_one
        ),
        Theme(
            id          = ThemeId.PANDA,
            displayName = "Panda 🐼",
            isPremium   = true,
            colors      = { ctx -> ThemeId.PANDA.toColors(ctx) },
            iconEmoji   = "🐼",
            skuId       = "theme_panda",
            fontResId   = R.font.fredoka_one
        ),
        Theme(
            id          = ThemeId.GLASS_ICE,
            displayName = "Glass Ice",
            isPremium   = true,
            colors      = { ctx -> ThemeId.GLASS_ICE.toColors(ctx) },
            iconRes     = R.drawable.ic_theme_glass_ice,
            iconEmoji   = "❄️",
            skuId       = "theme_glass_ice"
        ),
        Theme(
            id                = ThemeId.CHERRY_BLOSSOM,
            displayName       = "Cherry Blossom",
            isPremium         = true,
            colors            = { ctx -> ThemeId.CHERRY_BLOSSOM.toColors(ctx) },
            iconEmoji         = "🌸",
            skuId             = "theme_cherry_blossom",
            backgroundImageRes = R.drawable.bg_cherry_blossom
        )
    )

    private val byId: Map<ThemeId, Theme> = all.associateBy { it.id }

    /** Returns the Theme for [id], falling back to CLASSIC if somehow not found. */
    fun forId(id: ThemeId): Theme = byId[id] ?: all.first()
}