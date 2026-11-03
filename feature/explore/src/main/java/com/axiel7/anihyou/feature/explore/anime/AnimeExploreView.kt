package com.axiel7.anihyou.feature.explore.anime

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.axiel7.anihyou.core.model.media.ChartType
import com.axiel7.anihyou.core.model.media.currentAnimeSeason
import com.axiel7.anihyou.core.model.media.iconSmall
import com.axiel7.anihyou.core.model.media.nextAnimeSeason
import com.axiel7.anihyou.core.network.type.MediaSort
import com.axiel7.anihyou.core.network.type.MediaType
import com.axiel7.anihyou.core.resources.R
import com.axiel7.anihyou.core.ui.common.LocalNavActionManager
import com.axiel7.anihyou.core.ui.common.rememberSnackbarManager
import com.axiel7.anihyou.core.ui.composables.common.ErrorDialogHandler
import com.axiel7.anihyou.core.ui.composables.list.OnBottomReached
import com.axiel7.anihyou.core.ui.theme.AniHyouTheme
import com.axiel7.anihyou.feature.editmedia.EditMediaSheet
import com.axiel7.anihyou.feature.explore.explore.content.AiringContent
import com.axiel7.anihyou.feature.explore.explore.content.DiscoverMediaContent
import com.axiel7.anihyou.feature.explore.explore.content.SeasonAnimeContent
import org.koin.compose.viewmodel.koinActivityViewModel
import java.time.LocalDateTime

enum class AnimeDiscoverInfo {
    AIRING,
    THIS_SEASON,
    TRENDING_ANIME,
    NEXT_SEASON,
    POPULAR_ANIME,
    NEWLY_ANIME
}

@Composable
fun AnimeDiscoverView(
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val viewModel: AnimeExploreViewModel = koinActivityViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnimeDiscoverContent(
        isLoggedIn = isLoggedIn,
        uiState = uiState,
        event = viewModel,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AnimeDiscoverContent(
    isLoggedIn: Boolean,
    uiState: AnimeExploreUiState,
    event: AnimeExploreEvent?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val navActionManager = LocalNavActionManager.current
    val snackbarManager = rememberSnackbarManager()
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    listState.OnBottomReached(
        buffer = 0,
        onLoadMore = { event?.addNextInfo() }
    )

    val haptic = LocalHapticFeedback.current
    var showEditSheet by rememberSaveable {
        mutableStateOf(false)
    }

    fun showEditSheetAction() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        if (isLoggedIn) {
            showEditSheet = true
        } else {
            snackbarManager.showNotLoggedInSnackbar()
        }
    }

    if (showEditSheet && uiState.selectedMediaDetails != null) {
        EditMediaSheet(
            mediaDetails = uiState.selectedMediaDetails,
            listEntry = uiState.selectedMediaListEntry,
            onEntryUpdated = { newListEntry ->
                event?.onUpdateListEntry(newListEntry)
            },
            onDismissed = {
                showEditSheet = false
            }
        )
    }

    ErrorDialogHandler(
        uiState,
        onDismiss = { event?.onErrorDisplayed() },
        isLoggedIn = isLoggedIn
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = snackbarManager::SnackbarHost,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { event?.refresh() },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isLoading,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {

            LazyColumn(
                modifier = modifier,
                state = listState,
                contentPadding = contentPadding
            ) {

                item {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            )
                    ) {

                        AssistChip(
                            shape = RoundedCornerShape(14.dp),
                            onClick = {
                                navActionManager.toAnimeSeason(
                                    uiState.nowAnimeSeason.year,
                                    uiState.nowAnimeSeason.season
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.season)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(
                                        uiState.nowAnimeSeason.season.iconSmall()
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(
                                        AssistChipDefaults.IconSize
                                    )
                                )
                            }
                        )

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        AssistChip(
                            shape = RoundedCornerShape(14.dp),
                            onClick = {
                                navActionManager.toCalendar()
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.calendar)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(
                                        R.drawable.calendar_month_20
                                    ),
                                    contentDescription = null
                                )
                            }
                        )

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        ChartType.animeCharts.forEach { chartType ->

                            AssistChip(
                                shape = RoundedCornerShape(14.dp),
                                onClick = {
                                    navActionManager.toMediaChart(chartType)
                                },
                                label = {
                                    Text(
                                        text = chartType.localized()
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(
                                            chartType.icon()
                                        ),
                                        contentDescription = null
                                    )
                                }
                            )

                            Spacer(
                                modifier = Modifier.width(12.dp)
                            )
                        }
                    }
                }

                items(uiState.infos) { item ->

                    when (item) {

                        AnimeDiscoverInfo.AIRING -> {

                            LaunchedEffect(uiState.airingOnMyList) {
                                if (uiState.airingOnMyList == true) {
                                    event?.fetchAiringAnimeOnMyList()
                                } else if (uiState.airingOnMyList == false) {
                                    event?.fetchAiringAnime()
                                }
                            }

                            AiringContent(
                                airingOnMyList = uiState.airingOnMyList,
                                airingAnime = uiState.airingAnime,
                                airingAnimeOnMyList = uiState.airingAnimeOnMyList,
                                isLoading = uiState.isLoadingAiring,
                                onLongClickItem = { details, listEntry ->
                                    event?.selectItem(details, listEntry)
                                    showEditSheetAction()
                                },
                                navigateToCalendar = navActionManager::toCalendar,
                                navigateToMediaDetails = navActionManager::toMediaDetails
                            )
                        }

                        AnimeDiscoverInfo.THIS_SEASON -> {

                            // Kod asal:
                            // LaunchedEffect(uiState.currentSeason) {
                            //     event?.fetchSeasonAnime()
                            // }

                            LaunchedEffect(uiState.nowAnimeSeason) {
                                event?.fetchThisSeasonAnime()
                            }

                            SeasonAnimeContent(
                                animeSeason = uiState.nowAnimeSeason,
                                seasonAnime = uiState.thisSeasonAnime,
                                isLoading = uiState.isLoadingThisSeason,
                                isNextSeason = false,
                                onLongClickItem = { media ->
                                    event?.selectItem(
                                        details = media.basicMediaDetails,
                                        listEntry = media.mediaListEntry?.basicMediaListEntry
                                    )
                                    showEditSheetAction()
                                },
                                navigateToAnimeSeason = { season ->
                                    navActionManager.toAnimeSeason(
                                        season.year,
                                        season.season
                                    )
                                },
                                navigateToMediaDetails = navActionManager::toMediaDetails
                            )
                        }

                        AnimeDiscoverInfo.TRENDING_ANIME -> {

                            LaunchedEffect(MediaType.ANIME) {
                                event?.fetchTrendingAnime()
                            }

                            DiscoverMediaContent(
                                title = stringResource(R.string.trending_anime),
                                media = uiState.trendingAnime,
                                isLoading = uiState.isLoadingTrendingAnime,
                                onLongClickItem = { media ->
                                    event?.selectItem(
                                        details = media.basicMediaDetails,
                                        listEntry = media.mediaListEntry?.basicMediaListEntry
                                    )
                                    showEditSheetAction()
                                },
                                onClickHeader = {
                                    navActionManager.toExplore(
                                        MediaType.ANIME,
                                        MediaSort.TRENDING_DESC
                                    )
                                },
                                navigateToMediaDetails = navActionManager::toMediaDetails
                            )
                        }

                        AnimeDiscoverInfo.NEXT_SEASON -> {

                            // Kod asal lama dikekalkan sebagai komen:
                            // currentSeason dan parameter title/media asal.

                            LaunchedEffect(uiState.nextAnimeSeason) {
                                event?.fetchNextSeasonAnime()
                            }

                            SeasonAnimeContent(
                                animeSeason = uiState.nextAnimeSeason,
                                seasonAnime = uiState.nextSeasonAnime,
                                isLoading = uiState.isLoadingNextSeason,
                                isNextSeason = true,
                                onLongClickItem = { media ->
                                    event?.selectItem(
                                        details = media.basicMediaDetails,
                                        listEntry = media.mediaListEntry?.basicMediaListEntry
                                    )
                                    showEditSheetAction()
                                },
                                navigateToAnimeSeason = { season ->
                                    navActionManager.toAnimeSeason(
                                        season.year,
                                        season.season
                                    )
                                },
                                navigateToMediaDetails = navActionManager::toMediaDetails
                            )
                        }

                        AnimeDiscoverInfo.POPULAR_ANIME -> {

                            LaunchedEffect(MediaType.ANIME) {
                                event?.fetchPopularAnime()
                            }

                            DiscoverMediaContent(
                                title = stringResource(R.string.top_popular),
                                media = uiState.popularAnime,
                                isLoading = uiState.isLoadingPopularAnime,
                                onLongClickItem = { media ->
                                    event?.selectItem(
                                        details = media.basicMediaDetails,
                                        listEntry = media.mediaListEntry?.basicMediaListEntry
                                    )
                                    showEditSheetAction()
                                },
                                onClickHeader = {
                                    navActionManager.toMediaChart(
                                        ChartType.POPULAR_ANIME
                                    )
                                },
                                navigateToMediaDetails = navActionManager::toMediaDetails
                            )
                        }

                        AnimeDiscoverInfo.NEWLY_ANIME -> {

                            LaunchedEffect(MediaType.ANIME) {
                                event?.fetchNewlyAnime()
                            }

                            DiscoverMediaContent(
                                title = stringResource(R.string.newly_anime),
                                media = uiState.newlyAnime,
                                isLoading = uiState.isLoadingNewlyAnime,
                                onLongClickItem = { media ->
                                    event?.selectItem(
                                        details = media.basicMediaDetails,
                                        listEntry = media.mediaListEntry?.basicMediaListEntry
                                    )
                                    showEditSheetAction()
                                },
                                onClickHeader = {
                                    navActionManager.toExplore(
                                        MediaType.ANIME,
                                        MediaSort.ID_DESC
                                    )
                                },
                                navigateToMediaDetails = navActionManager::toMediaDetails
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AnimeDiscoverContentPreview() {
    AniHyouTheme {
        val now = LocalDateTime.now()

        AnimeDiscoverContent(
            isLoggedIn = false,
            uiState = AnimeExploreUiState(
                infos = mutableStateListOf(),
                nowAnimeSeason = now.currentAnimeSeason(),
                nextAnimeSeason = now.nextAnimeSeason()
            ),
            event = null
        )
    }
}
