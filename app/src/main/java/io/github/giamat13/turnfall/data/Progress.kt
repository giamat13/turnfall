package io.github.giamat13.turnfall.data

import io.github.giamat13.turnfall.game.Chapters
import io.github.giamat13.turnfall.game.dateKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** One result: the best stars [s] and the turns [t] of that best. */
@Serializable
data class Result(val s: Int, val t: Int)

/**
 * What this device remembers: the best result of each campaign level (keyed by its index) and of each daily puzzle
 * (keyed by its date). The stored JSON has the same shape as the page's localStorage entry.
 */
@Serializable
data class Progress(
    val best: Map<String, Result> = emptyMap(),
    val daily: Map<String, Result> = emptyMap(),
) {
    /** True when all six levels of [chapter] have a result. */
    fun isChapterDone(chapter: Int): Boolean =
        (0 until Chapters.SIZE).all { best.containsKey((chapter * Chapters.SIZE + it).toString()) }

    /**
     * The furthest chapter the player may open: the one holding the first unfinished level, or the furthest level
     * played, whichever is further on.
     */
    fun maxChapter(currentLevel: Int): Int {
        var first = 0
        while (best.containsKey(first.toString())) first++
        var far = currentLevel
        for (key in best.keys) far = maxOf(far, key.toInt())
        return Chapters.of(maxOf(first, far))
    }

    /** A level win. The result is kept when it beats the saved one. */
    fun withLevelResult(level: Int, stars: Int, turns: Int): Progress =
        copy(best = keepBetter(best, level.toString(), stars, turns))

    /** A daily win. The result is kept when it beats the saved one. */
    fun withDailyResult(day: String, stars: Int, turns: Int): Progress =
        copy(daily = keepBetter(daily, day, stars, turns))

    /** The figures of the settings page. A streak counts the days in a row with a daily puzzle, up to [today]. */
    fun stats(today: LocalDate): Stats {
        val starSum = best.values.sumOf { it.s }
        val threeStars = best.values.count { it.s == 3 }
        var day = today
        if (!daily.containsKey(dateKey(day))) day = day.minusDays(1)
        var streak = 0
        while (daily.containsKey(dateKey(day))) {
            streak++
            day = day.minusDays(1)
        }
        return Stats(
            solved = best.size,
            threeStars = threeStars,
            stars = starSum,
            dailyPuzzles = daily.size,
            streak = streak,
        )
    }

    private fun keepBetter(table: Map<String, Result>, key: String, stars: Int, turns: Int): Map<String, Result> {
        val old = table[key]
        if (old != null && !(stars > old.s || (stars == old.s && turns < old.t))) return table
        return table + (key to Result(stars, turns))
    }
}

/** The numbers on the settings page. */
data class Stats(
    val solved: Int,
    val threeStars: Int,
    val stars: Int,
    val dailyPuzzles: Int,
    val streak: Int,
)

/** How fast the world turns, as in the page: normal, fast (half the time), or off (no animation). */
@Serializable
enum class Speed(val factor: Float) {
    @SerialName("normal") NORMAL(1f),
    @SerialName("fast") FAST(0.5f),
    @SerialName("off") OFF(0f),
}

/** The colour scheme: follow the system, or set light or dark. */
@Serializable
enum class ThemeChoice {
    @SerialName("auto") AUTO,
    @SerialName("light") LIGHT,
    @SerialName("dark") DARK,
}

/** The settings of the page: speed, sound, vibration (the "Vibration" choice) and the colour scheme. */
@Serializable
data class Settings(
    val speed: Speed = Speed.NORMAL,
    val sound: Boolean = false,
    val vibe: Boolean = true,
    val theme: ThemeChoice = ThemeChoice.AUTO,
)
