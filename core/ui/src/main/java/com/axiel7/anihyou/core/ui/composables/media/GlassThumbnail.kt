package com.axiel7.anihyou.core.ui.composables.media

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Lightweight, static glass reflection for image thumbnails.
 * Uses drawing brushes instead of blur or per-item animations, so large lists stay smooth.
 * The caller clips the thumbnail to [shape] before applying this modifier.
 */
fun Modifier.glassThumbnailFinish(
    shape: Shape,
    strength: Float = 1f,
): Modifier {
    val tint = strength.coerceIn(0f, 1f)
    return this
        .border(0.8.dp, Color.White.copy(alpha = 0.24f * tint), shape)
        .drawWithCache {
            val softGlow = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(alpha = 0.30f * tint),
                    0.35f to Color.White.copy(alpha = 0.09f * tint),
                    1f to Color.Transparent,
                ),
                startY = 0f,
                endY = size.height * 0.80f,
            )
            val diagonalReflection = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width * 0.74f, 0f)
                lineTo(size.width * 0.28f, size.height * 0.56f)
                lineTo(0f, size.height * 0.30f)
                close()
            }
            val reflectionBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(alpha = 0.20f * tint),
                    0.58f to Color.White.copy(alpha = 0.08f * tint),
                    1f to Color.Transparent,
                ),
                start = Offset.Zero,
                end = Offset(size.width * 0.65f, size.height * 0.60f),
            )

            onDrawWithContent {
                drawContent()
                drawRect(brush = softGlow)
                drawPath(path = diagonalReflection, brush = reflectionBrush)
            }
        }
}
