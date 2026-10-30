package com.synap.music.recognition

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

/**
 * Utility for capturing audio samples from the device microphone using [AudioRecord].
 * Outputs standard 16-bit 44.1kHz PCM mono WAV files.
 */
class AudioRecorder(private val context: Context) {

    /**
     * Records an audio sample for the specified duration and writes it to a temporary WAV file.
     * Emits progress callbacks every second.
     */
    @SuppressLint("MissingPermission")
    suspend fun recordSample(durationSec: Int = 5, onProgress: (Int) -> Unit = {}): File = withContext(Dispatchers.IO) {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

        val outputFile = File(context.cacheDir, "temp_recognition_sample.wav")
        if (outputFile.exists()) outputFile.delete()

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        val pcmData = ByteArray(bufferSize)
        val fos = FileOutputStream(outputFile)

        // Write 44-byte WAV header placeholder
        fos.write(ByteArray(44))

        var totalBytesRead = 0
        val targetBytes = sampleRate * 2 * durationSec // 16-bit = 2 bytes per sample

        try {
            audioRecord.startRecording()
            val startTime = System.currentTimeMillis()

            while (totalBytesRead < targetBytes) {
                val read = audioRecord.read(pcmData, 0, pcmData.size)
                if (read > 0) {
                    fos.write(pcmData, 0, read)
                    totalBytesRead += read
                }
                val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                onProgress(elapsedSec.coerceAtMost(durationSec))
            }
        } finally {
            try {
                audioRecord.stop()
                audioRecord.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            fos.close()
        }

        // Overwrite standard 44-byte WAV header over the placeholder
        writeWavHeader(outputFile, totalBytesRead, sampleRate, 1, 16)

        return@withContext outputFile
    }

    /**
     * Writes a standard 44-byte WAV header for 16-bit PCM audio into the file header.
     */
    private fun writeWavHeader(
        file: File,
        totalAudioLen: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val raf = RandomAccessFile(file, "rw")
        raf.seek(0)

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 16 for PCM format
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // Audio format 1 = PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()

        raf.write(header)
        raf.close()
    }
}
