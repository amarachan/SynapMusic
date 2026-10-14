package com.synap.music.ui.screens.player

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import com.valentinilk.shimmer.shimmer
import com.synap.pulse.SynapService
import com.synap.pulse.requests.lyrics
import com.synap.pulse.models.bodies.NextBody
import com.synap.lyrics.SkyLyrics
import com.synap.lyrics.LrcLib
import com.synap.music.Database
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.models.Lyrics
import com.synap.music.query
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.themed.Menu
import com.synap.music.ui.components.themed.MenuEntry
import com.synap.music.ui.components.themed.TextFieldDialog
import com.synap.music.ui.components.themed.TextPlaceholder
import com.synap.music.ui.styling.DefaultDarkColorPalette
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.PureBlackColorPalette
import com.synap.music.ui.styling.onOverlayShimmer
import com.synap.music.utils.SynchronizedLyrics
import com.synap.music.utils.center
import com.synap.music.utils.color
import com.synap.music.ui.components.themed.ValueSelectorDialog
import com.synap.music.enums.LyricsLanguage
import com.synap.music.lyrics.LyricsTranslation
import com.synap.music.utils.isShowingSynchronizedLyricsKey
import com.synap.music.utils.lyricsLanguageKey
import com.synap.music.utils.medium
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.toast
import com.synap.music.utils.verticalFadingEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private fun cleanLyricsQuery(text: String?): String {
    if (text.isNullOrBlank()) return ""
    return text
        .replace(Regex("(?i)\\(official\\s*(music)?\\s*(video|audio|lyric\\s*video)?\\)"), "")
        .replace(Regex("(?i)\\[official\\s*(music)?\\s*(video|audio|lyric\\s*video)?\\]"), "")
        .replace(Regex("(?i)\\b(from|from the movie|from the film)\\s*[\"'].*?[\"']"), "")
        .replace(Regex("(?i)\\bft\\.?\\s*.*"), "")
        .replace(Regex("(?i)\\bfeat\\.?\\s*.*"), "")
        .replace(Regex("(?i)\\b - Topic\\b"), "")
        .replace(Regex("(?i)\\(lyric\\s*video\\)"), "")
        .replace(Regex("(?i)\\[lyric\\s*video\\]"), "")
        .replace(Regex("(?i)\\(audio\\)"), "")
        .replace(Regex("(?i)\\[audio\\]"), "")
        .replace(Regex("\\|.*"), "")
        .trim()
}

/**
 * The Lyrics UI.
 * This screen handles the automatic fetching, editing, and rendering 
 * of both plain-text and synchronized (karaoke-style) lyrics.
 */
@Composable
fun Lyrics(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    size: Dp,
    mediaMetadataProvider: () -> MediaMetadata,
    durationProvider: () -> Long,
    ensureSongInserted: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isDisplayed,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val (colorPalette, typography) = LocalAppearance.current
        val context = LocalContext.current
        val menuState = LocalMenuState.current
        val currentView = LocalView.current

        var isShowingSynchronizedLyrics by rememberPreference(isShowingSynchronizedLyricsKey, true)
        var lyricsLanguage by rememberPreference(lyricsLanguageKey, LyricsLanguage.Auto)

        var isEditing by remember(mediaId, isShowingSynchronizedLyrics) {
            mutableStateOf(false)
        }

        var lyrics by remember {
            mutableStateOf<Lyrics?>(null)
        }

        var translatedText by remember {
            mutableStateOf<String?>(null)
        }

        val rawText = if (isShowingSynchronizedLyrics) lyrics?.synced else lyrics?.fixed
        val text = translatedText ?: rawText

        LaunchedEffect(rawText, lyricsLanguage) {
            if (rawText != null && lyricsLanguage != LyricsLanguage.Auto) {
                translatedText = LyricsTranslation.translateLrc(rawText, lyricsLanguage)
            } else {
                translatedText = null
            }
        }

        var isError by remember(mediaId, isShowingSynchronizedLyrics) {
            mutableStateOf(false)
        }

        var isSelectingLanguage by remember {
            mutableStateOf(false)
        }

        /**
         * The Lyrics Fetcher.
         * We use a tiered strategy to find lyrics:
         * 1. Check the local Database first.
         * 2. Hit LRCLIB (Global, high quality synchronized lyrics).
         * 3. Hit SkyLyrics (Reliable fallback).
         * 4. Hit YouTube's internal lyrics as a final plain-text fallback.
         */
        LaunchedEffect(mediaId, isShowingSynchronizedLyrics) {
            withContext(Dispatchers.IO) {
                Database.lyrics(mediaId).collect { currentLyrics ->
                    withContext(Dispatchers.Main) {
                        lyrics = currentLyrics
                    }

                    val hasSynced = currentLyrics?.synced?.isNotBlank() == true
                    val hasFixed = currentLyrics?.fixed?.isNotBlank() == true

                    if (hasSynced || (!isShowingSynchronizedLyrics && hasFixed)) {
                        withContext(Dispatchers.Main) {
                            isError = false
                        }
                        return@collect
                    }

                    // Non-blocking metadata resolution with 1.5s max timeout
                    var mediaMetadata = mediaMetadataProvider()
                    var duration = withContext(Dispatchers.Main) { durationProvider() }
                    var waitCount = 0

                    while ((duration == C.TIME_UNSET || mediaMetadata.title?.toString()?.isBlank() == true) && waitCount < 15) {
                        delay(100)
                        waitCount++
                        mediaMetadata = mediaMetadataProvider()
                        duration = withContext(Dispatchers.Main) { durationProvider() }
                    }

                    // Fallback to Database song if metadata is still empty
                    val dbSong = Database.song(mediaId).firstOrNull()
                    val rawArtist = mediaMetadata.artist?.toString()?.ifBlank { dbSong?.artistsText } ?: dbSong?.artistsText ?: ""
                    val rawTitle = mediaMetadata.title?.toString()?.ifBlank { dbSong?.title } ?: dbSong?.title ?: ""
                    val rawAlbum = mediaMetadata.albumTitle?.toString()

                    var cleanArtist = cleanLyricsQuery(rawArtist)
                    var cleanTitle = cleanLyricsQuery(rawTitle)
                    val cleanAlbum = cleanLyricsQuery(rawAlbum)

                    // If YouTube title contains "Artist - Title", split it automatically!
                    if (cleanTitle.contains("-")) {
                        val parts = cleanTitle.split("-", limit = 2)
                        val titleArtistPart = parts[0].trim()
                        val titleSongPart = parts[1].trim()

                        if (titleSongPart.isNotBlank()) {
                            cleanTitle = titleSongPart
                            if (cleanArtist.isBlank() || cleanArtist == "YouTube Music") {
                                cleanArtist = titleArtistPart
                            }
                        }
                    }

                    val queryTitle = cleanTitle.ifBlank { rawTitle }
                    val queryArtist = cleanArtist.ifBlank { rawArtist }
                    val validDurationSec = if (duration != C.TIME_UNSET && duration > 0) duration / 1000 else 0L

                    // Parallel execution: Query LRCLIB, SkyLyrics, and YouTube Music simultaneously
                    val (lrcRes, skyRes, ytRes) = coroutineScope {
                        val lrcDeferred = async {
                            runCatching {
                                LrcLib.getLyrics(
                                    artist = queryArtist,
                                    title = queryTitle,
                                    album = cleanAlbum,
                                    duration = validDurationSec
                                ).getOrNull()?.value
                            }.getOrNull()
                        }

                        val skyDeferred = async {
                            runCatching {
                                SkyLyrics.lyrics(
                                    artist = queryArtist,
                                    title = queryTitle,
                                    duration = validDurationSec
                                )?.getOrNull()?.toString()
                            }.getOrNull()
                        }

                        val ytDeferred = async {
                            runCatching {
                                SynapService.lyrics(NextBody(videoId = mediaId))?.getOrNull()
                            }.getOrNull()
                        }

                        Triple(lrcDeferred.await(), skyDeferred.await(), ytDeferred.await())
                    }

                    if (lrcRes?.isNotBlank() == true) {
                        ensureSongInserted()
                        Database.upsert(
                            Lyrics(
                                songId = mediaId,
                                fixed = currentLyrics?.fixed,
                                synced = lrcRes
                            )
                        )
                        return@collect
                    }

                    if (skyRes?.isNotBlank() == true) {
                        ensureSongInserted()
                        Database.upsert(
                            Lyrics(
                                songId = mediaId,
                                fixed = currentLyrics?.fixed,
                                synced = skyRes
                            )
                        )
                        return@collect
                    }

                    if (ytRes?.isNotBlank() == true) {
                        ensureSongInserted()
                        Database.upsert(
                            Lyrics(
                                songId = mediaId,
                                fixed = ytRes,
                                synced = ytRes
                            )
                        )
                        return@collect
                    }

                    withContext(Dispatchers.Main) {
                        isError = true
                    }
                }
            }
        }

        if (isEditing) {
            TextFieldDialog(
                hintText = "Enter the lyrics",
                initialTextInput = text ?: "",
                singleLine = false,
                maxLines = 10,
                isTextInputValid = { true },
                onDismiss = { isEditing = false },
                onDone = {
                    query {
                        ensureSongInserted()
                        Database.upsert(
                            Lyrics(
                                songId = mediaId,
                                fixed = if (isShowingSynchronizedLyrics) lyrics?.fixed else it,
                                synced = if (isShowingSynchronizedLyrics) it else lyrics?.synced,
                            )
                        )
                    }
                }
            )
        }

        // Keep the screen awake when showing karaoke lyrics so it doesn't dim while you sing.
        if (isShowingSynchronizedLyrics) {
            DisposableEffect(Unit) {
                currentView.keepScreenOn = true
                onDispose {
                    currentView.keepScreenOn = false
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onDismiss() }
                    )
                }
                .fillMaxSize()
                .background(Color.Black.copy(0.8f))
        ) {
            AnimatedVisibility(
                visible = isError && text == null,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
            ) {
                BasicText(
                    text = "An error has occurred while fetching the ${if (isShowingSynchronizedLyrics) "synchronized " else ""}lyrics",
                    style = typography.xs.center.medium.color(PureBlackColorPalette.text),
                    modifier = Modifier
                        .background(Color.Black.copy(0.4f))
                        .padding(all = 8.dp)
                        .fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = text?.let(String::isEmpty) ?: false,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
            ) {
                val errorMessage = if (isShowingSynchronizedLyrics) "Synchronized lyrics" else "Lyrics"
                BasicText(
                    text = "$errorMessage are not available for this song",
                    style = typography.xs.center.medium.color(PureBlackColorPalette.text),
                    modifier = Modifier
                        .background(Color.Black.copy(0.4f))
                        .padding(all = 8.dp)
                        .fillMaxWidth()
                )
            }

            if (text?.isNotEmpty() == true) {
                if (isShowingSynchronizedLyrics) {
                    /**
                     * The Karaoke Renderer.
                     * It automatically scrolls the list to keep the current sentence 
                     * centered and highlighted.
                     */
                    val density = LocalDensity.current
                    val player = LocalPlayerServiceBinder.current?.player
                        ?: return@AnimatedVisibility

                    val synchronizedLyrics = remember(text, mediaId) {
                        val durationMs = durationProvider().let { if (it <= 0L || it == C.TIME_UNSET) 180000L else it }
                        val parsedSentences = SkyLyrics.Lyrics(text).sentences
                        val hasValidTimestamps = parsedSentences.any { it.first > 0 }

                        val finalSentences = if (hasValidTimestamps) {
                            parsedSentences
                        } else {
                            val totalLines = parsedSentences.size.coerceAtLeast(1)
                            parsedSentences.mapIndexed { idx, pair ->
                                val timestamp = (idx * durationMs) / totalLines
                                timestamp to pair.second
                            }
                        }

                        SynchronizedLyrics(finalSentences) {
                            player.currentPosition
                        }
                    }

                    val lazyListState = rememberLazyListState(
                        synchronizedLyrics.index,
                        with(density) { size.roundToPx() } / 3
                    )

                    LaunchedEffect(synchronizedLyrics, player) {
                        val center = with(density) { size.roundToPx() } / 3

                        while (isActive) {
                            delay(50)
                            if (synchronizedLyrics.update()) {
                                lazyListState.animateScrollToItem(
                                    synchronizedLyrics.index,
                                    center
                                )
                            }
                        }
                    }

                    LazyColumn(
                        state = lazyListState,
                        userScrollEnabled = true,
                        contentPadding = PaddingValues(vertical = size / 2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .verticalFadingEdge()
                    ) {
                        itemsIndexed(items = synchronizedLyrics.sentences) { index, sentence ->
                            val isActive = index == synchronizedLyrics.index
                            val isPast = index < synchronizedLyrics.index
                            KaraokeLyricsLine(
                                text = sentence.second,
                                isActive = isActive,
                                isPast = isPast,
                                onClick = {
                                    val timestamp = sentence.first
                                    if (timestamp >= 0) {
                                        player.seekTo(timestamp)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // Plain text renderer.
                    BasicText(
                        text = text,
                        style = typography.xs.center.medium.color(PureBlackColorPalette.text),
                        modifier = Modifier
                            .verticalFadingEdge()
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                            .padding(vertical = size / 4, horizontal = 32.dp)
                    )
                }
            }

            if (text == null && !isError) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .shimmer()
                ) {
                    repeat(4) {
                        TextPlaceholder(
                            color = colorPalette.onOverlayShimmer,
                            modifier = Modifier
                                .alpha(1f - it * 0.2f)
                        )
                    }
                }
            }

            Image(
                painter = painterResource(R.drawable.ellipsis_horizontal),
                contentDescription = null,
                colorFilter = ColorFilter.tint(DefaultDarkColorPalette.text),
                modifier = Modifier
                    .padding(all = 4.dp)
                    .clickable(
                        indication = rememberRipple(bounded = false),
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            menuState.display {
                                Menu {
                                    MenuEntry(
                                        icon = R.drawable.time,
                                        text = "Show ${if (isShowingSynchronizedLyrics) "un" else ""}synchronized lyrics",
                                        secondaryText = if (isShowingSynchronizedLyrics) null else "Provided by SkyLyrics",
                                        onClick = {
                                            menuState.hide()
                                            isShowingSynchronizedLyrics =
                                                !isShowingSynchronizedLyrics
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.globe,
                                        text = "Translate lyrics (${lyricsLanguage.label})",
                                        onClick = {
                                            menuState.hide()
                                            isSelectingLanguage = true
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.pencil,
                                        text = "Edit lyrics",
                                        onClick = {
                                            menuState.hide()
                                            isEditing = true
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.search,
                                        text = "Search lyrics online",
                                        onClick = {
                                            menuState.hide()
                                            val mediaMetadata = mediaMetadataProvider()

                                            try {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                        putExtra(
                                                            SearchManager.QUERY,
                                                            "${mediaMetadata.title} ${mediaMetadata.artist} lyrics"
                                                        )
                                                    }
                                                )
                                            } catch (_: ActivityNotFoundException) {
                                                context.toast("Couldn't find an application to browse the Internet")
                                            }
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.download,
                                        text = "Fetch lyrics again",
                                        enabled = lyrics != null,
                                        onClick = {
                                            menuState.hide()
                                            query {
                                                ensureSongInserted()
                                                Database.upsert(
                                                    Lyrics(
                                                        songId = mediaId,
                                                        fixed = if (isShowingSynchronizedLyrics) lyrics?.fixed else null,
                                                        synced = if (isShowingSynchronizedLyrics) null else lyrics?.synced,
                                                    )
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    )
                    .padding(all = 8.dp)
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
            )
        }
        if (isSelectingLanguage) {
            ValueSelectorDialog(
                onDismiss = { isSelectingLanguage = false },
                title = "Select Lyrics Language",
                selectedValue = lyricsLanguage,
                values = LyricsLanguage.entries,
                onValueSelected = { selectedLang ->
                    if (selectedLang != null) {
                        lyricsLanguage = selectedLang
                    }
                    isSelectingLanguage = false
                },
                valueText = { it.label }
            )
        }
    }
}
