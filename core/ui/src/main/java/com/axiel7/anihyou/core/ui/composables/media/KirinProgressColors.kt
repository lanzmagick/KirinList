package com.axiel7.anihyou.core.ui.composables.media

import androidx.compose.ui.graphics.Color

/**
 * KirinList's shared episode/chapter/volume stage palette.
 *
 * For a known total we use completion percentage. Ongoing manga and some anime
 * do not report a total; their non-zero progress uses count milestones instead,
 * so those entries are not excluded from the color effect.
 */
fun kirinProgressStageColor(progress: Int, total: Int?, enabled: Boolean): Color? {
    if (!enabled || progress <= 0) return null

    val stage = if (total != null && total > 0) {
        val fraction = (progress.toFloat() / total).coerceIn(0f, 1f)
        when {
            fraction < 0.2f -> 0
            fraction < 0.4f -> 1
            fraction < 0.6f -> 2
            fraction < 0.8f -> 3
            fraction < 1f -> 4
            else -> 5
        }
    } else {
        when {
            progress < 5 -> 0
            progress < 10 -> 1
            progress < 25 -> 2
            progress < 50 -> 3
            progress < 100 -> 4
            else -> 5
        }
    }

    return when (stage) {
        0 -> Color(0xFFFF526B)
        1 -> Color(0xFFFF9D35)
        2 -> Color(0xFFFFD54F)
        3 -> Color(0xFF39DB8A)
        4 -> Color(0xFF00CFFF)
        else -> Color(0xFFB65CFF)
    }
}
