package com.synap.music.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.bold

@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = LocalAppearance.current.colorPalette.accent,
    contentColor: Color = Color.White
) {
    val (_, typography) = LocalAppearance.current
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        BasicText(
            text = text,
            style = typography.xxs.bold.copy(
                color = contentColor,
                fontSize = 8.sp,
                lineHeight = 10.sp
            )
        )
    }
}
