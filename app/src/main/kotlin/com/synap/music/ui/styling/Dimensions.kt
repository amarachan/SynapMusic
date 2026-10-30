package com.synap.music.ui.styling

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Suppress("ClassName")
object Dimensions {
    val itemsVerticalPadding = 10.dp

    val navigationRailWidth = 72.dp
    val navigationRailWidthLandscape = 140.dp
    val headerHeight = 160.dp

    object thumbnails {
        val album = 140.dp
        val artist = 200.dp
        val song = 60.dp
        val playlist = album

        object player {
            val song: Dp
                @Composable
                get() = with(LocalConfiguration.current) {
                    minOf(screenHeightDp, screenWidthDp)
                }.dp
        }
    }

    val collapsedPlayer = 64.dp
    val miniPlayerPadding = 12.dp
}

inline val Dp.px: Int
    @Composable
    inline get() = with(LocalDensity.current) { roundToPx() }
