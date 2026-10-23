package com.example.data.remote

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/** Foreground push-to-talk only. Caller handles runtime permission; audio is held in RAM and never logged/saved. */
class PhonePcmRecorder {
    private val stopped = AtomicBoolean(false)
    private val cancelled = AtomicBoolean(false)
    fun stop() { stopped.set(true) }
    fun cancel() { cancelled.set(true); stopped.set(true) }

    @SuppressLint("MissingPermission")
    suspend fun record(): ByteArray = withContext(Dispatchers.IO) {
        check(!cancelled.get())
        var recorder: AudioRecord? = null
        for (rate in listOf(16000, 48000, 44100)) {
            currentCoroutineContext().ensureActive()
            val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (min <= 0) continue
            val candidate = runCatching { AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(maxOf(min, rate / 5 * 2)).build() }.getOrNull() ?: continue
            if (candidate.state == AudioRecord.STATE_INITIALIZED && candidate.sampleRate in 16000..192000) { recorder = candidate; break }
            candidate.release()
        }
        val mic = recorder ?: error("Phone cannot initialize mono PCM16 capture")
        val samples = ShortArray(mic.sampleRate * 4)
        val chunk = ShortArray(1024)
        var count = 0
        try {
            if (stopped.get() || cancelled.get()) error("Recording cancelled before start")
            mic.startRecording()
            check(mic.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone did not start" }
            val start = SystemClock.elapsedRealtime()
            while (!stopped.get() && count < samples.size && SystemClock.elapsedRealtime() - start < 4200) {
                currentCoroutineContext().ensureActive()
                val n = mic.read(chunk, 0, minOf(chunk.size, samples.size - count), AudioRecord.READ_NON_BLOCKING)
                check(n >= 0) { "Microphone capture failed ($n)" }
                if (n == 0) { kotlinx.coroutines.delay(10); continue }
                chunk.copyInto(samples, count, 0, n); count += n
            }
            check(!cancelled.get()) { "Recording cancelled" }
            GatewayPcm.encode(samples, count, mic.sampleRate)
        } finally {
            runCatching { mic.stop() }; mic.release(); samples.fill(0); chunk.fill(0)
        }
    }
}
