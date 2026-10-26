package com.axiel7.anihyou.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axiel7.anihyou.core.base.DataResult
import com.axiel7.anihyou.core.domain.repository.DefaultPreferencesRepository
import com.axiel7.anihyou.core.domain.repository.NotificationRepository
import com.axiel7.anihyou.core.domain.repository.UserRepository
import com.axiel7.anihyou.core.model.HomeTab
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HomeViewModel(
    private val userRepository: UserRepository,
    private val defaultPreferencesRepository: DefaultPreferencesRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    val unreadNotificationCount = userRepository.getUnreadNotificationCount()
        .map { it ?: 0 }

    private val _hasNewBellNotification = MutableStateFlow(false)
    val hasNewBellNotification = _hasNewBellNotification.asStateFlow()
    private val bellCheckMutex = Mutex()
    private var lastObservedNotificationId = 0
    private var bellDismissalGeneration = 0
    private var lastBellPressedAtSeconds = 0L
    private var bellMonitorJob: Job? = null

    /** Bell animates only once for each newly arriving AniList notification ID. */
    fun checkBellNotifications(unreadCount: Int) {
        if (unreadCount > 0) {
            viewModelScope.launch {
                try {
                    inspectLatestNotification(unreadCount)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // Keep the Home UI usable when notifications are unavailable.
                }
            }
        }
    }

    /** Refresh only while Home is resumed; do not poll while the app is idle. */
    fun startBellMonitoring() {
        if (bellMonitorJob?.isActive == true) return
        bellMonitorJob = viewModelScope.launch {
            while (isActive) {
                try {
                    userRepository.fetchUnreadNotificationCount()?.let { unreadCount ->
                        if (unreadCount > 0) inspectLatestNotification(unreadCount)
                        else _hasNewBellNotification.value = false
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // Offline or API errors must never restart the bell or crash Home.
                }
                delay(90_000L)
            }
        }
    }

    fun stopBellMonitoring() {
        bellMonitorJob?.cancel()
        bellMonitorJob = null
    }

    fun onBellPressed() {
        bellDismissalGeneration++
        val pressedAt = System.currentTimeMillis() / 1000L
        lastBellPressedAtSeconds = pressedAt
        _hasNewBellNotification.value = false
        viewModelScope.launch {
            defaultPreferencesRepository.setLastDismissedBellEpochSeconds(pressedAt)
        }
    }

    fun onLoggedOut() {
        stopBellMonitoring()
        bellDismissalGeneration++
        lastObservedNotificationId = 0
        lastBellPressedAtSeconds = 0L
        _hasNewBellNotification.value = false
    }

    private suspend fun inspectLatestNotification(unreadCount: Int) = bellCheckMutex.withLock {
        if (unreadCount <= 0) return@withLock
        val generationBeforeRequest = bellDismissalGeneration
        val latest = (notificationRepository.getNewNotifications(1) as? DataResult.Success)
            ?.data?.firstOrNull() ?: return@withLock
        val latestId = latest.id
        val persistedId = defaultPreferencesRepository.lastAnimatedBellNotificationId.first()
        val previousId = maxOf(persistedId, lastObservedNotificationId)

        // If the bell was pressed before a request finished, older events are
        // already acknowledged—even when the server's unread count is stale.
        val dismissedAt = maxOf(
            lastBellPressedAtSeconds,
            defaultPreferencesRepository.lastDismissedBellEpochSeconds.first(),
        )
        if (latest.createdAt != null && latest.createdAt.toLong() <= dismissedAt) {
            if (latestId > previousId) {
                lastObservedNotificationId = latestId
                defaultPreferencesRepository.setLastAnimatedBellNotificationId(latestId)
            }
            return@withLock
        }

        if (latestId > previousId) {
            lastObservedNotificationId = latestId
            // Persist before showing animation so reopening the app cannot replay it.
            defaultPreferencesRepository.setLastAnimatedBellNotificationId(latestId)
            if (generationBeforeRequest == bellDismissalGeneration) {
                _hasNewBellNotification.value = true
            }
        }
    }

    fun saveHomeTab(value: Int) {
        viewModelScope.launch {
            HomeTab.valueOf(value)?.let {
                defaultPreferencesRepository.setDefaultHomeTab(it)
            }
        }
    }
}
