package com.synap.music.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.rememberPulsatingHybridColor
import com.synap.music.utils.color
import com.synap.music.ui.styling.rememberButtonColor
import com.synap.music.utils.medium

/**
 * Action button used in dialogs. Primary actions fill with button color, secondary actions use an outline.
 */
@Composable
fun DialogTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    val (colorPalette, typography) = LocalAppearance.current
    val buttonColor = rememberButtonColor()

    val textColor = when {
        !enabled -> colorPalette.textDisabled
        primary -> colorPalette.onAccent
        else -> colorPalette.text
    }

    val buttonShape = RoundedCornerShape(10.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(buttonShape)
            .background(if (primary) buttonColor else colorPalette.background0)
            .border(
                width = 1.dp,
                color = if (primary) Color.Transparent else colorPalette.textDisabled.copy(alpha = 0.5f),
                shape = buttonShape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        BasicText(
            text = text,
            style = typography.xs.medium.color(textColor)
        )
    }
}
