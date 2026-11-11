package com.synap.music.ui.screens.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.synap.music.BuildConfig
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.R
import com.synap.music.ui.components.themed.ConfirmationDialog
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.bold
import com.synap.music.utils.secondary

/**
 * About settings tab showing app version, source code link, and legal info.
 */
@ExperimentalAnimationApi
@Composable
fun AboutSettings() {
    val (colorPalette, typography) = LocalAppearance.current
    val uriHandler = LocalUriHandler.current

    var isShowingLegalDialog by remember { mutableStateOf(false) }

    if (isShowingLegalDialog) {
        ConfirmationDialog(
            text = "SynapMusic is an independent open-source project and is NOT affiliated with, authorized, maintained, sponsored, or endorsed by Google LLC or YouTube. All trademarks belong to their respective owners.",
            confirmText = "Close",
            cancelText = "Back",
            onDismiss = { isShowingLegalDialog = false },
            onConfirm = { isShowingLegalDialog = false }
        )
    }

    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                PaddingValues(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    top = paddingValues.calculateTopPadding() + 72.dp,
                    end = paddingValues.calculateEndPadding(layoutDirection),
                    bottom = paddingValues.calculateBottomPadding()
                )
            )
    ) {
        SettingsGroupCard(title = "ABOUT") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colorPalette.background2)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "SynapMusic Logo",
                        modifier = Modifier.size(44.dp)
                    )
                }
                Column {
                    BasicText(
                        text = "SynapMusic",
                        style = typography.m.bold.copy(color = colorPalette.text)
                    )
                    BasicText(
                        text = "v${BuildConfig.VERSION_NAME} by ACVG",
                        style = typography.s.secondary
                    )
                }
            }

            SettingsEntry(
                title = "GitHub",
                text = "View the source code",
                onClick = {
                    uriHandler.openUri("https://github.com/amarachan/SynapMusic")
                }
            )

            SettingsEntry(
                title = "Report an issue",
                text = "You will be redirected to GitHub",
                onClick = {
                    uriHandler.openUri("https://github.com/amarachan/SynapMusic/issues/new?assignees=&labels=bug&template=bug_report.yaml")
                }
            )

            SettingsEntry(
                title = "Request a feature or suggest an idea",
                text = "You will be redirected to GitHub",
                onClick = {
                    uriHandler.openUri("https://github.com/amarachan/SynapMusic/issues/new?assignees=&labels=enhancement&template=feature_request.yaml")
                }
            )

            SettingsEntry(
                title = "Legal Information",
                text = "Trademark and non-affiliation notice",
                onClick = { isShowingLegalDialog = true }
            )
        }
    }
}
