package com.synap.music.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.medium
import com.synap.music.utils.secondary

/**
 * Root container component for dropdown and long-press sub-menus.
 * Wraps content in a scrollable column with an accent-tinted visual scrollbar track and thumb.
 */
@Composable
inline fun Menu(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .padding(top = 48.dp)
            .fillMaxWidth()
            .background(colorPalette.background1)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxWidth()
                .padding(top = 2.dp)
                .padding(vertical = 8.dp)
                .navigationBarsPadding(),
            content = content
        )

        if (scrollState.maxValue > 0) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .width(5.dp)
                    .fillMaxHeight()
            ) {
                val canvasHeight = size.height
                val scrollMax = scrollState.maxValue.toFloat()
                val scrollValue = scrollState.value.toFloat()

                val scrollbarHeight = (canvasHeight * (canvasHeight / (canvasHeight + scrollMax)))
                    .coerceAtLeast(24.dp.toPx())

                val scrollbarOffset = (scrollValue / scrollMax) * (canvasHeight - scrollbarHeight)

                drawRoundRect(
                    color = colorPalette.textDisabled.copy(alpha = 0.2f),
                    size = size,
                    cornerRadius = CornerRadius(2.5f.dp.toPx(), 2.5f.dp.toPx())
                )

                drawRoundRect(
                    color = colorPalette.accent,
                    topLeft = Offset(0f, scrollbarOffset),
                    size = Size(width = size.width, height = scrollbarHeight),
                    cornerRadius = CornerRadius(2.5f.dp.toPx(), 2.5f.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun MenuEntry(
    @DrawableRes icon: Int,
    text: String,
    onClick: () -> Unit,
    secondaryText: String? = null,
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val (colorPalette, typography) = LocalAppearance.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 24.dp)
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.text),
            modifier = Modifier
                .size(15.dp)
        )

        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .weight(1f)
        ) {
            BasicText(
                text = text,
                style = typography.xs.medium
            )

            secondaryText?.let { secondaryText ->
                BasicText(
                    text = secondaryText,
                    style = typography.xxs.medium.secondary
                )
            }
        }

        trailingContent?.invoke()
    }
}
