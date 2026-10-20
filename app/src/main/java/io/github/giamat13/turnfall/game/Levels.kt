package io.github.giamat13.turnfall.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A designed level as stored in data/levels.json. [sol] is its optimal turns, which the unit tests check. */
@Serializable
data class DesignedLevel(
    val n: Int,
    val rows: List<String>,
    val par: Int,
    val sol: String = "",
    val chapter: Int = 0,
)

/**
 * The levels the player sees, by index. The first [ENDLESS_FROM] are designed; the rest are made from their index the
 * first time they are asked for, and kept. Daily puzzles are made from their date and kept too.
 */
class LevelBank(designed: List<DesignedLevel>) {
    private val designedLevels = designed.map { Level(it.n, it.rows, it.par, it.sol, tries = 0) }
    private val endless = HashMap<Int, Level>()
    private val daily = HashMap<String, Level>()

    fun levelAt(index: Int): Level {
        if (index < designedLevels.size) return designedLevels[index]
        return endless.getOrPut(index) { endlessLevel(index) ?: designedLevels[index % designedLevels.size] }
    }

    fun dailyFor(dateKey: String): Level = daily.getOrPut(dateKey) { checkNotNull(dailyLevel(dateKey)) }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Reads the designed levels from the text of data/levels.json. */
        fun fromJson(text: String): LevelBank = LevelBank(json.decodeFromString<List<DesignedLevel>>(text))
    }
}
