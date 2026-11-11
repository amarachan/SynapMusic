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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.engine.SmartPlaylistEngine
import com.synap.music.models.Song
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.rememberButtonColor
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.forcePlayFromBeginning
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.toast
import kotlinx.coroutines.launch

/**
 * Dialog for creating and playing AI smart mixes.
 */
@Composable
fun SmartMixesDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val coroutineScope = rememberCoroutineScope()

    var selectedMixType by remember { mutableStateOf(SmartPlaylistEngine.MixType.MostPlayed) }
    var generatedSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    fun loadMix(type: SmartPlaylistEngine.MixType) {
        selectedMixType = type
        isLoading = true
        coroutineScope.launch {
            generatedSongs = SmartPlaylistEngine.generateMix(type)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadMix(SmartPlaylistEngine.MixType.MostPlayed)
    }

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicText(
                text = "Smart Auto-Playlist Generator",
                style = typography.s.semiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                // Mix Type Cards
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    SmartPlaylistEngine.MixType.entries.forEach { mix ->
                        val isSelected = selectedMixType == mix
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) colorPalette.accent else colorPalette.background0)
                                .border(
                                    1.dp,
                                    if (isSelected) colorPalette.accent else colorPalette.textDisabled.copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { loadMix(mix) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                BasicText(
                                    text = mix.label,
                                    style = if (isSelected) typography.xs.semiBold else typography.xs.semiBold
                                )
                                BasicText(
                                    text = mix.description,
                                    style = if (isSelected) typography.xxs.semiBold else typography.xxs.secondary
                                )
                            }
                        }
                    }
                }

                // Generated Tracks Summary & Action Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorPalette.background0)
                        .border(1.dp, colorPalette.accent.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    if (isLoading) {
                        BasicText(
                            text = "Generating Smart Mix...",
                            style = typography.xs.secondary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    BasicText(
                                        text = selectedMixType.label,
                                        style = typography.s.semiBold
                                    )
                                    BasicText(
                                        text = "${generatedSongs.size} tracks generated",
                                        style = typography.xs.secondary
                                    )
                                }
                            }

                            // High-Contrast Action Buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Save Button
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colorPalette.background1)
                                        .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .clickable(enabled = generatedSongs.isNotEmpty()) {
                                            coroutineScope.launch {
                                                val id = SmartPlaylistEngine.saveMixAsPlaylist(selectedMixType.label, generatedSongs)
                                                if (id > 0) {
                                                    context.toast("Saved '${selectedMixType.label}' to playlists!")
                                                    onDismiss()
                                                } else {
                                                    context.toast("Failed to save playlist")
                                                }
                                            }
                                        }
                                ) {
                                    BasicText(
                                        text = "Save as Playlist",
                                        style = typography.xs.semiBold
                                    )
                                }

                                // Play Button
                                val buttonColor = rememberButtonColor()
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(buttonColor)
                                        .clickable(enabled = generatedSongs.isNotEmpty()) {
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayFromBeginning(generatedSongs.map { it.asMediaItem })
                                            context.toast("Playing ${selectedMixType.label}")
                                            onDismiss()
                                        }
                                ) {
                                    BasicText(
                                        text = "Play Mix",
                                        style = typography.xs.semiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            DialogTextButton(
                text = "Close",
                primary = false,
                onClick = onDismiss
            )
        }
    }
}
