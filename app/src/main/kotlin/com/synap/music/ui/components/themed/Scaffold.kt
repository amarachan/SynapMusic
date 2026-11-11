package com.synap.music.ui.components.themed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.bold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.synap.music.LocalPlayerAwareWindowInsets

/**
 * The primary container Scaffold for tabbed screens.
 * Uses a fluid top action bar that dynamically collapses dead-zone spacers when start/end icon IDs
 * are null, allowing search bars and title elements to fill 100% of available screen width.
 */
@ExperimentalAnimationApi
@Composable
fun Scaffold(
    tabIndex: Int,
    onTabChanged: (Int) -> Unit,
    tabColumnContent: @Composable (@Composable (Int, String, Int) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    topStartIconButtonId: Int? = null,
    onTopStartIconButtonClick: (() -> Unit)? = null,
    topEndIconButtonId: Int? = null,
    onTopEndIconButtonClick: (() -> Unit)? = null,
    topBarActions: @Composable RowScope.() -> Unit = {},
    topBarModifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val (colorPalette) = LocalAppearance.current

    val paddingValues = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top).asPaddingValues()

    Column(
        modifier = modifier
            .background(colorPalette.background0)
            .fillMaxSize()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = tabIndex,
                transitionSpec = {
                    val slideDirection = when (targetState > initialState) {
                        true -> AnimatedContentScope.SlideDirection.Left
                        false -> AnimatedContentScope.SlideDirection.Right
                    }

                    val animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )

                    slideIntoContainer(slideDirection, animationSpec) with
                            slideOutOfContainer(slideDirection, animationSpec)
                },
                content = content
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(colorPalette.background0)
                    .then(topBarModifier)
                    .padding(paddingValues)
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 8.dp)
            ) {
                if (topStartIconButtonId != null) {
                    Image(
                        painter = painterResource(topStartIconButtonId),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onTopStartIconButtonClick ?: {})
                            .padding(all = 12.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    title?.invoke()
                }

                topBarActions()

                if (topEndIconButtonId != null) {
                    Image(
                        painter = painterResource(topEndIconButtonId),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onTopEndIconButtonClick ?: {})
                            .padding(all = 12.dp)
                    )
                }
            }
        }

        NavigationRail(
            tabIndex = tabIndex,
            onTabIndexChanged = onTabChanged,
            content = tabColumnContent
        )
    }
}
