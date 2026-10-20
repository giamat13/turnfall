package io.github.giamat13.turnfall.game

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Seeded random numbers, the same as mulberry32 in src/core.js. The same seed gives the same boards on every device,
 * so a level that is generated here is the level the page shows.
 */
class Mulberry32(seed: Int) {
    private var a = seed

    fun next(): Double {
        a += 0x6D2B79F5
        var t = (a xor (a ushr 15)) * (a or 1)
        t = (t + ((t xor (t ushr 7)) * (t or 61))) xor t
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }
}

/**
 * The settings of one generator: the density of walls, mud, pits, arrows and springs, the board size, and the par
 * (generate) or the window of fewest turns [lo]..[hi] (fit). [avoid] holds the boards already used, when given.
 */
class Spec(
    val n: Int,
    val walls: Double,
    val mud: Double,
    val pits: Double,
    val arrows: Double = 0.0,
    val springs: Double = 0.0,
    var par: Int = 0,
    val lo: Int = 0,
    val hi: Int = 0,
    val need: Need = Need.None,
    val tries: Int,
    var rnd: Mulberry32,
    val avoid: MutableMap<String, Boolean>? = null,
)

/** A board, its fewest turns, one optimal solution, and how many tries it took to find it. */
class Level(val n: Int, val rows: List<String>, val par: Int, val moves: String, val tries: Int)

private const val ARROW_CHARS = "v>^<"

/**
 * A random board: walls, then the start and the hole, then mud, pits, arrows and springs on the floor that is left.
 * A spec without arrows or springs draws exactly the same numbers as before. Returns null when the board is too small.
 */
fun randomRows(spec: Spec): List<String>? {
    val n = spec.n
    val rnd = spec.rnd
    val cells = Array(n) { CharArray(n) }
    val floors = ArrayList<Pos>()
    for (y in 0 until n) {
        for (x in 0 until n) {
            if (rnd.next() < spec.walls) {
                cells[y][x] = '#'
            } else {
                cells[y][x] = '.'
                floors.add(Pos(x, y))
            }
        }
    }
    if (floors.size < 8) return null
    fun pick(): Pos = floors[floor(rnd.next() * floors.size).toInt()]
    val s = pick()
    var o = pick()
    var guard = 0
    while (o.x == s.x && o.y == s.y && guard++ < 50) o = pick()
    if (o.x == s.x && o.y == s.y) return null
    cells[s.y][s.x] = 'S'
    cells[o.y][o.x] = 'O'
    for (c in floors) {
        if (cells[c.y][c.x] != '.') continue
        val r = rnd.next()
        when {
            r < spec.pits -> cells[c.y][c.x] = 'p'
            r < spec.pits + spec.mud -> cells[c.y][c.x] = 'm'
            r < spec.pits + spec.mud + spec.arrows -> cells[c.y][c.x] = ARROW_CHARS[floor(rnd.next() * 4).toInt()]
            r < spec.pits + spec.mud + spec.arrows + spec.springs -> cells[c.y][c.x] = 'z'
        }
    }
    return cells.map { String(it) }
}

/** Tries random boards until one fits: exactly spec.par turns, the mechanic needed, and not already in [Spec.avoid]. */
fun generate(spec: Spec): Level? {
    for (tries in 0 until spec.tries) {
        val rows = randomRows(spec) ?: continue
        val key = rows.joinToString("/")
        if (spec.avoid?.get(key) == true) continue
        val moves = evaluate(rows, spec.par)
        if (moves.isNullOrEmpty() || !meets(rows, spec.par, spec.need) || looping(rows)) continue
        spec.avoid?.put(key, true)
        return Level(spec.n, rows, spec.par, moves, tries + 1)
    }
    return null
}

/** A board whose fewest turns lie between spec.lo and spec.hi, the same with and without wasted turns. */
fun fit(spec: Spec): Level? {
    for (tries in 0 until spec.tries) {
        val rows = randomRows(spec) ?: continue
        val board = parseBoard(rows)
        if (board.start == null) continue
        val free = solve(board, strict = false, maxDepth = spec.hi) ?: continue
        if (free.par < spec.lo) continue
        val strict = solve(board, strict = true, maxDepth = spec.hi) ?: continue
        if (strict.par != free.par) continue
        if (!meets(rows, free.par, spec.need) || looping(rows)) continue
        return Level(spec.n, rows, free.par, strict.moves, tries + 1)
    }
    return null
}

/**
 * Level [index] from ENDLESS_FROM on, so the campaign never ends. Each level is found from its own seed, so a given
 * level is the same board for everyone. Chapters of six: the board grows, the fewest turns rise, and arrows and springs
 * become more common chapter by chapter.
 */
fun endlessLevel(index: Int): Level? {
    val j = index - ENDLESS_FROM
    val t = j / 6
    val k = j % 6
    val n = if (t < 4) 7 else if (t < 12) 8 else 9
    val base = min(12, 8 + t / 3)
    val lo = min(12, base + if (k >= 3) 1 else 0)
    val seed = 20261020L + j.toLong() * 7919L
    val arrows = min(0.1, 0.03 + t * 0.01)
    val springs = min(0.09, 0.02 + t * 0.008)
    for (attempt in 0 until 3) {
        val found = fit(
            Spec(
                n, 0.2, 0.1, 0.08, arrows, springs,
                lo = lo, hi = lo + 1, need = Need.AnyMechanic, tries = 20000,
                rnd = Mulberry32((seed + attempt * 1000003L).toInt()),
            ),
        )
        if (found != null) return found
    }
    // Not found in three tries (very rare): a wider window, then a small board that always fits.
    return fit(
        Spec(
            n, 0.2, 0.1, 0.08, arrows, springs,
            lo = max(3, lo - 1), hi = lo + 3, need = Need.AnyMechanic, tries = 200000,
            rnd = Mulberry32((seed + 77L).toInt()),
        ),
    ) ?: fit(
        Spec(
            6, 0.2, 0.1, 0.08,
            lo = 3, hi = 8, need = Need.AnyMechanic, tries = 200000,
            rnd = Mulberry32((seed + 99L).toInt()),
        ),
    )
}

/** The seed of a day's puzzle: year * 10000 + month * 100 + day. [dateKey] is 'YYYY-MM-DD'. */
fun dailySeed(dateKey: String): Int {
    val p = dateKey.split("-")
    return p[0].toInt() * 10000 + p[1].toInt() * 100 + p[2].toInt()
}

/** The daily board: the same for everyone on a given UTC date. */
fun dailyLevel(dateKey: String): Level? {
    val seed = dailySeed(dateKey)
    val spec = Spec(
        6, 0.2, 0.1, 0.08,
        par = 4 + seed % 3, need = Need.AnyMechanic, tries = 200000,
        rnd = Mulberry32(seed),
    )
    var level = generate(spec)
    if (level == null) {
        spec.par = 4
        spec.rnd = Mulberry32(seed + 1)
        level = generate(spec)
    }
    return level
}
