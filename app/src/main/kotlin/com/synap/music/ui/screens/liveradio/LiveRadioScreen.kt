package com.synap.music.ui.screens.liveradio

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.synap.compose.routing.RouteHandlerScope
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.network.RadioBrowserClient
import com.synap.music.network.RadioStation
import com.synap.music.ui.components.themed.Scaffold
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.rememberButtonColor
import com.synap.music.utils.bold
import com.synap.music.utils.color
import com.synap.music.utils.forcePlay
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.toast
import kotlinx.coroutines.launch

// Primary genre filters for fast discovery
private val radioGenreChips = listOf(
    "Top" to "Top Hits",
    "pop" to "Pop",
    "rock" to "Rock",
    "jazz" to "Jazz",
    "classical" to "Classical",
    "dance" to "Dance & EDM",
    "chill" to "Chill & Ambient",
    "hiphop" to "Hip Hop",
    "lofi" to "Lofi",
    "synthwave" to "Synthwave",
    "metal" to "Heavy Metal",
    "blues" to "Blues",
    "country" to "Country",
    "reggae" to "Reggae",
    "80s" to "80s",
    "90s" to "90s",
    "news" to "News & Talk"
)

/**
 * Full-screen Live Internet Radio browser.
 * Connects to open RadioBrowser API to stream online stations via ExoPlayer with ICY metadata.
 */
@ExperimentalAnimationApi
@Composable
fun RouteHandlerScope.LiveRadioScreen() {
    val context = LocalContext.current
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val coroutineScope = rememberCoroutineScope()
    val client = remember { RadioBrowserClient() }
    val buttonColor = rememberButtonColor()

    var searchQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var selectedTag by rememberSaveable { mutableStateOf("Top") }
    var stations by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val genreScrollState = rememberScrollState()
    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    // Fetch stations based on active search text or category tag
    fun loadStations(query: String = searchQuery.text, tag: String = selectedTag) {
        isLoading = true
        coroutineScope.launch {
            stations = when {
                query.isNotBlank() -> client.searchStations(query.trim(), limit = 60)
                tag == "Top" -> client.getTopStations(limit = 60)
                else -> client.getStationsByTag(tag, limit = 60)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadStations()
    }

    Scaffold(
        title = {
            Column {
                BasicText(
                    text = "Live Radio",
                    style = typography.l.bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    text = "Global Internet Radio Stations",
                    style = typography.xxs.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        topStartIconButtonId = R.drawable.chevron_back,
        onTopStartIconButtonClick = pop,
        topEndIconButtonId = R.drawable.sync,
        onTopEndIconButtonClick = { loadStations() },
        tabIndex = 0,
        onTabChanged = { },
        tabColumnContent = { }
    ) {
        val topContentPadding = paddingValues.calculateTopPadding() + 72.dp + 8.dp
        val bottomContentPadding = paddingValues.calculateBottomPadding() + 16.dp

        Column(
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
                .padding(top = topContentPadding)
        ) {
            // Search Input Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { newValue ->
                        searchQuery = newValue
                        if (newValue.text.isBlank() && searchQuery.text.isNotBlank()) {
                            loadStations(query = "", tag = selectedTag)
                        }
                    },
                    textStyle = typography.xs.semiBold.color(colorPalette.text),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { loadStations(query = searchQuery.text) }
                    ),
                    cursorBrush = SolidColor(buttonColor),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colorPalette.background1)
                                .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.search),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.text.isEmpty()) {
                                    BasicText(
                                        text = "Search stations by name, city, or stream...",
                                        style = typography.xs.secondary
                                    )
                                }
                                innerTextField()
                            }
                            if (searchQuery.text.isNotEmpty()) {
                                Image(
                                    painter = painterResource(id = R.drawable.close),
                                    contentDescription = "Clear search",
                                    colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            searchQuery = TextFieldValue("")
                                            loadStations(query = "", tag = selectedTag)
                                        }
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Horizontal Genre Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(genreScrollState)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                radioGenreChips.forEach { (tagKey, tagLabel) ->
                    val isSelected = selectedTag == tagKey && searchQuery.text.isBlank()
                    val chipBackground = if (isSelected) buttonColor else colorPalette.background1
                    val chipTextColor = if (isSelected) colorPalette.onAccent else colorPalette.textSecondary

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipBackground)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) buttonColor else colorPalette.textDisabled.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedTag = tagKey
                                searchQuery = TextFieldValue("")
                                loadStations(query = "", tag = tagKey)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        BasicText(
                            text = tagLabel,
                            style = typography.xxs.semiBold.color(chipTextColor)
                        )
                    }
                }
            }

            // Station List State / Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    isLoading -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.radio_tower),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(colorPalette.accent),
                                    modifier = Modifier.size(36.dp)
                                )
                                BasicText(
                                    text = "Tuning into radio frequencies...",
                                    style = typography.xs.secondary
                                )
                            }
                        }
                    }

                    stations.isEmpty() -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 32.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.radio),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(colorPalette.textDisabled),
                                    modifier = Modifier.size(44.dp)
                                )
                                BasicText(
                                    text = "No broadcast stations found",
                                    style = typography.s.bold
                                )
                                BasicText(
                                    text = "Try searching for a different frequency, station name, or genre tag.",
                                    style = typography.xxs.secondary
                                )
                            }
                        }
                    }

                    else -> {
                        val currentPlayingId = binder?.player?.currentMediaItem?.mediaId
                        val isCurrentPlaying = binder?.player?.isPlaying == true

                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = bottomContentPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = stations,
                                key = { it.stationUuid }
                            ) { station ->
                                val stationMediaId = "radio:${station.stationUuid}"
                                val isThisStationPlaying = currentPlayingId == stationMediaId && isCurrentPlaying

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isThisStationPlaying) colorPalette.background2 else colorPalette.background1)
                                        .border(
                                            width = 1.dp,
                                            color = if (isThisStationPlaying) colorPalette.accent else colorPalette.textDisabled.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            // Prefer resolved direct audio stream URL over aggregator redirect
                                            val streamUrl = station.urlResolved?.takeIf { it.isNotBlank() } ?: station.url
                                            if (streamUrl.isNotBlank()) {
                                                val mediaItem = MediaItem.Builder()
                                                    .setMediaId(stationMediaId)
                                                    .setUri(streamUrl.toUri())
                                                    .setMediaMetadata(
                                                        MediaMetadata.Builder()
                                                            .setTitle(station.name)
                                                            .setArtist(station.country ?: "Live Internet Radio")
                                                            .setArtworkUri(station.favicon?.takeIf { it.startsWith("http") }?.toUri())
                                                            .build()
                                                    )
                                                    .build()

                                                binder?.stopRadio()
                                                binder?.player?.forcePlay(mediaItem)
                                                context.toast("Streaming: ${station.name}")
                                            } else {
                                                context.toast("Radio stream URL is currently unreachable")
                                            }
                                        }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Station Favicon or fallback tower icon
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(colorPalette.background0)
                                                .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        ) {
                                            val faviconUrl = station.favicon?.takeIf { it.startsWith("http") }
                                            if (faviconUrl != null) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(faviconUrl)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = station.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Image(
                                                    painter = painterResource(id = R.drawable.radio_tower),
                                                    contentDescription = null,
                                                    colorFilter = ColorFilter.tint(if (isThisStationPlaying) colorPalette.accent else colorPalette.textSecondary),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Station Metadata (Name, Country, Bitrate)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                BasicText(
                                                    text = station.name,
                                                    style = typography.xs.semiBold.color(if (isThisStationPlaying) colorPalette.accent else colorPalette.text),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )

                                                if (isThisStationPlaying) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(colorPalette.accent)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        BasicText(
                                                            text = "LIVE",
                                                            style = typography.xxs.bold.color(colorPalette.onAccent)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            val metadataParts = buildList {
                                                add(station.country?.takeIf { it.isNotBlank() } ?: "Global")
                                                if (station.bitrate != null && station.bitrate > 0) {
                                                    add("${station.bitrate} kbps")
                                                }
                                                if (!station.codec.isNullOrBlank()) {
                                                    add(station.codec.uppercase())
                                                }
                                            }

                                            BasicText(
                                                text = metadataParts.joinToString(" • "),
                                                style = typography.xxs.secondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Play / Equalizer Indicator
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (isThisStationPlaying) colorPalette.accent else buttonColor)
                                        ) {
                                            Image(
                                                painter = painterResource(
                                                    id = if (isThisStationPlaying) R.drawable.soundwave else R.drawable.play
                                                ),
                                                contentDescription = if (isThisStationPlaying) "Playing" else "Play",
                                                colorFilter = ColorFilter.tint(colorPalette.onAccent),
                                                modifier = Modifier.size(16.dp)
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
}
