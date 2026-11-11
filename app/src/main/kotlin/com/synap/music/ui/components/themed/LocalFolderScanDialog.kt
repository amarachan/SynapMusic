package com.synap.music.ui.components.themed

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.synap.music.R
import com.synap.music.scanner.LocalFolderScanner
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.rememberButtonColor
import com.synap.music.ui.styling.rememberPulsatingHybridColor
import com.synap.music.utils.center
import com.synap.music.utils.color
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.toast
import kotlinx.coroutines.launch

/**
 * Compact, sleek Local Storage Music Scanner Dialog.
 */
@Composable
fun LocalFolderScanDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (colorPalette, typography) = LocalAppearance.current
    val coroutineScope = rememberCoroutineScope()
    val buttonColor = rememberButtonColor()

    var scannedCount by remember { mutableStateOf(0) }
    var isScanning by remember { mutableStateOf(false) }
    var isScanFinished by remember { mutableStateOf(false) }

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        permissionToRequest
    ) == PackageManager.PERMISSION_GRANTED

    fun startScan() {
        isScanning = true
        isScanFinished = false
        scannedCount = 0
        coroutineScope.launch {
            LocalFolderScanner.scanLocalAudio(context).collect { count ->
                scannedCount = count
            }
            isScanning = false
            isScanFinished = true
            context.toast("Scan complete! Found $scannedCount local tracks.")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScan()
        } else {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
                context.toast("Please allow Audio Storage permission for SynapMusic")
            } catch (_: Exception) {
                context.toast("Storage permission required to scan local music folders")
            }
        }
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
                text = "Local Storage Music Scanner",
                style = typography.xs.semiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BasicText(
                text = "Scans /Music, SD Card, and Downloads folders for audio files.",
                style = typography.xxs.secondary.center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Compact Status Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorPalette.background0)
                    .border(1.dp, if (isScanFinished) colorPalette.accent.copy(alpha = 0.6f) else colorPalette.textDisabled.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isScanning) {
                        BasicText(
                            text = "Scanning local audio files...",
                            style = typography.xxs.secondary.center
                        )
                        BasicText(
                            text = "$scannedCount tracks indexed",
                            style = typography.xs.semiBold.center.color(colorPalette.accent),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    } else if (isScanFinished) {
                        BasicText(
                            text = "Scan Complete!",
                            style = typography.xs.semiBold.center.color(colorPalette.accent)
                        )
                        BasicText(
                            text = "$scannedCount local tracks merged into Vault & Library",
                            style = typography.xxs.secondary.center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    } else {
                        BasicText(
                            text = "Ready to scan device storage",
                            style = typography.xxs.secondary.center
                        )
                        BasicText(
                            text = "Tap 'Start Scan' to begin",
                            style = typography.xs.semiBold.center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                        .background(if (isScanning) colorPalette.background1 else buttonColor)
                        .clickable(enabled = !isScanning) {
                            if (hasPermission) {
                                startScan()
                            } else {
                                permissionLauncher.launch(permissionToRequest)
                            }
                        }
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.search),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(if (isScanning) colorPalette.textDisabled else colorPalette.onAccent),
                            modifier = Modifier.size(16.dp)
                        )
                        BasicText(
                            text = if (isScanning) "Scanning..." else if (isScanFinished) "Scan Again" else "Start Scan",
                            style = typography.xs.semiBold.color(if (isScanning) colorPalette.textDisabled else colorPalette.onAccent)
                        )
                    }
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
                        text = "Close",
                        style = typography.xs.semiBold
                    )
                }
            }
        }
    }
}
