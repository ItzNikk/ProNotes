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
import android.view.HapticFeedbackConstants
import android.view.View
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * "Writing on paper". While a tool moves, a texture sound plays that follows its speed:
 * pencil scratch for pens, a smoother felt glide for the highlighter, a low rub for the eraser.
 * Short one-off sounds mark page turns, snaps, selections and taps. Haptic ticks are spaced by
 * distance like paper grain. Everything is synthesised on the fly; no audio files ship.
 */
class PaperFeel(context: Context, private val view: View) {
    enum class Texture { PENCIL, MARKER, ERASER }
    enum class Cue { PAGE, SNAP, SELECT, TAP, ADD, UNDO, LIFT }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)

    private val rate = 22050
    @Volatile private var running = false
    @Volatile private var target = 0f
    @Volatile private var texture = Texture.PENCIL
    @Volatile private var lastActive = 0L
    /** One-off sounds waiting to be mixed in. */
    private val cues = java.util.concurrent.ConcurrentLinkedQueue<ShortArray>()

    var sound = false
    var haptics = false
    var volume = 0.5f

    private var travelled = 0f
    private var lastTick = 0L
    private var tickEvery = 26f

    fun start(kind: Texture) {
        texture = kind
        travelled = 0f
        tickEvery = when (kind) { Texture.PENCIL -> 24f; Texture.MARKER -> 40f; Texture.ERASER -> 18f }
        lastActive = SystemClock.uptimeMillis()
        if (sound) ensureAudio()
    }

    /** The tool moved [distPx] screen pixels in [dtMs]. */
    fun move(distPx: Float, dtMs: Long) {
        val now = SystemClock.uptimeMillis()
        lastActive = now
        if (sound) {
            if (!running) ensureAudio()
            val speed = distPx / dtMs.coerceAtLeast(1)
            target = min(1f, speed / if (texture == Texture.ERASER) 0.9f else 2.0f)
        }
        if (haptics) {
            travelled += distPx
            if (travelled > tickEvery && now - lastTick > 24) {
                travelled = 0f; lastTick = now
                tick(if (texture == Texture.ERASER) 0.55f else 0.4f)
            }
        }
    }

    fun stop() { target = 0f }

    /** A short sound and buzz for an event. */
    fun cue(c: Cue) {
        if (haptics) when (c) {
            Cue.PAGE -> tick(0.5f)
            Cue.SNAP, Cue.ADD -> click()
            Cue.SELECT -> tick(0.7f)
            Cue.TAP -> tick(0.3f)
            Cue.UNDO -> tick(0.45f)
            Cue.LIFT -> Unit
        }
        if (!sound) return
        cues.add(synth(c))
        lastActive = SystemClock.uptimeMillis()
        ensureAudio()
    }

    private fun tick(strength: Float) {
        val v = vibrator
        val ok = v != null && v.hasVibrator() && runCatching {
            when {
                Build.VERSION.SDK_INT >= 30 && v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK) ->
                    v.vibrate(VibrationEffect.startComposition().addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, strength).compose())
                Build.VERSION.SDK_INT >= 29 -> v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                else -> v.vibrate(VibrationEffect.createOneShot(6, (60 * strength).toInt().coerceIn(1, 255)))
            }
        }.isSuccess
        // Some tablets ignore the vibrator API for short pulses; the view haptic still works there.
        if (!ok) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun click() {
        val v = vibrator
        val ok = v != null && v.hasVibrator() && runCatching {
            if (Build.VERSION.SDK_INT >= 29) v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            else v.vibrate(VibrationEffect.createOneShot(12, 120))
        }.isSuccess
        if (!ok) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun synth(c: Cue): ShortArray {
        val rnd = Random(c.ordinal + 3)
        return when (c) {
            Cue.PAGE -> { // paper swish: noise sweeping up, then down
                val n = (rate * 0.32f).toInt()
                var lp = 0f
                ShortArray(n) { i ->
                    val t = i / n.toFloat()
                    val env = sin(PI * t).toFloat().let { it * it }
                    lp += (rnd.nextFloat() * 2 - 1 - lp) * (0.08f + 0.5f * t)
                    (lp * env * 0.55f * Short.MAX_VALUE).toInt().toShort()
                }
            }
            Cue.SNAP, Cue.ADD -> { // soft wooden click
                val n = (rate * 0.06f).toInt()
                val f = if (c == Cue.SNAP) 1400.0 else 900.0
                ShortArray(n) { i ->
                    val t = i / rate.toDouble()
                    (sin(2 * PI * f * t) * exp(-t * 90) * 0.5 * Short.MAX_VALUE).toInt().toShort()
                }
            }
            Cue.SELECT -> { // two-note pop
                val n = (rate * 0.09f).toInt()
                ShortArray(n) { i ->
                    val t = i / rate.toDouble()
                    val f = if (t < 0.04) 880.0 else 1320.0
                    (sin(2 * PI * f * t) * exp(-t * 40) * 0.35 * Short.MAX_VALUE).toInt().toShort()
                }
            }
            Cue.UNDO -> { // quick reverse swoosh, falling pitch
                val n = (rate * 0.12f).toInt()
                ShortArray(n) { i ->
                    val t = i / rate.toDouble()
                    (sin(2 * PI * (900.0 - 3000.0 * t) * t) * exp(-t * 30) * 0.3 * Short.MAX_VALUE).toInt().toShort()
                }
            }
            Cue.LIFT -> { // tiny tap as the pen leaves the paper
                val n = (rate * 0.015f).toInt()
                var lpf = 0f
                ShortArray(n) { i -> lpf += ((rnd.nextFloat() * 2 - 1) - lpf) * 0.3f; (lpf * exp(-i / (n * 0.2)) * 0.25 * Short.MAX_VALUE).toInt().toShort() }
            }
            Cue.TAP -> {
                val n = (rate * 0.025f).toInt()
                ShortArray(n) { i -> ((rnd.nextFloat() * 2 - 1) * exp(-i / (n * 0.25)) * 0.35 * Short.MAX_VALUE).toInt().toShort() }
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
        running = true
        t.play()
        Thread({ loop(t) }, "paper-sound").apply { priority = Thread.MAX_PRIORITY - 1; start() }
    }

    private fun loop(t: AudioTrack) {
        val rnd = Random(7)
        val block = ShortArray(256)
        var level = 0f
        var lp = 0f; var lp2 = 0f; var prev = 0f
        var grain = 0f
        var squeak = 0f
        var cue: ShortArray? = null
        var cuePos = 0
        while (running) {
            if (cue == null) { cue = cues.poll(); cuePos = 0 }
            if (target == 0f && level < 0.001f && cue == null && SystemClock.uptimeMillis() - lastActive > 1200) break
            val tex = texture
            for (i in block.indices) {
                // Quick attack, softer release: no clicks when the pen lands or lifts.
                level += (target - level) * (if (target > level) 0.012f else 0.0035f)
                val n = rnd.nextFloat() * 2f - 1f
                val s = when (tex) {
                    Texture.PENCIL -> {
                        // Graphite on paper: faster strokes sound brighter (the filter opens with speed),
                        // a low body gives weight, and random fibre clicks add grit.
                        val open = 0.18f + 0.45f * level
                        lp += (n - lp) * open
                        val hp = lp - prev; prev = lp
                        lp2 += (n - lp2) * 0.04f
                        if (rnd.nextFloat() < 0.0018f + 0.004f * level) grain = 0.6f + rnd.nextFloat() * 0.6f
                        grain *= 0.94f
                        hp * 0.85f + lp2 * 0.5f + hp * grain * 2.8f
                    }
                    Texture.MARKER -> {
                        // Felt tip: smooth, breathy glide with a faint squeak that rises with speed.
                        lp += (n - lp) * 0.12f
                        lp2 += (lp - lp2) * 0.3f
                        squeak += 0.0045f + 0.012f * level
                        (lp - lp2) * 1.7f + sin(squeak * 6.283f * 40f) * 0.05f * level
                    }
                    Texture.ERASER -> { // low rubbing with a slow wobble
                        lp += (n - lp) * 0.05f
                        // Rubber on paper: low rumble plus a little grit, clearly audible.
                        if (rnd.nextFloat() < 0.004f) grain = 1f
                        grain *= 0.93f
                        lp * (0.8f + 0.2f * sin(i * 0.05f)) * 4.2f + n * grain * 0.35f
                    }
                }
                var mixed = s * level * volume * 0.55f
                val c = cue
                if (c != null) {
                    mixed += c[cuePos] / Short.MAX_VALUE.toFloat() * volume
                    cuePos++
                    if (cuePos >= c.size) { cue = cues.poll(); cuePos = 0 }
                }
                block[i] = (mixed.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
            if (t.write(block, 0, block.size) < 0) break
        }
        runCatching { t.stop(); t.release() }
        running = false
    }

    fun release() { running = false; target = 0f; cues.clear() }
}
