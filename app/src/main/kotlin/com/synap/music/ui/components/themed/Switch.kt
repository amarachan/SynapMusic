package com.synap.music.ui.components.themed

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.rememberButtonColor
import com.synap.music.utils.drawCircle

/**
 * Toggle switch component for settings entries.
 */
@Composable
fun Switch(
    isChecked: Boolean,
    modifier: Modifier = Modifier,
) {
    val (colorPalette) = LocalAppearance.current
    val buttonColor = rememberButtonColor()

    val transition = updateTransition(targetState = isChecked, label = null)

    val backgroundColor by transition.animateColor(label = "backgroundColor") {
        if (it) buttonColor else colorPalette.background2
    }

    val borderColor by transition.animateColor(label = "borderColor") {
        if (it) buttonColor else colorPalette.textSecondary.copy(alpha = 0.45f)
    }

    val thumbColor by transition.animateColor(label = "thumbColor") {
        if (it) colorPalette.onAccent else colorPalette.textSecondary
    }

    val offset by transition.animateDp(label = "offset") {
        if (it) 36.dp else 12.dp
    }

    Canvas(
        modifier = modifier
            .size(width = 48.dp, height = 24.dp)
    ) {
        // Draw track background
        drawRoundRect(
            color = backgroundColor,
            cornerRadius = CornerRadius(x = 12.dp.toPx(), y = 12.dp.toPx()),
        )

        // Draw track outline border
        drawRoundRect(
            color = borderColor,
            cornerRadius = CornerRadius(x = 12.dp.toPx(), y = 12.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Draw thumb circle
        drawCircle(
            color = thumbColor,
            radius = 8.dp.toPx(),
            center = size.center.copy(x = offset.toPx()),
            shadow = Shadow(
                color = Color.Black.copy(alpha = if (isChecked) 0.35f else 0.20f),
                blurRadius = 6.dp.toPx(),
                offset = Offset(x = 0f, y = 1.dp.toPx())
            )
        )
    }
}
