package com.axiel7.anihyou.core.ui.composables.media

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.axiel7.anihyou.core.model.media.episodesBehind
import com.axiel7.anihyou.core.model.media.isBehind
import com.axiel7.anihyou.core.network.fragment.CommonMediaListEntry
import com.axiel7.anihyou.core.resources.R

@Composable
fun AiringScheduleText(
    item: CommonMediaListEntry,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    item.media?.nextAiringEpisode?.let { nextAiringEpisode ->
        val isBehind = item.isBehind()
        Text(
            text =
            if (isBehind) {
                val episodes = item.episodesBehind()
                if (item.media?.basicMediaDetails?.episodes != 1) {
                    pluralStringResource(
                        id = R.plurals.num_episodes_behind,
                        count = episodes,
                        episodes
                    )
                } else ""
            } else {
                // The original behind text above stays untouched. Only the airing label
                // reads the shared ticking clock, so hidden countdowns do no UI work.
                val secondsLeft = (nextAiringEpisode.airingAt.toLong() -
                    LocalAiringEpochSeconds.current).coerceAtLeast(0L)
                val airingCountdown = secondsLeft.toAiringCountdownText()
                if (item.media?.basicMediaDetails?.episodes == 1) {
                    stringResource(
                        R.string.airing_in,
                        airingCountdown
                    )
                } else {
                    stringResource(
                        R.string.episode_in_time,
                        nextAiringEpisode.episode,
                        airingCountdown
                    )
                }
            },
            modifier = modifier,
            color = if (isBehind) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            textAlign = textAlign,
        )
    }
}

/** Compact units fit beside anime titles, including a seconds field that ticks live. */
private fun Long.toAiringCountdownText(): String {
    val left = coerceAtLeast(0L)
    val days = left / 86400L
    val hours = (left % 86400L) / 3600L
    val minutes = (left % 3600L) / 60L
    val seconds = left % 60L

    return buildList {
        if (days > 0L) add("${days}d")
        if (days > 0L || hours > 0L) add("${hours}h")
        add("${minutes}m")
        add("${seconds}s")
    }.joinToString(" ")
}
