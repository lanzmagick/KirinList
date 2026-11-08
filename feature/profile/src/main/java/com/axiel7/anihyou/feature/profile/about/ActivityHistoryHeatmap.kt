package com.axiel7.anihyou.feature.profile.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.axiel7.anihyou.core.resources.R
import com.axiel7.anihyou.feature.profile.ActivityHistoryEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private data class ActivityHistoryCell(
    val dateKey: Int,
    val dateMillis: Long,
    val amount: Int,
    val level: Int,
)

@Composable
fun ActivityHistoryHeatmap(
    history: List<ActivityHistoryEntry>,
    modifier: Modifier = Modifier,
) {
    val weeks = remember(history) { buildActivityHistoryWeeks(history) }
    val totalActivities = remember(history) { history.sumOf { it.amount.coerceAtLeast(0) } }
    val primaryColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    var selectedDateKey by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedDay = weeks.asSequence()
        .flatten()
        .firstOrNull { it?.dateKey == selectedDateKey }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.activity_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.activity_count,
                        totalActivities,
                        totalActivities,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(R.string.activity_history_last_six_months),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                weeks.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        week.forEach { day ->
                            if (day == null) {
                                Spacer(Modifier.size(10.dp))
                            } else {
                                val color = activityLevelColor(
                                    level = day.level,
                                    amount = day.amount,
                                    emptyColor = emptyColor,
                                    activeColor = primaryColor,
                                )
                                val activityDescription = pluralStringResource(
                                    R.plurals.activity_count,
                                    day.amount.coerceAtLeast(0),
                                    day.amount.coerceAtLeast(0),
                                )
                                val dateDescription = formatActivityDate(day.dateMillis)

                                Spacer(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color)
                                        .clickable { selectedDateKey = day.dateKey }
                                        .semantics {
                                            contentDescription = "$dateDescription, $activityDescription"
                                        }
                                )
                            }
                        }
                    }
                }
            }

            selectedDay?.let { day ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatActivityDate(day.dateMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.activity_count,
                            day.amount.coerceAtLeast(0),
                            day.amount.coerceAtLeast(0),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private fun activityLevelColor(
    level: Int,
    amount: Int,
    emptyColor: Color,
    activeColor: Color,
): Color {
    if (amount <= 0 || level <= 0) return emptyColor.copy(alpha = 0.55f)
    val intensity = 0.28f + (level.coerceIn(1, 10) - 1) / 9f * 0.72f
    return lerp(emptyColor, activeColor, intensity)
}

private fun buildActivityHistoryWeeks(
    history: List<ActivityHistoryEntry>,
): List<List<ActivityHistoryCell?>> {
    val utc = TimeZone.getTimeZone("UTC")
    val historyByDate = history.associateBy { timestamp ->
        val date = Calendar.getInstance(utc, Locale.ROOT).apply {
            timeInMillis = timestamp.date.toLong() * 1_000L
        }
        dateKey(date)
    }

    val today = Calendar.getInstance(utc, Locale.ROOT).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val cursor = (today.clone() as Calendar).apply {
        add(Calendar.MONTH, -6)
        add(Calendar.DAY_OF_MONTH, -(get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY))
    }

    val days = mutableListOf<ActivityHistoryCell?>()
    while (cursor.timeInMillis <= today.timeInMillis) {
        val key = dateKey(cursor)
        val entry = historyByDate[key]
        days += ActivityHistoryCell(
            dateKey = key,
            dateMillis = cursor.timeInMillis,
            amount = entry?.amount?.coerceAtLeast(0) ?: 0,
            level = entry?.level?.coerceIn(0, 10) ?: 0,
        )
        cursor.add(Calendar.DAY_OF_MONTH, 1)
    }

    while (days.size % 7 != 0) days += null
    return days.chunked(7)
}

private fun dateKey(calendar: Calendar): Int =
    calendar.get(Calendar.YEAR) * 10_000 +
            (calendar.get(Calendar.MONTH) + 1) * 100 +
            calendar.get(Calendar.DAY_OF_MONTH)

private fun formatActivityDate(timeMillis: Long): String =
    SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(timeMillis))
