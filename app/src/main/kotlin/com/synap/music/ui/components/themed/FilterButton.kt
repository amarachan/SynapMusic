package com.synap.music.ui.components.themed

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.synap.music.R
import com.synap.music.enums.SortOrder
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.rememberButtonColor
import com.synap.music.utils.color
import com.synap.music.utils.medium
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.bold

@Composable
fun FilterButton(
    modifier: Modifier = Modifier,
    label: String = "Filter",
    onShowMenu: () -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    val buttonColor = rememberButtonColor()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onShowMenu)
            .background(colorPalette.background2)
            .border(1.dp, buttonColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.sparkles),
            contentDescription = null,
            colorFilter = ColorFilter.tint(buttonColor),
            modifier = Modifier.size(14.dp)
        )

        BasicText(
            text = label,
            style = typography.xxs.semiBold.copy(color = colorPalette.text),
            modifier = Modifier.padding(start = 6.dp)
        )

        Image(
            painter = painterResource(R.drawable.chevron_down),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.textSecondary),
            modifier = Modifier
                .padding(start = 4.dp)
                .size(10.dp)
        )
    }
}

@Composable
fun SortOrderMenuEntry(
    sortOrder: SortOrder,
    onSortOrderChanged: (SortOrder) -> Unit
) {
    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing)
    )

    MenuEntry(
        icon = R.drawable.arrow_up,
        text = "Sort Order",
        secondaryText = if (sortOrder == SortOrder.Ascending) "Ascending" else "Descending",
        onClick = { onSortOrderChanged(!sortOrder) },
        trailingContent = {
            Image(
                painter = painterResource(R.drawable.sync),
                contentDescription = null,
                colorFilter = ColorFilter.tint(LocalAppearance.current.colorPalette.textSecondary),
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = sortOrderIconRotation }
            )
        }
    )
}

@Composable
fun InListSearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    hint: String = "Search in list...",
    onSearch: (() -> Unit)? = null
) {
    val (colorPalette, typography) = LocalAppearance.current
    val buttonColor = rememberButtonColor()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(colorPalette.background2, RoundedCornerShape(8.dp))
            .border(1.dp, buttonColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            colorFilter = ColorFilter.tint(buttonColor),
            modifier = Modifier.size(14.dp)
        )

        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        ) {
            if (query.isEmpty()) {
                BasicText(
                    text = hint,
                    style = typography.xs.medium.secondary
                )
            }

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = typography.xs.medium.color(colorPalette.text),
                cursorBrush = SolidColor(buttonColor),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { onSearch?.invoke() }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (query.isNotEmpty()) {
            Image(
                painter = painterResource(R.drawable.close),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                modifier = Modifier
                    .size(14.dp)
                    .clickable(onClick = onClear)
            )
        }
    }
}
