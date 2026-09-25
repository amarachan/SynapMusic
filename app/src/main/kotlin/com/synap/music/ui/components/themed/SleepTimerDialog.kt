package com.synap.music.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.sleepTimerFadeOutEnabledKey

val sleepTimerDurations = listOf(
    -1L to "End of current song",
    5 * 60 * 1000L to "5 minutes",
    15 * 60 * 1000L to "15 minutes",
    30 * 60 * 1000L to "30 minutes",
    45 * 60 * 1000L to "45 minutes",
    60 * 60 * 1000L to "60 minutes",
    90 * 60 * 1000L to "90 minutes"
)

/**
 * Compact, sleek Smart Sleep Timer Dialog with End of Song and Smooth 60s Fade-Out options.
 */
@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onDone: (Long, Boolean) -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    var fadeOutEnabled by rememberPreference(sleepTimerFadeOutEnabledKey, true)

    DefaultDialog(
        onDismiss = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicText(
                text = "Set Smart Sleep Timer",
                style = typography.xs.semiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Compact Fade-Out Toggle Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorPalette.background0)
                    .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable { fadeOutEnabled = !fadeOutEnabled }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    BasicText("Smooth 60s Fade-Out", style = typography.xs.semiBold)
                    BasicText("Gradually lower volume before pausing", style = typography.xxs.secondary)
                }
                Switch(
                    isChecked = fadeOutEnabled
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Compact Timer Durations
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorPalette.background0)
                    .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            ) {
                sleepTimerDurations.forEach { (millis, label) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDone(millis, fadeOutEnabled)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            text = label,
                            style = typography.xs.semiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            DialogTextButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    }
}
