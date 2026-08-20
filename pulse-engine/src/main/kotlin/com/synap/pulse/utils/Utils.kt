package com.synap.pulse.utils

import io.ktor.utils.io.CancellationException
import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.NextBody
import com.synap.pulse.models.SectionListRenderer
import com.synap.pulse.models.MusicShelfRenderer

internal fun MusicShelfRenderer?.toSongsPage(): SynapService.ItemsPage<SynapService.SongItem>? {
    val items = this?.contents?.mapNotNull(SynapService.SongItem::from) ?: emptyList()
    if (items.isEmpty()) return null
    return SynapService.ItemsPage(list = items, continuation = this?.continuations?.firstOrNull()?.nextContinuationData?.continuation)
}

internal fun SectionListRenderer.findSectionByTitle(text: String): SectionListRenderer.Content? {
    val lowercaseText = text.lowercase()
    return contents?.find { content ->
        val title = content
            .musicCarouselShelfRenderer
            ?.header
            ?.musicCarouselShelfBasicHeaderRenderer
            ?.title
            ?: content
                .musicShelfRenderer
                ?.title

        val titleText = title
            ?.runs
            ?.firstOrNull()
            ?.text
            ?.lowercase() ?: ""

        titleText == lowercaseText || titleText.contains(lowercaseText)
    }
}

internal fun SectionListRenderer.findSectionByStrapline(text: String): SectionListRenderer.Content? {
    return contents?.find { content ->
        content
            .musicCarouselShelfRenderer
            ?.header
            ?.musicCarouselShelfBasicHeaderRenderer
            ?.strapline
            ?.runs
            ?.firstOrNull()
            ?.text == text
    }
}

internal inline fun <R> runCatchingNonCancellable(block: () -> R): Result<R>? {
    val result = runCatching(block)
    return when (result.exceptionOrNull()) {
        is CancellationException -> null
        else -> result
    }
}

infix operator fun <T : SynapService.Item> SynapService.ItemsPage<T>?.plus(other: SynapService.ItemsPage<T>) =
    other.copy(
        list = (this?.list?.plus(other.list ?: emptyList())
            ?: other.list)?.distinctBy(SynapService.Item::key)
    )
