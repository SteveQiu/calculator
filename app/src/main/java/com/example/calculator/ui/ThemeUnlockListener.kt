package com.example.calculator.ui

import com.example.calculator.model.ThemeId

/**
 * Callback interface for theme picker interactions.
 * Implemented by MainActivity; dialogs resolve it via `requireActivity() as ThemeUnlockListener`
 * instead of talking to ThemeViewModel directly, keeping the dialog layer decoupled.
 */
interface ThemeUnlockListener {
    /** User tapped an already-unlocked theme card — apply and persist immediately. */
    fun onThemeSelected(themeId: ThemeId)

    /** User tapped "Watch Ad" — load/show rewarded ad; reward unlocks and applies this theme. */
    fun onWatchAdRequested(themeId: ThemeId)

    /** User tapped "Buy" — launch Play Billing one-time purchase flow for this theme. */
    fun onPurchaseRequested(themeId: ThemeId)

    /** Returns true when the theme is accessible (CLASSIC is always true). */
    fun isThemeUnlocked(themeId: ThemeId): Boolean
}
