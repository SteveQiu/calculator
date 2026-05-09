package com.example.calculator.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.calculator.model.ThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

class ThemeRepository(private val context: Context) {

    companion object {
        private val KEY_ACTIVE_THEME    = stringPreferencesKey("active_theme")
        private val KEY_UNLOCKED_THEMES = stringSetPreferencesKey("unlocked_themes")
    }

    val activeThemeFlow: Flow<ThemeId> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_ACTIVE_THEME] ?: ThemeId.CLASSIC.name
        ThemeId.entries.firstOrNull { it.name == name } ?: ThemeId.CLASSIC
    }

    val unlockedThemesFlow: Flow<Set<ThemeId>> = context.dataStore.data.map { prefs ->
        val names = prefs[KEY_UNLOCKED_THEMES] ?: emptySet()
        // Only premium themes are tracked as "unlocked"; free themes (isPremium=false)
        // are always accessible via the !isPremium short-circuit in isThemeUnlocked().
        ThemeId.entries.filter { it.isPremium && it.name in names }.toSet()
    }

    suspend fun setActiveTheme(themeId: ThemeId) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_THEME] = themeId.name
        }
    }

    suspend fun unlockTheme(themeId: ThemeId) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_UNLOCKED_THEMES] ?: setOf(ThemeId.CLASSIC.name)
            prefs[KEY_UNLOCKED_THEMES] = current + themeId.name
        }
    }

    fun isThemeUnlocked(themeId: ThemeId, unlockedSet: Set<ThemeId>): Boolean =
        !themeId.isPremium || themeId in unlockedSet
}