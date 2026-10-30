package com.synap.music.ui.components.themed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synap.music.R
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.ui.styling.shimmer
import com.synap.music.utils.isLandscape
import com.synap.music.utils.thumbnail
import com.valentinilk.shimmer.shimmer

@Composable
inline fun LayoutWithAdaptiveThumbnail(
    thumbnailContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    if (isLandscape) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                thumbnailContent()
            }
            Box(modifier = Modifier.weight(0.6f)) {
                content()
            }
        }
    } else {
        content()
    }
}

fun adaptiveThumbnailContent(
    isLoading: Boolean,
    url: String?,
    shape: Shape? = null
): @Composable () -> Unit = {
    val (colorPalette, _, thumbnailShape) = LocalAppearance.current

    BoxWithConstraints(
        modifier = if (isLandscape) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val thumbnailSizeDp = if (isLandscape) (maxHeight * 0.8f) else (maxWidth * 0.8f)
        val thumbnailSizePx = thumbnailSizeDp.px

        val modifier = Modifier
            .padding(all = 16.dp)
            .size(thumbnailSizeDp)
            .clip(shape ?: thumbnailShape)

        if (isLoading) {
            Spacer(
                modifier = modifier
                    .shimmer()
                    .background(colorPalette.shimmer)
            )
        } else if (url != null) {
            AsyncImage(
                model = url.thumbnail(thumbnailSizePx),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } else {
            Box(
                modifier = modifier
                    .background(colorPalette.background1),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.disc),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette.textDisabled),
                    modifier = Modifier.size(thumbnailSizeDp / 3)
                )
            }
        }
    }
}
