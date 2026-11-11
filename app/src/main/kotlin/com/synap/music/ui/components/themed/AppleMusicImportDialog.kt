package com.synap.music.ui.components.themed

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synap.music.R
import com.synap.music.engine.AppleMusicImportEngine
import com.synap.music.engine.ImportProgress
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.rememberButtonColor
import com.synap.music.ui.styling.rememberPulsatingHybridColor
import com.synap.music.utils.center
import com.synap.music.utils.color
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dialog for importing Apple Music playlists.
 */
@Composable
fun AppleMusicImportDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    val coroutineScope = rememberCoroutineScope()
    val importEngine = remember { AppleMusicImportEngine() }
    val buttonColor = rememberButtonColor()

    var urlInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    var progressState by remember { mutableStateOf<ImportProgress?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    fun triggerImport() {
        if (urlInput.text.isNotBlank()) {
            isImporting = true
            coroutineScope.launch {
                importEngine.importPlaylist(urlInput.text).collect { progress ->
                    progressState = progress
                    if (progress.error != null || (progress.current == progress.total && progress.total > 0)) {
                        delay(1500)
                        onDismiss()
                    }
                }
            }
        }
    }

    DefaultDialog(
        onDismiss = {
            if (!isImporting) onDismiss()
        },
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Compact Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.apple_music),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(0xFFFA233B)),
                    modifier = Modifier.size(20.dp)
                )
                BasicText(
                    text = "Import Apple Music Playlist",
                    style = typography.xs.semiBold
                )
            }

            if (!isImporting) {
                BasicText(
                    text = "Paste a public Apple Music playlist URL to convert and save it to your library.",
                    style = typography.xxs.secondary.center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                BasicTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    textStyle = typography.xs.semiBold.center,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { triggerImport() }
                    ),
                    cursorBrush = SolidColor(colorPalette.text),
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorPalette.background0)
                                .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            if (urlInput.text.isEmpty()) {
                                BasicText(
                                    text = "Paste Apple Music Playlist URL",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = typography.xxs.secondary,
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .padding(bottom = 12.dp)
                )

                // Compact Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorPalette.background0)
                            .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                    ) {
                        BasicText(
                            text = "Cancel",
                            style = typography.xs.semiBold
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(buttonColor)
                            .clickable { triggerImport() }
                    ) {
                        BasicText(
                            text = "Import",
                            style = typography.xs.semiBold.color(Color.White)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorPalette.background0)
                        .border(1.dp, colorPalette.accent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    val currentProgress = progressState
                    if (currentProgress != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (currentProgress.error != null) {
                                BasicText(
                                    text = "Error: ${currentProgress.error}",
                                    style = typography.xs.semiBold.center
                                )
                            } else {
                                BasicText(
                                    text = "Matching: ${currentProgress.current} / ${currentProgress.total}",
                                    style = typography.xs.semiBold.center.color(colorPalette.accent),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                BasicText(
                                    text = currentProgress.currentTrackName,
                                    style = typography.xxs.secondary.center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!isImporting) {
            delay(300)
            focusRequester.requestFocus()
        }
    }
}
