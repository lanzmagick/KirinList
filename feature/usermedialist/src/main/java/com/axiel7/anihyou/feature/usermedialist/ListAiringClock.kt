package com.axiel7.anihyou.feature.usermedialist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.axiel7.anihyou.core.network.fragment.CommonMediaListEntry
import com.axiel7.anihyou.core.ui.composables.media.LocalAiringEpochSeconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** One lifecycle-aware clock for every visible airing label in the Anime List. */
@Composable
internal fun ListAiringClock(
    enabled: Boolean,
    entries: SnapshotStateList<CommonMediaListEntry>,
    onAiringDue: () -> Unit,
    content: @Composable () -> Unit,
) {
    var isResumed by remember { mutableStateOf(false) }
    var currentSeconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000L) }
    var lastRefreshedAiringAt by remember { mutableLongStateOf(Long.MIN_VALUE) }
    val currentOnAiringDue by rememberUpdatedState(onAiringDue)

    LifecycleResumeEffect(enabled) {
        isResumed = true
        onPauseOrDispose { isResumed = false }
    }

    LaunchedEffect(enabled, isResumed) {
        if (!enabled || !isResumed) return@LaunchedEffect
        while (true) {
            currentSeconds = System.currentTimeMillis() / 1000L
            // Align with the wall clock, with no network requests per tick.
            delay(1000L - (System.currentTimeMillis() % 1000L))
        }
    }

    LaunchedEffect(enabled, isResumed, entries) {
        if (!enabled || !isResumed) return@LaunchedEffect
        snapshotFlow {
            entries.mapNotNull { it.media?.nextAiringEpisode?.airingAt }
        }.collectLatest { airingTimes ->
            val nextDue = airingTimes.asSequence()
                .map(Int::toLong)
                .filter { it > lastRefreshedAiringAt && it > 0L }
                .minOrNull() ?: return@collectLatest

            val remainingMillis = (nextDue * 1000L - System.currentTimeMillis())
                .coerceAtLeast(0L)
            delay(remainingMillis)
            // One refresh for this airing time, even if the API is still stale.
            if (nextDue > lastRefreshedAiringAt) {
                lastRefreshedAiringAt = nextDue
                currentOnAiringDue()
            }
        }
    }

    CompositionLocalProvider(LocalAiringEpochSeconds provides currentSeconds, content = content)
}
