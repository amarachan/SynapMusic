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

package com.synap.music.service

import android.os.Binder as AndroidBinder
import android.annotation.SuppressLint
import android.util.Log
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.database.SQLException
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaDescription
import android.media.MediaMetadata
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import com.synap.music.enums.AudioEnhancementCategory
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import com.synap.pulse.models.NavigationEndpoint
import com.synap.music.Database
import com.synap.music.discord.DiscordRPCManager
import com.synap.music.listentogether.PartySync
import com.synap.music.webdav.WebDavConfig
import com.synap.music.utils.webdavUsernameKey
import com.synap.music.utils.webdavPasswordKey
import com.synap.music.MainActivity
import com.synap.music.R
import com.synap.music.enums.CompressionEngine
import com.synap.music.enums.ExoPlayerDiskCacheMaxSize
import com.synap.music.enums.LoudnessTarget
import kotlinx.coroutines.flow.firstOrNull
import com.synap.music.models.Event
import com.synap.music.models.QueuedMediaItem
import com.synap.music.query
import com.synap.music.utils.InvincibleService
import com.synap.music.utils.RingBuffer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.synap.music.utils.YouTubeRadio
import com.synap.music.utils.MediaFetchUtils
import com.synap.music.utils.audioEnhancementCategoryKey
import com.synap.music.utils.dynamicRangeCompressionKey
import com.synap.music.utils.spatialMasteringKey
import com.synap.music.utils.immersiveDecodingKey
import com.synap.music.utils.loudnessTargetKey
import com.synap.music.utils.crossfeedEnabledKey
import com.synap.music.utils.activityPendingIntent
import com.synap.music.utils.broadCastPendingIntent
import com.synap.music.utils.exoPlayerDiskCacheMaxSizeKey
import com.synap.music.utils.forcePlayFromBeginning
import com.synap.music.utils.forceSeekToNext
import com.synap.music.utils.forceSeekToPrevious
import com.synap.music.utils.getEnum
import com.synap.music.utils.intent
import com.synap.music.utils.isAtLeastAndroid13
import com.synap.music.utils.isAtLeastAndroid6
import com.synap.music.utils.isAtLeastAndroid8
import com.synap.music.utils.isInvincibilityEnabledKey
import com.synap.music.utils.isShowingThumbnailInLockscreenKey
import com.synap.music.utils.mediaItems
import com.synap.music.utils.persistentQueueKey
import com.synap.music.utils.preferences
import com.synap.music.utils.queueLoopEnabledKey
import com.synap.music.utils.resumePlaybackWhenDeviceConnectedKey
import com.synap.music.utils.shouldBePlaying
import com.synap.music.utils.skipSilenceKey
import com.synap.music.utils.trackLoopEnabledKey
import com.synap.music.utils.volumeNormalizationKey
import com.synap.music.utils.sponsorBlockEnabledKey
import com.synap.music.utils.SponsorBlock
import com.synap.music.utils.SponsorBlockSegment
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * This service is the "brain" of the app. It handles everything related to music playback,
 * keeping it running even when the app is in the background. It also manages the audio
 * pipeline, including caching, EQ, and technical insights.
 */
@Suppress("DEPRECATION")
class PlayerService : InvincibleService(), Player.Listener, PlaybackStatsListener.Callback,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var mediaSession: MediaSession
    private lateinit var cache: SimpleCache
    private lateinit var player: ExoPlayer

    // Standard media controls (Play, Pause, Skip, etc.) for the lockscreen and notification.
    private val stateBuilder = PlaybackState.Builder()
        .setActions(
            PlaybackState.ACTION_PLAY
                    or PlaybackState.ACTION_PAUSE
                    or PlaybackState.ACTION_PLAY_PAUSE
                    or PlaybackState.ACTION_STOP
                    or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                    or PlaybackState.ACTION_SKIP_TO_NEXT
                    or PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM
                    or PlaybackState.ACTION_SEEK_TO
                    or PlaybackState.ACTION_REWIND
        )

    private val metadataBuilder = MediaMetadata.Builder()

    private var notificationManager: NotificationManager? = null

    private var timerJob: Job? = null

    private var radio: YouTubeRadio? = null

    private lateinit var bitmapProvider: BitmapProvider

    private val coroutineScope = CoroutineScope(Dispatchers.IO) + Job()

    private var volumeNormalizationJob: Job? = null
    private var sponsorBlockJob: Job? = null
    private var skipSegments = emptyList<SponsorBlockSegment>()

    private var isPersistentQueueEnabled = false
    private var isShowingThumbnailInLockscreen = true
    private var isSponsorBlockEnabled = false
    override var isInvincibilityEnabled = false

    private var audioManager: AudioManager? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var audioEnhancementManager: AudioEnhancementManager? = null
    private var crossfadeManager: CrossfadeManager? = null
    private var discordRPCManager: DiscordRPCManager? = null
    private var partySync: PartySync? = null

    private val isVideoMap = ConcurrentHashMap<String, Boolean>()

    private val binder = Binder()

    private var isNotificationStarted = false
    private var isSleepAtEndOfTrack = false

    override val notificationId: Int
        get() = NOTIFICATION_ID

    private lateinit var notificationActionReceiver: NotificationActionReceiver

    override fun onBind(intent: Intent?): AndroidBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        // Bitmap provider for notification artwork and lockscreen media controls.
        bitmapProvider = BitmapProvider(
            context = this,
            bitmapSize = (256 * resources.displayMetrics.density).roundToInt(),
            colorProvider = { isSystemInDarkMode ->
                if (isSystemInDarkMode) Color.BLACK else Color.WHITE
            }
        )

        createNotificationChannel()

        preferences.registerOnSharedPreferenceChangeListener(this)

        val preferences = preferences
        isPersistentQueueEnabled = preferences.getBoolean(persistentQueueKey, false)
        isInvincibilityEnabled = preferences.getBoolean(isInvincibilityEnabledKey, false)
        isSponsorBlockEnabled = preferences.getBoolean(sponsorBlockEnabledKey, false)
        isShowingThumbnailInLockscreen =
            preferences.getBoolean(isShowingThumbnailInLockscreenKey, false)

        // Set up the disk cache to save data. If you've already streamed a song, 
        // we play it from the disk next time instead of re-downloading it.
        val cacheEvictor = when (val size =
            preferences.getEnum<ExoPlayerDiskCacheMaxSize>(exoPlayerDiskCacheMaxSizeKey, ExoPlayerDiskCacheMaxSize.`2GB`)) {
            ExoPlayerDiskCacheMaxSize.Unlimited -> NoOpCacheEvictor()
            else -> LeastRecentlyUsedCacheEvictor(size.bytes)
        }

        // Migrate legacy cache folders if we find them. 
        val directory = cacheDir.resolve("exoplayer").also { directory ->
            if (directory.exists()) return@also

            directory.mkdir()

            cacheDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name.length == 1 && file.name.isDigitsOnly() || file.extension == "uid") {
                    if (!file.renameTo(directory.resolve(file.name))) {
                        file.deleteRecursively()
                    }
                }
            }

            filesDir.resolve("coil").deleteRecursively()
        }
        cache = SimpleCache(directory, cacheEvictor, StandaloneDatabaseProvider(this))

        // Build the main audio player engine (ExoPlayer).
        player = ExoPlayer.Builder(this, createRenderersFactory(), createMediaSourceFactory())
            .setHandleAudioBecomingNoisy(true) // Pause if headphones are unplugged.
            .setWakeMode(C.WAKE_MODE_LOCAL)    // Keep the CPU awake while music is playing.
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setUsePlatformDiagnostics(false)
            .build()

        player.repeatMode = when {
            preferences.getBoolean(trackLoopEnabledKey, false) -> Player.REPEAT_MODE_ONE
            preferences.getBoolean(queueLoopEnabledKey, true) -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }

        player.skipSilenceEnabled = preferences.getBoolean(skipSilenceKey, false)
        player.addListener(this)
        player.addAnalyticsListener(PlaybackStatsListener(false, this))

        crossfadeManager = CrossfadeManager(this, player, coroutineScope)
        discordRPCManager = DiscordRPCManager(this, coroutineScope)
        partySync = PartySync(player, coroutineScope).apply { start() }

        // MediaSession initialization for Bluetooth transport controls and Android Auto.
        mediaSession = MediaSession(baseContext, "PlayerService")
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setCallback(SessionCallback(player))
        mediaSession.setPlaybackState(stateBuilder.build())
        mediaSession.isActive = true

        // Restore persistent queue state from disk if enabled.
        maybeRestorePlayerQueue()

        notificationActionReceiver = NotificationActionReceiver(player)

        val filter = IntentFilter().apply {
            addAction(Action.play.value)
            addAction(Action.pause.value)
            addAction(Action.next.value)
            addAction(Action.previous.value)
        }

        registerReceiver(notificationActionReceiver, filter)

        maybeResumePlaybackWhenDeviceConnected()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // If the app is swiped away from Recents, we stop everything unless music is still playing.
        if (!player.shouldBePlaying) {
            broadCastPendingIntent<NotificationDismissReceiver>().send()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        partySync?.stop()
        discordRPCManager?.close()
        maybeSavePlayerQueue()

        preferences.unregisterOnSharedPreferenceChangeListener(this)

        player.removeListener(this)
        player.stop()
        player.release()

        unregisterReceiver(notificationActionReceiver)

        mediaSession.isActive = false
        mediaSession.release()
        cache.release()

        loudnessEnhancer?.release()
        audioEnhancementManager?.release()

        super.onDestroy()
    }

    override fun shouldBeInvincible(): Boolean {
        // "Invincible" mode keeps the service alive even when the app is minimized.
        // We only engage this if music is actually playing.
        return !player.shouldBePlaying
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (bitmapProvider.setDefaultBitmap() && player.currentMediaItem != null) {
            notificationManager?.notify(NOTIFICATION_ID, notification() ?: return)
        }
        super.onConfigurationChanged(newConfig)
    }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {
        // Track play counts. We only count it as a "play" if you listened for at least 5 seconds.
        val mediaItem =
            eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem

        val totalPlayTimeMs = playbackStats.totalPlayTimeMs

        if (totalPlayTimeMs > 5000) {
            query {
                Database.incrementTotalPlayTimeMs(mediaItem.mediaId, totalPlayTimeMs)
            }
        }

        // If you listened for 30 seconds, we record it in your detailed history.
        if (totalPlayTimeMs > 30000) {
            query {
                try {
                    Database.insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = System.currentTimeMillis(),
                            playTime = totalPlayTimeMs
                        )
                    )
                } catch (_: SQLException) {
                }
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (isSleepAtEndOfTrack) {
            isSleepAtEndOfTrack = false
            player.pause()
            val notification = NotificationCompat
                .Builder(this@PlayerService, SLEEP_TIMER_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Sleep timer ended (End of track)")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(true)
                .setSmallIcon(R.drawable.app_icon)
                .build()
            notificationManager?.notify(SLEEP_TIMER_NOTIFICATION_ID, notification)
        }

        crossfadeManager?.onMediaItemTransition()
        maybeUpdateDiscordRPC()
        partySync?.onLocalIntent(PartySync.Intent.TRACK_CHANGED)

        // Every time a new song starts, we run these background tasks.
        maybeRecoverPlaybackError()
        maybeNormalizeVolume()      // Adjust volume so loud songs don't blast your ears.
        maybeApplyAudioEnhancement() // Apply EQ and bass boost if enabled.
        maybeProcessRadio()          // Keep the endless radio queue populated.
        maybeFetchSponsorBlockSegments() // Get info on segments to skip (like intros).

        mediaItem?.let {
            isVideoMap[it.mediaId] = it.mediaMetadata.extras?.getBoolean("isVideo") ?: false

            // Record Play History immediately upon track start so it reflects in the Songs / History UI!
            query {
                Database.insert(it)
                Database.incrementTotalPlayTimeMs(it.mediaId, 1000L)
                try {
                    Database.insert(
                        Event(
                            songId = it.mediaId,
                            timestamp = System.currentTimeMillis(),
                            playTime = 1000L
                        )
                    )
                } catch (_: Exception) {
                }
            }
        }

        if (mediaItem == null) {
            bitmapProvider.listener?.invoke(null)
        } else if (mediaItem.mediaMetadata.artworkUri == bitmapProvider.lastUri) {
            bitmapProvider.listener?.invoke(bitmapProvider.lastBitmap)
        }

        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
            updateMediaSessionQueue(player.currentTimeline)
        }
        
        // Notify external scrobblers (e.g., Pano Scrobbler, Last.fm, ListenBrainz) that a new track started
        broadcastScrobbleState(0, mediaItem) // 0 = START
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        maybeUpdateDiscordRPC()
        partySync?.onLocalIntent(if (isPlaying) PartySync.Intent.PLAY else PartySync.Intent.PAUSE)
        if (isPlaying) {
            crossfadeManager?.startMonitoring()
        } else {
            crossfadeManager?.stopMonitoring()
        }

        // Notify external scrobblers of playback pause/resume state
        broadcastScrobbleState(if (isPlaying) 1 else 2, player.currentMediaItem) // 1 = RESUME, 2 = PAUSE
    }

    private fun maybeUpdateDiscordRPC() {
        val mediaId = player.currentMediaItem?.mediaId ?: return
        val position = player.currentPosition
        val duration = if (player.duration != C.TIME_UNSET) player.duration else 0L
        val isPlaying = player.isPlaying

        coroutineScope.launch(Dispatchers.IO) {
            val song = Database.getSong(mediaId)
            discordRPCManager?.updatePresence(
                song = song,
                currentPositionMs = position,
                durationMs = duration,
                isPlaying = isPlaying
            )
        }
    }

    /**
     * Emits the standard "Simple Last.fm Scrobbler" (SLS) broadcast intent.
     * This allows any installed third-party tracking app to intercept the playback
     * state and scrobble the track to the user's connected accounts.
     */
    private fun broadcastScrobbleState(state: Int, mediaItem: MediaItem?) {
        mediaItem ?: return
        val duration = player.duration.let { if (it == C.TIME_UNSET) 0L else it / 1000 }
        
        val intent = Intent("com.adam.aslfms.notify.playstatechanged").apply {
            putExtra("state", state)
            putExtra("app-name", "SynapMusic")
            putExtra("app-package", packageName)
            putExtra("artist", mediaItem.mediaMetadata.artist?.toString() ?: "")
            putExtra("album", mediaItem.mediaMetadata.albumTitle?.toString() ?: "")
            putExtra("track", mediaItem.mediaMetadata.title?.toString() ?: "")
            putExtra("duration", duration)
        }
        sendBroadcast(intent)
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        timeline.mediaItems.forEach { mediaItem ->
            isVideoMap[mediaItem.mediaId] = mediaItem.mediaMetadata.extras?.getBoolean("isVideo") ?: false
        }
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
            updateMediaSessionQueue(timeline)
        }
    }

    private fun updateMediaSessionQueue(timeline: Timeline) {
        val builder = MediaDescription.Builder()

        val currentMediaItemIndex = player.currentMediaItemIndex
        val lastIndex = timeline.windowCount - 1
        var startIndex = currentMediaItemIndex - 7
        var endIndex = currentMediaItemIndex + 7

        if (startIndex < 0) {
            endIndex -= startIndex
        }

        if (endIndex > lastIndex) {
            startIndex -= (endIndex - lastIndex)
            endIndex = lastIndex
        }

        startIndex = startIndex.coerceAtLeast(0)

        mediaSession.setQueue(
            List(endIndex - startIndex + 1) { index ->
                val mediaItem = timeline.getWindow(index + startIndex, Timeline.Window()).mediaItem
                MediaSession.QueueItem(
                    builder
                        .setMediaId(mediaItem.mediaId)
                        .setTitle(mediaItem.mediaMetadata.title)
                        .setSubtitle(mediaItem.mediaMetadata.artist)
                        .setIconUri(mediaItem.mediaMetadata.artworkUri)
                        .build(),
                    (index + startIndex).toLong()
                )
            }
        )
    }

    private fun maybeRecoverPlaybackError() {
        if (player.playerError != null) {
            player.prepare()
        }
    }

    /**
     * Dynamically fetches and appends related tracks when approaching queue end.
     */
    private fun maybeProcessRadio() {
        radio?.let { radio ->
            if (player.mediaItemCount - player.currentMediaItemIndex <= 3) {
                coroutineScope.launch(Dispatchers.Main) {
                    player.addMediaItems(radio.process())
                }
            }
        }
    }

    private fun maybeSavePlayerQueue() {
        if (!isPersistentQueueEnabled) return

        val mediaItems = player.currentTimeline.mediaItems
        val mediaItemIndex = player.currentMediaItemIndex
        val mediaItemPosition = player.currentPosition

        mediaItems.mapIndexed { index, mediaItem ->
            QueuedMediaItem(
                mediaItem = mediaItem,
                position = if (index == mediaItemIndex) mediaItemPosition else null
            )
        }.let { queuedMediaItems ->
            query {
                Database.clearQueue()
                Database.insert(queuedMediaItems)
            }
        }
    }

    private fun maybeRestorePlayerQueue() {
        if (!isPersistentQueueEnabled) return

        query {
            val queuedSong = Database.queue()
            Database.clearQueue()

            if (queuedSong.isEmpty()) return@query

            val index = queuedSong.indexOfFirst { it.position != null }.coerceAtLeast(0)

            runBlocking(Dispatchers.Main) {
                player.setMediaItems(
                    queuedSong.map { mediaItem ->
                        mediaItem.mediaItem.buildUpon()
                            .setUri(mediaItem.mediaItem.mediaId)
                            .setCustomCacheKey(mediaItem.mediaItem.mediaId)
                            .build().apply {
                                mediaMetadata.extras?.putBoolean("isFromPersistentQueue", true)
                            }
                    },
                    index,
                    queuedSong[index].position ?: C.TIME_UNSET
                )
                player.prepare()

                val notification = notification() ?: return@runBlocking
                try {
                    startForegroundService(this@PlayerService, intent<PlayerService>())
                    startForeground(NOTIFICATION_ID, notification)
                    isNotificationStarted = true
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Volume Normalization: We look up the "Loudness" data in the DB and adjust the 
     * player gain so all songs play at a consistent level.
     */
    private fun maybeNormalizeVolume() {
        val normalizationEnabled = preferences.getBoolean(volumeNormalizationKey, false)
        val loudnessTarget = preferences.getEnum(loudnessTargetKey, LoudnessTarget.Auto)

        if (!normalizationEnabled) {
            volumeNormalizationJob?.cancel()
            audioEnhancementManager?.updateLoudnessTarget(loudnessTarget, null, player.currentMediaItem)
            return
        }

        player.currentMediaItem?.mediaId?.let { songId ->
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob = coroutineScope.launch(Dispatchers.IO) {
                Database.loudnessDb(songId).cancellable().collectLatest { loudnessDb ->
                    withContext(Dispatchers.Main) {
                        audioEnhancementManager?.updateLoudnessTarget(
                            loudnessTarget,
                            loudnessDb,
                            player.currentMediaItem
                        )
                    }
                }
            }
        }
    }

    private fun maybeApplyAudioEnhancement() {
        if (audioEnhancementManager == null) {
            audioEnhancementManager = AudioEnhancementManager(player.audioSessionId)
        }
        val category = preferences.getEnum(audioEnhancementCategoryKey, AudioEnhancementCategory.Auto)

        audioEnhancementManager?.applyCategory(category, player.currentMediaItem)

        val drcEnabled = preferences.getBoolean(dynamicRangeCompressionKey, false)
        val spatialMasteringEnabled = preferences.getBoolean(spatialMasteringKey, false)
        audioEnhancementManager?.updateMasteringSettings(this, drcEnabled, spatialMasteringEnabled)

        val loudnessTarget = preferences.getEnum(loudnessTargetKey, LoudnessTarget.Auto)
        audioEnhancementManager?.updateLoudnessTarget(loudnessTarget, mediaItem = player.currentMediaItem)

        val crossfeedEnabled = preferences.getBoolean(crossfeedEnabledKey, false)
        audioEnhancementManager?.updateCrossfeed(crossfeedEnabled)

        maybeUpdateImmersiveDecoding()
    }

    private fun maybeUpdateImmersiveDecoding() {
        val enabled = preferences.getBoolean(immersiveDecodingKey, false)
        val isNativeSpatialActive = audioEnhancementManager?.isNativeSystemSpatializerActive(this) == true
        val maxChannels = if (enabled || isNativeSpatialActive) 8 else 2
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setMaxAudioChannelCount(maxChannels)
            .build()
    }

    private fun maybeFetchSponsorBlockSegments() {
        sponsorBlockJob?.cancel()
        skipSegments = emptyList()

        if (!isSponsorBlockEnabled) return

        val videoId = player.currentMediaItem?.mediaId ?: return
        
        sponsorBlockJob = coroutineScope.launch {
            skipSegments = SponsorBlock.getSegments(videoId)
            if (skipSegments.isNotEmpty()) {
                monitorSegments()
            }
        }
    }

    private suspend fun monitorSegments() {
        val skippedUUIDs = mutableSetOf<String>()
        while (true) {
            val (shouldBePlaying, position, duration) = withContext(Dispatchers.Main) {
                Triple(player.shouldBePlaying, player.currentPosition, player.duration)
            }

            if (shouldBePlaying && duration > 0 && position < duration) {
                val segmentToSkip = skipSegments.find {
                    it.UUID !in skippedUUIDs &&
                    position >= (it.start - 200L) &&
                    position < (it.end - 100L) &&
                    it.end < (duration - 1500L)
                }
                if (segmentToSkip != null) {
                    skippedUUIDs.add(segmentToSkip.UUID)
                    withContext(Dispatchers.Main) {
                        player.seekTo(segmentToSkip.end + 100L)
                    }
                }
            }
            delay(500.milliseconds)
        }
    }

    private fun maybeShowSongCoverInLockScreen() {
        val bitmap =
            if (isAtLeastAndroid13 || isShowingThumbnailInLockscreen) bitmapProvider.bitmap else null

        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)

        if (isAtLeastAndroid13 && player.currentMediaItemIndex == 0) {
            metadataBuilder.putText(
                MediaMetadata.METADATA_KEY_TITLE,
                "${player.mediaMetadata.title} "
            )
        }

        mediaSession.setMetadata(metadataBuilder.build())
    }

    @SuppressLint("NewApi")
    private fun maybeResumePlaybackWhenDeviceConnected() {
        if (!isAtLeastAndroid6) return

        if (preferences.getBoolean(resumePlaybackWhenDeviceConnectedKey, false)) {
            if (audioManager == null) {
                audioManager = getSystemService(AUDIO_SERVICE) as AudioManager?
            }

            audioDeviceCallback = object : AudioDeviceCallback() {
                private fun canPlayMusic(audioDeviceInfo: AudioDeviceInfo): Boolean {
                    return audioDeviceInfo.isSink && (
                        audioDeviceInfo.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        audioDeviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        audioDeviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        audioDeviceInfo.type == AudioDeviceInfo.TYPE_USB_HEADSET
                    )
                }

                override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                    if (!player.isPlaying && addedDevices.any(::canPlayMusic)) {
                        player.play()
                    }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) = Unit
            }

            audioManager?.registerAudioDeviceCallback(audioDeviceCallback, handler)

        } else {
            audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
            audioDeviceCallback = null
        }
    }

    private fun sendOpenEqualizerIntent() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        )
    }

    private fun sendCloseEqualizerIntent() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
            }
        )
    }

    private val Player.androidPlaybackState: Int
        get() = when (playbackState) {
            Player.STATE_BUFFERING -> if (playWhenReady) PlaybackState.STATE_BUFFERING else PlaybackState.STATE_PAUSED
            Player.STATE_READY -> if (playWhenReady) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            Player.STATE_ENDED -> PlaybackState.STATE_STOPPED
            Player.STATE_IDLE -> PlaybackState.STATE_NONE
            else -> PlaybackState.STATE_NONE
        }

    override fun onEvents(player: Player, events: Player.Events) {
        if (player.duration != C.TIME_UNSET) {
            mediaSession.setMetadata(
                metadataBuilder
                    .putText(MediaMetadata.METADATA_KEY_TITLE, player.mediaMetadata.title)
                    .putText(MediaMetadata.METADATA_KEY_ARTIST, player.mediaMetadata.artist)
                    .putText(MediaMetadata.METADATA_KEY_ALBUM, player.mediaMetadata.albumTitle)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, player.duration)
                    .build()
            )
        }

        stateBuilder
            .setState(player.androidPlaybackState, player.currentPosition, 1f)
            .setBufferedPosition(player.bufferedPosition)

        mediaSession.setPlaybackState(stateBuilder.build())

        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY
            )
        ) {
            val notification = notification()

            if (notification == null) {
                isNotificationStarted = false
                makeInvincible(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                sendCloseEqualizerIntent()
                notificationManager?.cancel(NOTIFICATION_ID)
                return
            }

            if (player.shouldBePlaying && !isNotificationStarted) {
                try {
                    startForegroundService(this@PlayerService, intent<PlayerService>())
                    startForeground(NOTIFICATION_ID, notification)
                    isNotificationStarted = true
                    makeInvincible(false)
                    sendOpenEqualizerIntent()
                } catch (_: Exception) {
                    isNotificationStarted = false
                }
            } else {
                if (!player.shouldBePlaying) {
                    isNotificationStarted = false
                    stopForeground(STOP_FOREGROUND_DETACH)
                    makeInvincible(true)
                    sendCloseEqualizerIntent()
                }
                notificationManager?.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    /**
     * Listens for shared preference updates to dynamically apply audio and playback configuration.
     * Note: Parameters are nullable for Android 15 (API 35) OnSharedPreferenceChangeListener compatibility.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences == null || key == null) return
        when (key) {
            persistentQueueKey -> isPersistentQueueEnabled =
                sharedPreferences.getBoolean(key, isPersistentQueueEnabled)

            volumeNormalizationKey -> maybeNormalizeVolume()

            sponsorBlockEnabledKey -> {
                isSponsorBlockEnabled = sharedPreferences.getBoolean(key, false)
                maybeFetchSponsorBlockSegments()
            }

            audioEnhancementCategoryKey -> maybeApplyAudioEnhancement()
            dynamicRangeCompressionKey, spatialMasteringKey, loudnessTargetKey, crossfeedEnabledKey -> maybeApplyAudioEnhancement()
            immersiveDecodingKey -> maybeUpdateImmersiveDecoding()

            resumePlaybackWhenDeviceConnectedKey -> maybeResumePlaybackWhenDeviceConnected()

            isInvincibilityEnabledKey -> isInvincibilityEnabled =
                sharedPreferences.getBoolean(key, isInvincibilityEnabled)

            skipSilenceKey -> player.skipSilenceEnabled = sharedPreferences.getBoolean(key, false)
            isShowingThumbnailInLockscreenKey -> {
                isShowingThumbnailInLockscreen = sharedPreferences.getBoolean(key, true)
                maybeShowSongCoverInLockScreen()
            }

            trackLoopEnabledKey, queueLoopEnabledKey -> {
                player.repeatMode = when {
                    preferences.getBoolean(trackLoopEnabledKey, false) -> Player.REPEAT_MODE_ONE
                    preferences.getBoolean(queueLoopEnabledKey, true) -> Player.REPEAT_MODE_ALL
                    else -> Player.REPEAT_MODE_OFF
                }
            }
        }
    }

    override fun notification(): Notification? {
        if (player.currentMediaItem == null) return null

        val playIntent = Action.play.pendingIntent(this)
        val pauseIntent = Action.pause.pendingIntent(this)
        val nextIntent = Action.next.pendingIntent(this)
        val prevIntent = Action.previous.pendingIntent(this)

        val mediaMetadata = player.mediaMetadata

        val builder = if (isAtLeastAndroid8) {
            Notification.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
        } else {
            Notification.Builder(applicationContext)
        }
            .setContentTitle(mediaMetadata.title)
            .setContentText(mediaMetadata.artist)
            .setSubText(player.playerError?.message)
            .setLargeIcon(bitmapProvider.bitmap)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSmallIcon(player.playerError?.let { R.drawable.alert_circle }
                ?: R.drawable.app_icon)
            .setOngoing(false)
            .setContentIntent(activityPendingIntent<MainActivity>(
                flags = PendingIntent.FLAG_UPDATE_CURRENT
            ) {
                putExtra("expandPlayerBottomSheet", true)
            })
            .setDeleteIntent(broadCastPendingIntent<NotificationDismissReceiver>())
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(
                Notification.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession.sessionToken)
            )
            .addAction(R.drawable.play_skip_back, "Skip back", prevIntent)
            .addAction(
                if (player.shouldBePlaying) R.drawable.pause else R.drawable.play,
                if (player.shouldBePlaying) "Pause" else "Play",
                if (player.shouldBePlaying) pauseIntent else playIntent
            )
            .addAction(R.drawable.play_skip_forward, "Skip forward", nextIntent)

        bitmapProvider.load(mediaMetadata.artworkUri) { bitmap ->
            maybeShowSongCoverInLockScreen()
            notificationManager?.notify(NOTIFICATION_ID, builder.setLargeIcon(bitmap).build())
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        notificationManager = getSystemService()

        if (!isAtLeastAndroid8) return

        notificationManager?.run {
            if (getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        "Now playing",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        setSound(null, null)
                        enableLights(false)
                        enableVibration(false)
                    }
                )
            }

            if (getNotificationChannel(SLEEP_TIMER_NOTIFICATION_CHANNEL_ID) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        SLEEP_TIMER_NOTIFICATION_CHANNEL_ID,
                        "Sleep timer",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        setSound(null, null)
                        enableLights(false)
                        enableVibration(false)
                    }
                )
            }
        }
    }

    private fun createCacheDataSource(): DataSource.Factory {
        return CacheDataSource.Factory().setCache(cache).apply {
            setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(
                    this@PlayerService,
                    DefaultHttpDataSource.Factory()
                        .setConnectTimeoutMs(16000)
                        .setReadTimeoutMs(8000)
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0")
                )
            )
        }
    }

    /**
     * This is our custom "Smart Data Source". It intelligently decides whether to play
     * a song from your local Vault (if you downloaded it), from the cache, or 
     * fetch a fresh high-quality stream from YouTube.
     */
    private fun createDataSourceFactory(): DataSource.Factory {
        val service = this
        val chunkLength = 512 * 1024L
        val ringBuffer = RingBuffer<Pair<String, Uri>?>(2) { null }

        val cacheDataSourceFactory = createCacheDataSource()
        val defaultDataSourceFactory = DefaultDataSource.Factory(
            this,
            DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(16000)
                .setReadTimeoutMs(8000)
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0")
        )

        return DataSource.Factory {
            object : DataSource {
                private var currentDataSource: DataSource? = null

                override fun addTransferListener(transferListener: TransferListener) {
                    cacheDataSourceFactory.createDataSource().addTransferListener(transferListener)
                    defaultDataSourceFactory.createDataSource().addTransferListener(transferListener)
                }

                override fun open(dataSpec: DataSpec): Long {
                    // Bypass all URL extraction/caching logic for live radio streams or raw HTTP streams.
                    val isRadioStream = dataSpec.key?.startsWith("radio:") == true || 
                            dataSpec.uri.path?.endsWith(".m3u8", ignoreCase = true) == true ||
                            (dataSpec.uri.scheme == "http" || dataSpec.uri.scheme == "https") && dataSpec.key == null

                    if (isRadioStream) {
                        currentDataSource = defaultDataSourceFactory.createDataSource()
                        return currentDataSource!!.open(dataSpec)
                    }

                    // For local files, dataSpec.key might be null.
                    val videoId = dataSpec.key ?: dataSpec.uri.lastPathSegment ?: dataSpec.uri.toString()

                    if (WebDavConfig.isWebDavId(videoId)) {
                        val fileUrl = WebDavConfig.fileUrlOf(videoId) ?: dataSpec.uri.toString()
                        val username = preferences.getString(webdavUsernameKey, "") ?: ""
                        val password = preferences.getString(webdavPasswordKey, "") ?: ""
                        val authHeader = WebDavConfig.basicAuthHeader(username, password)

                        val httpFactory = DefaultHttpDataSource.Factory()
                            .setConnectTimeoutMs(16000)
                            .setReadTimeoutMs(16000)

                        if (authHeader != null) {
                            httpFactory.setDefaultRequestProperties(mapOf("Authorization" to authHeader))
                        }

                        val ds = DefaultDataSource(service, httpFactory.createDataSource())
                        currentDataSource = ds
                        return ds.open(dataSpec.withUri(fileUrl.toUri()))
                    }

                    val isVideo = isVideoMap[videoId] ?: false

                    val resolvedDataSpec = if (cache.isCached(videoId, dataSpec.position, chunkLength)) {
                        dataSpec
                    } else {
                        // Priority 1: Check the Vault for an offline file.
                        val localFile = MediaFetchUtils.getAudioFile(service, videoId, isVideo)
                        if (localFile.exists() && localFile.length() > 0) {
                            dataSpec.withUri(localFile.toUri())
                        } else {
                            // Priority 2: Use the pre-fetched streaming URL from our buffer.
                            when (videoId) {
                                ringBuffer.getOrNull(0)?.first -> dataSpec.withUri(ringBuffer.getOrNull(0)!!.second)
                                ringBuffer.getOrNull(1)?.first -> dataSpec.withUri(ringBuffer.getOrNull(1)!!.second)
                                else -> {
                                    // Priority 3: Resolve a fresh signed URL from the server.
                                    val url = runBlocking(Dispatchers.IO) {
                                        MediaFetchUtils.getStreamingUrl(service, videoId, isVideo)
                                    } ?: throw PlaybackException(
                                        "Failed to get streaming URL",
                                        null,
                                        PlaybackException.ERROR_CODE_REMOTE_ERROR
                                    )

                                    ringBuffer.append(videoId to url.toUri())
                                    dataSpec.withUri(url.toUri())
                                        .subrange(dataSpec.uriPositionOffset, chunkLength)
                                }
                            }
                        }
                    }

                    currentDataSource = if (resolvedDataSpec.uri.scheme == "file") {
                        defaultDataSourceFactory.createDataSource()
                    } else {
                        cacheDataSourceFactory.createDataSource()
                    }

                    return currentDataSource!!.open(resolvedDataSpec)
                }

                override fun getUri(): Uri? = currentDataSource?.uri

                override fun getResponseHeaders(): Map<String, List<String>> =
                    currentDataSource?.responseHeaders ?: emptyMap()

                override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                    currentDataSource?.read(buffer, offset, length) ?: -1

                override fun close() {
                    currentDataSource?.close()
                    currentDataSource = null
                }
            }
        }
    }

    private fun createMediaSourceFactory(): MediaSource.Factory {
        val dataSourceFactory = createDataSourceFactory()
        val extractorsFactory = createExtractorsFactory()
        val delegate = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

        return object : MediaSource.Factory {
            override fun setDrmSessionManagerProvider(drmSessionManagerProvider: androidx.media3.exoplayer.drm.DrmSessionManagerProvider): MediaSource.Factory = apply {
                delegate.setDrmSessionManagerProvider(drmSessionManagerProvider)
            }

            override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy): MediaSource.Factory = apply {
                delegate.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
            }

            override fun getSupportedTypes(): IntArray = delegate.supportedTypes

            override fun createMediaSource(mediaItem: MediaItem): MediaSource {
                val mediaId = mediaItem.mediaId
                if (mediaId.isNotEmpty() && (mediaItem.localConfiguration?.uri?.toString()?.startsWith("http") != true)) {
                    val file = MediaFetchUtils.getAudioFile(this@PlayerService, mediaId)
                    if (file.exists()) {
                        val mimeType = when (file.extension.lowercase()) {
                            "mkv" -> {
                                val engine = runBlocking(Dispatchers.IO) {
                                    Database.format(mediaId).firstOrNull()?.compressionEngine
                                }
                                when (engine) {
                                    CompressionEngine.WavPack, CompressionEngine.L3AC, CompressionEngine.SAC -> "audio/x-wavpack"
                                    null -> "video/x-matroska"
                                    else -> "video/x-matroska"
                                }
                            }
                            "ogg" -> "audio/ogg"
                            "wv" -> "audio/x-wavpack"
                            "ape" -> "audio/x-ape"
                            "tta" -> "audio/x-tta"
                            "flac" -> "audio/flac"
                            "m4a" -> "audio/mp4"
                            "mp3" -> "audio/mpeg"
                            else -> null
                        }

                        if (mimeType != null) {
                            val updatedMediaItem = mediaItem.buildUpon()
                                .setUri(file.toUri())
                                .setMimeType(mimeType)
                                .build()
                            return delegate.createMediaSource(updatedMediaItem)
                        }
                    }
                }
                if (mediaItem.mediaId.startsWith("radio:")) {
                    return delegate.createMediaSource(mediaItem)
                }

                return delegate.createMediaSource(mediaItem)
            }
        }
    }

    private fun createExtractorsFactory(): ExtractorsFactory {
        return DefaultExtractorsFactory()
    }

    private fun createRenderersFactory(): RenderersFactory {
        return object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
                enableOffload: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder()
                    .setAudioCapabilities(AudioCapabilities.getCapabilities(context))
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setOffloadMode(
                        if (enableOffload) DefaultAudioSink.OFFLOAD_MODE_ENABLED_GAPLESS_REQUIRED
                        else DefaultAudioSink.OFFLOAD_MODE_DISABLED
                    )
                    .build()
            }
        }.apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        }
    }

    inner class Binder : AndroidBinder() {
        val player: ExoPlayer
            get() = this@PlayerService.player

        val cache: Cache
            get() = this@PlayerService.cache

        val mediaSession
            get() = this@PlayerService.mediaSession

        val audioEnhancementManager: AudioEnhancementManager?
            get() = this@PlayerService.audioEnhancementManager

        private var radioJob: Job? = null

        var isLoadingRadio by mutableStateOf(false)
            private set

        fun setBitmapListener(listener: ((Bitmap?) -> Unit)?) {
            bitmapProvider.listener = listener
        }

        fun startSleepTimer(delayMillis: Long, fadeOut: Boolean = true) {
            timerJob?.cancel()
            player.volume = 1.0f
            isSleepAtEndOfTrack = (delayMillis == -1L)

            if (delayMillis <= 0L) return

            val targetTime = System.currentTimeMillis() + delayMillis

            timerJob = coroutineScope.launch(Dispatchers.Main) {
                while (isActive) {
                    val remainingMs = targetTime - System.currentTimeMillis()
                    if (remainingMs <= 0) {
                        player.volume = 1.0f
                        player.pause()
                        val notification = NotificationCompat
                            .Builder(this@PlayerService, SLEEP_TIMER_NOTIFICATION_CHANNEL_ID)
                            .setContentTitle("Sleep timer ended")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true)
                            .setOnlyAlertOnce(true)
                            .setShowWhen(true)
                            .setSmallIcon(R.drawable.app_icon)
                            .build()

                        notificationManager?.notify(SLEEP_TIMER_NOTIFICATION_ID, notification)
                        break
                    } else if (fadeOut && remainingMs <= 60000L) {
                        val fadeVolume = (remainingMs.toFloat() / 60000L).coerceIn(0.0f, 1.0f)
                        player.volume = fadeVolume
                    }
                    delay(200.milliseconds)
                }
            }
        }

        fun setupRadio(endpoint: NavigationEndpoint.Endpoint.Watch?, strictArtistName: String? = null) =
            startRadio(endpoint = endpoint, justAdd = true, strictArtistName = strictArtistName)

        fun playRadio(endpoint: NavigationEndpoint.Endpoint.Watch?, strictArtistName: String? = null) =
            startRadio(endpoint = endpoint, justAdd = false, strictArtistName = strictArtistName)

        private fun startRadio(endpoint: NavigationEndpoint.Endpoint.Watch?, justAdd: Boolean, strictArtistName: String? = null) {
            radioJob?.cancel()
            radio = null
            YouTubeRadio(
                endpoint?.videoId,
                endpoint?.playlistId,
                endpoint?.playlistSetVideoId,
                endpoint?.params,
                strictArtistName = strictArtistName
            ).let { radioInstance ->
                isLoadingRadio = true
                radioJob = coroutineScope.launch(Dispatchers.Main) {
                    try {
                        val fetchedItems = radioInstance.process()
                        if (justAdd) {
                            /**
                             * SMART FILTER: When playing a song from Discover, we already
                             * started the first song. We need to avoid adding it to the
                             * queue again as a duplicate. We filter by Video ID to be 100% sure.
                             */
                            val currentVideoId = player.currentMediaItem?.mediaId
                            val uniqueItems = fetchedItems.filter { it.mediaId != currentVideoId }
                            player.addMediaItems(uniqueItems)
                        } else {
                            // If we are starting the radio from scratch, use the whole list.
                            player.forcePlayFromBeginning(fetchedItems)
                        }
                        radio = radioInstance
                    } catch (e: Exception) {
                        Log.e("PlayerService", "Failed to load radio", e)
                    } finally {
                        isLoadingRadio = false
                    }
                }
            }
        }

        fun stopRadio() {
            isLoadingRadio = false
            radioJob?.cancel()
            radio = null
        }
    }

    private class SessionCallback(private val player: Player) : MediaSession.Callback() {
        override fun onPlay() = player.play()
        override fun onPause() = player.pause()
        override fun onSkipToPrevious() = runCatching(player::forceSeekToPrevious).let { }
        override fun onSkipToNext() = runCatching(player::forceSeekToNext).let { }
        override fun onSeekTo(pos: Long) = player.seekTo(pos)
        override fun onStop() = player.pause()
        override fun onRewind() = player.seekToDefaultPosition()
        override fun onSkipToQueueItem(id: Long) = runCatching { player.seekToDefaultPosition(id.toInt()) }.let { }
    }

    private class NotificationActionReceiver(private val player: Player) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Action.pause.value -> player.pause()
                Action.play.value -> player.play()
                Action.next.value -> player.forceSeekToNext()
                Action.previous.value -> player.forceSeekToPrevious()
            }
        }
    }

    class NotificationDismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            context.stopService(context.intent<PlayerService>())
        }
    }

    @JvmInline
    private value class Action(val value: String) {
        fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            100,
            Intent(value).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT.or(if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        companion object {
            val pause = Action("com.synap.music.pause")
            val play = Action("com.synap.music.play")
            val next = Action("com.synap.music.next")
            val previous = Action("com.synap.music.previous")
        }
    }

    private companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "default_channel_id"

        const val SLEEP_TIMER_NOTIFICATION_ID = 1002
        const val SLEEP_TIMER_NOTIFICATION_CHANNEL_ID = "sleep_timer_channel_id"
    }
}
