<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="128" />
  <h1>🎶 SynapMusic</h1>
  <p><b>An open-source Android music player for offline storage & YouTube Music streaming.</b></p>

  <p>SynapMusic combines local music playback, YouTube Music streaming, offline Vault downloads, custom audio DSP, real-time lyrics translation, WebDAV cloud drives, and instant library backups—with individual settings to turn off any feature you don't need.</p>

  <p>
    <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android" />
    <img src="https://img.shields.io/badge/Language-Kotlin-orange?style=for-the-badge&logo=kotlin" />
    <a href="https://github.com/amarachan"><img src="https://img.shields.io/badge/Developer-amarachan-blue?style=for-the-badge&logo=github" /></a>
    <img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge" />
  </p>

  <p>
    <a href="#-features">✨ Features</a> •
    <a href="#-privacy--principles-non-features">🔒 Privacy</a> •
    <a href="#-adaptive-codec-selection">⚡ Codecs</a> •
    <a href="#-tech-stack">🛠️ Tech Stack</a> •
    <a href="#-legal-disclaimer">⚖️ Legal</a>
  </p>
</div>

---

## ✨ Features

### 🎶 Full-Screen Gesture Player
Full-screen gesture player bottom sheet featuring dynamic album artwork recoloring, playback controls, and queue drawer.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/music%20playing.png" width="180" alt="Full Player Main" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/music%20playing%202.png" width="180" alt="Full Player Alt" />
</p>

---

### 🎨 Dynamic Album Cover Palette Extraction
Theme engine extracts HSL swatches directly from the currently playing track's artwork (`Palette.from(bitmap)`), mathematically calculating primary accent, dark vibrant secondary, and light vibrant background tints across the UI.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/dynamic%20theme%20and%20coloring.png" width="180" alt="Dynamic Palette 1" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/dynamic%20theme%20and%20coloring%202.png" width="180" alt="Dynamic Palette 2" />
</p>

---

### 🎨 Appearance & Typography Settings
* **Color Themes**: Select from Dynamic Album Art (recolors the UI using current cover art), Dark, Pure Black AMOLED, Light, or custom color presets (Nord, Dracula, Catppuccin, Tokyo Night, Emerald Forest, Cyberpunk, Sunset OLED).
* **Pulsating Color Effects**: Animated color transitions with customizable speed (Chill, Natural, Energetic) and intensity (Gentle, Balanced, Vibrant). Can be enabled or disabled separately for action buttons, DEN tiles, and Discover cards.
* **Fonts**: Default `Poppins` font, with options to switch to your device's native system font and toggle font padding.
* **Thumbnail Roundness**: Set image corner roundness across lists and grids to None, Light, Medium, or Heavy.
* **System Bars**: Automatic status bar and navigation bar icon color matching.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/theme%20settings.png" width="180" alt="Theme Settings" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/theme%20settings%20font.png" width="180" alt="Font Settings" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/theme%20settings%20thumbnail%20roundness.png" width="180" alt="Thumbnail Roundness" />
</p>

---

### 🦁 The DEN Lair & Storage Scanner
Personal audio fortress tab featuring custom Playlists, Local Storage, Smart Mixes, Synap Recap, Live Radio, and Import Portals.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/den.png" width="180" alt="The DEN Lair" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/local%20storage.png" width="180" alt="Local Storage Scanner" />
</p>

---

### ⭐ Star Collection & Offline Vault
Access your liked tracks and offline Vault downloads with support for both 2-column **Grid View** and high-density **List View**.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/star%20collection.png" width="180" alt="Star Collection" />
</p>

---

### 💿 Studio Albums & Artist Discographies
Browse studio tracklists, official album pages, and complete artist discographies.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/album%20section.png" width="180" alt="Album Section" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/artists%20section.png" width="180" alt="Artist Section" />
</p>

---

### 🏷️ ID3 Audio Tag & Cover Art Editor
Edit track title, artist name, album title, and select custom album cover artwork from your phone's photo gallery with real-time Coil image cache eviction.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/edit%20audio%20tag,%20cover%20art,%20artist%20name.png" width="180" alt="ID3 Tag & Cover Art Editor" />
</p>

---

### 📦 SynapCube Backups (.synapcube)
Export and restore multiple playlists, tracks, or your entire library as portable `.synapcube` archives compressed with Zstandard JNI and protected by XOR stream obfuscation.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/memory%20settings%20back%20restore%20audio%20cubes.png" width="180" alt="SynapCube Backup & Restore" />
</p>

---

### 🧹 Memory, Cache & Database Settings
Granular controls for Coil image cache size, song/lyrics cache eviction, and Vault database healing/optimization in **Settings -> Memory Settings**.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/memory%20settings%20image%20song%20cache%20offline%20vault%20database%20discovery.png" width="180" alt="Memory Cache & Vault DB" />
</p>

---

### 🎛️ Equalizer, Mastering & Audio DSP
In-app 5-band interactive frequency sliders, hardware Bass Boost (0–100%), 3D Surround Virtualizer (0–100%), Loudness Target, Headphone Crossfeed, and Immersive Decoding.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/audio%20settings.png" width="180" alt="Graphic Equalizer & DSP" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/audio%20settings%20mastering%20and%20immersive.png" width="180" alt="Mastering & Immersive DSP" />
</p>

---

### 🔍 Discover Feed & Filters
Dynamic Discover feed with granular filters for Language, Genre, Decade, and Exclude AI Songs toggle.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/discover.png" width="180" alt="Discover Feed" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/discover%20filter.png" width="180" alt="Discover Filters" />
</p>

---

### 🌐 Language Discovery Filters
Filter discovery feed by regional languages (Hindi, English, Japanese, Spanish, Korean, Tamil, Telugu, etc.).

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/languages.png" width="180" alt="Language Filter 1" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/languages1.png" width="180" alt="Language Filter 2" />
</p>

---

### 🎵 Genre & Year Discovery Filters
Filter discovery feed by music genres, release decade, or year.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/genre.png" width="180" alt="Genre Filter" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/year,%20era.png" width="180" alt="Year Era Filter" />
</p>

---

### 🔎 Interactive Search Bar & Modes
In-header editable search bar for modifying terms on-the-fly, dynamic Language query suffix resolution, and search modes.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/search.png" width="180" alt="Search Screen" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/search%20modes.png" width="180" alt="Search Modes" />
</p>

---

### 🎵 Song & Album Search Results
Tabbed search results with instant preview for songs, studio albums, and singles.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/search%20results%20songs.png" width="180" alt="Search Results Songs" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/search%20results%20album.png" width="180" alt="Search Results Albums" />
</p>

---

### 👤 Artist Search & Discography Navigation
Search artist profiles, browse full discographies, or navigate directly to artist pages.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/search%20results%20artist%20page.png" width="180" alt="Search Results Artists" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/search%20results%20artist%20page%202.png" width="180" alt="Artist Details" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/search%20results%20go%20to%20artist.png" width="180" alt="Go To Artist" />
</p>

---

### 📻 Live Internet Radio Browser
Search and stream tens of thousands of live global FM/AM and online web radio stations via RadioBrowser API.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/radio.png" width="180" alt="Live Radio" />
</p>

---

### 🎤 Real-Time Karaoke Lyrics Engine
Multi-provider synced lyrics (LRCLIB, SkyLyrics, YouTube Captions) featuring spring animations, accent glow, tap-to-seek, and Google Auto-Translation into 15 languages.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/song%20lyrics.png" width="180" alt="Animated Karaoke Lyrics" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/lyrics%20menu.png" width="180" alt="Lyrics Translation Menu" />
</p>

---

### 🎙️ Music Recognition ("Shazam" Mode)
Identify real-world ambient music playing around you using AudD and automatically match & play it on YouTube Music.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/music%20detection%20by%20listening.png" width="180" alt="Music Recognition" />
</p>

---

### ⚙️ Feature Visibility Switches
Enable or disable feature tiles (*Local Storage*, *Smart Mixes*, *Synap Recap*, *Spotify Import*, *Apple Music Import*, *Live Radio*, *WebDAV*) in **Settings -> Other Settings**.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/other%20settings%20playlist%20tiles%20%26%20features,%20local%20device%20storage.png" width="180" alt="Playlist Tiles & Feature Switches" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/other%20settings%20smart%20auto-playlists%20generator,%20live%20internet%20radio,%20listen%20together,%20spotify%20%26%20apple%20music%20import.png" width="180" alt="Smart Mixes & Imports" />
</p>

---

### 🌐 Integrations & System Reset Settings
Manage Shazam/AudD music recognition, WebDAV NAS storage, Discord Rich Presence, Android Auto, search history, and factory settings reset.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/other%20settings%20music%20recognition,%20network%20storage,%20discord%20rich%20presence,%20android%20auto.png" width="180" alt="Integrations Settings" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/other%20settings%20search%20history,%20content%20%26%20lyrics,%20service%20lifetime,%20reset%20all%20settings.png" width="180" alt="Search History & Reset" />
</p>

---

### 🚫 SponsorBlock Segment Skipping
Integrates SponsorBlock (`https://sponsor.ajay.app`) to automatically skip non-music chatter (`music_offtopic`), video intros, outros, sponsorships, and subscribe reminders.

---

## 🔒 Privacy & Principles (Non-Features)

* 🔒 **No Google Login**: No account or sign-in needed.
* 🛡️ **No Tracking / Ads**: Zero telemetry or ads.
* 💚 **No Subscriptions**: 100% free and open-source.

---

## ⚡ Adaptive Codec Selection

The `CompressionManager` selects the codec based on your device status:

| Priority | Trigger | Codec Choice |
| :--- | :--- | :--- |
| **Endurance** | Battery < 15% | **Opus** *(Fastest zero-transcode download & 85% data/storage saving)* |
| **Efficiency** | Storage < 8GB | **Opus** / **AAC** *(Minimal file size footprint)* |
| **Performance** | High-End Hardware & Charging | **SAC** / **L3AC** / **SQZ** *(Audiophile WavPack / Lossless)* |
| **Streaming** | Mobile Data | **Opus** *(Extreme bandwidth efficiency)* |

### Integrated Formats & Performance Matrix

| Format | Compression | Quality | Storage Saving | Battery Usage |
| :--- | :---: | :---: | :---: | :---: |
| **Opus** (`.opus`) | Lossy | High (92k–510k) | Extreme | Excellent |
| **AAC** (`.m4a`) | Lossy | Standard (192k–256k) | High | Excellent |
| **FLAC** (`.flac`) | Lossless | Perfect Bit-Exact | Moderate | Best |
| **WavPack** (`.wv` / `.mkv`) | Lossless / Hybrid | Perfect Bit-Exact | High | Good |
| **L3AC** (`.mkv`) | Lossless Hi-Res | Audiophile (24-bit/48kHz) | High | Good |
| **SAC** (`.mkv`) | Lossless Super Audio | Audiophile (24-bit/48kHz) | High | Good |
| **SLAC** (`.mkv`) | Lossless Super Lite | Perfect Bit-Exact | High | Good |
| **SQZ** (`.mkv`) | Lossless Compressed | Perfect Bit-Exact | High | Good |

---

## 📦 Build Variants & APK Optimization

* **Original Unsplit Footprint**: `136 MB`
* **Optimized Release Size**: **`29 MB`** (ARM64 Split, ~79% footprint reduction)

### Available Download Variants (GitHub Releases)

| File Name | Purpose | Recommended For | Size | ABIs |
| :--- | :--- | :--- | :---: | :--- |
| **`SynapMusic-app-arm64-v8a-release.apk`** | Modern Android Phones | Most Modern Devices | `29 MB` | `arm64-v8a` |
| **`SynapMusic-app-armeabi-v7a-release.apk`** | Older Phones / Android TVs | Legacy Devices & TVs | `27 MB` | `armeabi-v7a` |
| **`SynapMusic-app-x86_64-release.apk`** | Intel Chromebooks & Emulators | 64-bit Intel Devices | `30 MB` | `x86_64` |
| **`SynapMusic-app-x86-release.apk`** | 32-bit Intel Emulators | Legacy Intel Devices | `29 MB` | `x86` |
| **`SynapMusic-app-universal-release.apk`** | Single File for All Devices | Universal Sideloading | `89 MB` | All 4 ABIs |

> **Note**: Download pre-built APKs directly from the [**GitHub Releases Page**](https://github.com/amarachan/SynapMusic/releases).

---

## 🛠️ Tech Stack

* **UI**: Jetpack Compose (Poppins typography, dynamic album artwork HSL palette extraction).
* **Audio Engine**: AndroidX Media3 ExoPlayer with native Android `AudioEffect` DSP (`Equalizer`, `BassBoost`, `Virtualizer`, `LoudnessEnhancer`, `DynamicsProcessingManager`).
* **C/C++ Native Processing**: FFmpeg binaries and yt-dlp stream extraction (16 KB page-aligned for Android 15+).
* **Database**: Room DB (`Database.kt`) storing tracks, albums, artists, playlists, and listening stats.
* **Networking**: Ktor HTTP client (`ktor-client-okhttp`) with Brotli decompression.
* **Image Pipeline**: Coil WebP pipeline with 512MB disk cache.
* **APIs**: SponsorBlock, LRCLIB, SkyLyrics, Google Translate, MusicBrainz, TheAudioDB, AudD, RadioBrowser.
* **Background Jobs**: WorkManager (`OptimizationWorker` & `MaintenanceHealer`) for daily library optimization and Vault healing.

---

## 📥 Download & Installation

1. Go to the [**GitHub Releases Page**](https://github.com/amarachan/SynapMusic/releases).
2. Download the latest `.apk` file (Choose **`SynapMusic-app-arm64-v8a-release.apk`** for modern phones, or **`SynapMusic-app-universal-release.apk`** if unsure).
3. Enable "Install from Unknown Sources" on your Android device if prompted.
4. Install and enjoy!

---

## 🤝 Contributing

We welcome contributions! If you want to fix bugs or add features, check out our [**Contributing Guidelines**](CONTRIBUTING.md).

---

## ⚖️ Legal Disclaimer

### 🚫 Non-Affiliation
SynapMusic is **NOT** affiliated with or endorsed by **Google LLC**, **YouTube**, or **YouTube Music**.

### 🏷️ Trademarks
"YouTube" and "YouTube Music" are registered trademarks of Google LLC, used here solely for descriptive purposes.

### ⚖️ Fair Use
Provided for educational and personal use under fair use principles. Does not circumvent DRM.

**SynapMusic** is licensed under the [GNU General Public License v3.0](LICENSE).

Copyright © 2026 SynapMusic Maintainers & amarachan (ACVG)
