package com.synap.music.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.primaryButton
import com.synap.music.utils.medium

@Composable
fun SecondaryTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    alternative: Boolean = false
) {
    val (colorPalette, typography) = LocalAppearance.current

    BasicText(
        text = text,
        style = typography.xxs.medium,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .background(if (alternative) colorPalette.background0 else colorPalette.primaryButton)
            .padding(vertical = 5.dp, horizontal = 10.dp)
    )
}
