package com.axiel7.anihyou.feature.home

import app.cash.turbine.test
import com.axiel7.anihyou.core.base.DataResult
import com.axiel7.anihyou.core.domain.repository.DefaultPreferencesRepository
import com.axiel7.anihyou.core.domain.repository.NotificationRepository
import com.axiel7.anihyou.core.domain.repository.UserRepository
import com.axiel7.anihyou.core.model.HomeTab
import com.axiel7.anihyou.core.model.notification.GenericNotification
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val userRepository: UserRepository = mockk()
    private val defaultPreferencesRepository: DefaultPreferencesRepository = mockk(relaxed = true)
    private val notificationRepository: NotificationRepository = mockk()
    private lateinit var viewModel: HomeViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { userRepository.getUnreadNotificationCount() } returns flowOf(5)
        every { defaultPreferencesRepository.lastDismissedBellEpochSeconds } returns flowOf(0L)
        viewModel = HomeViewModel(userRepository, defaultPreferencesRepository, notificationRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `unreadNotificationCount should emit values from userRepository`() = runTest {
        viewModel.unreadNotificationCount.test {
            assertEquals(5, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unreadNotificationCount should emit 0 when repository emits null`() = runTest {
        every { userRepository.getUnreadNotificationCount() } returns flowOf(null)
        // Re-initialize because flow is assigned at init
        viewModel = HomeViewModel(userRepository, defaultPreferencesRepository, notificationRepository)
        
        viewModel.unreadNotificationCount.test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveHomeTab should call defaultPreferencesRepository`() = runTest {
        val tabIndex = HomeTab.ACTIVITY_FEED.ordinal
        viewModel.saveHomeTab(tabIndex)

        coVerify { defaultPreferencesRepository.setDefaultHomeTab(HomeTab.ACTIVITY_FEED) }
    }

    @Test
    fun `bell animates for a new ID and stops immediately on press`() = runTest {
        every { defaultPreferencesRepository.lastAnimatedBellNotificationId } returns flowOf(100)
        coEvery { notificationRepository.getNewNotifications(1) } returns
            DataResult.Success(listOf(notification(101)))

        viewModel.checkBellNotifications(1)
        assertTrue(viewModel.hasNewBellNotification.value)
        coVerify { defaultPreferencesRepository.setLastAnimatedBellNotificationId(101) }

        viewModel.onBellPressed()
        assertFalse(viewModel.hasNewBellNotification.value)

        // An unchanged unread count and the same notification cannot wake it again.
        viewModel.checkBellNotifications(1)
        assertFalse(viewModel.hasNewBellNotification.value)
    }

    @Test
    fun `reopening with a previously animated notification stays quiet until a newer ID`() = runTest {
        every { defaultPreferencesRepository.lastAnimatedBellNotificationId } returns flowOf(101)
        coEvery { notificationRepository.getNewNotifications(1) } returns
            DataResult.Success(listOf(notification(101)))

        viewModel.checkBellNotifications(1)
        assertFalse(viewModel.hasNewBellNotification.value)

        coEvery { notificationRepository.getNewNotifications(1) } returns
            DataResult.Success(listOf(notification(102)))
        viewModel.checkBellNotifications(1)
        assertTrue(viewModel.hasNewBellNotification.value)
    }

    @Test
    fun `pressing bell silences older in-flight notifications`() = runTest {
        every { defaultPreferencesRepository.lastAnimatedBellNotificationId } returns flowOf(100)
        coEvery { notificationRepository.getNewNotifications(1) } returns
            DataResult.Success(listOf(notification(101, createdAt = 1)))

        viewModel.onBellPressed()
        viewModel.checkBellNotifications(1)
        assertFalse(viewModel.hasNewBellNotification.value)
    }

    private fun notification(id: Int, createdAt: Int? = null) = GenericNotification(
        id = id,
        text = "New episode",
        imageUrl = null,
        contentId = 1,
        type = null,
        createdAt = createdAt,
    )
}
