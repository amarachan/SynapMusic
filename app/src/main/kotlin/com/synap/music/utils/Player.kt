package com.synap.music.utils

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.core.net.toUri
import android.content.Context

val Player.currentWindow: Timeline.Window?
    get() = if (mediaItemCount == 0) null else currentTimeline.getWindow(currentMediaItemIndex, Timeline.Window())

val Timeline.mediaItems: List<MediaItem>
    get() = List(windowCount) {
        getWindow(it, Timeline.Window()).mediaItem
    }

inline val Timeline.windows: List<Timeline.Window>
    get() = List(windowCount) {
        getWindow(it, Timeline.Window())
    }

val Player.shouldBePlaying: Boolean
    get() = !(playbackState == Player.STATE_ENDED || !playWhenReady)

fun Player.seamlessPlay(mediaItem: MediaItem) {
    if (mediaItem.mediaId == currentMediaItem?.mediaId) {
        if (currentMediaItemIndex > 0) removeMediaItems(0, currentMediaItemIndex)
        if (currentMediaItemIndex < mediaItemCount - 1) removeMediaItems(currentMediaItemIndex + 1, mediaItemCount)
    } else {
        forcePlay(mediaItem)
    }
}

fun Player.shuffleQueue() {
    val mediaItems = currentTimeline.mediaItems.toMutableList().apply { removeAt(currentMediaItemIndex) }
    if (currentMediaItemIndex > 0) removeMediaItems(0, currentMediaItemIndex)
    if (currentMediaItemIndex < mediaItemCount - 1) removeMediaItems(currentMediaItemIndex + 1, mediaItemCount)
    addMediaItems(mediaItems.shuffled())
}

fun Player.forcePlay(mediaItem: MediaItem, context: android.content.Context? = null) {
    var finalMediaItem = mediaItem
    if (context != null) {
        val file = MediaFetchUtils.getAudioFile(context, mediaItem.mediaId)
        if (file.exists()) {
            val mimeType = when (file.extension.lowercase()) {
                "mkv" -> "video/x-matroska"
                "ogg" -> "audio/ogg"
                "wv" -> "audio/x-wavpack"
                "ape" -> "audio/x-ape"
                "tta" -> "audio/x-tta"
                "flac" -> "audio/flac"
                "m4a" -> "audio/mp4"
                "mp3" -> "audio/mpeg"
                else -> null
            }
            if (mimeType != null) {
                finalMediaItem = mediaItem.buildUpon()
                    .setUri(file.toUri())
                    .setMimeType(mimeType)
                    .build()
            }
        }
    }
    setMediaItem(finalMediaItem, true)
    playWhenReady = true
    prepare()
}

fun Player.forcePlayAtIndex(mediaItems: List<MediaItem>, mediaItemIndex: Int, context: android.content.Context? = null) {
    if (mediaItems.isEmpty()) return
    
    val finalMediaItems = if (context != null) {
        mediaItems.map { item ->
            val file = MediaFetchUtils.getAudioFile(context, item.mediaId)
            if (file.exists()) {
                val mimeType = when (file.extension.lowercase()) {
                    "mkv" -> "video/x-matroska"
                    "ogg" -> "audio/ogg"
                    "wv" -> "audio/x-wavpack"
                    "ape" -> "audio/x-ape"
                    "tta" -> "audio/x-tta"
                    "flac" -> "audio/flac"
                    "m4a" -> "audio/mp4"
                    "mp3" -> "audio/mpeg"
                    else -> null
                }
                if (mimeType != null) {
                    item.buildUpon()
                        .setUri(file.toUri())
                        .setMimeType(mimeType)
                        .build()
                } else item
            } else item
        }
    } else mediaItems

    setMediaItems(finalMediaItems, mediaItemIndex, C.TIME_UNSET)
    playWhenReady = true
    prepare()
}

fun Player.forcePlayFromBeginning(mediaItems: List<MediaItem>, context: android.content.Context? = null) =
    forcePlayAtIndex(mediaItems, 0, context)

fun Player.forceSeekToPrevious() {
    if (hasPreviousMediaItem() || currentPosition > maxSeekToPreviousPosition) {
        seekToPrevious()
    } else if (mediaItemCount > 0) {
        seekTo(mediaItemCount - 1, C.TIME_UNSET)
    }
}

fun Player.forceSeekToNext() =
    if (hasNextMediaItem()) seekToNext() else seekTo(0, C.TIME_UNSET)

fun Player.addNext(mediaItem: MediaItem) {
    if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
        forcePlay(mediaItem)
    } else {
        addMediaItem(currentMediaItemIndex + 1, mediaItem)
    }
}

fun Player.enqueue(mediaItem: MediaItem) {
    if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
        forcePlay(mediaItem)
    } else {
        addMediaItem(mediaItemCount, mediaItem)
    }
}

fun Player.enqueue(mediaItems: List<MediaItem>) {
    if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
        forcePlayFromBeginning(mediaItems)
    } else {
        addMediaItems(mediaItemCount, mediaItems)
    }
}

fun Player.findNextMediaItemById(mediaId: String): MediaItem? {
    for (i in currentMediaItemIndex until mediaItemCount) {
        if (getMediaItemAt(i).mediaId == mediaId) {
            return getMediaItemAt(i)
        }
    }
    return null
}
