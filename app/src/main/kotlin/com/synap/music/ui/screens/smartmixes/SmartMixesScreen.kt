package com.synap.music.ui.screens.smartmixes

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synap.compose.routing.RouteHandlerScope
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.engine.SmartPlaylistEngine
import com.synap.music.models.Song
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.themed.MarqueeText
import com.synap.music.ui.components.themed.NonQueuedMediaItemMenu
import com.synap.music.ui.components.themed.Scaffold
import androidx.compose.ui.draw.shadow
import com.synap.music.ui.styling.rememberPulsatingHybridColor
import com.synap.music.ui.items.SongItem
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.bold
import com.synap.music.utils.color
import com.synap.music.utils.forcePlayAtIndex
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.toast
import kotlinx.coroutines.launch

val smartMixesList = listOf(
    SmartPlaylistEngine.MixType.MostPlayed to R.drawable.trending,
    SmartPlaylistEngine.MixType.RecentlySaved to R.drawable.heart,
    SmartPlaylistEngine.MixType.OfflineVault to R.drawable.airplane,
    SmartPlaylistEngine.MixType.HighEnergy to R.drawable.lightning,
    SmartPlaylistEngine.MixType.ChillVibes to R.drawable.sparkles
)

/**
 * Dedicated Full-Screen Smart Mixes Page.
 * Uses SynapMusic's standard Scaffold top bar (exact same as Vault page) with 2-column adaptive tiles.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun RouteHandlerScope.SmartMixesScreen() {
    val context = LocalContext.current
    val (colorPalette, typography) = LocalAppearance.current
    val menuState = LocalMenuState.current
    val binder = LocalPlayerServiceBinder.current
    val coroutineScope = rememberCoroutineScope()

    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    var generatedSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val lazyGridState = rememberLazyGridState()
    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val thumbnailSizeDp = 56.dp
    val thumbnailSizePx = thumbnailSizeDp.px

    val currentMix = smartMixesList.getOrElse(selectedIndex) { smartMixesList[0] }
    val currentMixType = currentMix.first
    val tileHybridPulse = rememberPulsatingHybridColor(primaryColor = colorPalette.accent, secondaryColor = colorPalette.blue)

    fun loadMix(type: SmartPlaylistEngine.MixType) {
        isLoading = true
        coroutineScope.launch {
            generatedSongs = SmartPlaylistEngine.generateMix(type)
            isLoading = false
        }
    }

    LaunchedEffect(selectedIndex) {
        loadMix(currentMixType)
    }

    Scaffold(
        title = {
            BasicText(
                text = "Smart Mixes",
                style = typography.l.bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        topStartIconButtonId = R.drawable.chevron_back,
        onTopStartIconButtonClick = pop,
        tabIndex = 0,
        onTabChanged = { },
        tabColumnContent = { } // No bottom navigation rail
    ) {
        val gridPaddingValues = PaddingValues(
            top = paddingValues.calculateTopPadding() + 72.dp + 12.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp,
            start = 16.dp,
            end = 16.dp
        )

        // Adaptive 2-Column Grid of Smart Mix Tiles + Song List
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Fixed(2),
            contentPadding = gridPaddingValues,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            // Render the 5 Adaptive Mix Tile Cards
            itemsIndexed(
                items = smartMixesList,
                span = { index, _ ->
                    if (index == smartMixesList.lastIndex) GridItemSpan(2) else GridItemSpan(1)
                }
            ) { idx, (mixType, iconRes) ->
                val isSelected = selectedIndex == idx
                val cardShape = RoundedCornerShape(14.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .shadow(if (isSelected) 8.dp else 2.dp, cardShape, spotColor = tileHybridPulse)
                        .clip(cardShape)
                        .background(if (isSelected) tileHybridPulse else colorPalette.background0)
                        .border(
                            1.dp,
                            if (isSelected) tileHybridPulse else colorPalette.textDisabled.copy(alpha = 0.4f),
                            cardShape
                        )
                        .clickable {
                            selectedIndex = idx
                            loadMix(mixType)
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) colorPalette.onAccent.copy(alpha = 0.2f) else tileHybridPulse.copy(alpha = 0.15f))
                                    .border(1.dp, if (isSelected) colorPalette.onAccent.copy(alpha = 0.4f) else tileHybridPulse.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(if (isSelected) colorPalette.onAccent else tileHybridPulse),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                MarqueeText(
                                    text = mixType.label,
                                    style = if (isSelected) typography.xs.semiBold.color(colorPalette.onAccent) else typography.xs.semiBold
                                )
                                MarqueeText(
                                    text = mixType.description,
                                    style = if (isSelected) typography.xxs.semiBold.color(colorPalette.onAccent) else typography.xxs.secondary
                                )
                            }
                        }
                        if (isSelected) {
                            Image(
                                painter = painterResource(id = R.drawable.checkmark),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPalette.onAccent),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Action Bar: Play All & Save Playlist
            item(span = { GridItemSpan(2) }) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    // Play Mix Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorPalette.background0)
                            .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable(enabled = generatedSongs.isNotEmpty()) {
                                if (generatedSongs.isNotEmpty()) {
                                    binder?.stopRadio()
                                    binder?.player?.forcePlayAtIndex(generatedSongs.map { it.asMediaItem }, 0)
                                    context.toast("Playing ${currentMixType.label}")
                                } else {
                                    context.toast("No tracks available in ${currentMixType.label}")
                                }
                            }
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.play),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPalette.text),
                                modifier = Modifier.size(20.dp)
                            )
                            MarqueeText(
                                text = "Play Mix (${generatedSongs.size})",
                                style = typography.xs.semiBold,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }

                    // Save Playlist Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorPalette.background0)
                            .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable(enabled = generatedSongs.isNotEmpty()) {
                                if (generatedSongs.isNotEmpty()) {
                                    coroutineScope.launch {
                                        val id = SmartPlaylistEngine.saveMixAsPlaylist(currentMixType.label, generatedSongs)
                                        if (id > 0) {
                                            context.toast("Saved '${currentMixType.label}' to playlists!")
                                        }
                                    }
                                } else {
                                    context.toast("No tracks available in ${currentMixType.label}")
                                }
                            }
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.playlist),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPalette.text),
                                modifier = Modifier.size(20.dp)
                            )
                            MarqueeText(
                                text = "Save as Playlist",
                                style = typography.xs.semiBold,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }
            }

            // Generated Songs Header / Loading
            item(span = { GridItemSpan(2) }) {
                if (isLoading) {
                    BasicText(
                        text = "Generating ${currentMixType.label}...",
                        style = typography.s.secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                } else if (generatedSongs.isEmpty()) {
                    BasicText(
                        text = "No tracks found for ${currentMixType.label}",
                        style = typography.s.secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                } else {
                    BasicText(
                        text = "${currentMixType.label} Tracks",
                        style = typography.m.semiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                }
            }

            // Song List Items
            itemsIndexed(
                items = generatedSongs,
                key = { _, song -> song.id },
                span = { _, _ -> GridItemSpan(2) }
            ) { index, song ->
                SongItem(
                    song = song,
                    thumbnailSizePx = thumbnailSizePx,
                    thumbnailSizeDp = thumbnailSizeDp,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                binder?.stopRadio()
                                binder?.player?.forcePlayAtIndex(generatedSongs.map { it.asMediaItem }, index)
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
}
