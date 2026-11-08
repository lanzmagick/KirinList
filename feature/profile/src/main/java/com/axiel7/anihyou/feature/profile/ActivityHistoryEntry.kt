package com.axiel7.anihyou.feature.profile

/**
 * One daily activity bucket returned by AniList's activity history endpoint.
 * [date] is a Unix timestamp in seconds.
 */
data class ActivityHistoryEntry(
    val date: Int,
    val amount: Int,
    val level: Int,
)
