package io.github.giamat13.turnfall.data

import io.github.giamat13.turnfall.game.Chapters
import io.github.giamat13.turnfall.game.dateKey
import io.github.giamat13.turnfall.game.dayLabel
import io.github.giamat13.turnfall.game.stars
import io.github.giamat13.turnfall.game.starText
import io.github.giamat13.turnfall.game.turnWord
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The progress rules of the page: stars, the best result, the chapters, the streak and the labels. */
class ProgressTest {
    @Test
    fun starsFollowParAsOnThePage() {
        assertEquals(3, stars(turns = 4, par = 4))
        assertEquals(2, stars(turns = 6, par = 4))
        assertEquals(1, stars(turns = 7, par = 4))
        assertEquals("★★☆", starText(2))
        assertEquals("☆☆☆", starText(0))
        assertEquals("1 turn", turnWord(1))
        assertEquals("0 turns", turnWord(0))
    }

    @Test
    fun aBetterResultReplacesTheSavedOne() {
        val first = Progress().withLevelResult(level = 3, stars = 2, turns = 6)
        assertEquals(Result(2, 6), first.best["3"])
        // Same stars in fewer turns is better.
        assertEquals(Result(2, 5), first.withLevelResult(3, 2, 5).best["3"])
        // Same stars in more turns is kept as it was.
        assertEquals(Result(2, 5), first.withLevelResult(3, 2, 5).withLevelResult(3, 2, 9).best["3"])
        // More stars always wins.
        assertEquals(Result(3, 9), first.withLevelResult(3, 3, 9).best["3"])
    }

    @Test
    fun dailyResultsAreKeptByDate() {
        val progress = Progress().withDailyResult("2026-10-20", stars = 3, turns = 4)
        assertEquals(Result(3, 4), progress.daily["2026-10-20"])
        assertTrue(progress.best.isEmpty())
    }

    @Test
    fun chaptersOpenAsFarAsTheFirstUnfinishedLevel() {
        var progress = Progress()
        assertEquals(0, progress.maxChapter(currentLevel = 0))
        for (i in 0 until 6) progress = progress.withLevelResult(i, 1, 9)
        assertTrue(progress.isChapterDone(0))
        assertFalse(progress.isChapterDone(1))
        assertEquals(1, progress.maxChapter(currentLevel = 0))
        // A level played further on opens its chapter too.
        assertEquals(3, progress.withLevelResult(20, 1, 9).maxChapter(currentLevel = 0))
    }

    @Test
    fun statsCountSolvedStarsAndTheStreak() {
        val today = LocalDate.of(2026, 10, 20)
        val progress = Progress(
            best = mapOf("0" to Result(3, 4), "1" to Result(2, 6), "2" to Result(3, 5)),
            daily = mapOf(
                "2026-10-18" to Result(1, 9),
                "2026-10-19" to Result(2, 5),
                "2026-10-20" to Result(3, 4),
            ),
        )
        val stats = progress.stats(today)
        assertEquals(3, stats.solved)
        assertEquals(2, stats.threeStars)
        assertEquals(8, stats.stars)
        assertEquals(3, stats.dailyPuzzles)
        assertEquals(3, stats.streak)
    }

    @Test
    fun streakCountsFromYesterdayUntilTodayIsPlayed() {
        val today = LocalDate.of(2026, 10, 20)
        val progress = Progress(daily = mapOf("2026-10-19" to Result(2, 5), "2026-10-18" to Result(1, 9)))
        assertEquals(2, progress.stats(today).streak)
    }

    @Test
    fun dateKeysAndLabelsUseTheUtcCalendarDay() {
        assertEquals("2026-01-05", dateKey(LocalDate.of(2026, 1, 5)))
        assertEquals("Oct 20", dayLabel("2026-10-20"))
        assertEquals("Feb 28", dayLabel("2026-02-28"))
    }

    @Test
    fun chapterTitlesAndTagsMatchThePage() {
        assertEquals("Chapter 1: Falling", Chapters.title(0))
        assertEquals("Chapter 10", Chapters.title(9))
        assertEquals("Chapter 10", Chapters.info(9).name)
        assertEquals(4, Chapters.info(5).tags.size)
        assertEquals(1, Chapters.of(6))
    }
}
