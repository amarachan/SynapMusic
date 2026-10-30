package com.synap.music.ui.screens.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.PureBlackColorPalette
import com.synap.music.utils.center
import com.synap.music.utils.color
import com.synap.music.utils.semiBold

/**
 * Animated Karaoke Lyrics Sentence Line.
 * Animates scale, alpha, and text color in real-time when the sentence becomes active.
 */
@Composable
fun KaraokeLyricsLine(
    text: String,
    isActive: Boolean,
    isPast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current

    // Smooth spring scale animation (1.0x -> 1.12x when singing)
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // Smooth alpha transition
    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1.0f
            isPast -> 0.5f
            else -> 0.35f
        },
        animationSpec = tween(durationMillis = 300)
    )

    // Dynamic text color
    val textColor by animateColorAsState(
        targetValue = when {
            isActive -> colorPalette.accent
            isPast -> PureBlackColorPalette.text.copy(alpha = 0.6f)
            else -> PureBlackColorPalette.textDisabled
        },
        animationSpec = tween(durationMillis = 300)
    )

    BasicText(
        text = text,
        style = typography.s.center.semiBold.color(textColor.copy(alpha = alpha)),
        modifier = modifier
            .scale(scale)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 28.dp)
    )
}
