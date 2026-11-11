package com.synap.music.ui.components.themed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val MARQUEE_DP_PER_SEC = 28f
private val MARQUEE_GAP = 36.dp
private const val MARQUEE_REST_MS = 3_000L

/**
 * Live Horizontally Scrolling Marquee Text Component.
 * Automatically crawls leftwards in real-time when song titles or artist names overflow container width.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun MarqueeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    if (!enabled) {
        BasicText(
            text = text,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
        return
    }

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val maxWidthPx = constraints.maxWidth
        val layout = remember(text, style, maxWidthPx) {
            textMeasurer.measure(text = AnnotatedString(text), style = style, maxLines = 1, softWrap = false)
        }
        val overflowing = layout.size.width > maxWidthPx

        val travelPx = if (overflowing) {
            layout.size.width + with(density) { MARQUEE_GAP.roundToPx() }
        } else {
            0
        }

        val offsetX = remember { Animatable(0f) }

        LaunchedEffect(text, travelPx) {
            offsetX.snapTo(0f)
            if (travelPx <= 0) return@LaunchedEffect
            val pxPerMs = with(density) { MARQUEE_DP_PER_SEC.dp.toPx() } / 1000f
            val scrollMs = (travelPx / pxPerMs).roundToInt().coerceAtLeast(400)
            delay(1500L)
            while (true) {
                offsetX.animateTo(-travelPx.toFloat(), tween(scrollMs, easing = LinearEasing))
                offsetX.snapTo(0f)
                delay(MARQUEE_REST_MS)
            }
        }

        Row(
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = text,
                style = style,
                maxLines = 1,
                softWrap = false
            )
            if (overflowing) {
                Spacer(Modifier.width(MARQUEE_GAP))
                BasicText(
                    text = text,
                    style = style,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
