package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * High-performance audio player for Gemini native audio and natural TTS playback.
 * Provides instant stopping for voice barge-in and interruptions,
 * and auto-encapsulates raw PCM data with RIFF WAV headers.
 */
class MahiAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val lock = Any()
    private var isCurrentlyPlaying = false

    fun isPlaying(): Boolean = synchronized(lock) { isCurrentlyPlaying }

    /**
     * Plays audio bytes (WAV, MP3, or raw PCM).
     * Automatically wraps raw PCM with a standard 16-bit 24kHz mono WAV header.
     */
    suspend fun playAudio(
        audioBytes: ByteArray,
        mimeType: String? = null,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        stop()

        if (audioBytes.isEmpty()) {
            withContext(Dispatchers.Main) { onDone() }
            return@withContext
        }

        try {
            val processedBytes = prepareAudioPayload(audioBytes, mimeType)
            val tempFile = File.createTempFile("mahi_voice_", ".wav", context.cacheDir)
            tempFile.deleteOnExit()

            FileOutputStream(tempFile).use { fos ->
                fos.write(processedBytes)
                fos.flush()
            }

            withContext(Dispatchers.Main) {
                synchronized(lock) {
                    try {
                        mediaPlayer = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                                    .build()
                            )
                            setDataSource(tempFile.absolutePath)
                            setOnPreparedListener { mp ->
                                synchronized(lock) {
                                    isCurrentlyPlaying = true
                                }
                                onStart()
                                mp.start()
                            }
                            setOnCompletionListener {
                                cleanupPlayer()
                                tempFile.delete()
                                onDone()
                            }
                            setOnErrorListener { _, what, extra ->
                                Log.w("MahiAudioPlayer", "Playback error: what=$what, extra=$extra")
                                cleanupPlayer()
                                tempFile.delete()
                                onDone()
                                true
                            }
                            prepareAsync()
                        }
                    } catch (e: Exception) {
                        Log.e("MahiAudioPlayer", "Error preparing MediaPlayer", e)
                        cleanupPlayer()
                        tempFile.delete()
                        onDone()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MahiAudioPlayer", "Error saving temp audio file", e)
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    /**
     * Immediately stops playback for voice barge-in or manual interruption.
     */
    fun stop() {
        synchronized(lock) {
            try {
                if (mediaPlayer != null) {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.stop()
                    }
                    mediaPlayer?.reset()
                    mediaPlayer?.release()
                    mediaPlayer = null
                }
            } catch (e: Exception) {
                Log.w("MahiAudioPlayer", "Exception stopping MediaPlayer", e)
            } finally {
                isCurrentlyPlaying = false
            }
        }
    }

    private fun cleanupPlayer() {
        synchronized(lock) {
            try {
                mediaPlayer?.reset()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (_: Exception) {}
            isCurrentlyPlaying = false
        }
    }

    fun release() {
        stop()
    }

    /**
     * Checks if audio contains RIFF or ID3/MP3 header.
     * If raw PCM, prepends standard 44-byte WAV header.
     */
    private fun prepareAudioPayload(data: ByteArray, mimeType: String?): ByteArray {
        // If data is already WAV or MP3
        if (data.size >= 4) {
            val isRiff = data[0] == 'R'.code.toByte() && data[1] == 'I'.code.toByte() &&
                    data[2] == 'F'.code.toByte() && data[3] == 'F'.code.toByte()
            val isMp3 = (data[0] == 'I'.code.toByte() && data[1] == 'D'.code.toByte() && data[2] == '3'.code.toByte()) ||
                    (data[0] == 0xFF.toByte() && (data[1].toInt() and 0xE0) == 0xE0)

            if (isRiff || isMp3) {
                return data
            }
        }

        // Parse sample rate if specified in mimeType, e.g. "audio/pcm;rate=24000"
        var sampleRate = 24000
        if (mimeType != null && mimeType.contains("rate=")) {
            val parts = mimeType.split("rate=")
            if (parts.size > 1) {
                val rateStr = parts[1].takeWhile { it.isDigit() }
                rateStr.toIntOrNull()?.let { sampleRate = it }
            }
        }

        return wrapPcmWithWavHeader(data, sampleRate = sampleRate, channels = 1, bitDepth = 16)
    }

    private fun wrapPcmWithWavHeader(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int = 1,
        bitDepth: Int = 16
    ): ByteArray {
        val totalAudioLen = pcmData.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitDepth / 8).toLong()
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xffL).toByte()
        header[5] = ((totalDataLen shr 8) and 0xffL).toByte()
        header[6] = ((totalDataLen shr 16) and 0xffL).toByte()
        header[7] = ((totalDataLen shr 24) and 0xffL).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xffL).toByte()
        header[29] = ((byteRate shr 8) and 0xffL).toByte()
        header[30] = ((byteRate shr 16) and 0xffL).toByte()
        header[31] = ((byteRate shr 24) and 0xffL).toByte()
        header[32] = (channels * bitDepth / 8).toByte()
        header[33] = 0
        header[34] = bitDepth.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xffL).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xffL).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xffL).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xffL).toByte()

        val wavPayload = ByteArray(44 + pcmData.size)
        System.arraycopy(header, 0, wavPayload, 0, 44)
        System.arraycopy(pcmData, 0, wavPayload, 44, pcmData.size)
        return wavPayload
    }
}
