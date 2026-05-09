package com.example.calculator.model

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.example.calculator.R

data class ThemeColors(
    @ColorInt val background: Int,
    @ColorInt val btnNumber: Int,
    @ColorInt val btnSpecial: Int,
    @ColorInt val btnOperator: Int,
    @ColorInt val textPrimary: Int,
    @ColorInt val textSecondary: Int,
    @ColorInt val textOnNumber: Int,
    @ColorInt val textOnSpecial: Int,
    @ColorInt val textOnOperator: Int
)

fun ThemeId.toColors(context: Context): ThemeColors = when (this) {
    ThemeId.CLASSIC -> ThemeColors(
        background     = ContextCompat.getColor(context, R.color.classic_background),
        btnNumber      = ContextCompat.getColor(context, R.color.classic_btn_number),
        btnSpecial     = ContextCompat.getColor(context, R.color.classic_btn_special),
        btnOperator    = ContextCompat.getColor(context, R.color.classic_btn_operator),
        textPrimary    = ContextCompat.getColor(context, R.color.classic_text_primary),
        textSecondary  = ContextCompat.getColor(context, R.color.classic_text_secondary),
        textOnNumber   = ContextCompat.getColor(context, R.color.classic_text_on_number),
        textOnSpecial  = ContextCompat.getColor(context, R.color.classic_text_on_special),
        textOnOperator = ContextCompat.getColor(context, R.color.classic_text_on_operator)
    )
    ThemeId.MIDNIGHT -> ThemeColors(
        background     = ContextCompat.getColor(context, R.color.midnight_background),
        btnNumber      = ContextCompat.getColor(context, R.color.midnight_btn_number),
        btnSpecial     = ContextCompat.getColor(context, R.color.midnight_btn_special),
        btnOperator    = ContextCompat.getColor(context, R.color.midnight_btn_operator),
        textPrimary    = ContextCompat.getColor(context, R.color.midnight_text_primary),
        textSecondary  = ContextCompat.getColor(context, R.color.midnight_text_secondary),
        textOnNumber   = ContextCompat.getColor(context, R.color.midnight_text_on_number),
        textOnSpecial  = ContextCompat.getColor(context, R.color.midnight_text_on_special),
        textOnOperator = ContextCompat.getColor(context, R.color.midnight_text_on_operator)
    )
    ThemeId.OCEAN -> ThemeColors(
        background     = ContextCompat.getColor(context, R.color.ocean_background),
        btnNumber      = ContextCompat.getColor(context, R.color.ocean_btn_number),
        btnSpecial     = ContextCompat.getColor(context, R.color.ocean_btn_special),
        btnOperator    = ContextCompat.getColor(context, R.color.ocean_btn_operator),
        textPrimary    = ContextCompat.getColor(context, R.color.ocean_text_primary),
        textSecondary  = ContextCompat.getColor(context, R.color.ocean_text_secondary),
        textOnNumber   = ContextCompat.getColor(context, R.color.ocean_text_on_number),
        textOnSpecial  = ContextCompat.getColor(context, R.color.ocean_text_on_special),
        textOnOperator = ContextCompat.getColor(context, R.color.ocean_text_on_operator)
    )
    ThemeId.SUNSET -> ThemeColors(
        background     = ContextCompat.getColor(context, R.color.sunset_background),
        btnNumber      = ContextCompat.getColor(context, R.color.sunset_btn_number),
        btnSpecial     = ContextCompat.getColor(context, R.color.sunset_btn_special),
        btnOperator    = ContextCompat.getColor(context, R.color.sunset_btn_operator),
        textPrimary    = ContextCompat.getColor(context, R.color.sunset_text_primary),
        textSecondary  = ContextCompat.getColor(context, R.color.sunset_text_secondary),
        textOnNumber   = ContextCompat.getColor(context, R.color.sunset_text_on_number),
        textOnSpecial  = ContextCompat.getColor(context, R.color.sunset_text_on_special),
        textOnOperator = ContextCompat.getColor(context, R.color.sunset_text_on_operator)
    )
    ThemeId.RABBIT -> ThemeColors(
        background     = ContextCompat.getColor(context, R.color.rabbit_background),
        btnNumber      = ContextCompat.getColor(context, R.color.rabbit_btn_number),
        btnSpecial     = ContextCompat.getColor(context, R.color.rabbit_btn_special),
        btnOperator    = ContextCompat.getColor(context, R.color.rabbit_btn_operator),
        textPrimary    = ContextCompat.getColor(context, R.color.rabbit_text_primary),
        textSecondary  = ContextCompat.getColor(context, R.color.rabbit_text_secondary),
        textOnNumber   = ContextCompat.getColor(context, R.color.rabbit_text_on_number),
        textOnSpecial  = ContextCompat.getColor(context, R.color.rabbit_text_on_special),
        textOnOperator = ContextCompat.getColor(context, R.color.rabbit_text_on_operator)
    )
    ThemeId.PANDA -> ThemeColors(
        background     = ContextCompat.getColor(context, R.color.panda_background),
        btnNumber      = ContextCompat.getColor(context, R.color.panda_btn_number),
        btnSpecial     = ContextCompat.getColor(context, R.color.panda_btn_special),
        btnOperator    = ContextCompat.getColor(context, R.color.panda_btn_operator),
        textPrimary    = ContextCompat.getColor(context, R.color.panda_text_primary),
        textSecondary  = ContextCompat.getColor(context, R.color.panda_text_secondary),
        textOnNumber   = ContextCompat.getColor(context, R.color.panda_text_on_number),
        textOnSpecial  = ContextCompat.getColor(context, R.color.panda_text_on_special),
        textOnOperator = ContextCompat.getColor(context, R.color.panda_text_on_operator)
    )
    ThemeId.GLASS_ICE -> ThemeColors(
        background     = ContextCompat.getColor(context, R.color.glass_ice_background),
        btnNumber      = ContextCompat.getColor(context, R.color.glass_ice_btn_number),
        btnSpecial     = ContextCompat.getColor(context, R.color.glass_ice_btn_special),
        btnOperator    = ContextCompat.getColor(context, R.color.glass_ice_btn_operator),
        textPrimary    = ContextCompat.getColor(context, R.color.glass_ice_text_primary),
        textSecondary  = ContextCompat.getColor(context, R.color.glass_ice_text_secondary),
        textOnNumber   = ContextCompat.getColor(context, R.color.glass_ice_text_on_number),
        textOnSpecial  = ContextCompat.getColor(context, R.color.glass_ice_text_on_special),
        textOnOperator = ContextCompat.getColor(context, R.color.glass_ice_text_on_operator)
    )
}
