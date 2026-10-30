package com.synap.music.ui.components.themed

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.synap.music.R
import com.synap.music.ui.styling.rememberButtonColor
import com.synap.music.utils.color
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.center
import com.synap.music.utils.drawCircle
import com.synap.music.utils.medium
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import kotlinx.coroutines.delay

@Composable
fun TextFieldDialog(
    hintText: String,
    onDismiss: () -> Unit,
    onDone: (String) -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = "Cancel",
    doneText: String = "Done",
    initialTextInput: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    onCancel: () -> Unit = onDismiss,
    isTextInputValid: (String) -> Boolean = { it.isNotEmpty() }
) {
    val focusRequester = remember {
        FocusRequester()
    }
    val (colorPalette, typography) = LocalAppearance.current

    var textFieldValue by rememberSaveable(initialTextInput, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = initialTextInput,
                selection = TextRange(initialTextInput.length)
            )
        )
    }

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                textStyle = typography.xs.semiBold.center,
                singleLine = singleLine,
                maxLines = maxLines,
                keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Done else ImeAction.None),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (isTextInputValid(textFieldValue.text)) {
                            onDismiss()
                            onDone(textFieldValue.text)
                        }
                    }
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
                            .padding(horizontal = 10.dp)
                    ) {
                        if (textFieldValue.text.isEmpty()) {
                            BasicText(
                                text = hintText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = typography.xs.semiBold.secondary,
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .focusRequester(focusRequester)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DialogTextButton(
                    text = cancelText,
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                )

                DialogTextButton(
                    primary = true,
                    text = doneText,
                    enabled = isTextInputValid(textFieldValue.text),
                    onClick = {
                        if (isTextInputValid(textFieldValue.text)) {
                            onDismiss()
                            onDone(textFieldValue.text)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }
}

@Composable
fun ConfirmationDialog(
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = "Cancel",
    confirmText: String = "Confirm",
    onCancel: () -> Unit = onDismiss
) {
    val (_, typography) = LocalAppearance.current

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        BasicText(
            text = text,
            style = typography.xs.medium.center,
            modifier = Modifier
                .padding(all = 16.dp)
        )

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            DialogTextButton(
                text = cancelText,
                onClick = onCancel
            )

            DialogTextButton(
                text = confirmText,
                primary = true,
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
inline fun DefaultDialog(
    noinline onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    crossinline content: @Composable ColumnScope.() -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val dialogShape = RoundedCornerShape(14.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = horizontalAlignment,
                modifier = modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume clicks to prevent dismissal
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .widthIn(max = 320.dp)
                    .clip(dialogShape)
                    .background(colorPalette.background1)
                    .border(1.dp, colorPalette.textDisabled.copy(alpha = 0.35f), dialogShape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                content = content
            )
        }
    }
}

@Composable
inline fun <T> ValueSelectorDialog(
    noinline onDismiss: () -> Unit,
    title: String,
    selectedValue: T,
    values: List<T>,
    crossinline onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    crossinline valueText: (T) -> String = { it.toString() }
) {
    val (colorPalette, typography) = LocalAppearance.current
    val buttonColor = rememberButtonColor()

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
        ) {
            BasicText(
                text = title,
                style = typography.xs.semiBold,
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .align(Alignment.Start)
            )

            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(end = 8.dp)
                ) {
                    values.forEach { value ->
                        val isSelected = selectedValue == value
                        val rowShape = RoundedCornerShape(10.dp)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(rowShape)
                                .background(if (isSelected) buttonColor.copy(alpha = 0.15f) else colorPalette.background0)
                                .border(
                                    1.dp,
                                    if (isSelected) buttonColor else colorPalette.textDisabled.copy(alpha = 0.3f),
                                    rowShape
                                )
                                .clickable {
                                    onDismiss()
                                    onValueSelected(value)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BasicText(
                                    text = valueText(value),
                                    style = if (isSelected) typography.xs.semiBold.color(buttonColor) else typography.xs.medium
                                )

                                if (isSelected) {
                                    Image(
                                        painter = painterResource(R.drawable.checkmark),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(buttonColor),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Visual Scrollbar
                if (scrollState.maxValue > 0) {
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(5.dp)
                            .fillMaxHeight()
                    ) {
                        val canvasHeight = size.height
                        val scrollMax = scrollState.maxValue.toFloat()
                        val scrollValue = scrollState.value.toFloat()

                        val scrollbarHeight = (canvasHeight * (canvasHeight / (canvasHeight + scrollMax)))
                            .coerceAtLeast(24.dp.toPx())

                        val scrollbarOffset = (scrollValue / scrollMax) * (canvasHeight - scrollbarHeight)

                        // Track
                        drawRoundRect(
                            color = colorPalette.textDisabled.copy(alpha = 0.2f),
                            size = size,
                            cornerRadius = CornerRadius(2.5f.dp.toPx(), 2.5f.dp.toPx())
                        )

                        // Accent Thumb
                        drawRoundRect(
                            color = colorPalette.accent,
                            topLeft = Offset(0f, scrollbarOffset),
                            size = Size(width = size.width, height = scrollbarHeight),
                            cornerRadius = CornerRadius(2.5f.dp.toPx(), 2.5f.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Full-Width Cancel Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
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
