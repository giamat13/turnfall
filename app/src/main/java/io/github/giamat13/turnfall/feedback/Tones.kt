package io.github.giamat13.turnfall.feedback

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

/** The four shapes of the page's tones. */
enum class Wave { SINE, TRIANGLE, SQUARE, SAWTOOTH }

/**
 * One short tone, as the page makes it: a [wave] at [freq] Hz, [dur] seconds long, that starts after [delay] seconds
 * and fades from [vol] to almost nothing over its length.
 */
class Tone(
    val freq: Double,
    val dur: Double,
    val wave: Wave,
    val vol: Double,
    val delay: Double = 0.0,
)

/**
 * Makes the page's sounds. Each call builds the tones into one short buffer and plays it. Nothing plays unless the
 * [enabled] check says the Sound setting is on.
 */
class Tones(private val enabled: () -> Boolean) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun play(vararg tones: Tone) {
        if (!enabled()) return
        scope.launch {
            val samples = synthesize(tones.toList())
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
            try {
                track.write(samples, 0, samples.size)
                track.play()
                delay(samples.size * 1000L / SAMPLE_RATE + 50)
            } finally {
                track.release()
            }
        }
    }

    private fun synthesize(tones: List<Tone>): ShortArray {
        val length = tones.maxOf { ((it.delay + it.dur + 0.02) * SAMPLE_RATE).toInt() }
        val mix = DoubleArray(length)
        for (tone in tones) {
            val start = (tone.delay * SAMPLE_RATE).toInt()
            val count = ((tone.dur + 0.02) * SAMPLE_RATE).toInt()
            for (i in 0 until count) {
                val t = i / SAMPLE_RATE.toDouble()
                // The page fades the gain exponentially from vol to 0.0001 over dur seconds, then stops.
                val gain = tone.vol * (0.0001 / tone.vol).pow(minOf(t / tone.dur, 1.0))
                mix[start + i] += gain * wave(tone.wave, tone.freq * t)
            }
        }
        return ShortArray(length) { (mix[it].coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort() }
    }

    private fun wave(shape: Wave, cycles: Double): Double = when (shape) {
        Wave.SINE -> sin(2 * PI * cycles)
        Wave.TRIANGLE -> 2 / PI * asin(sin(2 * PI * cycles))
        Wave.SQUARE -> if (sin(2 * PI * cycles) >= 0) 1.0 else -1.0
        Wave.SAWTOOTH -> 2 * (cycles - floor(cycles)) - 1
    }

    private companion object {
        const val SAMPLE_RATE = 44100
    }
}
