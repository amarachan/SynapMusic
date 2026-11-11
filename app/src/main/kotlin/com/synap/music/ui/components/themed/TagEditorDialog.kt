package com.synap.music.ui.components.themed

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synap.music.engine.TagEditorEngine
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.rememberButtonColor
import com.synap.music.ui.styling.rememberPulsatingHybridColor
import com.synap.music.utils.color
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.toast
import kotlinx.coroutines.launch

/**
 * Dialog for editing song metadata and cover art.
 */
@Composable
fun TagEditorDialog(
    songId: String,
    initialTitle: String,
    initialArtist: String,
    initialArtworkUrl: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current
    val coroutineScope = rememberCoroutineScope()
    val buttonColor = rememberButtonColor()

    var titleInput by remember { mutableStateOf(initialTitle) }
    var artistInput by remember { mutableStateOf(initialArtist) }
    var selectedArtworkUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedArtworkUri = uri
        }
    }

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            BasicText(
                text = "Edit Audio Tags & Cover Art",
                style = typography.xs.semiBold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Compact Artwork Preview
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(80.dp)
                    .clip(thumbnailShape)
                    .background(colorPalette.background1)
                    .clickable { imagePickerLauncher.launch("image/*") }
            ) {
                AsyncImage(
                    model = selectedArtworkUri ?: initialArtworkUrl,
                    contentDescription = "Cover Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(buttonColor)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    BasicText(
                        text = "Change",
                        style = typography.xxs.semiBold.color(colorPalette.onAccent)
                    )
                }
            }

            // Editable Title Field
            BasicText(
                text = "TRACK TITLE",
                style = typography.xxs.secondary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 2.dp)
            )

            BasicTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                textStyle = typography.xs.semiBold,
                singleLine = true,
                cursorBrush = SolidColor(colorPalette.text),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorPalette.background0)
                            .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp)
                    ) {
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Editable Artist Field
            BasicText(
                text = "ARTIST NAME",
                style = typography.xxs.secondary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 2.dp)
            )

            BasicTextField(
                value = artistInput,
                onValueChange = { artistInput = it },
                textStyle = typography.xs.semiBold,
                singleLine = true,
                cursorBrush = SolidColor(colorPalette.text),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorPalette.background0)
                            .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp)
                    ) {
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
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
                        .background(buttonColor)
                        .clickable(enabled = !isSaving) {
                            isSaving = true
                            coroutineScope.launch {
                                val success = TagEditorEngine.updateSongTags(
                                    context = context,
                                    songId = songId,
                                    newTitle = titleInput,
                                    newArtist = artistInput,
                                    customArtworkUri = selectedArtworkUri
                                )
                                isSaving = false
                                if (success) {
                                    context.toast("Tags & Artwork updated successfully!")
                                    onDismiss()
                                } else {
                                    context.toast("Failed to update tags")
                                }
                            }
                        }
                ) {
                    BasicText(
                        text = if (isSaving) "Saving..." else "Save Changes",
                        style = typography.xs.semiBold.color(colorPalette.onAccent)
                    )
                }

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
            }
        }
    }
}
