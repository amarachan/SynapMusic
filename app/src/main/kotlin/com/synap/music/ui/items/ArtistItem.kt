package com.synap.music.ui.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synap.music.R
import com.synap.music.models.Artist
import com.synap.music.models.ArtistWithDownloadStatus
import com.synap.music.ui.components.themed.TextPlaceholder
import com.synap.music.ui.components.themed.Badge
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.shimmer
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.thumbnail
import com.synap.music.utils.thumbnailLowRes
import com.synap.pulse.SynapService
import coil.request.ImageRequest
import coil.request.CachePolicy
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.platform.LocalContext
import com.synap.music.ui.styling.px

@Composable
fun ArtistItem(
    artistWithStatus: ArtistWithDownloadStatus,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false,
) {
    val artist = artistWithStatus.artist
    ArtistItem(
        thumbnailUrl = artist.thumbnailUrl,
        name = artist.name,
        subscribersCount = null,
        thumbnailSizePx = thumbnailSizePx,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier,
        alternative = alternative,
        thumbnailContent = {
            if (artistWithStatus.anyFormatSongs > artistWithStatus.downloadedSongs) {
                Badge(
                    text = "DOWNLOADING",
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
                )
            } else if (artistWithStatus.downloadedSongs > 0) {
                val isFullyDownloaded = artistWithStatus.downloadedSongs >= artistWithStatus.totalSongs && artistWithStatus.totalSongs > 0
                Badge(
                    text = if (isFullyDownloaded) "VAULT" else "${artistWithStatus.downloadedSongs}/${artistWithStatus.totalSongs}",
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
                )
            }
        }
    )
}

@Composable
fun ArtistItem(
    artist: Artist,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false,
    thumbnailContent: (@Composable BoxScope.() -> Unit)? = null
) {
    ArtistItem(
        thumbnailUrl = artist.thumbnailUrl,
        name = artist.name,
        subscribersCount = null,
        thumbnailSizePx = thumbnailSizePx,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier,
        alternative = alternative,
        thumbnailContent = thumbnailContent
    )
}

@Composable
fun ArtistItem(
    artist: SynapService.ArtistItem,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false,
) {
    ArtistItem(
        thumbnailUrl = artist.thumbnail?.url,
        name = artist.info?.name,
        subscribersCount = artist.subscribersCountText,
        thumbnailSizePx = thumbnailSizePx,
        thumbnailSizeDp = thumbnailSizeDp,
        modifier = modifier,
        alternative = alternative
    )
}

@Composable
fun ArtistItem(
    thumbnailUrl: String?,
    name: String?,
    subscribersCount: String?,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false,
    thumbnailContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val (colorPalette, typography) = LocalAppearance.current

    if (alternative) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .padding(8.dp)
                .width(thumbnailSizeDp + 16.dp)
        ) {
            Box(
                modifier = Modifier
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
                    error = painterResource(R.drawable.person),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(colorPalette.background1)
                )
                
                thumbnailContent?.invoke(this)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                BasicText(
                    text = name ?: "Unknown Artist",
                    style = typography.xs.semiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                subscribersCount?.let {
                    BasicText(
                        text = it,
                        style = typography.xxs.semiBold.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier.size(thumbnailSizeDp)
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
                    error = painterResource(R.drawable.person),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(colorPalette.background1)
                )
                
                thumbnailContent?.invoke(this)
            }

            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                BasicText(
                    text = name ?: "Unknown Artist",
                    style = typography.s.semiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                subscribersCount?.let {
                    BasicText(
                        text = it,
                        style = typography.xs.semiBold.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistItemPlaceholder(
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false,
) {
    val (colorPalette) = LocalAppearance.current

    if (alternative) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .padding(8.dp)
                .width(thumbnailSizeDp + 16.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .size(thumbnailSizeDp)
                    .background(color = colorPalette.shimmer, shape = CircleShape)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                TextPlaceholder()
                TextPlaceholder(modifier = Modifier.padding(top = 2.dp))
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .size(thumbnailSizeDp)
                    .background(color = colorPalette.shimmer, shape = CircleShape)
            )

            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                TextPlaceholder()
                TextPlaceholder(modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
