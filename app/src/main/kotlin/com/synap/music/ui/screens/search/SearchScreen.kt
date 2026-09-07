package com.synap.music.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.synap.compose.persist.PersistMapCleanup
import com.synap.music.R
import com.synap.music.ui.components.themed.Scaffold
import com.synap.music.ui.components.themed.SecondaryTextButton
import com.synap.music.ui.components.themed.FilterButton
import com.synap.music.ui.components.themed.Menu
import com.synap.music.ui.components.themed.MenuEntry
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.enums.DiscoveryFilter
import com.synap.music.utils.discoveryLanguageKey
import com.synap.music.utils.discoveryGenreKey
import com.synap.music.utils.discoveryDecadeKey
import androidx.compose.foundation.layout.Spacer
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.rememberButtonColor
import com.synap.music.utils.medium
import com.synap.music.utils.secondary
import com.synap.music.utils.isProSearchModeKey
import com.synap.music.utils.searchResultScreenTabIndexKey
import com.synap.music.utils.rememberPreference
import android.content.Context
import androidx.core.content.edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.synap.compose.routing.RouteHandlerScope

/**
 * Search screen supporting online catalog and local library search.
 */
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun RouteHandlerScope.SearchScreen(
    initialTextInput: String,
    onSearch: (String) -> Unit,
    onViewPlaylist: (String) -> Unit
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current

    var isProMode by rememberPreference(isProSearchModeKey, false)
    var language by rememberPreference(discoveryLanguageKey, "None")
    var genre by rememberPreference(discoveryGenreKey, "None")
    var decade by rememberPreference(discoveryDecadeKey, "None")

    val (tabIndex, onTabChanged) = rememberSaveable {
        mutableStateOf(0)
    }

    val (textFieldValue, onTextFieldValueChanged) = rememberSaveable(
        initialTextInput,
        stateSaver = TextFieldValue.Saver
    ) {
        mutableStateOf(
            TextFieldValue(
                text = initialTextInput,
                selection = TextRange(initialTextInput.length)
            )
        )
    }

    PersistMapCleanup(tagPrefix = "search/")

    val (colorPalette, typography) = LocalAppearance.current
    val buttonColor = rememberButtonColor()

    val decorationBox: @Composable (@Composable () -> Unit) -> Unit = { innerTextField ->
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .background(colorPalette.background1, RoundedCornerShape(8.dp))
                .border(1.dp, buttonColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            AnimatedVisibility(
                visible = textFieldValue.text.isEmpty(),
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
            ) {
                BasicText(
                    text = "Search for songs, artists, albums...",
                    maxLines = 1,
                    style = typography.s.secondary
                )
            }

            innerTextField()
        }
    }

    Scaffold(
        title = {
            BasicTextField(
                value = textFieldValue,
                onValueChange = onTextFieldValueChanged,
                textStyle = typography.s.medium,
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (textFieldValue.text.isNotEmpty()) {
                            focusManager.clearFocus()
                            if (isProMode) {
                                context.getSharedPreferences("preferences", Context.MODE_PRIVATE).edit {
                                    putInt(searchResultScreenTabIndexKey, 3) // 3 = Videos tab
                                }
                            }

                            val langSuffix = DiscoveryFilter.Language.entries.find { it::class.simpleName == language }?.querySuffix ?: ""
                            val genreSuffix = DiscoveryFilter.Genre.entries.find { it::class.simpleName == genre }?.querySuffix ?: ""
                            val decadeSuffix = DiscoveryFilter.Decade.entries.find { it::class.simpleName == decade }?.querySuffix ?: ""

                            val searchQuery = buildString {
                                append(textFieldValue.text)
                                if (langSuffix.isNotEmpty()) append(" $langSuffix")
                                if (genreSuffix.isNotEmpty()) append(" $genreSuffix")
                                if (decadeSuffix.isNotEmpty()) append(" $decadeSuffix")
                            }.trim()

                            onSearch(searchQuery)
                        }
                    }
                ),
                cursorBrush = SolidColor(buttonColor),
                decorationBox = decorationBox,
                modifier = Modifier
                    .fillMaxWidth()
            )
        },
        topStartIconButtonId = R.drawable.chevron_back,
        onTopStartIconButtonClick = pop,
        topBarActions = {
            val playlistId = remember(textFieldValue.text) {
                val isPlaylistUrl = listOf(
                    "https://www.youtube.com/playlist?",
                    "https://youtube.com/playlist?",
                    "https://music.youtube.com/playlist?",
                    "https://m.youtube.com/playlist?"
                ).any(textFieldValue.text::startsWith)

                if (isPlaylistUrl) textFieldValue.text.toUri().getQueryParameter("list") else null
            }

            if (playlistId != null) {
                val isAlbum = playlistId.startsWith("OLAK5uy_")

                SecondaryTextButton(
                    text = "View ${if (isAlbum) "album" else "playlist"}",
                    onClick = { onViewPlaylist(textFieldValue.text) }
                )
            }

            FilterButton(
                label = "Filter",
                onShowMenu = {
                    menuState.display {
                        var currentSubMenu by remember { mutableStateOf<String?>(null) }

                        Menu {
                            when (currentSubMenu) {
                                "SearchType" -> {
                                    MenuEntry(icon = R.drawable.chevron_back, text = "Back", onClick = { currentSubMenu = null })
                                    MenuEntry(
                                        icon = if (!isProMode) R.drawable.checkmark else R.drawable.disc,
                                        text = "Normal Search",
                                        secondaryText = "Standard Music catalog",
                                        onClick = {
                                            isProMode = false
                                            menuState.hide()
                                        }
                                    )
                                    MenuEntry(
                                        icon = if (isProMode) R.drawable.checkmark else R.drawable.disc,
                                        text = "PRO Search",
                                        secondaryText = "Deep search for videos & channels)",
                                        onClick = {
                                            isProMode = true
                                            menuState.hide()
                                        }
                                    )
                                }
                                "Language" -> {
                                    MenuEntry(icon = R.drawable.chevron_back, text = "Back", onClick = { currentSubMenu = null })
                                    DiscoveryFilter.Language.entries.forEach { filter ->
                                        val name = filter::class.simpleName!!
                                        MenuEntry(
                                            icon = if (name == language) R.drawable.checkmark else R.drawable.disc,
                                            text = filter.label,
                                            onClick = {
                                                language = name
                                                menuState.hide()
                                            }
                                        )
                                    }
                                }
                                "Genre" -> {
                                    MenuEntry(icon = R.drawable.chevron_back, text = "Back", onClick = { currentSubMenu = null })
                                    DiscoveryFilter.Genre.entries.forEach { filter ->
                                        val name = filter::class.simpleName!!
                                        MenuEntry(
                                            icon = if (name == genre) R.drawable.checkmark else R.drawable.disc,
                                            text = filter.label,
                                            onClick = {
                                                genre = name
                                                menuState.hide()
                                            }
                                        )
                                    }
                                }
                                "Year" -> {
                                    MenuEntry(icon = R.drawable.chevron_back, text = "Back", onClick = { currentSubMenu = null })
                                    DiscoveryFilter.Decade.entries.forEach { filter ->
                                        val name = filter::class.simpleName!!
                                        MenuEntry(
                                            icon = if (name == decade) R.drawable.checkmark else R.drawable.disc,
                                            text = filter.label,
                                            onClick = {
                                                decade = name
                                                menuState.hide()
                                            }
                                        )
                                    }
                                }
                                else -> {
                                    MenuEntry(
                                        icon = R.drawable.search,
                                        text = "Search Mode",
                                        secondaryText = if (isProMode) "PRO Search (Videos & Channels)" else "Normal Search (Catalog)",
                                        onClick = { currentSubMenu = "SearchType" }
                                    )

                                    Spacer(Modifier.padding(vertical = 4.dp))

                                    MenuEntry(
                                        icon = R.drawable.text,
                                        text = "Language Filter",
                                        secondaryText = if (language == "None") "All Languages" else language,
                                        onClick = { currentSubMenu = "Language" }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.musical_notes,
                                        text = "Genre Filter",
                                        secondaryText = if (genre == "None") "All Genres" else genre,
                                        onClick = { currentSubMenu = "Genre" }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.calendar,
                                        text = "Year / Era Filter",
                                        secondaryText = if (decade == "None") "All Time" else decade.replace("Year", ""),
                                        onClick = { currentSubMenu = "Year" }
                                    )
                                }
                            }
                        }
                    }
                }
            )

            if (textFieldValue.text.isNotEmpty()) {
                SecondaryTextButton(
                    text = "Clear",
                    onClick = { onTextFieldValueChanged(TextFieldValue()) }
                )
            }
        },
        tabIndex = tabIndex,
        onTabChanged = onTabChanged,
        tabColumnContent = { item ->
            item(0, "Online", R.drawable.globe)
            item(1, "Library", R.drawable.library)
        }
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(currentTabIndex) {
            when (currentTabIndex) {
                0 -> OnlineSearch(
                    textFieldValue = textFieldValue,
                    onTextFieldValueChanged = onTextFieldValueChanged,
                    onSearch = onSearch,
                )

                1 -> LocalSongSearch(
                    textFieldValue = textFieldValue,
                )
            }
        }
    }
}
