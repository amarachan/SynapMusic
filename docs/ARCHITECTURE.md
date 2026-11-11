# SynapMusic - Detailed Technical Documentation

SynapMusic is a high-performance Android music player focused on audio fidelity, privacy, and seamless integration with the YouTube Music ecosystem. This document provides an in-depth look at its architecture, core algorithms, and implementation details.

---

## 1. System Architecture

SynapMusic follows a modular architecture designed for isolation, performance, and maintainability.

### 1.1 Module Breakdown
- **`:app`**: The central entry point. Contains the UI (Jetpack Compose), `PlayerService` (Media3), and the Room Database.
- **`:pulse-engine`**: A specialized, low-level engine for interacting with YouTube Music's internal APIs. It handles request signing, tree walking, and track discovery.
- **`:lyric-flow`**: A precision rendering engine for synchronized lyrics, optimized for low-latency UI updates.
- **`:compose-persist`**: A custom state persistence layer that ensures UI state (like scroll positions and search history) survives process death and navigation.
- **`:compose-routing`**: A type-safe, lightweight navigation library built specifically for this project's MVI-like flow.
- **`:compose-reordering`**: A custom drag-and-drop reordering engine for playlists and queues.
- **`:ktor-client-brotli`**: Specialized networking interceptors for Brotli compression, reducing API overhead by up to 80%.

### 1.2 Android 15 & 16 KB Page Size Alignment
SynapMusic targets `compileSdk = 35` (Android 15) and fully supports Android's 16 KB page size memory architecture.
- **Native Library Alignment**: All embedded native C/C++ libraries (FFmpeg, Zstandard JNI `1.5.7-10`) are compiled with 16 KB segment alignment (`-Wl,-z,max-page-size=16384`).
- **Page-Aligned Packaging**: `useLegacyPackaging = false` stores native `.so` binaries uncompressed and aligned directly inside the APK, allowing zero-copy memory mapping on both 4 KB and 16 KB page size kernel environments.

---

## 2. The SynapPulse Engine

The core of SynapMusic is the **Universal Discovery Engine**. Unlike traditional parsers that rely on fixed JSON paths, SynapPulse uses a recursive tree-walking algorithm.

### 2.1 Universal Tree Walker
Implemented in `SynapPulse.kt`, the `walk` function scans every node in a JSON response for known renderer keys (e.g., `musicShelfRenderer`). This makes the app resilient to YouTube Music's frequent server-side layout changes.

### 2.2 Universal Thumbnail Scraper
If standard thumbnail fields are empty (common in "Radio" and "Mix" items), the engine triggers a **Deep Scraper Pass**. It recursively scans the entire JSON node tree for any valid `thumbnails` array, ensuring every item in the UI has valid artwork.

### 2.3 Accurate Identity Discovery (Header First)
To maintain 100% accuracy in album and playlist metadata, the discovery engine follows a two-stage process:
1.  **Strict Header Extraction**: Before scanning tracks, the engine performs a targeted search for Header renderers (e.g., `musicDetailHeaderRenderer`). It establishes the page's "Primary Identity" (Title and Artwork) exclusively from these headers.
2.  **Exclusion Strategy**: During the general tree-walk for tracks, the engine strictly ignores metadata found inside item renderers to prevent deep-nested song details from "hijacking" the page identity.

### 2.4 Radio & Strict Artist Logic
The `YouTubeRadio` engine has been hardened to prevent "Trending Intrusion":
- **Strict Artist Mode**: In Artist Radios, every track returned by the server is verified against the target artist's name. Unrelated tracks from different artists are automatically filtered out of the queue.
- **Playlist Panel Parser**: A dedicated parser for `playlistPanelVideoRenderer` ensures that Radio queues retain full metadata and high-res thumbnails, matching the quality of standard lists.

### 2.5 Global Search & Discover Filtering Engine
- **Language Query Suffix Resolution**: When a language filter (e.g., Hindi, Japanese, Spanish, Korean) is active, `SearchResultScreen` appends query suffixes (`$query $querySuffix`) directly to YouTube Music API requests (`SearchBody`) and updates the cache key (`tag`).
- **Dynamic Tag Invalidation**: `ItemsPage.kt` observes `tag` changes and clears cached results (`itemsPage = null`), immediately dispatching fresh network queries to YouTube Music on `Dispatchers.IO`.
- **In-Memory Post-Filtering & Sorting**: Applies `isGenuineSong()` (AI song exclusion) and `filterBySelectedLanguage()` (keyword bans and foreign character script detection) to filter search result items, sorting them **By Title** or **By Relevance** in **Ascending** or **Descending** order across all 5 search tabs.

---

## 3. The Vault Pipeline (Offline Storage)

The "Vault" is a robust, local-first persistence system designed for bit-perfect audio.

### 3.1 Smart Contextual Decision Engine
The `CompressionManager` executes a real-time hardware probe before downloading streams:
- **Auto-Quality**: Automatically drops network streams from 320kbps High-Res on Wi-Fi to 128kbps Data-Saver when connected to Cellular Data or 64kbps on Low Battery.
- **Native Stream Pass-Through (Zero Conversion)**: When downloading in **Opus** or **AAC**, SynapMusic saves YouTube Music's native incoming stream directly without transcoding—delivering 100% bit-exact original stream quality in ~0.2s with zero CPU/battery overhead.
- **Hardware Adaptation**: Uses `BatteryManager`, `StatFs`, and `ActivityManager` to assign the best local compression engine:
  - Low Battery (<15%) & Power Save Mode use **Opus** for zero-transcode, instant downloads, 85% storage reduction, and low CPU power drain.
  - Low Storage (<3GB) uses AAC for minimal disk footprint and universal device compatibility.
  - Storage Constrained (<8GB) uses Opus for high-efficiency compression.
  - Charging + High-End Hardware (≥6GB RAM) uses WavPack/SAC for 24-bit/48kHz audiophile fidelity.

### 3.2 Transcoding & Resiliency Flow
1. **Direct Save / Transcoding**: Opus/AAC streams are saved directly. Lossless targets (FLAC/WavPack) are processed via self-contained FFmpeg binaries with dynamically resolved `LD_LIBRARY_PATH` linkages and full ProGuard/R8 JNI retention.
2. **Resilience Fallbacks**: Transcode failure (e.g. Signal 139 on x86_64) triggers a raw stream copy. If that fails, a pure Kotlin direct file copy executes as a final fallback.
3. **True Container Extension Preservation**: During direct file copy fallbacks, `MediaFetchUtils.kt` preserves the downloaded stream's true container extension (`$videoId.${downloadedFile.extension}`) and registers its exact MIME type (`audio/m4a`, `audio/webm`) in Room DB. This ensures ExoPlayer's container extractors (`Mp4Extractor`, `MatroskaExtractor`) parse and play the file without error.

### 3.3 Database Healing & Hardware Locks
Because YouTube Search often returns Music Video IDs while official Album pages use Audio IDs, the database must dynamically self-heal offline mappings:
- **Synthetic ID Healing**: When an album is generated from a downloaded single, it receives a `synthetic_` ID. Opening the album triggers a background search to find the official ID, safely merging the local mapping to the official network tracklist using an atomic commit (`atomicAlbumUpdate`).
- **The Hardware Lock (`clearAlbum`)**: When clearing an album to sync the latest tracklist from the server, the SQL query explicitly enforces `AND songId NOT IN (SELECT songId FROM Format WHERE contentLength > 0)`. This physical "Hardware Lock" forbids the deletion of any tracks you have downloaded, preventing albums from mysteriously disappearing from your offline library if the official API tracklist changes.

### 3.4 Registration & Deletion Integrity
When a file is registered in the Vault, the system follows a **Synchronous atomic update**:
- **Format Table**: Uses `UPSERT` to update file size and codec information.
- **Song Table**: Uses **Preserve-then-Replace** logic to freshen metadata while protecting playback history and "Liked" status.

---

## 4. Extended Network & Utility Services

### 4.1 Spotify & Apple Music Import Engines (`com.synap.music.network` & `com.synap.music.engine`)
- **`SpotifyClient.kt` & `AppleMusicClient.kt`**: Fetches tracklists from public Spotify OAuth Web API endpoints and parses embedded Apple Music JSON-LD metadata.
- **`PlaylistImportEngine.kt` & `AppleMusicImportEngine.kt`**: Converts extracted tracklists, matches audio streams via YouTube Music (`SynapService.searchPage`), and saves the imported playlist into Room DB.

### 4.2 Live Internet Radio Engine (`com.synap.music.network` & `com.synap.music.ui.components.themed`)
- **`RadioBrowserClient.kt`**: Connects to the open-source RadioBrowser REST API (`https://de1.api.radio-browser.info/json/`).
- **`LiveRadioDialog.kt`**: Interactive radio station browser with 16 radio categories (Pop, Rock, Jazz, Classical, News, Dance, Chill, Lofi, 80s, 90s, etc.), high-contrast borders, custom Radio Tower brand icons (`radio_tower.xml`), and real-time visual scrollbars.

### 4.3 WebDAV Network Storage Engine (`com.synap.music.webdav`)
- **`WebDavClient.kt` & `WebDavConfig.kt`**: Executes `PROPFIND` directory listings over OkHttp to crawl remote NAS servers for audio files (`.flac`, `.wav`, `.mp3`, `.m4a`, `.opus`, etc.).
- **`WebDavImportEngine.kt`**: Indexes remote audio files into a local "Network Drive" playlist in Room DB.

### 4.4 Music Recognition Engine (`com.synap.music.recognition`)
- **`AudioRecorder.kt`**: Captures a 5-second 16-bit 44.1kHz PCM/WAV sample from the device's microphone.
- **`MusicRecognitionClient.kt`**: Posts the WAV file to AudD's recognition API (`https://api.audd.io/`), parses the match, and retrieves the matching track from YouTube Music.

### 4.5 Listen Together Engine (`com.synap.music.listentogether`)
- **`ListenTogether.kt` & `ServerClock.kt`**: WebSocket party client that manages room creation, 6-character room codes, member tracking, and relative server time offset calculations.
- **`PartySync.kt`**: Binds room position state (`partyPositionMs`) directly to ExoPlayer in `PlayerService.kt`, keeping playback time synchronized down to the millisecond across all connected devices.

### 4.6 Smart Auto-Playlist Generator (`com.synap.music.engine` & `com.synap.music.ui.screens.smartmixes`)
- **`SmartPlaylistEngine.kt`**: Generates dynamic mood and filter mixes (*Most Played*, *Recently Saved*, *Vault Offline*, *High Energy & Workout*, *Chill & Relax*) from Room DB playback stats.
- **`SmartMixesScreen.kt`**: Full-screen page using SynapMusic's standard Vault `Scaffold` container with Vault-style top bar (`typography.l.bold`, back button `R.drawable.chevron_back`, play icon `R.drawable.play`, save icon `R.drawable.playlist`) and 2-column adaptive tile cards with live marquee text scrolling (`MarqueeText`).

### 4.7 Synap Recap Listening Analytics (`com.synap.music.engine` & `com.synap.music.ui.screens.recap`)
- **`AnalyticsEngine.kt`**: Computes total hours listened, top 5 streamed songs, top 5 artists, and time-of-day habits (*Morning Energy*, *Afternoon Groove*, *Evening Chill*, *Late Night Listener*).
- **`SynapRecapScreen.kt`**: Full-screen analytics dashboard with Vault-style `Scaffold` top bar, stat summary cards, rank badges (#1 Gold, #2 Silver, #3 Bronze), top artists list, and top bar action controls to clear/reset listening analytics statistics and play history via synchronous database transactions (`Database.clearEvents()` & `Database.clearTotalPlayTimeMs()`).

### 4.8 Local Storage Scanner Engine (`com.synap.music.scanner`)
- **`LocalFolderScanner.kt`**: Scans Android's `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` for local audio files (`.mp3`, `.flac`, `.wav`, `.m4a`, `.opus`, `.ogg`).
- **`LocalFolderScanDialog.kt`**: Handles storage permissions (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`) and renders real-time progress counters (`Scanned X local tracks...`).

### 4.9 ID3 Audio Tag & Cover Artwork Editor (`com.synap.music.engine` & `com.synap.music.ui.components.themed`)
- **`TagEditorEngine.kt`**: Updates track title, artist name, and thumbnail artwork in Room DB. Copies custom artwork selected from gallery into local app storage (`artwork/art_{songId}.jpg`) and clears Coil memory/disk cache.
- **`TagEditorDialog.kt`**: Interactive editor with cover art preview, gallery image picker launcher (`ActivityResultContracts.GetContent`), and editable text fields for title and artist name.

### 4.10 Playlist Tiles & Feature Visibility Toggles (`com.synap.music.utils` & `com.synap.music.ui.screens.settings`)
- **Granular Preference Switches**: Exposes user-configurable toggle switches in **Settings -> Other Settings -> PLAYLIST TILES & FEATURES** (`isLocalStorageEnabledKey`, `isSmartMixesEnabledKey`, `isSynapRecapEnabledKey`, `isSpotifyImportEnabledKey`, `isAppleMusicImportEnabledKey`, `isLiveRadioEnabledKey`, `isWebdavEnabledKey`).
- **Dynamic Playlists Grid Rendering**: In `HomePlaylists.kt`, each feature tile card conditionally evaluates its preference key, dynamically inserting or hiding tile items from the Playlists grid in real-time.

### 4.11 The DEN Lair & Dedicated Playlists Navigation (`com.synap.music.ui.screens.playlists`)
- **The DEN Lair**: Renamed the primary home screen Playlists tab to **DEN** with a custom 3D isometric fortress icon (`den.xml`).
- **`SearchMode.DEN` Filtering**: Floating search bar in the DEN tab dispatches `SearchMode.DEN` with hint `"Search in DEN"`, instantly filtering personal playlists, local device storage, smart mixes, recap, live radio, and import portals in real-time.
- **`AllPlaylistsScreen.kt`**: Full-screen page containing user-created and imported playlists, featuring the Vault-style `Scaffold` header, `+` Create Playlist button (`R.drawable.add`), `UniversalFilterButton` (Search Mode, Search here "Playlists", Sort by Name, Date Added, Song Count, Order Ascending/Descending), and SynapCube backup exports.

### 4.12 Context Menus & Visual Scrollbars (`MediaItemMenu.kt` & `Menu.kt`)
- **Visual Scrollbar Rendering**: `MediaItemMenu` and root `Menu` observe vertical scroll state (`scrollState`) to calculate canvas height, scroll max, and thumb offset.
- **Accented Thumb & Track**: Draws a subtle background track (`colorPalette.textDisabled`) and a theme-accented thumb (`colorPalette.accent`) on the right edge whenever menu content overflows, providing visual scroll indicators for long-press menus.

### 4.13 SponsorBlock Non-Music Segment Skipping (`SponsorBlock.kt` & `PlayerService.kt`)
- **`SponsorBlock.kt`**: Client querying the open-source SponsorBlock REST API (`https://sponsor.ajay.app/api/skipSegments`) for skip timestamps across 6 categories: `music_offtopic` (non-music chatter), `intro` & `outro`, `sponsor` & `selfpromo`, and `interaction` ("Like & Subscribe" reminders).
- **Single-Skip UUID Guard (`monitorSegments`)**: `PlayerService`'s `monitorSegments()` polling worker tracks skipped segment UUIDs in `skippedUUIDs` to ensure segments are skipped at most once per playback session.
- **Duration Safety Guard**: Enforces `it.end < (duration - 1500L)` and thread-safe Main-thread context switching (`withContext(Dispatchers.Main)`), preventing SponsorBlock from ever seeking past the end of a track or causing playback crashes.

---

## 5. Audio Engineering & Karaoke Lyrics Engine

### 5.1 Real-Time Animated Karaoke Lyrics Engine (`Lyrics.kt` & `KaraokeLyricsLine.kt`)
- **Parallel Multi-Source Queries**: Queries LRCLIB, SkyLyrics, and YouTube Official Video ID endpoints simultaneously using `coroutineScope { async { ... } }`, bringing network fetch latency down from **8+ seconds to ~250ms**.
- **Real-Time Karaoke Animations**: Active lines scale up smoothly to `1.12x` with `Spring.DampingRatioMediumBouncy` spring physics, glowing with the theme's accent color (`colorPalette.accent`) and `1.0f` alpha.
- **Tap-to-Seek**: Tapping any line in the lyrics view seeks ExoPlayer directly to that timestamp in the song.
- **Google Auto-Translation**: Integrates on-demand Google Translate API (`LyricsTranslation.translateLrc`) into 15 languages, preserving LRC timestamp headers (`[00:12.34]`) so translated lyrics retain full karaoke animation and synchronization.

### 5.2 Dual Equalizer Architecture & Sound Effects Control Panel (`AudioEnhancementManager.kt` & `EqualizerDialog.kt`)
- **In-App Graphic Equalizer & Audio DSP**: Direct control over Android's native hardware `Equalizer`, `BassBoost`, `Virtualizer`, and `PresetReverb` instances attached to `PlayerService`'s audio session ID (`audioSessionId`). Provides interactive 5-band frequency sliders (-15dB to +15dB), hardware Bass Boost (0–100%), 3D Surround Virtualizer (0–100%), and 10 presets (**Flat**, **Bass Boost**, **BoostX**, **ProSoundEQ**, **Rock**, **Pop**, **Jazz**, **Vocal**, **Electronic**, **Custom**).
- **System Equalizer Launch Support**: Resolves and launches native vendor sound effect panels (`ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL` / `Settings.ACTION_SOUND_SETTINGS`) with `FLAG_ACTIVITY_NEW_TASK` and audio session broadcasts (`ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION`) to open Samsung SoundAlive, Xiaomi MIUI Sound, Dolby Atmos, or Wavelet.
- **Mutual Exclusion & Auto-Power Management**: Setting Bass Boost, Virtualizer, or EQ to 0% / disabled explicitly turns off internal DSP units (`enabled = false`) to eliminate processing overhead and prevent double-processing audio distortion with system equalizers.

### 5.3 Smart Sleep Timer (`PlayerService.kt` & `SleepTimerDialog.kt`)
- **Smooth 60s Fade-Out**: Ramps down `player.volume` from `100%` to `0%` during the final 60 seconds before pausing, automatically restoring volume to `100%` on new track starts.
- **End of Current Song Mode**: Waits for the active track to complete before pausing.

### 5.4 Crossfade & Volume Automation (`CrossfadeManager.kt`)
- Attached to `PlayerService.kt`, applies smooth volume automation curves (fade-out at track end, fade-in at track start) based on user-selected crossfade duration (0s to 12s).

---

### 5.5 Spatializer & Audio Enhancements (`AudioEnhancementManager.kt` & `PlayerSettings.kt`)
- **Virtualizer Engine**: Hardware `Virtualizer` integration for headphone spatialization.
- **Dynamic Range Compression (DRC)**: On Android 9+, engages `DynamicsProcessingManager` for multi-band compression.
- **Crossfeed**: Blends left and right channels to reduce fatigue on hard-panned stereo tracks.

### 5.6 System Spatializer Probing & Smart Passthrough (minSdk = 24 Safe)
- **Double-Processing Prevention**: On API 32+ (Android 12L/13+), `AudioEnhancementManager` probes `AudioManager.spatializer`. When native system spatialization is active on connected Bluetooth spatial headphones or Android XR headsets, software virtualizers are automatically bypassed to prevent audio distortion.
- **Smart Multi-Channel Passthrough**: Configures ExoPlayer's `setMaxAudioChannelCount(8)` when Immersive Decoding or system spatialization is active, allowing 5.1, 7.1, and IAMF multi-channel audio beds to pass directly to spatial hardware without forced stereo downmixing.
- **minSdk = 24 Safety**: All system Spatializer queries are guarded by version checks (`SDK_INT >= 32`). Android 7.0–12 devices seamlessly use SynapMusic's built-in software `Virtualizer` soundstage.

### 5.7 Pulsating Color System (`com.synap.music.ui.styling`)
- **`PulsatingTheme.kt`**: `rememberPulsatingHybridColor()` uses Compose's `rememberInfiniteTransition()` to interpolate (`lerp`) across 2, 3, or 4 color stops (primary, secondary, tertiary, quaternary).
- **Intensity & Speed Scaling**: Linear interpolation depth (`lerp`) is scaled by `pulsatingIntensityValueKey` (0% to 100%), allowing full reduction of color transitions down to static colors or subtle shimmers.
- **Dynamic Cover Art Extraction**: When `AlbumArtDynamic` is active, `Palette.from(bitmap)` extracts swatches to calculate primary accent, dark vibrant secondary, and light vibrant tertiary colors directly from cover art.
- **UI Integration**: Mapped across action buttons, player controls (`Controls.kt`), floating search bars (`SearchScreen`, `InListSearchHeader`), dialogs, switches, value selectors, and grid tiles.

### 5.8 Swipe-to-Dismiss Mini Player Engine (`Player.kt`)
- **Horizontal Gesture Tracking**: `collapsedContent` uses `pointerInput` with `detectHorizontalDragGestures` and `Animatable(0f)` offset tracking.
- **Dynamic Opacity Feedback**: As the user drags left or right, `graphicsLayer` smoothly scales the Mini Player alpha (`1f - abs(offsetX) / screenWidth`).
- **Spring Physics & Dismiss Threshold**: Releasing the gesture past the 35% screen width threshold triggers an `animateTo` off-screen slide, stops player/radio playback (`binder.stopRadio()`, `binder.player.stop()`, `binder.player.clearMediaItems()`), and dismisses the Mini Player. Releasing below the threshold smoothly springs the Mini Player back to `0f`.

### 5.9 Unified Loudness & Mastering Pipeline (`AudioEnhancementManager.kt` & `PlayerService.kt`)
- **Coordinated LoudnessEnhancer**: Unifies track-level volume normalization (`baseGainMb = -loudnessDb * 100 + 300`) and LUFS mastering targets (`masteringGainMb`) into a single, coordinated `LoudnessEnhancer` instance on ExoPlayer's audio session ID.
- **Safety Clamping**: Clamps total gain safely (`coerceIn(-1500, 1500)` millibels = -15dB to +15dB) to normalize track peaks and apply genre mastering without digital clipping or hardware audio distortion.

### 5.10 Preferences Engine & Real-Time Reset Architecture (`Preferences.kt` & `MainActivity.kt`)
- **Real-Time Reactive Preferences**: `rememberPreference` composables integrate `DisposableEffect` with Android's `OnSharedPreferenceChangeListener` to observe setting changes in real-time.
- **Atomic Factory Reset**: `clearAllPreferences()` executes disk clearing (`preferences.edit { clear() }`) and in-memory cache eviction (`persistMap?.clear()`). `MainActivity.kt` catches the `key == null` reset event, immediately restoring color palettes, typography, thumbnail shapes, and preferences to factory defaults across all active Compose screens.

---

## 6. SynapCube Framework

`SynapCube` (`.synapcube`) is the native backup and portability format for SynapMusic. It bypasses conventional cloud syncing paradigms in favor of fully decentralized, offline-first library management.

### 6.1 BlackBox Encrypted Containers
- **Zstandard (Zstd) JSON**: Complex database relations are serialized using `kotlinx.serialization` and heavily compressed using Zstd into a `metadata.json.zst` payload.
- **Audio Packaging**: Physical audio files (FLAC, OPUS, M4A) are packed into a ZIP stream with a dynamic Deflate level based on the user's free disk space.
- **Stream Obfuscation**: The entire output stream is piped through `BlackBoxOutputStream`, wrapping the archive in an XOR cipher with the proprietary `SYNAPCUBE_10` magic header. This prevents standard ZIP utilities or Android media scanners from tearing into or indexing the raw audio files.
- **Flexible Scope Export**: The `AudioCubeManager` supports exporting full libraries (`exportLibraryToCube`), single/multiple playlists (`exportPlaylistsToCube`), single tracks (`exportSongToCube`), and multi-selected tracks/albums/artists (`exportSongsToCube`).
- **Interactive UI Integration**: Multi-select mode replaces title text with a compact `SynapCubeTitleButton` pill (`[ N ] 📦 SynapCube ›`). Tapping it opens `SynapCubeExportMenu` for on-the-fly Zstd/Zip compression profile selection and instant export.
- **Atomic Restores**: The `AudioCubeManager` decrypts the stream in memory, decodes the Zstd metadata, and performs multi-table database transactions (`upsert`) to perfectly restore relational mapping and listening stats.

### 6.2 Security & Threat Mitigation
Because `.synapcube` archives can be downloaded from external sources, the parsing engine implements strict mitigations:
- **Zip-Slip Protection**: Verifies that the canonical destination path of extracted audio files remains exclusively inside the `vault` directory, preventing path-traversal attacks designed to overwrite critical internal application databases.
- **Zip Bomb Prevention**: Calculates the `Zstd.decompressedSize` of the metadata payload *before* executing the decompression, enforcing a hard 100MB memory cap to prevent Out-Of-Memory attacks.

---

## 7. Legal & Licensing

**SynapMusic** is licensed under the [GNU General Public License v3.0](LICENSE).

Copyright © 2026 SynapMusic Maintainers
