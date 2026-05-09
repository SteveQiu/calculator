package com.example.calculator.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.calculator.model.ThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "theme_prefs")

/**
 * Single source of truth for theme selection and unlock state.
 * Backed by DataStore (Preferences) for coroutine-safe, structured persistence.
 *
 * Keys:
 *   KEY_ACTIVE_THEME            — ThemeId.themeKey string of the active theme
 *   KEY_UNLOCKED_{themeKey}     — Boolean, whether that premium theme is unlocked
 *
 * Classic is always unlocked; no DataStore entry is needed for it.
 *
 * TODO: Initialise activeThemeIdFlow from dataStore.data mapping KEY_ACTIVE_THEME
 * TODO: Initialise unlockStateFlow by combining all per-theme boolean keys
 * TODO: Implement setActiveTheme(themeId) — write KEY_ACTIVE_THEME to dataStore
 * TODO: Implement unlockTheme(themeId) — write KEY_UNLOCKED_{themeKey} = true to dataStore
 */
class ThemeRepository(private val context: Context) {

    companion object {
        private val KEY_ACTIVE_THEME = stringPreferencesKey("active_theme")
        private fun unlockedKey(themeId: ThemeId) =
            booleanPreferencesKey("theme_unlocked_${themeId.themeKey}")
    }

    // TODO: Replace stubs with real DataStore flows
    val activeThemeIdFlow: StateFlow<ThemeId> = MutableStateFlow(ThemeId.CLASSIC)

    /** Alias used by tests; backed by [activeThemeIdFlow]. */
    val activeThemeFlow: Flow<ThemeId> get() = activeThemeIdFlow

    val unlockStateFlow: StateFlow<Map<ThemeId, Boolean>> = MutableStateFlow(
        ThemeId.entries.associateWith { it == ThemeId.CLASSIC }
    )

    /** Emits the set of currently-unlocked ThemeIds. CLASSIC is always included. */
    val unlockedThemesFlow: Flow<Set<ThemeId>> = unlockStateFlow.map { map ->
        map.entries.filter { it.value }.map { it.key }.toSet() + ThemeId.CLASSIC
    }

    suspend fun setActiveTheme(themeId: ThemeId) {
        // TODO: context.dataStore.edit { prefs -> prefs[KEY_ACTIVE_THEME] = themeId.themeKey }
    }

    suspend fun unlockTheme(themeId: ThemeId) {
        // TODO: context.dataStore.edit { prefs -> prefs[unlockedKey(themeId)] = true }
    }

    /**
     * Returns true if [themeId] is accessible given [unlockedSet].
     * Non-premium themes are always accessible. Premium themes require an explicit unlock.
     */
    fun isThemeUnlocked(themeId: ThemeId, unlockedSet: Set<ThemeId>): Boolean =
        !themeId.isPremium || themeId in unlockedSet
}
