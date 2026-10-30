package com.synap.music.ui.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import com.synap.music.Database
import com.synap.music.R
import com.synap.music.ui.components.themed.MarqueeText
import com.synap.music.ui.components.themed.TextPlaceholder
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.favoritesIcon
import com.synap.music.ui.styling.shimmer
import com.synap.music.ui.styling.px
import com.synap.music.utils.bold
import com.synap.music.utils.color
import com.synap.music.utils.medium
import com.synap.music.utils.secondary
import com.synap.music.utils.thumbnail
import com.synap.music.utils.thumbnailLowRes
import com.synap.music.utils.DownloadState
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.thumbnailAudioBadgeStyleKey
import com.synap.music.enums.ThumbnailAudioBadgeStyle
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.synap.music.ui.components.themed.Badge
import com.synap.pulse.SynapService
import com.synap.music.models.Song

// Universal song item handling different data sources: YT search results, Media3 items, and local DB models.
@Composable
fun SongItem(
    song: SynapService.SongItem,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false,
    onThumbnailContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    // Check if the song is liked or downloaded.
    val likedAt by Database.likedAt(song.key).collectAsState(initial = null)
    val downloadProgress = DownloadState.downloads[song.key]
    val format by Database.format(song.key).collectAsState(initial = null)
    val badgeStyle by rememberPreference(thumbnailAudioBadgeStyleKey, ThumbnailAudioBadgeStyle.FidelityRank)
    
    val isDownloaded = format != null && (format?.contentLength ?: 0L) > 0L

    SongItem(
        thumbnailUrl = song.thumbnail?.url?.thumbnail(thumbnailSizePx),
        title = song.info?.name,
        authors = song.authors?.joinToString(", ") { it.name ?: "" },
        duration = song.durationText,
        thumbnailSizeDp = thumbnailSizeDp,
        isVertical = isVertical,
        onThumbnailContent = {
            if (downloadProgress != null) {
                DownloadOverlay(progress = downloadProgress)
            } else {
                if (isDownloaded && badgeStyle != ThumbnailAudioBadgeStyle.Off) {
                    val badgeLabel = if (badgeStyle == ThumbnailAudioBadgeStyle.FidelityRank) {
                        format?.compressionEngine?.badgeFidelityText ?: run {
                            val mime = format?.mimeType?.lowercase() ?: ""
                            when {
                                mime.contains("opus") || mime.contains("aac") || mime.contains("mp4") -> "MID-FI"
                                else -> "LOSSLESS"
                            }
                        }
                    } else {
                        format?.compressionEngine?.badgeCodecText ?: run {
                            val mime = format?.mimeType?.lowercase() ?: ""
                            when {
                                mime.contains("opus") -> "OPUS"
                                mime.contains("mp4") || mime.contains("m4a") || mime.contains("aac") -> "AAC"
                                else -> "LOSSLESS"
                            }
                        }
                    }
                    if (!badgeLabel.isNullOrBlank()) {
                        Badge(
                            text = badgeLabel,
                            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                        )
                    }
                }
                onThumbnailContent?.invoke(this)
            }
        },
        trailingContent = {
            // Visual markers for "Downloaded" and "Liked" tracks.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDownloaded) {
                    val (colorPalette) = LocalAppearance.current
                    Image(
                        painter = painterResource(R.drawable.checkmark),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.accent),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(16.dp)
                    )
                }
                if (likedAt != null) {
                    val (colorPalette) = LocalAppearance.current
                    Image(
                        painter = painterResource(R.drawable.heart),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.favoritesIcon),
                        modifier = Modifier
                            .size(16.dp)
                    )
                }
            }
        },
        modifier = modifier,
    )
}

// Wrapper for Media3 MediaItem
@Composable
fun SongItem(
    song: MediaItem,
    thumbnailSizeDp: Dp,
    thumbnailSizePx: Int,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false,
    onThumbnailContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val likedAt by Database.likedAt(song.mediaId).collectAsState(initial = null)
    val downloadProgress = DownloadState.downloads[song.mediaId]
    val format by Database.format(song.mediaId).collectAsState(initial = null)
    val badgeStyle by rememberPreference(thumbnailAudioBadgeStyleKey, ThumbnailAudioBadgeStyle.FidelityRank)
    val isVideo = song.mediaMetadata.extras?.getBoolean("isVideo") ?: false
    
    val isDownloaded = format != null && (format?.contentLength ?: 0L) > 0L

    SongItem(
        thumbnailUrl = song.mediaMetadata.artworkUri.thumbnail(thumbnailSizePx)?.toString(),
        title = song.mediaMetadata.title?.toString(),
        authors = song.mediaMetadata.artist?.toString(),
        duration = song.mediaMetadata.extras?.getString("durationText"),
        thumbnailSizeDp = thumbnailSizeDp,
        isVertical = isVertical,
        onThumbnailContent = {
            if (downloadProgress != null) {
                DownloadOverlay(progress = downloadProgress)
            } else {
                if (isDownloaded && badgeStyle != ThumbnailAudioBadgeStyle.Off) {
                    if (isVideo) {
                        Badge(
                            text = "HD",
                            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                        )
                    } else {
                        val badgeLabel = if (badgeStyle == ThumbnailAudioBadgeStyle.FidelityRank) {
                            format?.compressionEngine?.badgeFidelityText ?: run {
                                val mime = format?.mimeType?.lowercase() ?: ""
                                when {
                                    mime.contains("opus") || mime.contains("aac") || mime.contains("mp4") -> "MID-FI"
                                    else -> "LOSSLESS"
                                }
                            }
                        } else {
                            format?.compressionEngine?.badgeCodecText ?: run {
                                val mime = format?.mimeType?.lowercase() ?: ""
                                when {
                                    mime.contains("opus") -> "OPUS"
                                    mime.contains("mp4") || mime.contains("m4a") || mime.contains("aac") -> "AAC"
                                    else -> "LOSSLESS"
                                }
                            }
                        }
                        if (badgeLabel.isNotBlank()) {
                            Badge(
                                text = badgeLabel,
                                modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                            )
                        }
                    }
                }
                onThumbnailContent?.invoke(this)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDownloaded) {
                    val (colorPalette) = LocalAppearance.current
                    Image(
                        painter = painterResource(R.drawable.checkmark),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.accent),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(16.dp)
                    )
                }
                if (likedAt != null) {
                    val (colorPalette) = LocalAppearance.current
                    Image(
                        painter = painterResource(R.drawable.heart),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.favoritesIcon),
                        modifier = Modifier
                            .size(16.dp)
                    )
                }
                trailingContent?.invoke()
            }
        },
        modifier = modifier,
    )
}

// Wrapper for local DB Song model
@Composable
fun SongItem(
    song: Song,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false,
    onThumbnailContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    thumbnailUrlOverride: String? = null
) {
    val likedAt by Database.likedAt(song.id).collectAsState(initial = null)
    val downloadProgress = DownloadState.downloads[song.id]
    val format by Database.format(song.id).collectAsState(initial = null)
    val badgeStyle by rememberPreference(thumbnailAudioBadgeStyleKey, ThumbnailAudioBadgeStyle.FidelityRank)

    val isDownloaded = format != null && (format?.contentLength ?: 0L) > 0L

    SongItem(
        thumbnailUrl = (song.thumbnailUrl ?: thumbnailUrlOverride)?.thumbnail(thumbnailSizePx),
        title = song.title,
        authors = song.artistsText,
        duration = song.durationText,
        thumbnailSizeDp = thumbnailSizeDp,
        isVertical = isVertical,
        onThumbnailContent = {
            if (downloadProgress != null) {
                DownloadOverlay(progress = downloadProgress)
            } else {
                if (isDownloaded && badgeStyle != ThumbnailAudioBadgeStyle.Off) {
                    val badgeLabel = if (badgeStyle == ThumbnailAudioBadgeStyle.FidelityRank) {
                        format?.compressionEngine?.badgeFidelityText ?: run {
                            val mime = format?.mimeType?.lowercase() ?: ""
                            when {
                                mime.contains("opus") || mime.contains("aac") || mime.contains("mp4") -> "MID-FI"
                                else -> "LOSSLESS"
                            }
                        }
                    } else {
                        format?.compressionEngine?.badgeCodecText ?: run {
                            val mime = format?.mimeType?.lowercase() ?: ""
                            when {
                                mime.contains("opus") -> "OPUS"
                                mime.contains("mp4") || mime.contains("m4a") || mime.contains("aac") -> "AAC"
                                else -> "LOSSLESS"
                            }
                        }
                    }
                    if (badgeLabel.isNotBlank()) {
                        Badge(
                            text = badgeLabel,
                            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                        )
                    }
                }
                onThumbnailContent?.invoke(this)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDownloaded) {
                    val (colorPalette) = LocalAppearance.current
                    Image(
                        painter = painterResource(R.drawable.checkmark),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.accent),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(16.dp)
                    )
                }
                if (likedAt != null) {
                    val (colorPalette) = LocalAppearance.current
                    Image(
                        painter = painterResource(R.drawable.heart),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.favoritesIcon),
                        modifier = Modifier
                            .size(16.dp)
                    )
                }
                trailingContent?.invoke()
            }
        },
        modifier = modifier,
    )
}

@Composable
fun SongItem(
    thumbnailUrl: String?,
    title: String?,
    authors: String?,
    duration: String?,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false,
    onThumbnailContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    SongItem(
        title = title,
        authors = authors,
        duration = duration,
        thumbnailSizeDp = thumbnailSizeDp,
        isVertical = isVertical,
        thumbnailContent = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl?.thumbnail(thumbnailSizeDp.px))
                    .placeholderMemoryCacheKey(thumbnailUrl?.thumbnailLowRes())
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(200)
                    .build(),
                placeholder = rememberAsyncImagePainter(thumbnailUrl?.thumbnailLowRes()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(if (isVertical) RoundedCornerShape(16.dp) else LocalAppearance.current.thumbnailShape)
                    .fillMaxSize()
            )

            onThumbnailContent?.invoke(this)
        },
        modifier = modifier,
        trailingContent = trailingContent
    )
}

@Composable
fun SongItem(
    thumbnailContent: @Composable BoxScope.() -> Unit,
    title: String?,
    authors: String?,
    duration: String?,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val (colorPalette, typography) = LocalAppearance.current

    ItemContainer(
        alternative = isVertical,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier
    ) {
        Box(
            modifier = if (isVertical) {
                Modifier.fillMaxWidth().size(thumbnailSizeDp)
            } else {
                Modifier.size(thumbnailSizeDp)
            }
        ) {
            thumbnailContent()
        }

        ItemInfoContainer(
            modifier = if (isVertical) {
                Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(52.dp)
            } else {
                Modifier
            }
        ) {
            trailingContent?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isVertical) {
                        MarqueeText(
                            text = title ?: "",
                            style = typography.s.bold,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        BasicText(
                            text = title ?: "",
                            style = typography.s.bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    it()
                }
            } ?: run {
                if (isVertical) {
                    MarqueeText(
                        text = title ?: "",
                        style = typography.s.bold
                    )
                } else {
                    BasicText(
                        text = title ?: "",
                        style = typography.s.bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isVertical) {
                    MarqueeText(
                        text = authors ?: "",
                        style = typography.xs.medium.secondary,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    BasicText(
                        text = authors ?: "",
                        style = typography.xs.medium.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                duration?.let {
                    BasicText(
                        text = duration,
                        style = typography.xxs.secondary.medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SongItemPlaceholder(
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) {
    val (colorPalette, _, thumbnailShape) = LocalAppearance.current

    ItemContainer(
        alternative = alternative,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier
    ) {
        Spacer(
            modifier = Modifier
                .background(color = colorPalette.shimmer, shape = thumbnailShape)
                .size(thumbnailSizeDp)
        )

        ItemInfoContainer(
            modifier = if (alternative) Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth() else Modifier
        ) {
            TextPlaceholder()
            TextPlaceholder()
        }
    }
}

@Composable
private fun DownloadOverlay(progress: Float) {
    val (colorPalette, typography) = LocalAppearance.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)) // Dim artwork so the percent is readable
    ) {
        val accentColor = colorPalette.accent
        Spacer(
            modifier = Modifier
                .fillMaxSize(0.8f)
                .drawWithContent {
                    drawContent()
                    val strokeWidth = 2.dp.toPx()
                    inset(strokeWidth / 2) {
                        // Drawing the background track
                        drawArc(
                            color = accentColor.copy(alpha = 0.3f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                        // Fill the progress arc
                        if (progress > 0) {
                            drawArc(
                                color = accentColor,
                                startAngle = -90f,
                                sweepAngle = (progress / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                        }
                    }
                }
        )
        // Show "..." for prep/connecting state, or actual % during transfer
        BasicText(
            text = when {
                progress < 1f -> "..."
                else -> "${progress.toInt()}%"
            },
            style = typography.xxs.bold.copy(color = Color.White)
        )
    }
}
