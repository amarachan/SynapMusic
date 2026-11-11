package com.synap.music.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synap.music.listentogether.ListenTogether
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.center
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.toast
import kotlinx.coroutines.launch

/**
 * Dialog UI component for creating or joining a synchronized "Listen Together" party room with a 6-digit code.
 */
@Composable
fun ListenTogetherDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val (colorPalette, typography) = LocalAppearance.current
    val coroutineScope = rememberCoroutineScope()

    val partyState by ListenTogether.state.collectAsState()

    var codeInput by remember { mutableStateOf(TextFieldValue("")) }
    var isJoining by remember { mutableStateOf(false) }

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicText(
                text = "Listen Together",
                style = typography.s.semiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (!partyState.inParty) {
                // Creation / Join View
                BasicText(
                    text = "Sync music playback with friends in real-time.",
                    style = typography.xs.secondary.center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 6-digit Code Input with background0 for high contrast
                BasicTextField(
                    value = codeInput,
                    onValueChange = { if (it.text.length <= 6) codeInput = it.copy(text = it.text.uppercase()) },
                    textStyle = typography.m.semiBold.center,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (codeInput.text.length == 6) {
                                isJoining = true
                                coroutineScope.launch {
                                    val res = ListenTogether.joinParty(codeInput.text)
                                    isJoining = false
                                    if (res.isFailure) {
                                        context.toast(res.exceptionOrNull()?.message ?: "Failed to join party")
                                    }
                                }
                            }
                        }
                    ),
                    cursorBrush = SolidColor(colorPalette.text),
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorPalette.background0)
                                .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            if (codeInput.text.isEmpty()) {
                                BasicText(
                                    text = "ENTER 6-DIGIT CODE",
                                    style = typography.xs.semiBold.secondary,
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DialogTextButton(
                        text = "Join Room",
                        primary = false,
                        onClick = {
                            if (codeInput.text.length == 6) {
                                isJoining = true
                                coroutineScope.launch {
                                    val res = ListenTogether.joinParty(codeInput.text)
                                    isJoining = false
                                    if (res.isFailure) {
                                        context.toast(res.exceptionOrNull()?.message ?: "Failed to join party")
                                    }
                                }
                            } else {
                                context.toast("Room code must be 6 characters")
                            }
                        }
                    )

                    DialogTextButton(
                        text = "Create Room",
                        primary = true,
                        onClick = {
                            isJoining = true
                            coroutineScope.launch {
                                val res = ListenTogether.createParty()
                                isJoining = false
                                if (res.isFailure) {
                                    context.toast(res.exceptionOrNull()?.message ?: "Failed to create party")
                                }
                            }
                        }
                    )
                }

            } else {
                // In-Party Room View
                val code = partyState.code.orEmpty()

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorPalette.background0)
                        .border(1.dp, colorPalette.accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BasicText(
                            text = "ROOM CODE",
                            style = typography.xxs.secondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        BasicText(
                            text = code,
                            style = typography.xxl.semiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        DialogTextButton(
                            text = "Copy Code",
                            primary = false,
                            onClick = {
                                clipboard.setText(AnnotatedString(code))
                                context.toast("Code copied to clipboard!")
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Active Members
                BasicText(
                    text = "Connected Members (${partyState.members.size})",
                    style = typography.xs.semiBold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    partyState.members.forEach { member ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (member.connected) colorPalette.accent else colorPalette.textSecondary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicText(
                                text = member.displayName + if (member.isHost) " (Host)" else "",
                                style = typography.xs.semiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Currently playing track in room
                val track = partyState.playback.track
                if (track != null) {
                    BasicText(
                        text = "Now Playing: ${track.title} - ${track.artist}",
                        style = typography.xxs.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DialogTextButton(
                        text = "Leave Room",
                        primary = true,
                        onClick = {
                            coroutineScope.launch {
                                ListenTogether.leaveParty()
                                onDismiss()
                            }
                        }
                    )
                }
            }
        }
    }
}
