package com.synap.music.ui.components.themed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.network.RadioBrowserClient
import com.synap.music.network.RadioStation
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.rememberButtonColor
import com.synap.music.ui.styling.rememberPulsatingHybridColor
import com.synap.music.utils.center
import com.synap.music.utils.color
import com.synap.music.utils.forcePlay
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.toast
import kotlinx.coroutines.launch

val radioGenreTags = listOf(
    "Top" to "Top",
    "pop" to "Pop",
    "rock" to "Rock",
    "jazz" to "Jazz",
    "classical" to "Classical",
    "news" to "News & Talk",
    "dance" to "Dance & Electronic",
    "chill" to "Chill & Ambient",
    "hiphop" to "Hip Hop & Rap",
    "country" to "Country",
    "metal" to "Heavy Metal",
    "blues" to "Blues",
    "reggae" to "Reggae",
    "lofi" to "Lofi",
    "80s" to "80s Hits",
    "90s" to "90s Hits"
)

/**
 * Compact, sleek Live Internet Radio Browser Dialog.
 */
@Composable
fun LiveRadioDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val coroutineScope = rememberCoroutineScope()
    val client = remember { RadioBrowserClient() }
    val buttonColor = rememberButtonColor()

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTag by remember { mutableStateOf("Top") }
    var isShowingTagMenu by remember { mutableStateOf(false) }
    var stations by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val menuScrollState = rememberScrollState()
    val stationListScrollState = rememberScrollState()

    fun loadStations(query: String = "", tag: String = selectedTag) {
        isLoading = true
        coroutineScope.launch {
            stations = when {
                query.isNotBlank() -> client.searchStations(query)
                tag == "Top" -> client.getTopStations()
                else -> client.getStationsByTag(tag)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadStations()
    }

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicText(
                text = "Live Internet Radio",
                style = typography.xs.semiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Compact Search Bar
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = typography.xs.semiBold.center,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { loadStations(query = searchQuery.text) }
                ),
                cursorBrush = SolidColor(buttonColor),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorPalette.background0)
                            .border(1.dp, buttonColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        if (searchQuery.text.isEmpty()) {
                            BasicText(
                                text = "Search stations by name...",
                                style = typography.xxs.semiBold.secondary,
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Compact Radio Type Dropdown Selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorPalette.background0)
                    .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { isShowingTagMenu = !isShowingTagMenu }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentTagLabel = radioGenreTags.firstOrNull { it.first == selectedTag }?.second ?: selectedTag
                    BasicText(
                        text = "RADIO TYPE: $currentTagLabel",
                        style = typography.xxs.semiBold
                    )
                    Image(
                        painter = painterResource(id = if (isShowingTagMenu) R.drawable.chevron_up else R.drawable.chevron_down),
                        contentDescription = "Dropdown Arrow",
                        colorFilter = ColorFilter.tint(colorPalette.text),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isShowingTagMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp)
                        .heightIn(max = 140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorPalette.background0)
                        .border(1.dp, colorPalette.accent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(menuScrollState)
                            .padding(end = 8.dp)
                    ) {
                        radioGenreTags.forEach { (tagKey, tagLabel) ->
                            val isSelected = selectedTag == tagKey
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTag = tagKey
                                        isShowingTagMenu = false
                                        searchQuery = TextFieldValue("")
                                        loadStations(tag = tagKey)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                BasicText(
                                    text = tagLabel,
                                    style = if (isSelected) typography.xs.semiBold else typography.xxs.secondary
                                )
                                if (isSelected) {
                                    Image(
                                        painter = painterResource(id = R.drawable.checkmark),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(colorPalette.accent),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Visual Scrollbar Track & Thumb for Dropdown
                    if (menuScrollState.maxValue > 0) {
                        Canvas(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 2.dp, top = 4.dp, bottom = 4.dp)
                                .width(4.dp)
                                .fillMaxHeight()
                        ) {
                            val canvasHeight = size.height
                            val scrollMax = menuScrollState.maxValue.toFloat()
                            val scrollValue = menuScrollState.value.toFloat()

                            val scrollbarHeight = (canvasHeight * (canvasHeight / (canvasHeight + scrollMax)))
                                .coerceAtLeast(20.dp.toPx())

                            val scrollbarOffset = (scrollValue / scrollMax) * (canvasHeight - scrollbarHeight)

                            drawRoundRect(
                                color = colorPalette.textDisabled.copy(alpha = 0.2f),
                                size = size,
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )

                            drawRoundRect(
                                color = colorPalette.accent,
                                topLeft = Offset(0f, scrollbarOffset),
                                size = Size(width = size.width, height = scrollbarHeight),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Compact Station List Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorPalette.background0)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(stationListScrollState)
                        .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 12.dp)
                ) {
                    if (isLoading) {
                        BasicText(
                            text = "Loading stations...",
                            style = typography.xxs.secondary.center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    } else if (stations.isEmpty()) {
                        BasicText(
                            text = "No stations found",
                            style = typography.xxs.secondary.center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    } else {
                        stations.forEach { station ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorPalette.background0)
                                    .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        val streamUrl = station.urlResolved ?: station.url
                                        if (streamUrl.isNotBlank()) {
                                            val mediaItem = MediaItem.Builder()
                                                .setMediaId("radio:${station.stationUuid}")
                                                .setUri(streamUrl.toUri())
                                                .setMediaMetadata(
                                                    MediaMetadata.Builder()
                                                        .setTitle(station.name)
                                                        .setArtist(station.country ?: "Live Radio")
                                                        .setArtworkUri(station.favicon?.takeIf { it.startsWith("http") }?.toUri())
                                                        .build()
                                                )
                                                .build()

                                            binder?.stopRadio()
                                            binder?.player?.forcePlay(mediaItem)
                                            context.toast("Streaming ${station.name}")
                                            onDismiss()
                                        } else {
                                            context.toast("Stream URL unavailable")
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        BasicText(
                                            text = station.name,
                                            style = typography.xs.semiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        BasicText(
                                            text = (station.country ?: "Global") + if (station.bitrate != null && station.bitrate > 0) " • ${station.bitrate} kbps" else "",
                                            style = typography.xxs.secondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Compact Play Button
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(buttonColor)
                                            .clickable {
                                                val streamUrl = station.urlResolved ?: station.url
                                                if (streamUrl.isNotBlank()) {
                                                    val mediaItem = MediaItem.Builder()
                                                        .setMediaId("radio:${station.stationUuid}")
                                                        .setUri(streamUrl.toUri())
                                                        .setMediaMetadata(
                                                            MediaMetadata.Builder()
                                                                .setTitle(station.name)
                                                                .setArtist(station.country ?: "Live Radio")
                                                                .setArtworkUri(station.favicon?.takeIf { it.startsWith("http") }?.toUri())
                                                                .build()
                                                        )
                                                        .build()

                                                    binder?.stopRadio()
                                                    binder?.player?.forcePlay(mediaItem)
                                                    context.toast("Streaming ${station.name}")
                                                    onDismiss()
                                                }
                                            }
                                            .padding(horizontal = 10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.play),
                                                contentDescription = null,
                                                colorFilter = ColorFilter.tint(colorPalette.onAccent),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            BasicText(
                                                text = "Play",
                                                style = typography.xxs.semiBold.color(colorPalette.onAccent)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Station List Scrollbar Track & Accent Thumb
                if (stationListScrollState.maxValue > 0) {
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 2.dp, top = 4.dp, bottom = 4.dp)
                            .width(4.dp)
                            .fillMaxHeight()
                    ) {
                        val canvasHeight = size.height
                        val scrollMax = stationListScrollState.maxValue.toFloat()
                        val scrollValue = stationListScrollState.value.toFloat()

                        val scrollbarHeight = (canvasHeight * (canvasHeight / (canvasHeight + scrollMax)))
                            .coerceAtLeast(28.dp.toPx())

                        val scrollbarOffset = (scrollValue / scrollMax) * (canvasHeight - scrollbarHeight)

                        // Track
                        drawRoundRect(
                            color = colorPalette.textDisabled.copy(alpha = 0.2f),
                            size = size,
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )

                        // Accent Thumb
                        drawRoundRect(
                            color = colorPalette.accent,
                            topLeft = Offset(0f, scrollbarOffset),
                            size = Size(width = size.width, height = scrollbarHeight),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact Close Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorPalette.background0)
                    .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { onDismiss() }
            ) {
                BasicText(
                    text = "Close",
                    style = typography.xs.semiBold
                )
            }
        }
    }
}
