package com.synap.music.ui.screens.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.synap.compose.persist.persist
import com.synap.compose.persist.persistList
import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.SearchSuggestionsBody
import com.synap.pulse.requests.searchSuggestions
import com.synap.music.Database
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.R
import com.synap.music.models.SearchQuery
import com.synap.music.query
import com.synap.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.synap.pulse.models.NavigationEndpoint
import com.synap.music.ui.screens.albumDetailsRoute
import com.synap.music.ui.screens.artistRoute
import com.synap.music.ui.screens.playlistRoute
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.center
import com.synap.music.utils.secondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The Online Search Screen.
 * It manages your search history locally and provides real-time 
 * "as-you-type" suggestions from the YouTube servers.
 */
@ExperimentalAnimationApi
@Composable
fun OnlineSearch(
    textFieldValue: TextFieldValue,
    onTextFieldValueChanged: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current

    // Local history: we persist this in memory during the session.
    var history by persistList<SearchQuery>("search/online/history")

    var shouldShowSuggestions by remember { mutableStateOf(true) }

    // Sync history with the local database as the user types.
    LaunchedEffect(textFieldValue.text) {
        Database.queries("%${textFieldValue.text}%")
            .distinctUntilChanged { old, new -> old.size == new.size }
            .collect { history = it }
        shouldShowSuggestions = true
    }

    var suggestionsResult by persist<Result<List<SynapService.Suggestion>?>?>("search/online/suggestionsResult")

    /**
     * Type-ahead Suggestions.
     * We wait 200ms after the last keypress (debounce) before asking 
     * the server for suggestions to keep things smooth and reduce data usage.
     */
    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text.isNotEmpty() && shouldShowSuggestions) {
            delay(200)
            suggestionsResult =
                SynapService.searchSuggestions(SearchSuggestionsBody(input = textFieldValue.text))
        } else {
            suggestionsResult = null
        }
    }

    val rippleIndication = rememberRipple(bounded = false)
    val timeIconPainter = painterResource(R.drawable.time)
    val closeIconPainter = painterResource(R.drawable.close)
    val arrowForwardIconPainter = painterResource(R.drawable.arrow_forward)

    val lazyListState = rememberLazyListState()

    val layoutDirection = LocalLayoutDirection.current
    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val contentPadding = PaddingValues(
        start = paddingValues.calculateStartPadding(layoutDirection),
        top = paddingValues.calculateTopPadding() + 72.dp,
        end = paddingValues.calculateEndPadding(layoutDirection),
        bottom = paddingValues.calculateBottomPadding()
    )

    Box {
        LazyColumn(
            state = lazyListState,
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(
                items = history,
                key = SearchQuery::id
            ) { searchQuery ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClick = { onSearch(searchQuery.query) })
                        .fillMaxWidth()
                        .padding(all = 16.dp)
                ) {
                    Spacer(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(20.dp)
                            .paint(
                                painter = timeIconPainter,
                                colorFilter = ColorFilter.tint(colorPalette.textDisabled)
                            )
                    )

                    BasicText(
                        text = searchQuery.query,
                        style = typography.s.secondary,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .weight(1f)
                    )

                    Image(
                        painter = closeIconPainter,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.textDisabled),
                        modifier = Modifier
                            .clickable(
                                indication = rippleIndication,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    query {
                                        Database.delete(searchQuery)
                                    }
                                }
                            )
                            .padding(horizontal = 8.dp)
                            .size(20.dp)
                    )

                    Image(
                        painter = arrowForwardIconPainter,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.textDisabled),
                        modifier = Modifier
                            .clickable(
                                indication = rippleIndication,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    onTextFieldValueChanged(
                                        TextFieldValue(
                                            text = searchQuery.query,
                                            selection = TextRange(searchQuery.query.length)
                                        )
                                    )
                                }
                            )
                            .rotate(225f)
                            .padding(horizontal = 8.dp)
                            .size(22.dp)
                    )
                }
            }

            suggestionsResult?.getOrNull()?.let { suggestions ->
                items(items = suggestions) { suggestion ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(onClick = {
                                shouldShowSuggestions = false
                                val endpoint = suggestion.endpoint?.endpoint
                                if (endpoint is NavigationEndpoint.Endpoint.Browse && endpoint.browseId != null &&
                                    (endpoint.pageType == "MUSIC_PAGE_TYPE_ALBUM" || endpoint.pageType == "MUSIC_PAGE_TYPE_PLAYLIST" || endpoint.pageType == "MUSIC_PAGE_TYPE_ARTIST")) {
                                    when (endpoint.pageType) {
                                        "MUSIC_PAGE_TYPE_ALBUM" -> albumDetailsRoute.global(endpoint.browseId!!, com.synap.music.models.Album(id = endpoint.browseId!!, title = suggestion.text, thumbnailUrl = suggestion.thumbnail?.url))
                                        "MUSIC_PAGE_TYPE_PLAYLIST" -> playlistRoute.global(endpoint.browseId)
                                        "MUSIC_PAGE_TYPE_ARTIST" -> artistRoute.global(endpoint.browseId!!, null)
                                    }
                                } else {
                                    onSearch(suggestion.text)
                                }
                            })
                            .fillMaxWidth()
                            .padding(all = 16.dp)
                    ) {
                        Spacer(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(20.dp)
                        )

                        BasicText(
                            text = suggestion.text,
                            style = typography.s.secondary,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .weight(1f)
                        )

                        Image(
                            painter = arrowForwardIconPainter,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette.textDisabled),
                            modifier = Modifier
                                .clickable(
                                    indication = rippleIndication,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        onTextFieldValueChanged(
                                            TextFieldValue(
                                                text = suggestion.text,
                                                selection = TextRange(suggestion.text.length)
                                            )
                                        )
                                    }
                                )
                                .rotate(225f)
                                .padding(horizontal = 8.dp)
                                .size(22.dp)
                        )
                    }
                }
            }
?: suggestionsResult?.exceptionOrNull()?.let {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        BasicText(
                            text = "An error has occurred.",
                            style = typography.s.secondary.center,
                            modifier = Modifier
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)
    }
}
