package io.github.giamat13.turnfall.game

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** The random numbers, the endless levels and the daily puzzles, checked against src/core.js. */
class GeneratorTest {
    @Test
    fun seededRandomNumbersMatchThePage() {
        for (entry in fixture("prng.json").jsonArray) {
            val o = entry.jsonObject
            // The page's `a |= 0` keeps the low 32 bits of the seed, so the seed wraps the same way.
            val seed = o.getValue("seed").jsonPrimitive.content.toDouble().toLong().toInt()
            val rnd = Mulberry32(seed)
            val values = o.getValue("values").jsonArray.map { it.jsonPrimitive.content.toDouble() }
            for ((i, expected) in values.withIndex()) {
                assertEquals("random number $i for seed $seed", expected, rnd.next(), 0.0)
            }
        }
    }

    @Test
    fun endlessLevelsMatchThePage() {
        for (entry in fixture("endless.json").jsonArray) {
            val o = entry.jsonObject
            val index = o.getValue("index").jsonPrimitive.content.toInt()
            val actual = endlessLevel(index)
            if (o.containsKey("missing")) {
                assertNull("level $index is not found by the page", actual)
                continue
            }
            assertNotNull("level $index", actual)
            actual!!
            assertEquals("size of level $index", o.getValue("n").jsonPrimitive.content.toInt(), actual.n)
            assertEquals("rows of level $index", o.rows(), actual.rows)
            assertEquals("par of level $index", o.getValue("par").jsonPrimitive.content.toInt(), actual.par)
            assertEquals("solution of level $index", o.getValue("moves").jsonPrimitive.content, actual.moves)
            assertEquals("tries of level $index", o.getValue("tries").jsonPrimitive.content.toInt(), actual.tries)
        }
    }

    @Test
    fun dailyPuzzlesMatchThePage() {
        for (entry in fixture("daily.json").jsonArray) {
            val o = entry.jsonObject
            val key = o.getValue("key").jsonPrimitive.content
            assertEquals("seed of $key", o.getValue("seed").jsonPrimitive.content.toInt(), dailySeed(key))
            val actual = dailyLevel(key)
            assertNotNull("daily puzzle $key", actual)
            actual!!
            assertEquals("rows of $key", o.rows(), actual.rows)
            assertEquals("par of $key", o.getValue("par").jsonPrimitive.content.toInt(), actual.par)
            assertEquals("solution of $key", o.getValue("moves").jsonPrimitive.content, actual.moves)
        }
    }

    @Test
    fun designedSolutionsAreTheOptimalTurns() {
        val levels = fixture("designed.json").jsonArray
        for (entry in levels) {
            val o = entry.jsonObject
            val index = o.getValue("index").jsonPrimitive.content.toInt()
            val rows = o.rows()
            val par = o.getValue("par").jsonPrimitive.content.toInt()
            assertEquals("evaluated solution of level $index", o.getValue("sol").jsonPrimitive.content, evaluate(rows, par))
        }
    }
}
