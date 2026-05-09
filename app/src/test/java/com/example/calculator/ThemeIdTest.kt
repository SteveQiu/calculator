package com.example.calculator

import com.example.calculator.model.ThemeId
import org.junit.Assert.*
import org.junit.Test

class ThemeIdTest {

    @Test fun `CLASSIC is not premium`() {
        assertFalse(ThemeId.CLASSIC.isPremium)
    }

    @Test fun `CLASSIC has no skuId`() {
        assertNull(ThemeId.CLASSIC.skuId)
    }

    @Test fun `MIDNIGHT is premium`() {
        assertTrue(ThemeId.MIDNIGHT.isPremium)
    }

    @Test fun `OCEAN is premium`() {
        assertTrue(ThemeId.OCEAN.isPremium)
    }

    @Test fun `SUNSET is premium`() {
        assertTrue(ThemeId.SUNSET.isPremium)
    }

    @Test fun `all premium themes have skuId`() {
        ThemeId.values().filter { it.isPremium }.forEach { themeId ->
            assertNotNull("${themeId.name} should have skuId", themeId.skuId)
        }
    }

    @Test fun `skuIds are unique`() {
        val skuIds = ThemeId.values().mapNotNull { it.skuId }
        assertEquals(skuIds.size, skuIds.distinct().size)
    }

    @Test fun `all themes have display names`() {
        ThemeId.values().forEach {
            assertTrue(it.displayName.isNotBlank())
        }
    }
}
