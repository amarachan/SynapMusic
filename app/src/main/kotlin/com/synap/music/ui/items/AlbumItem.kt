package com.synap.music.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.BoxScope
import com.synap.music.models.Album
import com.synap.music.models.AlbumWithDownloadStatus
import com.synap.music.ui.components.themed.TextPlaceholder
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.onOverlay
import com.synap.music.ui.styling.overlay
import com.synap.music.ui.styling.shimmer
import com.synap.music.utils.bold
import com.synap.music.utils.center
import com.synap.music.utils.color
import com.synap.music.utils.medium
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.thumbnail
import com.synap.music.utils.thumbnailLowRes
import com.synap.pulse.SynapService
import coil.request.ImageRequest
import coil.request.CachePolicy
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.synap.music.ui.components.themed.Badge

@Composable
fun AlbumItem(
    albumWithStatus: AlbumWithDownloadStatus,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false,
    onThumbnailContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val album = albumWithStatus.album
    AlbumItem(
        thumbnailUrl = album.thumbnailUrl,
        title = album.title,
        authors = album.authorsText,
        year = album.year,
        thumbnailSizePx = thumbnailSizePx,
        thumbnailSizeDp = thumbnailSizeDp,
        alternative = alternative,
        modifier = modifier,
        thumbnailContent = {
            if (albumWithStatus.anyFormatSongs > albumWithStatus.downloadedSongs) {
                Badge(
                    text = "DOWNLOADING",
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                )
            } else if (albumWithStatus.downloadedSongs > 0) {
                val isFullyDownloaded = albumWithStatus.downloadedSongs >= albumWithStatus.totalSongs && albumWithStatus.totalSongs > 0
                Badge(
                    text = if (isFullyDownloaded) "VAULT" else "${albumWithStatus.downloadedSongs}/${albumWithStatus.totalSongs}",
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                )
            }
            onThumbnailContent?.invoke(this)
        }
    )
}

@Composable
fun AlbumItem(
    album: Album,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false,
    onThumbnailContent: (@Composable BoxScope.() -> Unit)? = null
) {
    AlbumItem(
        thumbnailUrl = album.thumbnailUrl,
        title = album.title,
        authors = album.authorsText,
        year = album.year,
        thumbnailSizePx = thumbnailSizePx,
        thumbnailSizeDp = thumbnailSizeDp,
        alternative = alternative,
        modifier = modifier,
        thumbnailContent = {
            onThumbnailContent?.invoke(this)
        }
    )
}

@Composable
fun AlbumItem(
    album: SynapService.AlbumItem,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) {
    AlbumItem(
        thumbnailUrl = album.thumbnail?.url,
        title = album.info?.name,
        authors = album.authors?.joinToString(", ") { it.name ?: "" },
        year = album.year,
        thumbnailSizePx = thumbnailSizePx,
        thumbnailSizeDp = thumbnailSizeDp,
        alternative = alternative,
        modifier = modifier
    )
}

@Composable
fun AlbumItem(
    thumbnailUrl: String?,
    title: String?,
    authors: String?,
    year: String?,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false,
    thumbnailContent: @Composable (BoxScope.() -> Unit)? = null
) {
    val (_, typography, thumbnailShape) = LocalAppearance.current

    ItemContainer(
        alternative = alternative,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(thumbnailShape)
                .size(thumbnailSizeDp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl?.thumbnail(thumbnailSizePx))
                    .placeholderMemoryCacheKey(thumbnailUrl?.thumbnailLowRes())
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(200)
                    .build(),
                placeholder = rememberAsyncImagePainter(thumbnailUrl?.thumbnailLowRes()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            thumbnailContent?.invoke(this)
        }

        ItemInfoContainer {
            BasicText(
                text = title ?: "",
                style = typography.xs.semiBold,
                maxLines = if (alternative) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (!alternative) {
                authors?.let {
                    BasicText(
                        text = authors,
                        style = typography.xs.semiBold.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            BasicText(
                text = year ?: "",
                style = typography.xxs.semiBold.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AlbumItemPlaceholder(
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

        ItemInfoContainer {
            TextPlaceholder()

            if (!alternative) {
                TextPlaceholder()
            }

            TextPlaceholder(
                modifier = Modifier
                    .padding(top = 4.dp)
            )
        }
    }
}
