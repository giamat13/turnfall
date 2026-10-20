package io.github.giamat13.turnfall.game

import java.time.LocalDate
import java.util.Locale

/** The mechanics a chapter introduces. The page shows them as small pictures, not as sentences. */
enum class Tag { WALL, MUD, PIT, ARROW, SPRING }

/** A chapter of six levels. The first nine are named and show their mechanics; later ones are only numbered. */
object Chapters {
    class Info(val name: String, val tags: List<Tag>)

    /** Levels per chapter. */
    const val SIZE = 6

    /** Stars that one chapter can give: three per level. */
    const val MAX_STARS_PER_CHAPTER = 18

    val designed: List<Info> = listOf(
        Info("Falling", listOf(Tag.WALL)),
        Info("Mud", listOf(Tag.MUD)),
        Info("Pits", listOf(Tag.PIT)),
        Info("Mixed", listOf(Tag.MUD, Tag.PIT)),
        Info("Long Route", listOf(Tag.WALL, Tag.MUD, Tag.PIT)),
        Info("Whole World", listOf(Tag.MUD, Tag.PIT, Tag.ARROW, Tag.SPRING)),
        Info("Arrows", listOf(Tag.ARROW)),
        Info("Springs", listOf(Tag.SPRING)),
        Info("All Together", listOf(Tag.MUD, Tag.PIT, Tag.ARROW, Tag.SPRING)),
    )

    private val allTags = listOf(Tag.MUD, Tag.PIT, Tag.ARROW, Tag.SPRING)

    fun of(levelIndex: Int): Int = levelIndex / SIZE

    fun info(chapter: Int): Info =
        if (chapter < designed.size) designed[chapter] else Info("Chapter ${chapter + 1}", allTags)

    fun title(chapter: Int): String =
        if (chapter < designed.size) "Chapter ${chapter + 1}: ${designed[chapter].name}" else "Chapter ${chapter + 1}"
}

/** Stars as text: '★★☆' for two stars. */
fun starText(stars: Int): String = "★".repeat(stars) + "☆".repeat(3 - stars)

/** '1 turn' or '3 turns'. */
fun turnWord(turns: Int): String = "$turns " + if (turns == 1) "turn" else "turns"

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** The day as the page's key: 'YYYY-MM-DD'. The day is the UTC date, so everyone has the same puzzle at the same moment. */
fun dateKey(date: LocalDate): String =
    String.format(Locale.ROOT, "%04d-%02d-%02d", date.year, date.monthValue, date.dayOfMonth)

/** The short label of a daily puzzle, like 'Oct 20'. */
fun dayLabel(key: String): String {
    val p = key.split("-")
    return MONTHS[p[1].toInt() - 1] + " " + p[2].toInt()
}
