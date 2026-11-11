package com.synap.music.ui.components.themed

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.synap.music.Database
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.recognition.AudioRecorder
import com.synap.music.recognition.MusicRecognitionClient
import com.synap.music.recognition.RecognitionResult
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.rememberButtonColor
import com.synap.music.utils.auddApiKeyKey
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.forcePlay
import com.synap.music.utils.preferences
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dialog UI component for ambient music recognition ("Shazam" mode).
 * Handles microphone permission requests, displays listening pulse animation, and presents identified song cards.
 */
@Composable
fun MusicRecognitionDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (colorPalette, typography) = LocalAppearance.current
    val coroutineScope = rememberCoroutineScope()
    val binder = LocalPlayerServiceBinder.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var progressText by remember { mutableStateOf("Listening to ambient music...") }
    var resultState by remember { mutableStateOf<RecognitionResult?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            context.toast("Microphone permission is required to identify songs")
            onDismiss()
        }
    }

    fun startListening() {
        isListening = true
        errorMessage = null
        resultState = null

        coroutineScope.launch {
            try {
                val recorder = AudioRecorder(context)
                val audioFile = recorder.recordSample(durationSec = 5) { sec ->
                    progressText = "Listening... ${5 - sec}s"
                }

                progressText = "Identifying song..."

                val apiKey = context.preferences.getString(auddApiKeyKey, "test") ?: "test"
                val client = MusicRecognitionClient(apiKey)
                val recognitionResult = client.recognizeSong(audioFile)

                if (recognitionResult.isSuccess) {
                    resultState = recognitionResult.getOrNull()
                    progressText = "Song Identified!"
                } else {
                    errorMessage = recognitionResult.exceptionOrNull()?.message ?: "No match found"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "An error occurred during recording"
            } finally {
                isListening = false
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            BasicText(
                text = "Music Recognition",
                style = typography.s.semiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (!hasPermission) {
                BasicText(
                    text = "Microphone permission is needed to record audio.",
                    style = typography.xs.secondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                DialogTextButton(
                    primary = true,
                    text = "Grant Permission",
                    onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
            } else {
                if (resultState == null && errorMessage == null) {
                    val buttonColor = rememberButtonColor()
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(24.dp)
                            .size(72.dp)
                            .scale(if (isListening) pulseScale else 1.0f)
                            .clip(CircleShape)
                            .background(buttonColor)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.soundwave),
                            contentDescription = "Soundwave",
                            colorFilter = ColorFilter.tint(colorPalette.background0),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    BasicText(
                        text = progressText,
                        style = typography.xs.semiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (!isListening) {
                        DialogTextButton(
                            primary = true,
                            text = "Start Listening",
                            onClick = { startListening() }
                        )
                    }
                } else if (errorMessage != null) {
                    BasicText(
                        text = errorMessage ?: "No match found",
                        style = typography.xs.semiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        DialogTextButton(text = "Close", onClick = onDismiss)
                        DialogTextButton(primary = true, text = "Try Again", onClick = { startListening() })
                    }
                } else if (resultState != null) {
                    val result = resultState!!
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        BasicText(
                            text = result.title,
                            style = typography.m.semiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        BasicText(
                            text = result.artist,
                            style = typography.xs.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        result.album?.let {
                            BasicText(
                                text = it,
                                style = typography.xxs.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val matched = result.matchedSongItem
                    if (matched != null) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DialogTextButton(
                                primary = true,
                                text = "Play Now",
                                onClick = {
                                    val mediaItem = matched.asMediaItem
                                    binder?.stopRadio()
                                    binder?.player?.forcePlay(mediaItem)
                                    onDismiss()
                                }
                            )

                            DialogTextButton(
                                text = "Save",
                                onClick = {
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            Database.insert(matched.asMediaItem)
                                        }
                                        withContext(Dispatchers.Main) {
                                            context.toast("Saved ${result.title} to Library")
                                            onDismiss()
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        BasicText(
                            text = "Track identified, but not found on YouTube Music.",
                            style = typography.xxs.secondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        DialogTextButton(text = "Close", onClick = onDismiss)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (hasPermission && !isListening && resultState == null && errorMessage == null) {
            startListening()
        }
    }
}
