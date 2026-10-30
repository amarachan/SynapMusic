/*
 * Copyright (C) 2026 SynapMusic Maintainers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("SpellCheckingInspection")

package com.synap.music

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.valentinilk.shimmer.LocalShimmerTheme
import com.valentinilk.shimmer.defaultShimmerTheme
import com.synap.compose.persist.PersistMap
import com.synap.compose.persist.PersistMapOwner
import com.synap.music.cubes.AudioCubeManager
import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.requests.playlistPage
import com.synap.pulse.requests.song
import com.synap.music.enums.ColorPaletteName
import com.synap.music.enums.ThumbnailRoundness
import com.synap.music.service.PlayerService
import com.synap.music.ui.components.BottomSheetMenu
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.rememberBottomSheetState
import com.synap.music.ui.screens.albumRoute
import com.synap.music.ui.screens.artistRoute
import com.synap.music.ui.screens.home.HomeScreen
import com.synap.music.ui.screens.player.Player as PlayerScreen
import com.synap.music.ui.screens.playlistRoute
import com.synap.music.ui.styling.Appearance
import com.synap.music.ui.styling.Dimensions
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.colorPaletteOf
import com.synap.music.ui.styling.dynamicColorPaletteOf
import com.synap.music.ui.styling.typographyOf
import com.synap.music.utils.applyFontPaddingKey
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.colorPaletteNameKey
import com.synap.music.utils.forcePlay
import com.synap.music.utils.getEnum
import com.synap.music.utils.intent
import com.synap.music.utils.isAtLeastAndroid6
import com.synap.music.utils.isAtLeastAndroid8
import com.synap.music.utils.preferences
import com.synap.music.utils.thumbnailRoundnessKey
import com.synap.music.utils.useSystemFontKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity(), PersistMapOwner {
    // Binds to PlayerService to manage playback state.
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            (service as? PlayerService.Binder)?.let {
                this@MainActivity.binder = it
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }
    }

    private var binder by mutableStateOf<PlayerService.Binder?>(null)

    // Preserves UI state across process recreation.
    override lateinit var persistMap: PersistMap

    override fun onStart() {
        super.onStart()
        // Connect to PlayerService on activity start.
        bindService(intent<PlayerService>(), serviceConnection, BIND_AUTO_CREATE)
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        persistMap = (lastCustomNonConfigurationInstance as? PersistMap) ?: PersistMap()

        // Enable edge-to-edge window layout.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val launchedFromNotification = intent?.extras?.getBoolean("expandPlayerBottomSheet") == true

        setContent {
            val coroutineScope = rememberCoroutineScope()

            // Clean database metadata asynchronously on startup.
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    Database.cleanDatabaseMetadata()
                }
            }
            
            val isSystemInDarkTheme = isSystemInDarkTheme()

            // This is where we handle the app's look and feel.
            // It loads your preferences for things like round corners, font size, and colors.
            var appearance by rememberSaveable(
                isSystemInDarkTheme,
                stateSaver = Appearance.Companion,
            ) {
                with(preferences) {
                    val colorPaletteName = getEnum(colorPaletteNameKey, ColorPaletteName.Dynamic)
                    val thumbnailRoundness =
                        getEnum(thumbnailRoundnessKey, ThumbnailRoundness.Light)

                    val useSystemFont = getBoolean(useSystemFontKey, false)
                    val applyFontPadding = getBoolean(applyFontPaddingKey, false)

                    // Pick the colors based on whether you're in light/dark mode or using a custom theme.
                    val colorPalette =
                        colorPaletteOf(colorPaletteName, isSystemInDarkTheme)

                    setSystemBarAppearance(colorPalette.isDark)

                    mutableStateOf(
                        Appearance(
                            colorPalette = colorPalette,
                            typography = typographyOf(colorPalette.text, useSystemFont, applyFontPadding),
                            thumbnailShape = thumbnailRoundness.shape()
                        )
                    )
                }
            }

            // Dynamically updates UI color palette when album art changes.
            DisposableEffect(binder, isSystemInDarkTheme) {
                var bitmapListenerJob: Job? = null

                fun setDynamicPalette() {
                    binder?.setBitmapListener { bitmap: Bitmap? ->
                        if (bitmap == null) {
                            // If there's no artwork, fall back to the default colors.
                            val colorPalette =
                                colorPaletteOf(
                                    ColorPaletteName.Dynamic,
                                    isSystemInDarkTheme,
                                )

                            setSystemBarAppearance(colorPalette.isDark)

                            appearance = appearance.copy(
                                colorPalette = colorPalette,
                                typography = appearance.typography.copy(color = colorPalette.text)
                            )

                            return@setBitmapListener
                        }

                        // We found a new cover! Extract its colors and update the whole UI to match.
                        bitmapListenerJob = coroutineScope.launch(Dispatchers.IO) {
                            dynamicColorPaletteOf(bitmap, isSystemInDarkTheme)?.let {
                                withContext(Dispatchers.Main) {
                                    setSystemBarAppearance(it.isDark)
                                }
                                appearance = appearance.copy(
                                    colorPalette = it,
                                    typography = appearance.typography.copy(color = it.text),
                                )
                            }
                        }
                    }
                }

                // If you change a setting while the app is open, we apply it immediately here.
                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                        if (key == null) {
                            val colorPaletteName =
                                sharedPreferences.getEnum(colorPaletteNameKey, ColorPaletteName.Dynamic)
                            val thumbnailRoundness =
                                sharedPreferences.getEnum(thumbnailRoundnessKey, ThumbnailRoundness.Light)
                            val useSystemFont = sharedPreferences.getBoolean(useSystemFontKey, false)
                            val applyFontPadding = sharedPreferences.getBoolean(applyFontPaddingKey, false)

                            bitmapListenerJob?.cancel()
                            binder?.setBitmapListener(null)

                            val colorPalette = colorPaletteOf(colorPaletteName, isSystemInDarkTheme)
                            setSystemBarAppearance(colorPalette.isDark)

                            appearance = Appearance(
                                colorPalette = colorPalette,
                                typography = typographyOf(colorPalette.text, useSystemFont, applyFontPadding),
                                thumbnailShape = thumbnailRoundness.shape()
                            )
                            if (colorPaletteName == ColorPaletteName.Dynamic) {
                                setDynamicPalette()
                            }
                            return@OnSharedPreferenceChangeListener
                        }

                        when (key) {
                            colorPaletteNameKey -> {
                                val colorPaletteName =
                                    sharedPreferences.getEnum(
                                        colorPaletteNameKey,
                                        ColorPaletteName.Dynamic,
                                    )

                                if (colorPaletteName == ColorPaletteName.Dynamic) {
                                    setDynamicPalette()
                                } else {
                                    bitmapListenerJob?.cancel()
                                    binder?.setBitmapListener(null)

                                    val colorPalette = colorPaletteOf(
                                        colorPaletteName,
                                        isSystemInDarkTheme
                                    )

                                    setSystemBarAppearance(colorPalette.isDark)

                                    appearance = appearance.copy(
                                        colorPalette = colorPalette,
                                        typography = appearance.typography.copy(color = colorPalette.text),
                                    )
                                }
                            }

                            thumbnailRoundnessKey -> {
                                val thumbnailRoundness =
                                    sharedPreferences.getEnum(key, ThumbnailRoundness.Light)

                                appearance = appearance.copy(
                                    thumbnailShape = thumbnailRoundness.shape()
                                )
                            }

                            useSystemFontKey, applyFontPaddingKey -> {
                                val useSystemFont = sharedPreferences.getBoolean(useSystemFontKey, false)
                                val applyFontPadding = sharedPreferences.getBoolean(applyFontPaddingKey, false)

                                appearance = appearance.copy(
                                    typography = typographyOf(appearance.colorPalette.text, useSystemFont, applyFontPadding),
                                )
                            }
                        }
                    }

                with(preferences) {
                    registerOnSharedPreferenceChangeListener(listener)

                    val colorPaletteName = getEnum(colorPaletteNameKey, ColorPaletteName.Dynamic)
                    if (colorPaletteName == ColorPaletteName.Dynamic) {
                        setDynamicPalette()
                    }

                    onDispose {
                        bitmapListenerJob?.cancel()
                        binder?.setBitmapListener(null)
                        unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }
            }

            // Standard Material Design ripple effect (the subtle glow when you tap a button).
            val rippleTheme =
                remember(appearance.colorPalette.text, appearance.colorPalette.isDark) {
                    object : RippleTheme {
                        @Composable
                        override fun defaultColor(): Color = RippleTheme.defaultRippleColor(
                            contentColor = appearance.colorPalette.text,
                            lightTheme = !appearance.colorPalette.isDark
                        )

                        @Composable
                        override fun rippleAlpha(): RippleAlpha = RippleTheme.defaultRippleAlpha(
                            contentColor = appearance.colorPalette.text,
                            lightTheme = !appearance.colorPalette.isDark
                        )
                    }
                }

            // The shimmering loading animation you see before images load.
            val shimmerTheme = remember {
                defaultShimmerTheme.copy(
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 800,
                            easing = LinearEasing,
                            delayMillis = 250,
                        ),
                        repeatMode = RepeatMode.Restart
                    ),
                    shaderColors = listOf(
                        Color.Unspecified.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.50f),
                        Color.Unspecified.copy(alpha = 0.25f),
                    ),
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appearance.colorPalette.background0)
            ) {
                val density = LocalDensity.current
                val windowsInsets = WindowInsets.systemBars
                val bottomDp = with(density) { windowsInsets.getBottom(density).toDp() }

                // The logic for the sliding player panel at the bottom.
                val playerBottomSheetState = rememberBottomSheetState(
                    dismissedBound = 0.dp,
                    collapsedBound = Dimensions.collapsedPlayer + bottomDp,
                    expandedBound = maxHeight,
                )

                // Adjusts the rest of the UI (lists, etc.) so they don't get hidden behind the player bar.
                val playerAwareWindowInsets by remember(bottomDp, playerBottomSheetState.value) {
                    derivedStateOf {
                        val bottom = playerBottomSheetState.value.coerceIn(bottomDp, playerBottomSheetState.collapsedBound)

                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(bottom = bottom))
                    }
                }

                    CompositionLocalProvider(
                        LocalAppearance provides appearance,
                        LocalIndication provides rememberRipple(bounded = true),
                        LocalRippleTheme provides rippleTheme,
                        LocalShimmerTheme provides shimmerTheme,
                        LocalPlayerServiceBinder provides binder,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        HomeScreen()

                        PlayerScreen(
                        layoutState = playerBottomSheetState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter),
                    )

                    BottomSheetMenu(
                        state = LocalMenuState.current,
                        modifier = Modifier
                            .align(Alignment.BottomCenter),
                    )
                }

                // This automatically pops up the player when you start a new song.
                DisposableEffect(binder?.player) {
                    val player = binder?.player ?: return@DisposableEffect onDispose { }

                    if (player.currentMediaItem == null) {
                        if (!playerBottomSheetState.isDismissed) {
                            playerBottomSheetState.dismiss()
                        }
                    } else {
                        if (playerBottomSheetState.isDismissed) {
                            if (launchedFromNotification) {
                                intent.replaceExtras(Bundle())
                                playerBottomSheetState.expand(tween(700))
                            } else {
                                playerBottomSheetState.collapse(tween(700))
                            }
                        }
                    }

                    val listener = object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            if ((reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) && (mediaItem != null)) {
                                if (mediaItem.mediaMetadata.extras?.getBoolean("isFromPersistentQueue") != true) {
                                    playerBottomSheetState.expand(tween(500))
                                } else {
                                    playerBottomSheetState.collapse(tween(700))
                                }
                            }
                        }
                    }

                    player.addListener(listener)

                    onDispose { player.removeListener(listener) }
                }
            }
        }

        onNewIntent(intent)
    }

    // Handles deep link URL intents and .synapcube import files.
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val uri = intent?.data ?: return

        intent.data = null
        this.intent = null

        if (uri.path?.endsWith(".synapcube") == true || uri.scheme == "file" || (uri.scheme == "content" && contentResolver.getType(uri)?.contains("synapcube") == true)) {
            Toast.makeText(this, "Importing Audio Cube...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch(Dispatchers.Main) {
                val result = AudioCubeManager.importCube(this@MainActivity, uri)
                if (result.isSuccess) {
                    Toast.makeText(this@MainActivity, "Audio Cube imported successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to import Audio Cube", Toast.LENGTH_LONG).show()
                }
            }
            return
        }

        Toast.makeText(this, "Opening url...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            when (val path = uri.pathSegments.firstOrNull()) {
                "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                    val browseId = "VL$playlistId"

                    if (playlistId.startsWith("OLAK5uy_")) {
                        SynapService.playlistPage(BrowseBody(browseId = browseId))?.getOrNull()?.let {
                            it.songsPage?.list?.firstOrNull()?.album?.endpoint?.browseId?.let { browseId ->
                                albumRoute.ensureGlobal(browseId)
                            }
                        }
                    } else {
                        playlistRoute.ensureGlobal(browseId)
                    }
                }

                "channel", "c" -> uri.lastPathSegment?.let { channelId ->
                    artistRoute.global(channelId, null)
                }

                else -> when {
                    path == "watch" -> uri.getQueryParameter("v")
                    uri.host == "youtu.be" -> path
                    else -> null
                }?.let { videoId ->
                    SynapService.song(videoId)?.getOrNull()?.let { song ->
                        val binder = snapshotFlow { binder }.filterNotNull().first()
                        withContext(Dispatchers.Main) {
                            binder.player.forcePlay(song.asMediaItem)
                        }
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java", replaceWith = ReplaceWith("super.onRetainCustomNonConfigurationInstance()"))
    override fun onRetainCustomNonConfigurationInstance() = persistMap

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            persistMap.clear()
        }

        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun setSystemBarAppearance(isDark: Boolean) {
        with(WindowCompat.getInsetsController(window, window.decorView.rootView)) {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }

        if (!isAtLeastAndroid6) {
            window.statusBarColor =
                (if (isDark) Color.Black else Color.White).toArgb()
        }

        if (!isAtLeastAndroid8) {
            window.navigationBarColor =
                (if (isDark) Color.Black else Color.White).toArgb()
        }
    }
}

val LocalPlayerServiceBinder = staticCompositionLocalOf<PlayerService.Binder?> { null }

val LocalPlayerAwareWindowInsets = staticCompositionLocalOf { WindowInsets(0, 0, 0, 0) }
