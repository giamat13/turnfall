package io.github.giamat13.turnfall.feedback

import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect

/**
 * The short buzz of the page: one number is one buzz of that many milliseconds, a list is buzz, pause, buzz, and so on.
 * Nothing happens unless the [enabled] check (the Vibration setting) says so.
 */
class Buzzer(context: Context, private val enabled: () -> Boolean) {
    private val vibrator = context.getSystemService(Vibrator::class.java)

    fun buzz(vararg millis: Long) {
        if (!enabled() || vibrator == null || !vibrator.hasVibrator()) return
        if (millis.size == 1) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis[0], VibrationEffect.DEFAULT_AMPLITUDE))
            return
        }
        // Timings start with a silent wait, then alternate: on for millis[0], off for millis[1], and so on.
        val timings = LongArray(millis.size + 1)
        val amplitudes = IntArray(millis.size + 1)
        for (i in millis.indices) {
            timings[i + 1] = millis[i]
            amplitudes[i + 1] = if (i % 2 == 0) VibrationEffect.DEFAULT_AMPLITUDE else 0
        }
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }
}
