package com.synap.music.ui.components.themed

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.rememberPulsatingHybridColor
import com.synap.music.utils.center
import com.synap.music.utils.color
import com.synap.music.utils.semiBold

/**
 * Universal Navigation Rail with real-time pulsating theme-adaptive hybrid active tab icons.
 */
@Composable
inline fun NavigationRail(
    tabIndex: Int,
    crossinline onTabIndexChanged: (Int) -> Unit,
    content: @Composable (@Composable (Int, String, Int) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    val hybridPulseColor = rememberPulsatingHybridColor(primaryColor = colorPalette.accent, secondaryColor = colorPalette.red)

    val paddingValues = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .fillMaxWidth()
            .background(colorPalette.background1)
            .padding(paddingValues)
            .height(72.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        val transition = updateTransition(targetState = tabIndex, label = null)

        content { index, text, icon ->
            val isSelected = index == tabIndex

            val indicatorWidth by transition.animateDp(
                transitionSpec = { spring(dampingRatio = 0.7f) },
                label = ""
            ) {
                if (it == index) 48.dp else 0.dp
            }

            val textColor by transition.animateColor(label = "") {
                if (it == index) colorPalette.text else colorPalette.textDisabled
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onTabIndexChanged(index) }
                    .padding(vertical = 8.dp, horizontal = 8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(hybridPulseColor)
                                .size(width = indicatorWidth, height = 4.dp)
                                .align(Alignment.BottomCenter)
                        )
                    }
                    Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(if (isSelected) hybridPulseColor else colorPalette.textSecondary),
                        modifier = Modifier.size(24.dp)
                    )
                }

                BasicText(
                    text = text,
                    style = typography.xxs.semiBold.center.color(textColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun Modifier.vertical(enabled: Boolean = true) =
    if (enabled)
        layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(maxWidth = Int.MAX_VALUE))
            layout(placeable.height, placeable.width) {
                placeable.place(
                    x = -((placeable.width / 2) - (placeable.height / 2)),
                    y = -((placeable.height / 2) - (placeable.width / 2))
                )
            }
        } else this
