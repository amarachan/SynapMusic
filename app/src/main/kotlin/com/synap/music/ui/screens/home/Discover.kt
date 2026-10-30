/*
 * Copyright (C) 2026 SynapMusic Maintainers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.synap.music.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.synap.compose.persist.persist
import com.synap.music.Database
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.enums.DiscoveryFilter
import com.synap.music.enums.DiscoveryViewMode
import com.synap.music.enums.Region
import com.synap.music.models.Artist
import com.synap.music.models.Song
import com.synap.music.ui.styling.rememberPulsatingHybridColor
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.ShimmerHost
import com.synap.music.ui.components.themed.ArtistMenu
import com.synap.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.synap.music.ui.components.themed.NonQueuedMediaItemMenu
import com.synap.music.ui.items.ArtistItem
import com.synap.music.ui.items.SongItem
import com.synap.music.ui.items.SongItemPlaceholder
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.rememberTileHybridColor
import com.synap.music.ui.styling.px
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.center
import com.synap.music.enums.SongSortBy
import com.synap.music.enums.SortOrder
import com.synap.music.utils.songSortByKey
import com.synap.music.utils.songSortOrderKey
import com.synap.music.utils.discoveryDecadeKey
import com.synap.music.utils.discoveryGenreKey
import com.synap.music.utils.discoveryLanguageKey
import com.synap.music.utils.discoveryLimitKey
import com.synap.music.utils.discoveryStyleKey
import com.synap.music.utils.discoveryViewModeKey
import com.synap.music.utils.filterAiSongsKey
import com.synap.music.utils.forcePlayAtIndex
import com.synap.music.utils.regionKey
import com.synap.music.utils.applyPulsatingToDiscoverCardsKey
import com.synap.music.utils.isPulsatingEffectEnabledKey
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.synapStream
import com.synap.music.utils.getSynapStreamForLanguage
import com.synap.music.utils.toast
import com.synap.pulse.SynapService
import com.synap.pulse.models.NavigationEndpoint
import com.synap.pulse.models.bodies.NextBody
import com.synap.pulse.models.bodies.SearchBody
import com.synap.pulse.requests.listenBrainzDiscovery
import com.synap.pulse.requests.relatedPage
import com.synap.pulse.requests.searchPage
import com.synap.pulse.utils.from
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Evaluates whether a given item is a genuine musical track versus AI-generated content or covers.
 * Checks against keywords like "AI cover", "AI voice", "Suno AI", "Udio AI", etc.
 */
fun isGenuineSong(song: SynapService.Item): Boolean {
    val title = when (song) {
        is SynapService.SongItem -> song.info?.name
        is SynapService.VideoItem -> song.info?.name
        is SynapService.AlbumItem -> song.info?.name
        is SynapService.ArtistItem -> song.info?.name
        is SynapService.PlaylistItem -> song.info?.name
        else -> null
    }?.lowercase() ?: ""

    val artist = when (song) {
        is SynapService.SongItem -> song.authors?.joinToString(" ") { it.name ?: "" }
        is SynapService.VideoItem -> song.authors?.joinToString(" ") { it.name ?: "" }
        is SynapService.AlbumItem -> song.authors?.joinToString(" ") { it.name ?: "" }
        is SynapService.ArtistItem -> song.info?.name
        is SynapService.PlaylistItem -> song.channel?.name
        else -> null
    }?.lowercase() ?: ""

    val combined = "$title $artist"

    val aiKeywords = listOf(
        "ai cover", "ai song", "ai generated",
        "suno ai", "udio ai", "suno.com", "udio.com", "generated by ai",
        "ai music", "ai voice", "ai version", "ai remix"
    )

    return aiKeywords.none { keyword -> combined.contains(keyword) }
}

/**
 * Filters a list of items based on the active language filter selection.
 * Enforces keyword exclusions and script detection rules across global languages.
 */
fun <T : SynapService.Item> filterBySelectedLanguage(songs: List<T>, language: String): List<T> {
    if (language == "None" || language.isBlank()) return songs

    val langLower = language.lowercase()
    
    // A comprehensive master list encompassing major global languages and music industries/genres
    val allKeywords = listOf(
        // Global Languages
        "english", "spanish", "french", "german", "italian", "portuguese", "dutch", "russian", 
        "ukrainian", "polish", "arabic", "turkish", "persian", "hebrew", "chinese", "mandarin", 
        "cantonese", "japanese", "korean", "vietnamese", "thai", "indonesian", "malay", "tagalog",
        "greek", "swedish", "norwegian", "finnish", "danish", "swahili",
        
        // Indian/South Asian Languages & Genres
        "hindi", "punjabi", "tamil", "telugu", "malayalam", "kannada", "bhojpuri", "marathi", 
        "bengali", "gujarati", "urdu", "odia", "assamese", "nepali", "sinhala",
        "bollywood", "kollywood", "tollywood", "mollywood", "sandalwood", "pollywood",
        "carnatic", "hindustani", "qawwali", "bhajan", "kirtan", "bhangra", "garba",
        
        // East/Southeast Asian Music
        "k-pop", "j-pop", "c-pop", "t-pop", "v-pop", "mandopop", "cantopop", 
        "k-indie", "j-rock", "anime", "k-drama",
        
        // Latin/Hispanic/Brazilian
        "latin", "reggaeton", "afrobeats", "amapiano", "dancehall", "soca", 
        "norteno", "banda", "mariachi", "cumbia", "salsa", "bachata", "flamenco",
        "trap latino", "urbano", "ranchera", "corridos", "bolero", "bossa nova", "baile funk",

        // European/Other
        "chanson", "schlager", "eurovision", "canzone", "celtic", "nordic",
        
        // Global/Western Genres
        "pop", "rock", "hip hop", "r&b", "country", "indie", "jazz", "blues", 
        "electronic", "dance", "edm", "techno", "house", "dubstep", "metal", "punk", 
        "classical", "acoustic", "ambient", "reggae", "soul", "funk", "lo-fi", "lofi", "synthwave"
    )

    // Map each language to the specific industries and genres it is allowed to show
    val allowedRelatedKeywords = mapOf(
        "english" to listOf(
            "pop", "rock", "hip hop", "r&b", "country", "indie", "jazz", "blues", 
            "electronic", "dance", "edm", "techno", "house", "dubstep", "metal", "punk", 
            "classical", "acoustic", "ambient", "reggae", "soul", "funk", "lo-fi", "lofi", "synthwave"
        ),
        "spanish" to listOf("latin", "reggaeton", "norteno", "banda", "mariachi", "cumbia", "salsa", "bachata", "flamenco", "trap latino", "urbano", "ranchera", "corridos", "bolero"),
        "portuguese" to listOf("bossa nova", "baile funk"),
        "french" to listOf("chanson"),
        "german" to listOf("schlager"),
        "italian" to listOf("canzone"),
        "korean" to listOf("k-pop", "k-indie", "k-drama"),
        "japanese" to listOf("j-pop", "j-rock", "anime"),
        "chinese" to listOf("mandarin", "cantonese", "c-pop", "mandopop", "cantopop"),
        "vietnamese" to listOf("v-pop"),
        "thai" to listOf("t-pop"),
        "hindi" to listOf("bollywood", "qawwali", "bhajan", "hindustani"),
        "punjabi" to listOf("pollywood", "bhangra"),
        "tamil" to listOf("kollywood", "carnatic"),
        "telugu" to listOf("tollywood", "carnatic"),
        "malayalam" to listOf("mollywood"),
        "kannada" to listOf("sandalwood", "carnatic"),
        "gujarati" to listOf("garba")
    )

    // Ban ALL keywords, except the user's selected language and its directly associated industries
    val excludeKeywords = allKeywords.filter { keyword ->
        keyword != langLower && keyword !in (allowedRelatedKeywords[langLower] ?: emptyList())
    }

    return songs.filter { song ->
        val title = when (song) {
            is SynapService.SongItem -> song.info?.name
            is SynapService.VideoItem -> song.info?.name
            is SynapService.AlbumItem -> song.info?.name
            is SynapService.ArtistItem -> song.info?.name
            is SynapService.PlaylistItem -> song.info?.name
            else -> null
        } ?: ""
        val authors = when (song) {
            is SynapService.SongItem -> song.authors?.joinToString(" ") { it.name ?: "" }
            is SynapService.VideoItem -> song.authors?.joinToString(" ") { it.name ?: "" }
            is SynapService.AlbumItem -> song.authors?.joinToString(" ") { it.name ?: "" }
            is SynapService.ArtistItem -> song.info?.name
            is SynapService.PlaylistItem -> song.channel?.name
            else -> null
        } ?: ""
        val text = "$title $authors".lowercase()
        
        // 1. Keyword check (Regex ensures we match whole words only, avoiding false positives)
        val hasBannedKeyword = excludeKeywords.any { keyword -> 
            Regex("\\b$keyword\\b").containsMatchIn(text) 
        }
        
        // 2. Foreign Script detection (Blocks non-Latin scripts if not requested)
        val hasKoreanChars = text.any { it in '\uAC00'..'\uD7AF' }
        val hasJapaneseChars = text.any { it in '\u3040'..'\u30FF' || it in '\u4E00'..'\u9FAF' }
        val hasChineseChars = text.any { it in '\u4E00'..'\u9FFF' }
        val hasThaiChars = text.any { it in '\u0E00'..'\u0E7F' }
        val hasCyrillicChars = text.any { it in '\u0400'..'\u04FF' }
        val hasGreekChars = text.any { it in '\u0370'..'\u03FF' }
        val hasArabicChars = text.any { it in '\u0600'..'\u06FF' }
        val hasHebrewChars = text.any { it in '\u0590'..'\u05FF' }
        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        val hasBengaliChars = text.any { it in '\u0980'..'\u09FF' }
        val hasTamilChars = text.any { it in '\u0B80'..'\u0BFF' }
        val hasTeluguChars = text.any { it in '\u0C00'..'\u0C7F' }

        // Decide if we keep the song based on the chosen language
        when (langLower) {
            "english", "spanish", "french", "german", "portuguese", "italian", "dutch", "swedish", "norwegian", "finnish", "danish" -> {
                !hasBannedKeyword && !hasKoreanChars && !hasJapaneseChars && !hasChineseChars && 
                !hasThaiChars && !hasCyrillicChars && !hasGreekChars && !hasArabicChars && 
                !hasHebrewChars && !hasDevanagari && !hasBengaliChars && !hasTamilChars && !hasTeluguChars
            }
            "korean" -> !hasBannedKeyword && !hasJapaneseChars && !hasChineseChars && !hasThaiChars && !hasDevanagari
            "japanese" -> !hasBannedKeyword && !hasKoreanChars && !hasThaiChars && !hasDevanagari
            "chinese", "mandarin", "cantonese" -> !hasBannedKeyword && !hasKoreanChars && !hasThaiChars && !hasDevanagari
            "thai" -> !hasBannedKeyword && !hasKoreanChars && !hasJapaneseChars && !hasChineseChars
            "russian", "ukrainian" -> !hasBannedKeyword && !hasKoreanChars && !hasJapaneseChars && !hasChineseChars && !hasArabicChars
            "arabic", "persian", "urdu" -> !hasBannedKeyword && !hasKoreanChars && !hasJapaneseChars && !hasHebrewChars
            "hebrew" -> !hasBannedKeyword && !hasArabicChars && !hasCyrillicChars
            "greek" -> !hasBannedKeyword && !hasCyrillicChars && !hasArabicChars
            "hindi", "marathi", "nepali" -> !hasBannedKeyword && !hasKoreanChars && !hasJapaneseChars && !hasArabicChars
            "telugu", "malayalam", "kannada" -> !hasBannedKeyword && !hasKoreanChars && !hasJapaneseChars && !hasCyrillicChars
            "tamil" -> !hasBannedKeyword && !hasKoreanChars && !hasJapaneseChars && !hasCyrillicChars
            else -> !hasBannedKeyword
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun Discover(
    onArtistClick: (Artist) -> Unit,
    onSearchClick: (SearchMode) -> Unit,
    onRefresh: (() -> Unit) -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val context = LocalContext.current

    val region by rememberPreference(regionKey, Region.WORLDWIDE)
    val language by rememberPreference(discoveryLanguageKey, "None")
    val genre by rememberPreference(discoveryGenreKey, "None")
    val decade by rememberPreference(discoveryDecadeKey, "None")
    val limit by rememberPreference(discoveryLimitKey, 40)
    val style by rememberPreference(discoveryStyleKey, "Random")
    val viewMode by rememberPreference(discoveryViewModeKey, DiscoveryViewMode.Grid)
    val filterAiSongs by rememberPreference(filterAiSongsKey, false)
    val sortBy by rememberPreference(songSortByKey, SongSortBy.DateAdded)
    val sortOrder by rememberPreference(songSortOrderKey, SortOrder.Descending)

    var refreshTrigger by remember {
        mutableStateOf(0)
    }

    onRefresh {
        refreshTrigger++
    }

    var relatedPageResult by persist<Result<SynapService.RelatedPage?>?>(tag = "home/v3/relatedPageResult")

    val trendingSong: Song? by Database.trending().distinctUntilChanged().collectAsState(initial = null)

    var lastFilters by remember {
        mutableStateOf<Triple<String, String, String>?>(null)
    }
    var lastLimit by remember {
        mutableStateOf<Int?>(null)
    }
    var lastStyle by remember {
        mutableStateOf<String?>(null)
    }
    var lastFilterAiSongs by remember {
        mutableStateOf<Boolean?>(null)
    }
    var lastSortBy by remember {
        mutableStateOf<SongSortBy?>(null)
    }
    var lastSortOrder by remember {
        mutableStateOf<SortOrder?>(null)
    }

    // We keep track of what you've seen before so we can show you fresh stuff every time.
    var previousArtistIds by persist<Set<String>>(tag = "home/v3/previousArtistIds", initialValue = emptySet())
    var previousSongIds by persist<Set<String>>(tag = "home/v3/previousSongIds", initialValue = emptySet())
    var totalSeenSongIds by persist<Set<String>>(tag = "home/v3/totalSeenSongIds", initialValue = emptySet())
    var seedIndex by persist(tag = "home/v3/seedIndex", initialValue = (0..999).random())

    LaunchedEffect(refreshTrigger, language, genre, decade, style, limit, filterAiSongs, sortBy, sortOrder, trendingSong) {
        val currentFilters = Triple(language, genre, decade)
        val filtersChanged = lastFilters != null && lastFilters != currentFilters
        val limitChanged = lastLimit != null && lastLimit != limit
        val styleChanged = lastStyle != null && lastStyle != style
        val filterAiSongsChanged = lastFilterAiSongs != null && lastFilterAiSongs != filterAiSongs
        val sortByChanged = lastSortBy != null && lastSortBy != sortBy
        val sortOrderChanged = lastSortOrder != null && lastSortOrder != sortOrder
        val isInitialLoad = relatedPageResult == null
        val explicitRefresh = refreshTrigger > 0

        // If nothing changed, don't bother fetching again.
        if (filtersChanged || limitChanged || styleChanged || filterAiSongsChanged || sortByChanged || sortOrderChanged || explicitRefresh || isInitialLoad) {
            relatedPageResult = null
        } else {
            return@LaunchedEffect
        }

        lastFilters = currentFilters
        lastLimit = limit
        lastStyle = style
        lastFilterAiSongs = filterAiSongs
        lastSortBy = sortBy
        lastSortOrder = sortOrder

        // Randomize the seed for every fetch to confuse YTM's cache and get new results.
        seedIndex = (0..9999).random()

        val varietyPool = listOf(
            "vibes", "party", "acoustic", "deep", "soul", "electric", "classic", "latest", "hits",
            "chill", "energetic", "relaxing", "mood", "instrumental", "vocal", "fresh", "rhythm",
            "beat", "smooth", "raw", "indie", "alternative", "modern", "retro", "underground",
            "mellow", "hype", "ambient", "lofi", "groove", "jazz", "blues", "rock", "pop", "metal"
        )

        // Mix active filters with random keywords to get varied search results
        val querySuffix = buildString {
            val strictFilters = mutableListOf<String>()
            val varietyKeywords = mutableListOf<String>()

            DiscoveryFilter.Language.entries.find { it.javaClass.simpleName == language }?.let {
                if (it.querySuffix.isNotEmpty()) strictFilters.add(it.querySuffix)
            }
            DiscoveryFilter.Genre.entries.find { it.javaClass.simpleName == genre }?.let {
                if (it.querySuffix.isNotEmpty()) strictFilters.add(it.querySuffix)
            }
            DiscoveryFilter.Decade.entries.find { it.javaClass.simpleName == decade }?.let {
                if (it.querySuffix.isNotEmpty()) strictFilters.add(it.querySuffix)
            }

            val selectedStyle = DiscoveryFilter.Style.entries.find { it.javaClass.simpleName == style }
            if (selectedStyle != null && selectedStyle.querySuffix.isNotEmpty()) {
                strictFilters.add(selectedStyle.querySuffix)
                varietyKeywords.add(varietyPool.random())
            } else {
                varietyKeywords.addAll(varietyPool.shuffled().take(2))
            }

            // Build the query strictly: "Language Genre Decade Style [Random Varieties] songs"
            // Keeping the strict filters at the front ensures YouTube respects them instead of fuzzy matching.
            if (strictFilters.isNotEmpty()) {
                append(strictFilters.joinToString(" "))
                append(" ")
            }

            if (varietyKeywords.isNotEmpty()) {
                // Only shuffle the variety keywords, not the strict language/genre definitions
                append(varietyKeywords.shuffled().joinToString(" "))
                append(" ")
            }

            append("songs")
        }.trim()

        val isFiltered = querySuffix.isNotEmpty()

        // Fetch trending charts with search fallback for initial install.
        val chartsResult = withTimeoutOrNull(15.seconds) {
            val primary = SynapService.listenBrainzDiscovery(querySuffix)
            if (primary.isSuccess && !primary.getOrNull().isNullOrEmpty()) {
                primary
            } else {
                // Fallback search to guarantee initial launch populates songs
                val fallbackSearch = SynapService.searchPage(
                    body = SearchBody(
                        query = querySuffix.ifEmpty { "trending top music hits" },
                        params = SynapService.SearchFilter.Song.value
                    ),
                    fromMusicShelfRendererContent = SynapService.SongItem.Companion::from
                )
                val list = fallbackSearch?.getOrNull()?.list
                if (!list.isNullOrEmpty()) Result.success(list) else primary
            }
        }

        // Deep Search for Artists matching the current vibe.
        val filteredArtistsResult = if (isFiltered) {
            withTimeoutOrNull(5.seconds) {
                SynapService.searchPage(
                    body = SearchBody(
                        query = querySuffix,
                        params = SynapService.SearchFilter.Artist.value
                    ),
                    fromMusicTwoRowItemRenderer = SynapService.ArtistItem::from
                )
            }
        } else null

        // If no filters are set, we just show stuff related to your most-played song.
        val result = if (!isFiltered) {
            withTimeoutOrNull(5.seconds) {
                SynapService.relatedPage(
                    NextBody(
                        videoId = (trendingSong?.id ?: "J7p4bzqLvCw"),
                        region = region.code
                    )
                )
            }
        } else null

        val varietyFillPool = listOf(
            "trending", "hits", "popular", "hot", "top charts", "new releases", "featured", "viral", "top 100", "essential"
        )

        /**
         * The Variety Engine.
         * If the filtered results are too thin, we perform a second "Expansion Search"
         * using broad trending terms combined with your active filters. This ensures you
         * always get the full number of songs you requested (e.g., 40 or 100) without gaps
         * and without breaking the language/genre filter.
         */
        val activeStream = getSynapStreamForLanguage(language)

        val (rawSongs, artists) = if (isFiltered) {
            val filteredSongs = chartsResult?.getOrNull() ?: emptyList()
            val filteredArtists = filteredArtistsResult?.getOrNull()?.list ?: emptyList()

            val fallbackArtists = withTimeoutOrNull(5.seconds) {
                SynapService.searchPage(
                    body = SearchBody(
                        query = "$querySuffix artist",
                        params = SynapService.SearchFilter.Artist.value
                    ),
                    fromMusicTwoRowItemRenderer = SynapService.ArtistItem::from
                )
            }?.getOrNull()?.list ?: emptyList()
            
            val needed = limit - filteredSongs.size
            val fillSongs = if (needed > 0) {
                val pagesNeeded = (needed / 20) + 1
                (1..pagesNeeded.coerceAtMost(4)).map {
                    async {
                        withTimeoutOrNull(8.seconds) {
                            SynapService.searchPage(
                                body = SearchBody(
                                    query = "$querySuffix ${varietyFillPool.random()}",
                                    params = SynapService.SearchFilter.Song.value
                                ),
                                fromMusicShelfRendererContent = SynapService.SongItem.Companion::from
                            )?.getOrNull()?.list
                        } ?: emptyList()
                    }
                }.awaitAll().flatten()
            } else emptyList()

            (activeStream + filteredSongs + fillSongs).distinctBy { it.key }.shuffled() to
                    (filteredArtists + fallbackArtists).distinctBy { it.key }.take(20)
        } else {
            val related = result?.getOrNull()
            val globalCharts = chartsResult?.getOrNull() ?: emptyList()
            val suggestions = related?.songs ?: emptyList()
            val originalArtists = related?.artists ?: emptyList()

            val needed = limit - (globalCharts.size + suggestions.size)
            val fillSongs = if (needed > 0) {
                val pagesNeeded = (needed / 20) + 1
                (1..pagesNeeded.coerceAtMost(4)).map {
                    async {
                        withTimeoutOrNull(8.seconds) {
                            SynapService.searchPage(
                                body = SearchBody(
                                    query = "${varietyFillPool.random()} music hits",
                                    params = SynapService.SearchFilter.Song.value
                                ),
                                fromMusicShelfRendererContent = SynapService.SongItem.Companion::from
                            )?.getOrNull()?.list
                        } ?: emptyList()
                    }
                }.awaitAll().flatten()
            } else emptyList()

            (activeStream + globalCharts + suggestions + fillSongs).distinctBy { it.key }.shuffled() to
                    (originalArtists).distinctBy { it.key }.take(20)
        }

        val songsToFilter = if (filterAiSongs) rawSongs.filter { isGenuineSong(it) } else rawSongs
        val songs = filterBySelectedLanguage(songsToFilter, language)

        val synapStreamKeys = synapStream.map { it.key }.toSet()
        val targetCount = limit.coerceAtLeast(10)
        
        // Filter out recently seen songs so the feed stays fresh
        val finalSongs = if (style == "Random") {
            val fresh = songs.filter { song ->
                val isSynapStream = song.key in synapStreamKeys
                isSynapStream || (song.key !in previousSongIds && song.key !in totalSeenSongIds)
            }

            if (fresh.size >= targetCount) {
                fresh.take(targetCount)
            } else {
                // If we ran out of new songs, fill with the general pool so the list isn't too short.
                (fresh + songs).distinctBy { it.key }.take(targetCount)
            }
        } else songs.take(targetCount)

        val finalDiscoverySongs = finalSongs.filter { it.key !in synapStreamKeys }
        previousSongIds = finalDiscoverySongs.map { it.key }.toSet()
        previousArtistIds = finalDiscoverySongs
            .flatMap { it.authors ?: emptyList() }
            .mapNotNull { it.endpoint?.browseId }
            .toSet()
        
        totalSeenSongIds = (totalSeenSongIds + previousSongIds).toList().takeLast(100).toSet()

        val sortedFinalSongs = when (sortBy) {
            SongSortBy.Title -> if (sortOrder == SortOrder.Ascending) finalSongs.sortedBy { it.info?.name ?: "" } else finalSongs.sortedByDescending { it.info?.name ?: "" }
            SongSortBy.PlayTime -> if (sortOrder == SortOrder.Ascending) finalSongs.sortedBy { it.info?.name ?: "" } else finalSongs.sortedByDescending { it.info?.name ?: "" }
            else -> if (sortOrder == SortOrder.Ascending) finalSongs.reversed() else finalSongs
        }

        relatedPageResult = Result.success(
            SynapService.RelatedPage(
                songs = sortedFinalSongs,
                artists = artists,
                albums = null
            )
        )
    }

    val itemThumbnailSizeDp = if (viewMode == DiscoveryViewMode.Grid) 180.dp else 60.dp
    val itemThumbnailSizePx = itemThumbnailSizeDp.px
    val artistThumbnailSizeDp = 92.dp
    val artistThumbnailSizePx = artistThumbnailSizeDp.px

    val quickPicksLazyGridState = rememberLazyGridState()

    val layoutDirection = LocalLayoutDirection.current
    val paddingValues = windowInsets.asPaddingValues()

    val topPadding = if (viewMode == DiscoveryViewMode.Grid) 64.dp else 110.dp

    val contentPadding = PaddingValues(
        start = paddingValues.calculateStartPadding(layoutDirection),
        top = paddingValues.calculateTopPadding() + topPadding,
        end = paddingValues.calculateEndPadding(layoutDirection),
        bottom = paddingValues.calculateBottomPadding()
    )

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 32.dp, bottom = 12.dp)
        .padding(endPaddingValues)

    val applyPulsatingToDiscoverCards by rememberPreference(applyPulsatingToDiscoverCardsKey, true)
    val isPulsatingEffectEnabled by rememberPreference(isPulsatingEffectEnabledKey, true)
    val shouldPulsateCards = isPulsatingEffectEnabled && applyPulsatingToDiscoverCards

    val discoverPulse = rememberPulsatingHybridColor(primaryColor = colorPalette.accent, secondaryColor = colorPalette.blue)

    BoxWithConstraints {
        LazyVerticalGrid(
            state = quickPicksLazyGridState,
            columns = if (viewMode == DiscoveryViewMode.Grid) GridCells.Fixed(2) else GridCells.Fixed(1),
            contentPadding = contentPadding,
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize(),
        ) {

            val related = relatedPageResult?.getOrNull()
            if (related != null) {
                val songItems = related.songs ?: emptyList()

                itemsIndexed(
                    items = songItems,
                    key = { _, song -> song.key }
                ) { index, song ->
                    val tilePulse = rememberTileHybridColor(index)
                    val liteHybridBg = if (shouldPulsateCards) lerp(colorPalette.background0, tilePulse, 0.18f) else colorPalette.background1

                    SongItem(
                        song = song,
                        thumbnailSizePx = itemThumbnailSizePx,
                        thumbnailSizeDp = itemThumbnailSizeDp,
                        isVertical = viewMode == DiscoveryViewMode.Grid,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = if (viewMode == DiscoveryViewMode.Grid) 8.dp else 4.dp)
                            .then(if (viewMode == DiscoveryViewMode.Grid) {
                                val cardShape = RoundedCornerShape(16.dp)
                                Modifier
                                    .shadow(4.dp, cardShape, spotColor = if (shouldPulsateCards) tilePulse else Color.Black)
                                    .clip(cardShape)
                                    .background(liteHybridBg)
                                    .border(1.dp, if (shouldPulsateCards) tilePulse.copy(alpha = 0.35f) else colorPalette.textDisabled.copy(alpha = 0.15f), cardShape)
                            } else Modifier)
                            .combinedClickable(
                                onLongClick = {
                                    val mediaItem = song.asMediaItem
                                    menuState.display {
                                        NonQueuedMediaItemMenu(
                                            onDismiss = menuState::hide,
                                            mediaItem = mediaItem,
                                            onRemoveFromDiscover = {
                                                // Blacklist the song from appearing in Discovery feed again
                                                totalSeenSongIds = totalSeenSongIds + setOf(song.key)
                                                previousSongIds = previousSongIds + setOf(song.key)
                                                refreshTrigger++
                                                context.toast("Removed from Discover")
                                            }
                                        )
                                    }
                                },
                            ) {
                                val mediaItems = songItems.map { it.asMediaItem }
                                binder?.stopRadio()
                                binder?.player?.forcePlayAtIndex(mediaItems, index, context)
                                
                                /**
                                 * We also start a radio based on the clicked song,
                                 * but we append it to the end of the discover list
                                 * so the music never stops.
                                 */
                                binder?.setupRadio(
                                    endpoint = NavigationEndpoint.Endpoint.Watch(videoId = song.key),
                                )
                            }
                    )
                }

                related.artists?.takeIf { it.isNotEmpty() }?.let { artists ->
                    item(span = { GridItemSpan(if (viewMode == DiscoveryViewMode.Grid) 2 else 1) }) {
                        BasicText(
                            text = "Similar artists",
                            style = typography.m.semiBold,
                            modifier = sectionTextModifier,
                        )
                    }

                    item(span = { GridItemSpan(if (viewMode == DiscoveryViewMode.Grid) 2 else 1) }) {
                        LazyRow(
                            contentPadding = endPaddingValues,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                artists,
                                key = { it.key },
                            ) { artist ->
                                ArtistItem(
                                    artist = artist,
                                    thumbnailSizePx = artistThumbnailSizePx,
                                    thumbnailSizeDp = artistThumbnailSizeDp,
                                    alternative = true,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = { 
                                                onArtistClick(Artist(
                                                    id = artist.key,
                                                    name = artist.info?.name,
                                                    thumbnailUrl = artist.thumbnail?.url
                                                )) 
                                            },
                                            onLongClick = {
                                                menuState.display {
                                                    ArtistMenu(
                                                        artist = artist,
                                                        onDismiss = menuState::hide
                                                    )
                                                }
                                            }
                                        ),
                                )
                            }
                        }
                    }
                }
            } else if (relatedPageResult?.exceptionOrNull() != null) {
                item(span = { GridItemSpan(if (viewMode == DiscoveryViewMode.Grid) 2 else 1) }) {
                    BasicText(
                        text = "An error has occurred",
                        style = typography.s.secondary.center,
                        modifier = Modifier
                            .padding(all = 16.dp),
                    )
                }
            } else {
                item(span = { GridItemSpan(if (viewMode == DiscoveryViewMode.Grid) 2 else 1) }) {
                    ShimmerHost {
                        repeat(8) {
                            SongItemPlaceholder(
                                thumbnailSizeDp = itemThumbnailSizeDp,
                                alternative = viewMode == DiscoveryViewMode.Grid
                            )
                        }
                    }
                }
            }
        }

        val layoutInfo = quickPicksLazyGridState.layoutInfo
        val totalItemsCount = layoutInfo.totalItemsCount
        val visibleItemsCount = layoutInfo.visibleItemsInfo.size

        if (totalItemsCount > visibleItemsCount && visibleItemsCount > 0) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp, top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding())
                    .width(5.dp)
                    .fillMaxHeight()
            ) {
                val canvasHeight = size.height

                val scrollbarHeight = (canvasHeight * (visibleItemsCount.toFloat() / totalItemsCount.toFloat()))
                    .coerceAtLeast(36.dp.toPx())

                val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
                val scrollProgress = if (firstVisibleItem != null) {
                    val maxScrollIndex = (totalItemsCount - visibleItemsCount).coerceAtLeast(1)
                    val itemHeight = firstVisibleItem.size.height.toFloat()

                    val fractionalOffset = if (itemHeight > 0) {
                        -firstVisibleItem.offset.y.toFloat() / itemHeight
                    } else 0f

                    ((firstVisibleItem.index + fractionalOffset) / maxScrollIndex).coerceIn(0f, 1f)
                } else 0f

                val scrollbarOffset = scrollProgress * (canvasHeight - scrollbarHeight)

                // Track
                drawRoundRect(
                    color = colorPalette.textDisabled.copy(alpha = 0.2f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f.dp.toPx(), 2.5f.dp.toPx())
                )

                // Accent Thumb (Pulsating)
                drawRoundRect(
                    color = discoverPulse,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, scrollbarOffset),
                    size = androidx.compose.ui.geometry.Size(width = size.width, height = scrollbarHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f.dp.toPx(), 2.5f.dp.toPx())
                )
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyGridState = quickPicksLazyGridState,
            iconId = R.drawable.search,
            onClick = { onSearchClick(SearchMode.None) }, // None triggers global search in HomeScreen logic or we can just pass a special value
        )
    }
}
