package com.synap.music.ui.screens.searchresult

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.synap.compose.persist.persist
import com.synap.pulse.SynapService
import com.synap.pulse.requests.lyrics
import com.synap.pulse.models.bodies.NextBody
import com.synap.pulse.utils.plus
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.ui.components.ShimmerHost
import com.synap.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.center
import com.synap.music.utils.secondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generic infinite-scrolling items page component for Search Results.
 * 
 * Supports dynamic cache invalidation on tag changes, infinite pagination,
 * custom item transformations (filtering and sorting), and shimmer loading placeholders.
 */
@ExperimentalAnimationApi
@Composable
inline fun <T : SynapService.Item> ItemsPage(
    tag: String,
    crossinline headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit,
    crossinline itemContent: @Composable LazyItemScope.(item: T, index: Int, list: List<T>) -> Unit,
    noinline itemPlaceholderContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    initialPlaceholderCount: Int = 8,
    continuationPlaceholderCount: Int = 3,
    emptyItemsText: String = "No items found",
    noinline filterTransform: ((List<T>) -> List<T>)? = null,
    noinline itemsPageProvider: (suspend (String?) -> Result<SynapService.ItemsPage<T>?>?)? = null,
) {
    val (_, typography) = LocalAppearance.current

    val updatedItemsPageProvider by rememberUpdatedState(itemsPageProvider)

    val lazyListState = rememberLazyListState()

    var itemsPage by persist<SynapService.ItemsPage<T>?>(tag)
    var lastTag by remember { mutableStateOf(tag) }

    LaunchedEffect(tag, updatedItemsPageProvider) {
        val currentItemsPageProvider = updatedItemsPageProvider ?: return@LaunchedEffect
        if (tag != lastTag || itemsPage == null) {
            if (tag != lastTag) {
                lastTag = tag
                itemsPage = null
            }
            withContext(Dispatchers.IO) {
                currentItemsPageProvider(null)
            }?.onSuccess { result ->
                itemsPage = result ?: SynapService.ItemsPage(emptyList(), null)
            }
        }
    }

    LaunchedEffect(lazyListState, tag, updatedItemsPageProvider) {
        val currentItemsPageProvider = updatedItemsPageProvider ?: return@LaunchedEffect

        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" } }
            .collect { shouldLoadMore ->
                if (!shouldLoadMore) return@collect
                val continuation = itemsPage?.continuation ?: return@collect

                withContext(Dispatchers.IO) {
                    currentItemsPageProvider(continuation)
                }?.onSuccess { result ->
                    if (result != null) {
                        if (itemsPage == null) {
                            itemsPage = result
                        } else {
                            itemsPage += result
                        }
                    }
                }
            }
    }

    val layoutDirection = LocalLayoutDirection.current
    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val contentPadding = PaddingValues(
        start = paddingValues.calculateStartPadding(layoutDirection),
        top = paddingValues.calculateTopPadding() + 64.dp,
        end = paddingValues.calculateEndPadding(layoutDirection),
        bottom = paddingValues.calculateBottomPadding()
    )

    Box {
        LazyColumn(
            state = lazyListState,
            contentPadding = contentPadding,
            modifier = modifier
                .fillMaxSize()
        ) {
            item(
                key = "header",
                contentType = "header",
            ) {
                headerContent(null)
            }

            val rawList = itemsPage?.list ?: emptyList()
            val list = filterTransform?.invoke(rawList) ?: rawList
            itemsIndexed(
                items = list,
                key = { _, item -> item.key }
            ) { index, item ->
                itemContent(item, index, list)
            }

            if (itemsPage != null && list.isEmpty()) {
                item(key = "empty") {
                    BasicText(
                        text = emptyItemsText,
                        style = typography.xs.secondary.center,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 32.dp)
                            .fillMaxWidth()
                    )
                }
            }

            if (!(itemsPage != null && itemsPage?.continuation == null)) {
                item(key = "loading") {
                    val isFirstLoad = itemsPage?.list.isNullOrEmpty()
                    ShimmerHost(
                        modifier = Modifier
                            .run {
                                if (isFirstLoad) fillParentMaxSize() else this
                            }
                    ) {
                        repeat(if (isFirstLoad) initialPlaceholderCount else continuationPlaceholderCount) {
                            itemPlaceholderContent()
                        }
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)
    }
}
