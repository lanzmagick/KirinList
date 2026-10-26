package com.axiel7.anihyou.core.ui.composables.media

import androidx.compose.runtime.compositionLocalOf

/** Current Unix time shared by visible airing labels in the Anime List. */
val LocalAiringEpochSeconds = compositionLocalOf { System.currentTimeMillis() / 1000L }
