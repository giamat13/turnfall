package io.github.giamat13.turnfall.game

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The page's rules, checked on all 54 designed levels against src/core.js. */
class RulesTest {
    private val designed = fixture("designed.json").jsonArray

    @Test
    fun designedLevelsHaveTheSameStartsSolutionsAndPars() {
        for (entry in designed) {
            val o = entry.jsonObject
            val index = o.int("index")
            val rows = o.rows()
            val board = parseBoard(rows)
            val startFixture = o.getValue("start")
            val expectedStart = if (startFixture.isNull()) null else startFixture.asInts()
            assertEquals("start of level $index", expectedStart, board.start?.let { listOf(it.x, it.y) })
            assertSameFound(o.getValue("strict").asFound(), board.start?.let { solve(board, strict = true, maxDepth = 16) }, "strict solve of level $index")
            assertSameFound(o.getValue("free").asFound(), board.start?.let { solve(board, strict = false, maxDepth = 16) }, "free solve of level $index")
            assertEquals("par of level $index", o.int("parOf"), parOf(rows))
            val without = o.getValue("parWithout").jsonObject
            assertEquals("par without mud, level $index", without.int("mud"), parOf(rows, Mechanics(mud = false)))
            assertEquals("par without pits, level $index", without.int("pits"), parOf(rows, Mechanics(pits = false)))
            assertEquals("par without arrows, level $index", without.int("arrows"), parOf(rows, Mechanics(arrows = false)))
            assertEquals("par without springs, level $index", without.int("springs"), parOf(rows, Mechanics(springs = false)))
        }
    }

    @Test
    fun evaluateAndLoopingMatchThePage() {
        for (entry in designed) {
            val o = entry.jsonObject
            val index = o.int("index")
            val rows = o.rows()
            val expected = o.getValue("evaluate")
            val actual = evaluate(rows, o.int("par"))
            if (expected.isNull()) {
                assertNull("evaluate of level $index", actual)
            } else {
                assertEquals("evaluate of level $index", expected.jsonPrimitive.content, actual)
            }
            assertEquals("looping of level $index", o.getValue("looping").jsonPrimitive.boolean, looping(rows))
        }
    }

    @Test
    fun hintsFollowTheSamePlayAsThePage() {
        for (entry in designed) {
            val o = entry.jsonObject
            val index = o.int("index")
            val board = parseBoard(o.rows())
            val start = board.start ?: continue
            val hints = o.getValue("hints").jsonObject
            val expectedSeq = hints.getValue("seq").jsonArray.map { it.jsonPrimitive.content.single() }
            val (seq, end) = playHints(board, start)
            assertEquals("hint turns of level $index", expectedSeq, seq)
            assertEquals("hint end of level $index", hints.getValue("end").jsonPrimitive.content, end)
        }
    }

    @Test
    fun everySlideMatchesThePage() {
        var checked = 0
        for (entry in designed) {
            val o = entry.jsonObject
            val board = parseBoard(o.rows())
            for (item in o.getValue("slides").jsonArray) {
                val a = item.jsonArray
                val x = a[0].jsonPrimitive.int
                val y = a[1].jsonPrimitive.int
                val g = a[2].jsonPrimitive.int
                val r = slide(board, x, y, g, keepPath = true)
                val label = "slide from ($x,$y) gravity $g on level ${o.int("index")}"
                assertEquals("$label: x", a[3].jsonPrimitive.int, r.x)
                assertEquals("$label: y", a[4].jsonPrimitive.int, r.y)
                assertEquals("$label: end", a[5].jsonPrimitive.content, r.end.name.lowercase())
                assertEquals("$label: moved", a[6].jsonPrimitive.int == 1, r.moved)
                assertEquals("$label: loop", a[7].jsonPrimitive.int == 1, r.loop)
                val length = a[8].jsonPrimitive.int
                assertEquals("$label: path length", length, r.path.size)
                for (i in 0 until length) {
                    assertEquals("$label: path x at $i", a[9 + 2 * i].jsonPrimitive.int, r.path[i].x)
                    assertEquals("$label: path y at $i", a[10 + 2 * i].jsonPrimitive.int, r.path[i].y)
                }
                checked++
            }
        }
        assertEquals("number of slides checked", designed.sumOf { it.jsonObject.getValue("slides").jsonArray.size }, checked)
    }

    /** The page's hint loop: each hint is played as a turn, until the hole, a pit, or no hint is left. */
    private fun playHints(board: Board, start: Pos): Pair<List<Char>, String> {
        val s0 = slide(board, start.x, start.y, 0, keepPath = false)
        var x = s0.x
        var y = s0.y
        var g = 0
        val seq = ArrayList<Char>()
        var end = "open"
        for (step in 0 until 40) {
            val d = hint(board, x, y, g)
            if (d == null) {
                end = "none"
                break
            }
            seq.add(d)
            g = (g + if (d == 'R') 1 else 3) % 4
            val r = slide(board, x, y, g, keepPath = false)
            if (!r.moved) continue
            x = r.x
            y = r.y
            if (r.end == End.WIN || r.end == End.PIT) {
                end = r.end.name.lowercase()
                break
            }
        }
        return seq to end
    }
}
