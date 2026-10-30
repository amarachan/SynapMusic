package com.synap.music.enums

/**
 * Defines the compression profile for exporting `.synapcube` archives.
 * 
 * Configures both the Zstandard (Zstd) compression level for metadata JSON
 * and the ZIP Deflate level for audio payloads.
 * 
 * - [Low]: Fastest export, minimal CPU usage (Zstd level 1, Zip level 0).
 * - [Medium]: Balanced speed and archive size (Zstd level 3, Zip level 0).
 * - [High]: Tighter compression for smaller files (Zstd level 9, Zip level 5).
 * - [Extreme]: Maximum space savings for low-storage environments (Zstd level 19, Zip level 9).
 * - [Auto]: Dynamically selects compression level based on available device storage space.
 */
enum class CubeCompressionLevel {
    Low, Medium, High, Extreme, Auto
}
