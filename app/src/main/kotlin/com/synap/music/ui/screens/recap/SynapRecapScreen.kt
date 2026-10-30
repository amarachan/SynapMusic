package com.synap.music.ui.screens.recap

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synap.music.Database
import com.synap.music.ui.components.themed.ConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.synap.compose.routing.RouteHandlerScope
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.engine.AnalyticsEngine
import com.synap.music.engine.RecapData
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.themed.MarqueeText
import com.synap.music.ui.components.themed.NonQueuedMediaItemMenu
import com.synap.music.ui.components.themed.Scaffold
import com.synap.music.ui.items.SongItem
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.bold
import com.synap.music.utils.center
import com.synap.music.utils.color
import com.synap.music.utils.forcePlayAtIndex
import com.synap.music.utils.medium
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.toast

/**
 * Dedicated Full-Screen Synap Recap Listening Analytics Dashboard.
 * Displays on-device listening stats, top songs, top artists, and peak listening habits.
 * Includes header controls to reset/clear listening analytics statistics and play history.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun RouteHandlerScope.SynapRecapScreen() {
    val context = LocalContext.current
    val (colorPalette, typography) = LocalAppearance.current
    val menuState = LocalMenuState.current
    val binder = LocalPlayerServiceBinder.current

    val coroutineScope = rememberCoroutineScope()
    var recapData by remember { mutableStateOf<RecapData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isShowingClearConfirmation by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current

    val thumbnailSizeDp = 56.dp
    val thumbnailSizePx = thumbnailSizeDp.px

    LaunchedEffect(Unit) {
        recapData = AnalyticsEngine.getRecapData()
        isLoading = false
    }

    Scaffold(
        title = {
            BasicText(
                text = "Synap Recap",
                style = typography.l.bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        topStartIconButtonId = R.drawable.chevron_back,
        onTopStartIconButtonClick = pop,
        topEndIconButtonId = if (recapData != null && (recapData?.totalListeningTimeMs ?: 0L) > 0L) R.drawable.trash else null,
        onTopEndIconButtonClick = { isShowingClearConfirmation = true },
        tabIndex = 0,
        onTabChanged = { },
        tabColumnContent = { } // No bottom navigation rail
    ) {
        val listPaddingValues = PaddingValues(
            start = paddingValues.calculateStartPadding(layoutDirection) + 16.dp,
            top = paddingValues.calculateTopPadding() + 72.dp + 12.dp,
            end = paddingValues.calculateEndPadding(layoutDirection) + 16.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        )

        Box(
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            if (isShowingClearConfirmation) {
                ConfirmationDialog(
                    text = "Are you sure you want to clear all Synap Recap listening analytics and play history statistics?",
                    confirmText = "Clear Stats",
                    cancelText = "Cancel",
                    onDismiss = { isShowingClearConfirmation = false },
                    onConfirm = {
                        isShowingClearConfirmation = false
                        coroutineScope.launch(Dispatchers.IO) {
                            Database.clearEvents()
                            Database.clearTotalPlayTimeMs()
                            val newData = AnalyticsEngine.getRecapData()
                            withContext(Dispatchers.Main) {
                                recapData = newData
                                context.toast("Synap Recap stats cleared")
                            }
                        }
                    }
                )
            }

            if (isLoading) {
                BasicText(
                    text = "Computing your Synap Recap...",
                    style = typography.s.secondary.center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                )
            } else {
                val data = recapData ?: return@Scaffold
                if (data.totalListeningTimeMs == 0L) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.sparkles),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette.accent),
                            modifier = Modifier
                                .size(64.dp)
                                .padding(bottom = 16.dp)
                        )
                        BasicText(
                            text = "No Listening Stats Yet",
                            style = typography.s.bold.center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        BasicText(
                            text = "Stream some songs in SynapMusic to generate your personal listening analytics & recap!",
                            style = typography.xs.secondary.center
                        )
                    }
                } else {
                    val totalHours = data.totalListeningTimeMs / (1000 * 60 * 60)
                    val totalMinutes = (data.totalListeningTimeMs / (1000 * 60)) % 60

                LazyColumn(
                    state = lazyListState,
                    contentPadding = listPaddingValues,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Big Stat Summary Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colorPalette.accent)
                                .padding(20.dp)
                        ) {
                            Column {
                                BasicText(
                                    text = "TOTAL LISTENING TIME",
                                    style = typography.xxs.semiBold.color(colorPalette.onAccent)
                                )
                                BasicText(
                                    text = if (totalHours > 0) "${totalHours}h ${totalMinutes}m" else "${totalMinutes}m",
                                    style = typography.xxl.bold.color(colorPalette.onAccent),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicText(
                                        text = "${data.totalSongsCount} Unique Songs Streamed",
                                        style = typography.xs.medium.color(colorPalette.onAccent)
                                    )
                                }
                            }
                        }
                    }

                    // Listening Habit & Vibe Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorPalette.background0)
                                .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    BasicText(
                                        text = "YOUR LISTENING VIBE",
                                        style = typography.xxs.secondary
                                    )
                                    BasicText(
                                        text = data.peakListeningHabit,
                                        style = typography.m.semiBold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Image(
                                    painter = painterResource(id = R.drawable.sparkles),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(colorPalette.accent),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Top Tracks Leaderboard Section Header
                    if (data.topSongs.isNotEmpty()) {
                        item {
                            BasicText(
                                text = "Top 5 Streamed Tracks",
                                style = typography.m.semiBold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        itemsIndexed(
                            items = data.topSongs,
                            key = { _, song -> song.id }
                        ) { index, song ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Rank Badge (#1, #2, #3, etc.)
                                val rankBadgeColor = when (index) {
                                    0 -> colorPalette.accent // Gold/Accent #1
                                    1 -> colorPalette.textDisabled // Silver #2
                                    2 -> colorPalette.textDisabled.copy(alpha = 0.7f) // Bronze #3
                                    else -> colorPalette.textSecondary
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(rankBadgeColor.copy(alpha = 0.2f))
                                ) {
                                    BasicText(
                                        text = "#${index + 1}",
                                        style = typography.xs.semiBold.color(rankBadgeColor)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                SongItem(
                                    song = song,
                                    thumbnailSizePx = thumbnailSizePx,
                                    thumbnailSizeDp = thumbnailSizeDp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .combinedClickable(
                                            onClick = {
                                                binder?.stopRadio()
                                                binder?.player?.forcePlayAtIndex(data.topSongs.map { it.asMediaItem }, index)
                                            },
                                            onLongClick = {
                                                menuState.display {
                                                    NonQueuedMediaItemMenu(
                                                        mediaItem = song.asMediaItem,
                                                        onDismiss = menuState::hide
                                                    )
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }

                    // Top Artists Section
                    if (data.topArtists.isNotEmpty()) {
                        item {
                            BasicText(
                                text = "Top 5 Most Played Artists",
                                style = typography.m.semiBold,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }

                        itemsIndexed(
                            items = data.topArtists,
                            key = { _, (artist, _) -> artist }
                        ) { index, (artistName, timeMs) ->
                            val mins = timeMs / (1000 * 60)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colorPalette.background0)
                                    .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        BasicText(
                                            text = "${index + 1}",
                                            style = typography.m.bold.color(colorPalette.accent)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            MarqueeText(
                                                text = artistName,
                                                style = typography.xs.semiBold
                                            )
                                            BasicText(
                                                text = "$mins minutes listened",
                                                style = typography.xxs.secondary
                                            )
                                        }
                                    }
                                    Image(
                                        painter = painterResource(id = R.drawable.person),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
