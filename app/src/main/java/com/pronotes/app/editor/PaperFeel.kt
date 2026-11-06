package com.pronotes.app.editor

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * "Writing on paper": a soft pencil-on-paper scratch while the pen moves, louder when you write
 * faster, plus tiny haptic ticks spaced by distance like paper grain. The sound is made on the
 * fly from filtered noise, so no audio files ship with the app.
 */
class PaperFeel(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)

    private val rate = 22050
    private var track: AudioTrack? = null
    private var thread: Thread? = null
    @Volatile private var running = false
    /** Target loudness 0..1, set from pen speed. */
    @Volatile private var target = 0f
    @Volatile private var lastMove = 0L

    var sound = false
    var haptics = false
    /** 0..1 */
    var volume = 0.5f

    private var travelled = 0f
    private var lastTick = 0L

    /** Pen went down. */
    fun start() {
        travelled = 0f
        if (sound) ensureAudio()
    }

    /** Pen moved [distPx] screen pixels in [dtMs]. */
    fun move(distPx: Float, dtMs: Long) {
        val now = SystemClock.uptimeMillis()
        lastMove = now
        if (sound) {
            val speed = distPx / dtMs.coerceAtLeast(1)          // px per ms
            target = min(1f, speed / 2.2f)
        }
        if (haptics) {
            travelled += distPx
            // A grain tick every ~26 px of travel, never faster than every 28 ms.
            if (travelled > 26f && now - lastTick > 28) {
                travelled = 0f; lastTick = now
                tick()
            }
        }
    }

    /** Pen lifted. */
    fun stop() { target = 0f }

    private fun tick() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching {
            when {
                Build.VERSION.SDK_INT >= 30 && v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK) ->
                    v.vibrate(VibrationEffect.startComposition().addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.35f).compose())
                Build.VERSION.SDK_INT >= 29 -> v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                else -> v.vibrate(VibrationEffect.createOneShot(4, 20))
            }
        }
    }

    private fun ensureAudio() {
        if (running) return
        val buf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(2048)
        val t = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(buf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return
        track = t
        running = true
        t.play()
        thread = Thread({ loop(t) }, "paper-sound").apply { priority = Thread.MAX_PRIORITY - 1; start() }
    }

    private fun loop(t: AudioTrack) {
        val rnd = Random(7)
        val block = ShortArray(256)
        var level = 0f
        var lp = 0f; var hp = 0f; var prev = 0f
        var grain = 0f
        while (running) {
            // Fade out and park the audio after a second of silence, to save battery.
            if (target == 0f && level < 0.001f && SystemClock.uptimeMillis() - lastMove > 1000) break
            for (i in block.indices) {
                level += (target - level) * 0.004f
                val n = rnd.nextFloat() * 2f - 1f
                // Band-pass noise (paper hiss) with random grain bursts (fibres).
                lp += (n - lp) * 0.35f
                hp = lp - prev; prev = lp
                if (rnd.nextFloat() < 0.0025f) grain = 1f
                grain *= 0.96f
                val s = (hp * 0.9f + hp * grain * 2.5f) * level * volume * 0.55f
                block[i] = (s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            if (t.write(block, 0, block.size) < 0) break
        }
        runCatching { t.stop(); t.release() }
        running = false
        track = null
    }

    fun release() { running = false; target = 0f }
}
