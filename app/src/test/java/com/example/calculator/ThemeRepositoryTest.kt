package com.example.calculator

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.calculator.model.ThemeId
import com.example.calculator.repository.ThemeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ThemeRepositoryTest {

    private lateinit var repository: ThemeRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository = ThemeRepository(context)
    }

    @Test fun `default active theme is CLASSIC`() = runTest {
        val theme = repository.activeThemeFlow.first()
        assertEquals(ThemeId.CLASSIC, theme)
    }

    @Test fun `CLASSIC is unlocked by default`() = runTest {
        val unlocked = repository.unlockedThemesFlow.first()
        assertTrue(ThemeId.CLASSIC in unlocked)
    }

    @Test fun `unlockTheme persists theme`() = runTest {
        repository.unlockTheme(ThemeId.MIDNIGHT)
        val unlocked = repository.unlockedThemesFlow.first()
        assertTrue(ThemeId.MIDNIGHT in unlocked)
    }

    @Test fun `setActiveTheme changes active theme`() = runTest {
        repository.unlockTheme(ThemeId.OCEAN)
        repository.setActiveTheme(ThemeId.OCEAN)
        val active = repository.activeThemeFlow.first()
        assertEquals(ThemeId.OCEAN, active)
    }

    @Test fun `CLASSIC not premium — isThemeUnlocked true without being in set`() {
        assertTrue(repository.isThemeUnlocked(ThemeId.CLASSIC, emptySet()))
    }

    @Test fun `MIDNIGHT premium — locked when not in set`() {
        assertFalse(repository.isThemeUnlocked(ThemeId.MIDNIGHT, emptySet()))
    }

    @Test fun `MIDNIGHT premium — unlocked when in set`() {
        assertTrue(repository.isThemeUnlocked(ThemeId.MIDNIGHT, setOf(ThemeId.MIDNIGHT)))
    }

    @Test fun `multiple themes can be unlocked`() = runTest {
        repository.unlockTheme(ThemeId.MIDNIGHT)
        repository.unlockTheme(ThemeId.OCEAN)
        val unlocked = repository.unlockedThemesFlow.first()
        assertTrue(ThemeId.MIDNIGHT in unlocked)
        assertTrue(ThemeId.OCEAN in unlocked)
    }
}
