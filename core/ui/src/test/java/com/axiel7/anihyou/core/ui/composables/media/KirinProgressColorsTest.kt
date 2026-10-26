package com.axiel7.anihyou.core.ui.composables.media

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KirinProgressColorsTest {
    @Test
    fun knownTotalUsesAllSixPercentageStages() {
        assertEquals(Color(0xFFFF526B), kirinProgressStageColor(1, 10, true))
        assertEquals(Color(0xFFFF9D35), kirinProgressStageColor(2, 10, true))
        assertEquals(Color(0xFFFFD54F), kirinProgressStageColor(4, 10, true))
        assertEquals(Color(0xFF39DB8A), kirinProgressStageColor(6, 10, true))
        assertEquals(Color(0xFF00CFFF), kirinProgressStageColor(8, 10, true))
        assertEquals(Color(0xFFB65CFF), kirinProgressStageColor(10, 10, true))
    }

    @Test
    fun ongoingMangaWithoutTotalUsesCountMilestones() {
        assertEquals(Color(0xFFFF526B), kirinProgressStageColor(1, null, true))
        assertEquals(Color(0xFFFF9D35), kirinProgressStageColor(5, null, true))
        assertEquals(Color(0xFFFFD54F), kirinProgressStageColor(10, null, true))
        assertEquals(Color(0xFF39DB8A), kirinProgressStageColor(25, null, true))
        assertEquals(Color(0xFF00CFFF), kirinProgressStageColor(50, null, true))
        assertEquals(Color(0xFFB65CFF), kirinProgressStageColor(100, null, true))
    }

    @Test
    fun missingProgressAndDisabledEffectKeepOriginalTheme() {
        assertNull(kirinProgressStageColor(0, 12, true))
        assertNull(kirinProgressStageColor(0, null, true))
        assertNull(kirinProgressStageColor(9, 12, false))
        assertNull(kirinProgressStageColor(9, null, false))
    }
}
